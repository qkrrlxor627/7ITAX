package com.ssafy.tax7i.banking.client;

import com.ssafy.tax7i.banking.client.dto.*;
import com.ssafy.tax7i.config.SsafyFinanceProperties;
import com.ssafy.tax7i.global.exception.BusinessException;
import com.ssafy.tax7i.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class SsafyCreditCardClient {

    private final RestTemplate restTemplate;
    private final SsafyFinanceProperties properties;
    private final SsafyFinanceHeaderBuilder headerBuilder;

    /**
     * 카드 상품 목록 조회 (CREDIT_CARD_05)
     */
    public SsafyCreditCardProductListResponse getCreditCardProducts(String userKey) {
        SsafyCommonHeader header = headerBuilder.build("inquireCreditCardList", userKey);
        SsafyCreditCardProductListRequest request = new SsafyCreditCardProductListRequest(header);
        return post("/creditCard/inquireCreditCardList", request, SsafyCreditCardProductListResponse.class);
    }

    /**
     * 가맹점 목록 조회 (CREDIT_CARD_08)
     */
    public SsafyMerchantListResponse getMerchantList(String userKey) {
        SsafyCommonHeader header = headerBuilder.build("inquireMerchantList", userKey);
        SsafyMerchantListRequest request = new SsafyMerchantListRequest(header);
        return post("/creditCard/inquireMerchantList", request, SsafyMerchantListResponse.class);
    }

    /**
     * 카드 발급 (CREDIT_CARD_06)
     */
    public SsafyCreateCreditCardResponse createCreditCard(String userKey, String cardUniqueNo,
                                                            String withdrawalAccountNo, String withdrawalDate) {
        SsafyCommonHeader header = headerBuilder.build("createCreditCard", userKey);
        SsafyCreateCreditCardRequest request = new SsafyCreateCreditCardRequest(
                header, cardUniqueNo, withdrawalAccountNo, withdrawalDate);
        return post("/creditCard/createCreditCard", request, SsafyCreateCreditCardResponse.class);
    }

    /**
     * 내 카드 목록 조회 (CREDIT_CARD_07)
     */
    public SsafyCreditCardListResponse getMyCards(String userKey) {
        SsafyCommonHeader header = headerBuilder.build("inquireSignUpCreditCardList", userKey);
        SsafyCreditCardListRequest request = new SsafyCreditCardListRequest(header);
        return post("/creditCard/inquireSignUpCreditCardList", request, SsafyCreditCardListResponse.class);
    }

    /**
     * 카드 결제 (CREDIT_CARD_09)
     */
    public SsafyCreditCardTransactionResponse createTransaction(String userKey, String cardNo, String cvc,
                                                                  Long merchantId, Long paymentBalance) {
        SsafyCommonHeader header = headerBuilder.build("createCreditCardTransaction", userKey);
        SsafyCreditCardTransactionRequest request = new SsafyCreditCardTransactionRequest(
                header, cardNo, cvc, merchantId, paymentBalance);
        return post("/creditCard/createCreditCardTransaction", request, SsafyCreditCardTransactionResponse.class);
    }

    /**
     * 카드 결제 내역 조회 (CREDIT_CARD_10)
     */
    public SsafyCreditCardTransactionListResponse getTransactionHistory(String userKey, String cardNo,
                                                                         String cvc, String startDate, String endDate) {
        SsafyCommonHeader header = headerBuilder.build("inquireCreditCardTransactionList", userKey);
        SsafyCreditCardTransactionListRequest request = new SsafyCreditCardTransactionListRequest(
                header, cardNo, cvc, startDate, endDate);
        return post("/creditCard/inquireCreditCardTransactionList", request, SsafyCreditCardTransactionListResponse.class);
    }

    /**
     * 카드 결제 취소 (CREDIT_CARD_11)
     */
    public SsafyDeleteCreditCardTransactionResponse deleteTransaction(String userKey, String cardNo,
                                                                        String cvc, Long transactionUniqueNo) {
        SsafyCommonHeader header = headerBuilder.build("deleteTransaction", userKey);
        SsafyDeleteCreditCardTransactionRequest request = new SsafyDeleteCreditCardTransactionRequest(
                header, cardNo, cvc, transactionUniqueNo);
        return post("/creditCard/deleteTransaction", request, SsafyDeleteCreditCardTransactionResponse.class);
    }

    /**
     * 청구서 조회 (CREDIT_CARD_12)
     */
    public SsafyBillingStatementsResponse getBillingStatements(String userKey, String cardNo,
                                                                String cvc, String startMonth, String endMonth) {
        SsafyCommonHeader header = headerBuilder.build("inquireBillingStatements", userKey);
        SsafyBillingStatementsRequest request = new SsafyBillingStatementsRequest(
                header, cardNo, cvc, startMonth, endMonth);
        return post("/creditCard/inquireBillingStatements", request, SsafyBillingStatementsResponse.class);
    }

    private <T extends SsafyApiResponse> T post(String path, Object request, Class<T> responseType) {
        String url = properties.baseUrl() + path;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(request, headers);

        try {
            T response = restTemplate.postForObject(url, entity, responseType);
            if (response == null) {
                throw new BusinessException(ErrorCode.BANK_SERVICE_UNAVAILABLE, "금융망 응답이 비어있습니다.");
            }
            if (!response.header().isSuccess()) {
                log.error("SSAFY 신용카드 API 오류: {} - {}", response.header().responseCode(), response.header().responseMessage());
                throw new BusinessException(ErrorCode.BANK_SERVICE_UNAVAILABLE, response.header().responseMessage());
            }
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("SSAFY 신용카드 API 호출 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.BANK_SERVICE_UNAVAILABLE);
        }
    }
}
