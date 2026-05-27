package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsafyMemberResponse(
        @JsonProperty("userId") String userId,
        @JsonProperty("userName") String userName,
        @JsonProperty("institutionCode") String institutionCode,
        @JsonProperty("userKey") String userKey,
        @JsonProperty("created") String created,
        @JsonProperty("modified") String modified
) {}
