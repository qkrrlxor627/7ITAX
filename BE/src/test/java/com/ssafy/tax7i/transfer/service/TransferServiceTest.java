package com.ssafy.tax7i.transfer.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.banking.client.SsafyFinanceClient;
import com.ssafy.tax7i.banking.client.dto.*;
import com.ssafy.tax7i.card.entity.Card;
import com.ssafy.tax7i.card.entity.CardType;
import com.ssafy.tax7i.card.repository.CardRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.transfer.dto.P2pTransferRequest;
import com.ssafy.tax7i.transfer.dto.TransferResponse;
import com.ssafy.tax7i.transfer.dto.WithdrawRequest;
import com.ssafy.tax7i.transfer.entity.Transfer;
import com.ssafy.tax7i.transfer.entity.TransferStatus;
import com.ssafy.tax7i.transfer.entity.TransferType;
import com.ssafy.tax7i.transfer.repository.TransferRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock private TransferRepository transferRepository;
    @Mock private UserRepository userRepository;
    @Mock private CardRepository cardRepository;
    @Mock private SsafyFinanceClient ssafyFinanceClient;
    @Mock private TransferFailureSaveService transferFailureSaveService;
    @Mock private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TransferService transferService;

    // ───────────── p2pTransfer ─────────────

    @Test
    void p2pTransfer_성공() {
        User sender = createUser(1L, "sender-key");
        Card senderCard = createCard(1L, sender, "1111111111");
        User receiver = createUser(2L, "receiver-key");
        Card receiverCard = createCard(2L, receiver, "2222222222");

        given(userRepository.findById(1L)).willReturn(Optional.of(sender));
        given(cardRepository.findByIdAndUser_IdAndDeletedFalse(1L, 1L)).willReturn(Optional.of(senderCard));
        given(userRepository.findById(2L)).willReturn(Optional.of(receiver));
        given(cardRepository.findByUser_IdAndIsDefaultTrueAndDeletedFalse(2L)).willReturn(Optional.of(receiverCard));

        SsafyTransferResult transferResult = new SsafyTransferResult(
                new SsafyWithdrawResponse(successHeader(),
                        new SsafyWithdrawResponse.WithdrawRec("TX001", "1111111111", "20260320", "10000", "490000")),
                new SsafyDepositResponse(successHeader(),
                        new SsafyDepositResponse.DepositRec("TX002", "2222222222", "20260320", "10000", "510000"))
        );
        given(ssafyFinanceClient.transfer(
                eq("sender-key"), eq("1111111111"),
                eq("receiver-key"), eq("2222222222"),
                eq(10000L), any(String.class), any(String.class)))
                .willReturn(transferResult);

        given(transferRepository.save(any(Transfer.class))).willAnswer(invocation -> {
            Transfer t = invocation.getArgument(0);
            setField(t, "id", 1L);
            return t;
        });

        P2pTransferRequest request = new P2pTransferRequest(1L, 2L, 10000L, "테스트 송금");

        TransferResponse response = transferService.p2pTransfer(1L, request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.transferType()).isEqualTo(TransferType.P2P);
        assertThat(response.senderUserId()).isEqualTo(1L);
        assertThat(response.receiverUserId()).isEqualTo(2L);
        assertThat(response.amount()).isEqualTo(10000L);
        assertThat(response.status()).isEqualTo(TransferStatus.COMPLETED);
    }

    @Test
    void p2pTransfer_자기송금_예외() {
        P2pTransferRequest request = new P2pTransferRequest(1L, 1L, 10000L, "자기 송금");

        assertThatThrownBy(() -> transferService.p2pTransfer(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SELF_TRANSFER_NOT_ALLOWED));
    }

    @Test
    void p2pTransfer_카드없음_예외() {
        User sender = createUser(1L, "sender-key");

        given(userRepository.findById(1L)).willReturn(Optional.of(sender));
        given(cardRepository.findByIdAndUser_IdAndDeletedFalse(99L, 1L)).willReturn(Optional.empty());

        P2pTransferRequest request = new P2pTransferRequest(99L, 2L, 10000L, "송금");

        assertThatThrownBy(() -> transferService.p2pTransfer(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CARD_NOT_FOUND));
    }

    // ───────────── withdraw ─────────────

    @Test
    void withdraw_성공() {
        User user = createUser(1L, "user-key");
        Card card = createCard(1L, user, "1234567890");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(cardRepository.findByIdAndUser_IdAndDeletedFalse(1L, 1L)).willReturn(Optional.of(card));

        SsafyWithdrawResponse withdrawResponse = new SsafyWithdrawResponse(
                successHeader(),
                new SsafyWithdrawResponse.WithdrawRec("TX001", "1234567890", "20260320", "50000", "450000"));
        given(ssafyFinanceClient.withdraw(eq("user-key"), eq("1234567890"), eq(50000L), any(String.class)))
                .willReturn(withdrawResponse);

        given(transferRepository.save(any(Transfer.class))).willAnswer(invocation -> {
            Transfer t = invocation.getArgument(0);
            setField(t, "id", 1L);
            return t;
        });

        WithdrawRequest request = new WithdrawRequest(1L, "9876543210", 50000L, "출금 테스트");

        TransferResponse response = transferService.withdraw(1L, request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.transferType()).isEqualTo(TransferType.WITHDRAW);
        assertThat(response.amount()).isEqualTo(50000L);
        assertThat(response.status()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(response.targetAccountNo()).isEqualTo("9876543210");
    }

    @Test
    void withdraw_카드없음_예외() {
        User user = createUser(1L, "user-key");

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(cardRepository.findByIdAndUser_IdAndDeletedFalse(99L, 1L)).willReturn(Optional.empty());

        WithdrawRequest request = new WithdrawRequest(99L, "9876543210", 50000L, "출금");

        assertThatThrownBy(() -> transferService.withdraw(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.CARD_NOT_FOUND));
    }

    // ───────────── getTransfer ─────────────

    @Test
    void getTransfer_성공() {
        User sender = createUser(1L, "user-key");
        Card senderCard = createCard(1L, sender, "1234567890");

        Transfer transfer = Transfer.builder()
                .senderUser(sender)
                .senderCard(senderCard)
                .transferType(TransferType.WITHDRAW)
                .amount(30000L)
                .description("출금")
                .targetAccountNo("9876543210")
                .build();
        setField(transfer, "id", 1L);

        given(transferRepository.findByIdAndUserId(1L, 1L)).willReturn(Optional.of(transfer));

        TransferResponse response = transferService.getTransfer(1L, 1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.amount()).isEqualTo(30000L);
        assertThat(response.transferType()).isEqualTo(TransferType.WITHDRAW);
        assertThat(response.status()).isEqualTo(TransferStatus.COMPLETED);
    }

    @Test
    void getTransfer_없음_예외() {
        given(transferRepository.findByIdAndUserId(99L, 1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.getTransfer(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.TRANSFER_NOT_FOUND));
    }

    // ───────────── helpers ─────────────

    private User createUser(Long id, String userKey) {
        User user = User.builder()
                .ci("ci-hash")
                .di("di-hash")
                .name("홍길동")
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender("M")
                .phoneNumber("01012345678")
                .phoneLast4("5678")
                .ssafyUserKey(userKey)
                .build();
        setField(user, "id", id);
        return user;
    }

    private Card createCard(Long id, User user, String withdrawalAccountNo) {
        Card card = Card.builder()
                .user(user)
                .cardName("테스트 카드")
                .cardType(CardType.BUSINESS)
                .last4Digits("7890")
                .cardNo("1005518816097890")
                .cvc("725")
                .cardUniqueNo("1003-xxx")
                .ssafyAccountNo(withdrawalAccountNo)
                .withdrawalAccountNo(withdrawalAccountNo)
                .withdrawalDate("4")
                .cardExpiryDate("20290401")
                .build();
        setField(card, "id", id);
        return card;
    }

    private SsafyResponseHeader successHeader() {
        return new SsafyResponseHeader("H0000", "정상처리 되었습니다.", "test", "20260320", "120000", "00100", "key", "test", "uniqueNo");
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
