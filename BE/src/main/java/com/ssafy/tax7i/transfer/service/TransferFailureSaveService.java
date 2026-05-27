package com.ssafy.tax7i.transfer.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.card.entity.Card;
import com.ssafy.tax7i.transfer.entity.Transfer;
import com.ssafy.tax7i.transfer.entity.TransferType;
import com.ssafy.tax7i.transfer.repository.TransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferFailureSaveService {

    private final TransferRepository transferRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailedTransfer(User sender, User receiver,
                                   Card senderCard, Card receiverCard,
                                   Long amount, String description, String failureReason) {
        Transfer failedTransfer = Transfer.builder()
                .senderUser(sender)
                .receiverUser(receiver)
                .senderCard(senderCard)
                .receiverCard(receiverCard)
                .transferType(TransferType.P2P)
                .amount(amount)
                .description(description)
                .build();
        failedTransfer.fail(failureReason);
        transferRepository.save(failedTransfer);
        log.error("P2P 송금 실패 기록 저장: transferId={}, senderId={}, amount={}, reason={}",
                failedTransfer.getId(), sender.getId(), amount, failureReason);
    }
}
