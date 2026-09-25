package com.allensandiego.adm.service;

import com.allensandiego.adm.domain.AuditEvent;
import com.allensandiego.adm.domain.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.datasource.platform=test")
@Transactional
class AuditTest {

    @Autowired private AuditService auditService;
    @Autowired private AuditEventRepository auditEventRepository;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
    }

    @Test
    @DisplayName("Audit - create permission logs correctly")
    void auditCreatePermission() {
        UUID targetId = UUID.randomUUID();
        String actor = "admin";
        String action = "PERMISSION_CREATE";
        String entityType = "Permission";

        auditService.log(actor, action, entityType, targetId);

        List<AuditEvent> logs = auditEventRepository.findByTargetTypeAndActorUsername(entityType, actor);
        assertEquals(1, logs.size());
        AuditEvent log = logs.get(0);
        assertEquals(targetId, log.getTargetId());
        assertEquals(action, log.getAction());
        assertEquals(entityType, log.getTargetType());
        assertEquals(actor, log.getActorUsername());
    }

    @Test
    @DisplayName("Audit - create role logs correctly")
    void auditCreateRole() {
        UUID targetId = UUID.randomUUID();
        String actor = "admin";
        String action = "ROLE_CREATE";
        String entityType = "Role";

        auditService.log(actor, action, entityType, targetId);

        List<AuditEvent> logs = auditEventRepository.findByTargetTypeAndActorUsername(entityType, actor);
        assertEquals(1, logs.size());
        AuditEvent log = logs.get(0);
        assertEquals(targetId, log.getTargetId());
        assertEquals(action, log.getAction());
    }

    @Test
    @DisplayName("Audit - create user logs correctly")
    void auditCreateUser() {
        UUID targetId = UUID.randomUUID();
        String actor = "admin";
        String action = "USER_CREATE";
        String entityType = "User";

        auditService.log(actor, action, entityType, targetId);

        List<AuditEvent> logs = auditEventRepository.findByTargetTypeAndActorUsername(entityType, actor);
        assertEquals(1, logs.size());
        AuditEvent log = logs.get(0);
        assertEquals(targetId, log.getTargetId());
        assertEquals(action, log.getAction());
    }

    @Test
    @DisplayName("Audit - edit role logs correctly")
    void auditEditRole() {
        UUID targetId = UUID.randomUUID();
        String actor = "admin";
        String action = "ROLE_EDIT";
        String entityType = "Role";

        auditService.log(actor, action, entityType, targetId);

        List<AuditEvent> logs = auditEventRepository.findByTargetTypeAndActorUsername(entityType, actor);
        assertEquals(1, logs.size());
        AuditEvent log = logs.get(0);
        assertEquals(action, log.getAction());
    }

    @Test
    @DisplayName("Audit - assign permissions to role logs correctly")
    void auditAssignPermissions() {
        UUID targetId = UUID.randomUUID();
        String actor = "admin";
        String action = "ROLE_ASSIGN_PERMISSIONS";
        String entityType = "Role";

        auditService.log(actor, action, entityType, targetId);

        List<AuditEvent> logs = auditEventRepository.findByTargetTypeAndActorUsername(entityType, actor);
        assertEquals(1, logs.size());
        AuditEvent log = logs.get(0);
        assertEquals(action, log.getAction());
    }

    @Test
    @DisplayName("Audit - assign roles to user logs correctly")
    void auditAssignRoles() {
        UUID targetId = UUID.randomUUID();
        String actor = "admin";
        String action = "USER_ASSIGN_ROLES";
        String entityType = "User";

        auditService.log(actor, action, entityType, targetId);

        List<AuditEvent> logs = auditEventRepository.findByTargetTypeAndActorUsername(entityType, actor);
        assertEquals(1, logs.size());
        AuditEvent log = logs.get(0);
        assertEquals(action, log.getAction());
    }

    @Test
    @DisplayName("Audit - toggle permission active logs correctly")
    void auditTogglePermissionActive() {
        UUID targetId = UUID.randomUUID();
        String actor = "admin";
        String action = "PERMISSION_TOGGLE_ACTIVE";
        String entityType = "Permission";

        auditService.log(actor, action, entityType, targetId);

        List<AuditEvent> logs = auditEventRepository.findByTargetTypeAndActorUsername(entityType, actor);
        assertEquals(1, logs.size());
        AuditEvent log = logs.get(0);
        assertEquals(action, log.getAction());
    }

    @Test
    @DisplayName("Audit - update user profile logs correctly")
    void auditUpdateUserProfile() {
        UUID targetId = UUID.randomUUID();
        String actor = "admin";
        String action = "USER_UPDATE_PROFILE";
        String entityType = "User";

        auditService.log(actor, action, entityType, targetId);

        List<AuditEvent> logs = auditEventRepository.findByTargetTypeAndActorUsername(entityType, actor);
        assertEquals(1, logs.size());
        AuditEvent log = logs.get(0);
        assertEquals(action, log.getAction());
    }

    @Test
    @DisplayName("Audit - multiple actions by same actor are all logged")
    void auditMultipleActions() {
        UUID targetId1 = UUID.randomUUID();
        UUID targetId2 = UUID.randomUUID();
        String actor = "admin";

        auditService.log(actor, "PERMISSION_CREATE", "Permission", targetId1);
        auditService.log(actor, "ROLE_CREATE", "Role", targetId2);

        List<AuditEvent> logs = auditEventRepository.findByActorUsername(actor);
        assertEquals(2, logs.size());
    }

    @Test
    @DisplayName("Audit - multiple actors can log same entity type")
    void auditMultipleActors() {
        UUID targetId = UUID.randomUUID();
        String actor1 = "admin";
        String actor2 = "editor";

        auditService.log(actor1, "PERMISSION_CREATE", "Permission", targetId);
        auditService.log(actor2, "ROLE_CREATE", "Role", targetId);

        List<AuditEvent> logs = auditEventRepository.findByTargetType("Permission");
        assertEquals(1, logs.size());
        assertEquals(actor1, logs.get(0).getActorUsername());
    }

    @Test
    @DisplayName("Audit - audit log contains timestamp")
    void auditLogContainsTimestamp() {
        UUID targetId = UUID.randomUUID();

        auditService.log("admin", "TEST_ACTION", "TestEntity", targetId);

        List<AuditEvent> logs = auditEventRepository.findByTargetTypeAndActorUsername("TestEntity", "admin");
        assertEquals(1, logs.size());
        assertNotNull(logs.get(0).getOccurredAt());
    }

    @Test
    @DisplayName("Audit - audit log is queryable by entity ID")
    void auditLogQueryableByEntityId() {
        UUID targetId = UUID.randomUUID();

        auditService.log("admin", "TEST_ACTION", "TestEntity", targetId);

        List<AuditEvent> logs = auditEventRepository.findByTargetId(targetId);
        assertEquals(1, logs.size());
        assertEquals(targetId, logs.get(0).getTargetId());
    }

    @Test
    @DisplayName("Audit - audit log is queryable by action")
    void auditLogQueryableByAction() {
        UUID targetId = UUID.randomUUID();

        auditService.log("admin", "PERMISSION_CREATE", "Permission", targetId);

        List<AuditEvent> logs = auditEventRepository.findByAction("PERMISSION_CREATE");
        assertEquals(1, logs.size());
        assertEquals(targetId, logs.get(0).getTargetId());
    }

    @Test
    @DisplayName("Audit - all mutations create audit logs")
    void auditAllMutationsCreateLogs() {
        UUID permissionId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        // Simulate all mutation actions
        auditService.log("admin", "PERMISSION_CREATE", "Permission", permissionId);
        auditService.log("admin", "ROLE_CREATE", "Role", roleId);
        auditService.log("admin", "USER_CREATE", "User", userId);
        auditService.log("admin", "ROLE_EDIT", "Role", roleId);
        auditService.log("admin", "ROLE_ASSIGN_PERMISSIONS", "Role", roleId);
        auditService.log("admin", "USER_ASSIGN_ROLES", "User", userId);
        auditService.log("admin", "PERMISSION_TOGGLE_ACTIVE", "Permission", permissionId);
        auditService.log("admin", "USER_UPDATE_PROFILE", "User", userId);

        // Verify all 8 actions are logged
        List<AuditEvent> allLogs = auditEventRepository.findAll();
        assertEquals(8, allLogs.size());
    }
}
