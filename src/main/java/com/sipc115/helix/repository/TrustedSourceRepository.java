package com.sipc115.helix.repository;

import com.sipc115.helix.model.entity.TrustedSource;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrustedSourceRepository extends JpaRepository<TrustedSource, Long> {
    List<TrustedSource> findByEnabledTrueOrderByPriorityDescDomainAsc();

    Optional<TrustedSource> findByDomain(String domain);
}
