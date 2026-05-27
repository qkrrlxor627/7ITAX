package com.ssafy.tax7i.tax.service;

import com.ssafy.tax7i.auth.domain.User;
import com.ssafy.tax7i.auth.repository.UserRepository;
import com.ssafy.tax7i.banking.client.SsafyFinanceClient;
import com.ssafy.tax7i.banking.client.dto.SsafyWithdrawResponse;
import com.ssafy.tax7i.card.entity.Card;
import com.ssafy.tax7i.card.repository.CardRepository;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import com.ssafy.tax7i.tax.dto.TaxPayResponse;
import com.ssafy.tax7i.tax.entity.TaxPayment;
import com.ssafy.tax7i.tax.entity.TaxPaymentStatus;
import com.ssafy.tax7i.tax.entity.TaxPaymentType;
import com.ssafy.tax7i.tax.entity.TaxReturn;
import com.ssafy.tax7i.tax.entity.TaxReturnStatus;
import com.ssafy.tax7i.tax.repository.TaxPaymentRepository;
import com.ssafy.tax7i.tax.repository.TaxReturnRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaxPaymentService {

    private final TaxReturnRepository taxReturnRepository;
    private final TaxPaymentRepository taxPaymentRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final SsafyFinanceClient ssafyFinanceClient;

    @Transactional
    public TaxPayResponse payNationalTax(Long userId, Long returnId, Long cardId) {
        TaxReturn taxReturn = taxReturnRepository.findByIdAndUserId(returnId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TAX_RETURN_NOT_FOUND));

        TaxPayment payment = taxPaymentRepository.findByTaxReturn_IdAndPaymentType(returnId, TaxPaymentType.NATIONAL)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "국세 납부 정보를 찾을 수 없습니다."));

        if (payment.getStatus() != TaxPaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.TAX_ALREADY_PAID);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Card card = cardRepository.findByIdAndUser_IdAndDeletedFalse(cardId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        String userKey = user.getSsafyUserKey();
        if (userKey == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "SSAFY 사용자 키가 없습니다.");
        }

        payment.markProcessing();

        try {
            SsafyWithdrawResponse response = ssafyFinanceClient.withdraw(
                    userKey,
                    card.getWithdrawalAccountNo(),
                    payment.getAmount(),
                    taxReturn.getTaxYear() + "귀속_종합소득세"
            );

            if (response == null || response.rec() == null) {
                throw new BusinessException(ErrorCode.BANK_SERVICE_UNAVAILABLE, "은행 응답이 비정상입니다.");
            }

            String txId = response.rec().transactionUniqueNo();
            payment.complete(txId, card.getWithdrawalAccountNo());

            log.info("국세 납부 완료: returnId={}, amount={}, txId={}", returnId, payment.getAmount(), txId);
            return TaxPayResponse.from(payment);
        } catch (Exception e) {
            payment.fail(e.getMessage());
            throw e;
        }
    }

    @Transactional
    public TaxPayResponse payLocalTax(Long userId, Long returnId, Long cardId) {
        TaxReturn taxReturn = taxReturnRepository.findByIdAndUserId(returnId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TAX_RETURN_NOT_FOUND));

        TaxPayment nationalPayment = taxPaymentRepository.findByTaxReturn_IdAndPaymentType(returnId, TaxPaymentType.NATIONAL)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "국세 납부 정보를 찾을 수 없습니다."));

        if (nationalPayment.getStatus() != TaxPaymentStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.TAX_NATIONAL_FIRST);
        }

        TaxPayment payment = taxPaymentRepository.findByTaxReturn_IdAndPaymentType(returnId, TaxPaymentType.LOCAL)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "지방세 납부 정보를 찾을 수 없습니다."));

        if (payment.getStatus() != TaxPaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.TAX_ALREADY_PAID);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Card card = cardRepository.findByIdAndUser_IdAndDeletedFalse(cardId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        String userKey = user.getSsafyUserKey();
        if (userKey == null) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT, "SSAFY 사용자 키가 없습니다.");
        }

        payment.markProcessing();

        try {
            SsafyWithdrawResponse response = ssafyFinanceClient.withdraw(
                    userKey,
                    card.getWithdrawalAccountNo(),
                    payment.getAmount(),
                    taxReturn.getTaxYear() + "귀속_지방소득세"
            );

            if (response == null || response.rec() == null) {
                throw new BusinessException(ErrorCode.BANK_SERVICE_UNAVAILABLE, "은행 응답이 비정상입니다.");
            }

            String txId = response.rec().transactionUniqueNo();
            payment.complete(txId, card.getWithdrawalAccountNo());

            taxReturn.transitionTo(TaxReturnStatus.PAID);
            taxReturn.transitionTo(TaxReturnStatus.COMPLETED);

            log.info("지방세 납부 완료: returnId={}, amount={}, txId={}", returnId, payment.getAmount(), txId);
            return TaxPayResponse.from(payment);
        } catch (Exception e) {
            payment.fail(e.getMessage());
            throw e;
        }
    }
}
