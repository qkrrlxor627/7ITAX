package com.ssafy.tax7i.classification.service;

import com.ssafy.tax7i.classification.entity.MccTaxRule;
import com.ssafy.tax7i.classification.entity.Merchant;
import com.ssafy.tax7i.classification.repository.MccTaxRuleRepository;
import com.ssafy.tax7i.classification.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClassificationCacheService {

    private final MccTaxRuleRepository mccTaxRuleRepository;
    private final MerchantRepository merchantRepository;

    @Cacheable(value = "mccTaxRules", key = "#mcc")
    public List<MccTaxRule> getMccRules(String mcc) {
        return mccTaxRuleRepository.findByMccOrderByTierAscIdAsc(mcc);
    }

    @Cacheable(value = "merchants", key = "#merchantName")
    public Optional<Merchant> getMerchantByName(String merchantName) {
        return merchantRepository.findFirstByMerchantName(merchantName);
    }
}
