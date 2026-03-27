package com.sipc115.helix.controller;

import com.sipc115.helix.model.entity.TrustedSource;
import com.sipc115.helix.repository.TrustedSourceRepository;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sources")
public class TrustedSourceController {

    private final TrustedSourceRepository trustedSourceRepository;

    public TrustedSourceController(TrustedSourceRepository trustedSourceRepository) {
        this.trustedSourceRepository = trustedSourceRepository;
    }

    @GetMapping
    public List<TrustedSource> list() {
        return trustedSourceRepository.findAll();
    }

    @PostMapping
    @Transactional
    public TrustedSource create(@RequestBody UpsertSourceRequest request) {
        TrustedSource source = new TrustedSource();
        apply(source, request);
        return trustedSourceRepository.save(source);
    }

    @PutMapping("/{id}")
    @Transactional
    public TrustedSource update(@PathVariable Long id, @RequestBody UpsertSourceRequest request) {
        TrustedSource source = trustedSourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Source not found: " + id));
        apply(source, request);
        return trustedSourceRepository.save(source);
    }

    private void apply(TrustedSource source, UpsertSourceRequest request) {
        source.setDomain(request.domain());
        source.setSourceName(request.sourceName());
        source.setSourceType(request.sourceType());
        source.setPriority(request.priority());
        source.setEnabled(request.enabled());
        source.setNotes(request.notes());
    }

    public record UpsertSourceRequest(
            String domain,
            String sourceName,
            String sourceType,
            Integer priority,
            boolean enabled,
            String notes) {
    }
}
