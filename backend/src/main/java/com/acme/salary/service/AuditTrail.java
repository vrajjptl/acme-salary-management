package com.acme.salary.service;

import com.acme.salary.dto.Views;
import com.acme.salary.domain.Pages;
import com.acme.salary.repo.AuditRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditTrail {
    private final AuditRepository audits;

    public AuditTrail(AuditRepository audits) {
        this.audits = audits;
    }

    public void record(String actor, String action, String entityType, String entityId, String summary) {
        audits.record(actor, action, entityType, entityId, summary);
    }

    public Views.AuditPage page(int page, int size) {
        int safePage = Pages.page(page);
        int safeSize = Pages.size(size);
        List<Views.AuditItem> items = audits.page(safeSize, safePage * safeSize).stream()
                .map(row -> new Views.AuditItem(
                        row.id(),
                        row.occurredAt(),
                        row.actor(),
                        row.action(),
                        row.entityType(),
                        row.entityId(),
                        row.summary()
                ))
                .toList();
        return new Views.AuditPage(safePage, safeSize, audits.count(), items);
    }
}
