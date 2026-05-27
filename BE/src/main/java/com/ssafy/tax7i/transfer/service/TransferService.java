package com.ssafy.tax7i.transfer.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.banking.client.SsafyFinanceClient;
import com.ssafy.tax7i.banking.client.dto.SsafyTransferResult;
import com.ssafy.tax7i.banking.client.dto.SsafyWithdrawResponse;
import com.ssafy.tax7i.card.entity.Card;
import com.ssafy.tax7i.card.repository.CardRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.transfer.dto.P2pTransferRequest;
import com.ssafy.tax7i.transfer.dto.TransferResponse;
import com.ssafy.tax7i.transfer.dto.WithdrawRequest;
import com.ssafy.tax7i.transfer.entity.Transfer;
import com.ssafy.tax7i.transfer.entity.TransferType;
import com.ssafy.tax7i.transfer.repository.TransferRepository;
import com.ssafy.tax7i.bookentry.event.TransferReceivedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransferService {

    private final TransferRepository transferRepository;
    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final SsafyFinanceClient ssafyFinanceClient;
    private final TransferFailureSaveService transferFailureSaveService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public TransferResponse p2pTransfer(Long userId, P2pTransferRequest request) {
        if (userId.equals(request.receiverUserId())) {
            throw new BusinessException(ErrorCode.SELF_TRANSFER_NOT_ALLOWED);
        }

        User sender = getUser(userId);
        Card senderCard = cardRepository.findByIdAndUser_IdAndDeletedFalse(request.senderCardId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        User receiver = userRepository.findById(request.receiverUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "수신자를 찾을 수 없습니다."));
        Card receiverCard = cardRepository.findByUser_IdAndIsDefaultTrueAndDeletedFalse(receiver.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND, "수신자의 기본 카드가 설정되지 않았습니다."));

        String senderKey = getUserKey(sender);
        String receiverKey = getUserKey(receiver);

        String description = request.description() != null ? request.description() : "P2P 송금";

        SsafyTransferResult result;
        try {
            result = ssafyFinanceClient.transfer(
                    senderKey, senderCard.getWithdrawalAccountNo(),
                    receiverKey, receiverCard.getWithdrawalAccountNo(),
                    request.amount(), description, description);
        } catch (BusinessException e) {
            // REQUIRES_NEW 트랜잭션으로 실패 기록 저장 — 외부 트랜잭션 롤백과 무관하게 커밋됨
            transferFailureSaveService.saveFailedTransfer(
                    sender, receiver, senderCard, receiverCard,
                    request.amount(), description, e.getMessage());
            throw e;
        }

        Transfer transfer = Transfer.builder()
                .senderUser(sender)
                .receiverUser(receiver)
                .senderCard(senderCard)
                .receiverCard(receiverCard)
                .transferType(TransferType.P2P)
                .amount(request.amount())
                .description(description)
                .build();
        transfer.assignSsafyTransactionUniqueNo(result.withdrawResponse().rec().transactionUniqueNo());

        transferRepository.save(transfer);

        // 수신자가 사업자인 경우에만 매출 장부 자동 생성 (개인 간 송금은 매출이 아님)
        if (Boolean.TRUE.equals(receiver.getIsBusiness())) {
            eventPublisher.publishEvent(new TransferReceivedEvent(
                    transfer.getId(),
                    receiver.getId(),
                    request.amount(),
                    sender.getName(),
                    description,
                    java.time.LocalDateTime.now()
            ));
        }

        return TransferResponse.from(transfer);
    }

    @Transactional
    public TransferResponse withdraw(Long userId, WithdrawRequest request) {
        User user = getUser(userId);
        Card card = cardRepository.findByIdAndUser_IdAndDeletedFalse(request.cardId(), userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        String userKey = getUserKey(user);
        String description = request.description() != null ? request.description() : "출금";

        SsafyWithdrawResponse withdrawResponse = ssafyFinanceClient.withdraw(
                userKey, card.getWithdrawalAccountNo(), request.amount(), description);

        Transfer transfer = Transfer.builder()
                .senderUser(user)
                .senderCard(card)
                .transferType(TransferType.WITHDRAW)
                .amount(request.amount())
                .description(description)
                .targetAccountNo(request.targetAccountNo())
                .build();
        transfer.assignSsafyTransactionUniqueNo(withdrawResponse.rec().transactionUniqueNo());

        transferRepository.save(transfer);
        return TransferResponse.from(transfer);
    }

    public Page<TransferResponse> getTransfers(Long userId, Pageable pageable) {
        return transferRepository.findByUserId(userId, pageable)
                .map(TransferResponse::from);
    }

    public TransferResponse getTransfer(Long userId, Long transferId) {
        Transfer transfer = transferRepository.findByIdAndUserId(transferId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRANSFER_NOT_FOUND));
        return TransferResponse.from(transfer);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private String getUserKey(User user) {
        if (user.getSsafyUserKey() == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "SSAFY 금융망 사용자 키가 등록되지 않았습니다.");
        }
        return user.getSsafyUserKey();
    }
}
