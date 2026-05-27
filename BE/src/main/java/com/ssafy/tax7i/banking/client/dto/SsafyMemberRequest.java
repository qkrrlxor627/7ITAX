package com.ssafy.tax7i.banking.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SsafyMemberRequest(
        @JsonProperty("apiKey") String apiKey,
        @JsonProperty("userId") String userId
) {}
