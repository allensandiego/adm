package com.allensandiego.adm.web;

import com.allensandiego.adm.domain.AuditEvent;
import com.allensandiego.adm.domain.AuditEventRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/audit")
public class AuditController {

    private final AuditEventRepository auditEventRepository;

    public AuditController(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public List<AuditEvent> getAuditLogs() {
        return auditEventRepository.findAll();
    }
}
