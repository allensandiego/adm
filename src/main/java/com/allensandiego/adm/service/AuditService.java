package com.allensandiego.adm.service;

import com.allensandiego.adm.domain.AuditEvent;
import com.allensandiego.adm.domain.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public void log(String actorUsername, String action, String targetType, UUID targetId,
                    String before, String after) {
        AuditEvent event = new AuditEvent();
        event.setActorUsername(actorUsername);
        event.setAction(action);
        event.setTargetType(targetType);
        event.setTargetId(targetId);
        event.setBefore(before);
        event.setAfter(after);
        auditEventRepository.save(event);
    }

    @Transactional
    public void log(String actorUsername, String action, String targetType, UUID targetId) {
        log(actorUsername, action, targetType, targetId, null, null);
    }
}
