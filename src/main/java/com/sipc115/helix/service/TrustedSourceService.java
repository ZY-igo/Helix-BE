package com.sipc115.helix.service;

import com.sipc115.helix.config.BotProperties;
import com.sipc115.helix.model.entity.TrustedSource;
import com.sipc115.helix.repository.TrustedSourceRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TrustedSourceService {

    private final TrustedSourceRepository trustedSourceRepository;
    private final BotProperties properties;

    public TrustedSourceService(TrustedSourceRepository trustedSourceRepository, BotProperties properties) {
        this.trustedSourceRepository = trustedSourceRepository;
        this.properties = properties;
    }

    public List<String> getActiveDomains() {
        List<String> domains = trustedSourceRepository.findByEnabledTrueOrderByPriorityDescDomainAsc()
                .stream()
                .map(TrustedSource::getDomain)
                .toList();
        if (!domains.isEmpty()) {
            return domains;
        }
        return new ArrayList<>(properties.getSourceWhitelist());
    }
}
