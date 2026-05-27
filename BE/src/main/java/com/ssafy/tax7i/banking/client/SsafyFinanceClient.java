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
public class SsafyFinanceClient {

    private final RestTemplate restTemplate;
    private final SsafyFinanceProperties properties;
    private final SsafyFinanceHeaderBuilder headerBuilder;

    public SsafyCreateAccountResponse createAccount(String userKey, String accountTypeUniqueNo) {
        SsafyCommonHeader header = headerBuilder.build("createDemandDepositAccount", userKey);
        SsafyCreateAccountRequest request = new SsafyCreateAccountRequest(header, accountTypeUniqueNo);
        return post("/demandDeposit/createDemandDepositAccount", request, SsafyCreateAccountResponse.class);
    }

    public SsafyAccountListResponse getAccountList(String userKey) {
        SsafyCommonHeader header = headerBuilder.build("inquireDemandDepositAccountList", userKey);
        SsafyAccountListRequest request = new SsafyAccountListRequest(header);
        return post("/demandDeposit/inquireDemandDepositAccountList", request, SsafyAccountListResponse.class);
    }

    public SsafyBalanceResponse getBalance(String userKey, String accountNo) {
        SsafyCommonHeader header = headerBuilder.build("inquireDemandDepositAccountBalance", userKey);
        SsafyBalanceRequest request = new SsafyBalanceRequest(header, accountNo);
        return post("/demandDeposit/inquireDemandDepositAccountBalance", request, SsafyBalanceResponse.class);
    }

    public SsafyDepositResponse deposit(String userKey, String accountNo, long amount, String summary) {
        SsafyCommonHeader header = headerBuilder.build("updateDemandDepositAccountDeposit", userKey);
        SsafyDepositRequest request = new SsafyDepositRequest(header, accountNo, String.valueOf(amount), summary);
        return post("/demandDeposit/updateDemandDepositAccountDeposit", request, SsafyDepositResponse.class);
    }

    public SsafyWithdrawResponse withdraw(String userKey, String accountNo, long amount, String summary) {
        SsafyCommonHeader header = headerBuilder.build("updateDemandDepositAccountWithdrawal", userKey);
        SsafyWithdrawRequest request = new SsafyWithdrawRequest(header, accountNo, String.valueOf(amount), summary);
        return post("/demandDeposit/updateDemandDepositAccountWithdrawal", request, SsafyWithdrawResponse.class);
    }

    public SsafyTransferResult transfer(String senderUserKey, String fromAccountNo,
                                         String receiverUserKey, String toAccountNo,
                                         long amount, String withdrawSummary, String depositSummary) {
        SsafyWithdrawResponse withdrawResponse = withdraw(senderUserKey, fromAccountNo, amount, withdrawSummary);
        try {
            SsafyDepositResponse depositResponse = deposit(receiverUserKey, toAccountNo, amount, depositSummary);
            return new SsafyTransferResult(withdrawResponse, depositResponse);
        } catch (Exception depositEx) {
            log.error("이체 중 입금 실패, 환불 처리 시도: fromAccount={}, amount={}, error={}",
                    fromAccountNo, amount, depositEx.getMessage());
            try {
                deposit(senderUserKey, fromAccountNo, amount, "이체 실패 환불");
                log.info("이체 실패 환불 성공: fromAccount={}, amount={}", fromAccountNo, amount);
            } catch (Exception compensationEx) {
                // 환불 실패 — 수동 조치 필요
                log.error("[CRITICAL] 이체 실패 후 환불도 실패 — 수동 조치 필요: " +
                        "senderAccount={}, amount={}, 원인={}, 환불실패원인={}",
                        fromAccountNo, amount, depositEx.getMessage(), compensationEx.getMessage());
                throw new BusinessException(ErrorCode.BANK_SERVICE_UNAVAILABLE,
                        "이체 실패 및 환불 처리 실패 — 관리자에게 문의하세요.");
            }
            throw new BusinessException(ErrorCode.BANK_SERVICE_UNAVAILABLE, "이체 중 입금에 실패하여 환불 처리되었습니다.");
        }
    }

    public String searchMember(String userId) {
        SsafyMemberRequest request = new SsafyMemberRequest(properties.apiKey(), userId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(request, headers);

        try {
            SsafyMemberResponse response = restTemplate.postForObject(
                    getMemberBaseUrl() + "search", entity, SsafyMemberResponse.class);
            if (response == null) {
                return null;
            }
            return response.userKey();
        } catch (RestClientException e) {
            log.warn("SSAFY 멤버 조회 실패 (미등록일 수 있음): {}", e.getMessage());
            return null;
        }
    }

    public String registerMember(String userId) {
        SsafyMemberRequest request = new SsafyMemberRequest(properties.apiKey(), userId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(request, headers);

        try {
            SsafyMemberResponse response = restTemplate.postForObject(
                    getMemberBaseUrl(), entity, SsafyMemberResponse.class);
            if (response == null || response.userKey() == null) {
                throw new BusinessException(ErrorCode.BANK_SERVICE_UNAVAILABLE, "SSAFY 멤버 등록 응답이 비어있습니다.");
            }
            return response.userKey();
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("SSAFY 멤버 등록 API 호출 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.BANK_SERVICE_UNAVAILABLE);
        }
    }

    public String getOrRegisterMember(String userId) {
        String userKey = searchMember(userId);
        if (userKey != null) {
            return userKey;
        }
        return registerMember(userId);
    }

    private String getMemberBaseUrl() {
        return properties.baseUrl().replace("/edu", "") + "/member/";
    }

    public SsafyTransactionHistoryResponse getTransactionHistory(String userKey, String accountNo,
                                                                  String startDate, String endDate) {
        SsafyCommonHeader header = headerBuilder.build("inquireTransactionHistoryList", userKey);
        SsafyTransactionHistoryRequest request = new SsafyTransactionHistoryRequest(
                header, accountNo, startDate, endDate, "A", "DESC");
        return post("/demandDeposit/inquireTransactionHistoryList", request, SsafyTransactionHistoryResponse.class);
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
                log.error("SSAFY API 오류: {} - {}", response.header().responseCode(), response.header().responseMessage());
                throw new BusinessException(ErrorCode.BANK_SERVICE_UNAVAILABLE, response.header().responseMessage());
            }
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("SSAFY 금융망 API 호출 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.BANK_SERVICE_UNAVAILABLE);
        }
    }
}
