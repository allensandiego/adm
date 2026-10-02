INSERT INTO roles (id, code, name, description, enabled)
VALUES
    ('11111111-1111-4111-8111-111111111111', 'admin', 'Administrator', 'Full system access', true),
    ('22222222-2222-4222-8222-222222222222', 'user', 'User', 'Standard application access', true),
    ('33333333-3333-4333-8333-333333333333', 'manager', 'Manager', 'Operational access for team oversight', true);

INSERT INTO permissions (id, code, name, path, description, enabled)
VALUES
    ('44444444-4444-4444-8444-444444444401', 'permission.view', 'View Permissions', '/permissions', 'View permission list and details', true),
    ('44444444-4444-4444-8444-444444444402', 'permission.create', 'Create Permission', '/permissions/new', 'Create permissions', true),
    ('44444444-4444-4444-8444-444444444403', 'permission.edit', 'Edit Permission', '/permissions/{id}/edit', 'Edit permission details and status', true),
    ('44444444-4444-4444-8444-444444444404', 'user.view', 'View Users', '/users', 'View user list and details', true),
    ('44444444-4444-4444-8444-444444444405', 'user.create', 'Create User', '/users/new', 'Create users', true),
    ('44444444-4444-4444-8444-444444444406', 'user.edit', 'Edit User', '/users/{id}/edit', 'Edit user account details', true),
    ('44444444-4444-4444-8444-444444444407', 'user.activate', 'Activate/Deactivate User', '/users/{id}/status', 'Change user status', true),
    ('44444444-4444-4444-8444-444444444408', 'user.roles.assign', 'Assign User Roles', '/users/{id}/roles', 'Assign or remove user roles', true),
    ('44444444-4444-4444-8444-444444444409', 'role.view', 'View Roles', '/roles', 'View role list and details', true),
    ('44444444-4444-4444-8444-444444444410', 'role.create', 'Create Role', '/roles/new', 'Create roles', true),
    ('44444444-4444-4444-8444-444444444411', 'role.edit', 'Edit Role', '/roles/{id}/edit', 'Rename roles', true),
    ('44444444-4444-4444-8444-444444444412', 'role.delete', 'Delete Role', '/roles/{id}/delete', 'Delete roles', true),
    ('44444444-4444-4444-8444-444444444413', 'role.permissions.edit', 'Edit Role Permissions', '/roles/{id}/permissions', 'Edit role permissions', true);

INSERT INTO role_permissions (role_id, permission_id, enabled)
VALUES
    ('11111111-1111-4111-8111-111111111111', '44444444-4444-4444-8444-444444444401', true),
    ('11111111-1111-4111-8111-111111111111', '44444444-4444-4444-8444-444444444402', true),
    ('11111111-1111-4111-8111-111111111111', '44444444-4444-4444-8444-444444444403', true),
    ('11111111-1111-4111-8111-111111111111', '44444444-4444-4444-8444-444444444404', true),
    ('11111111-1111-4111-8111-111111111111', '44444444-4444-4444-8444-444444444405', true),
    ('11111111-1111-4111-8111-111111111111', '44444444-4444-4444-8444-444444444406', true),
    ('11111111-1111-4111-8111-111111111111', '44444444-4444-4444-8444-444444444407', true),
    ('11111111-1111-4111-8111-111111111111', '44444444-4444-4444-8444-444444444408', true),
    ('11111111-1111-4111-8111-111111111111', '44444444-4444-4444-8444-444444444409', true),
    ('11111111-1111-4111-8111-111111111111', '44444444-4444-4444-8444-444444444410', true),
    ('11111111-1111-4111-8111-111111111111', '44444444-4444-4444-8444-444444444411', true),
    ('11111111-1111-4111-8111-111111111111', '44444444-4444-4444-8444-444444444412', true),
    ('11111111-1111-4111-8111-111111111111', '44444444-4444-4444-8444-444444444413', true),
    ('22222222-2222-4222-8222-222222222222', '44444444-4444-4444-8444-444444444401', true),
    ('22222222-2222-4222-8222-222222222222', '44444444-4444-4444-8444-444444444404', true),
    ('22222222-2222-4222-8222-222222222222', '44444444-4444-4444-8444-444444444409', true),
    ('33333333-3333-4333-8333-333333333333', '44444444-4444-4444-8444-444444444401', true),
    ('33333333-3333-4333-8333-333333333333', '44444444-4444-4444-8444-444444444404', true),
    ('33333333-3333-4333-8333-333333333333', '44444444-4444-4444-8444-444444444406', true),
    ('33333333-3333-4333-8333-333333333333', '44444444-4444-4444-8444-444444444407', true),
    ('33333333-3333-4333-8333-333333333333', '44444444-4444-4444-8444-444444444408', true),
    ('33333333-3333-4333-8333-333333333333', '44444444-4444-4444-8444-444444444409', true);

-- Users passwords are valid BCrypt hashes (strength 10) in Spring Security {id}encoded delegation format:
--   adminuser : 'admin123'
--   jdoe      : 'password123'
--   jsmith    : 'password123'
INSERT INTO users (id, username, password, email, first_name, last_name, enabled)
VALUES
    ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', 'adminuser', '{bcrypt}$2a$10$vYaKSJ8YFxCgFseMrfRQ4ekKjCqs5/VzhXDjKDd3VmLX0xRTdxHdG', 'admin@example.com', 'Alice', 'Admin', true),
    ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', 'jdoe', '{bcrypt}$2a$10$UkghHvYWozrTG9wiCkZ8mOpND46MTSPkcCx3507iJx0XrCLaYOl2G', 'john@example.com', 'John', 'Doe', true),
    ('cccccccc-cccc-4ccc-8ccc-cccccccccccc', 'jsmith', '{bcrypt}$2a$10$74NLRoGyuV41nojnuWcACOKFToT2yRmt6.w94JY9eGgdpqZVhChFK', 'jane@example.com', 'Jane', 'Smith', true);

INSERT INTO user_roles (user_id, role_id, enabled)
VALUES
    ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa', '11111111-1111-4111-8111-111111111111', true),
    ('bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb', '22222222-2222-4222-8222-222222222222', true),
    ('cccccccc-cccc-4ccc-8ccc-cccccccccccc', '22222222-2222-4222-8222-222222222222', true),
    ('cccccccc-cccc-4ccc-8ccc-cccccccccccc', '33333333-3333-4333-8333-333333333333', true);