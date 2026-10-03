create table permissions (
    id uuid primary key,
    code varchar(30) not null unique,
    name varchar(255) not null unique,
    path varchar(255) not null unique,
    description varchar(255),
    enabled boolean not null default true,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

create table roles (
    id uuid primary key,
    code varchar(30) not null unique,
    name varchar(255) not null unique,
    description varchar(255),
    enabled boolean not null default true,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

create table role_permissions (
    role_id uuid not null,
    permission_id uuid not null,
    enabled boolean not null default true,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    primary key (role_id, permission_id),
    foreign key (role_id) references roles(id) on delete cascade,
    foreign key (permission_id) references permissions(id) on delete cascade
);

create table users (
    id uuid primary key,
    username varchar(255) not null unique,
    password varchar(255) not null,
    email varchar(255) not null unique,
    first_name varchar(255) not null,
    last_name varchar(255) not null,
    enabled boolean not null default true,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

create table user_roles (
    user_id uuid not null,
    role_id uuid not null,
    enabled boolean not null default true,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    primary key (user_id, role_id),
    foreign key (user_id) references users(id) on delete cascade,
    foreign key (role_id) references roles(id) on delete cascade
);

create table audit_event (
    id uuid primary key,
    actor varchar(255),
    action varchar(50) not null,
    target_type varchar(50) not null,
    target_id uuid,
    before_value text,
    after_value text,
    created_at timestamp not null default now()
);

create table auth_event (
    id uuid primary key,
    username varchar(255),
    account_id uuid,
    outcome varchar(50) not null,
    occurred_at timestamp not null default now(),
    foreign key (account_id) references users(id) on delete set null
);

