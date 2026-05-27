package com.ssafy.tax7i.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tax")
public class TaxOfficeProperties {

    private String officeCode = "0305";
}
