package com.sipc115.helix.service;

import com.sipc115.helix.config.BotProperties;
import com.sipc115.helix.model.entity.TrustedSource;
import com.sipc115.helix.repository.TrustedSourceRepository;
import jakarta.annotation.PostConstruct;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class TrustedSourceBootstrap {

    private final TrustedSourceRepository trustedSourceRepository;
    private final BotProperties properties;

    public TrustedSourceBootstrap(TrustedSourceRepository trustedSourceRepository, BotProperties properties) {
        this.trustedSourceRepository = trustedSourceRepository;
        this.properties = properties;
    }

    @PostConstruct
    @Transactional
    public void initIfEmpty() {
        if (trustedSourceRepository.count() > 0) {
            return;
        }
        List<String> seeds = properties.getSourceWhitelist();
        int priority = 100;
        for (String domain : seeds) {
            if (!StringUtils.hasText(domain)) {
                continue;
            }
            TrustedSource source = new TrustedSource();
            source.setDomain(domain.trim());
            source.setSourceName(domain.trim());
            source.setSourceType("official");
            source.setPriority(priority--);
            source.setEnabled(true);
            source.setNotes("Seeded from app.source-whitelist");
            trustedSourceRepository.save(source);
        }
    }
}
