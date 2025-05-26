create TABLE users(
    id bigserial PRIMARY KEY ,
    first_name varchar(100) not null ,
    last_name varchar(100) not null ,
    email varchar(255) not null unique ,
    active boolean default true,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp
);

create table projects(
    id bigserial primary key ,
    name varchar(255) not null,
    description text,
    status varchar(50) not null default 'ACTIVE',
    owner_id bigint not null,
    created_at timestamp default current_timestamp,
    updated_ar timestamp default current_timestamp,
    CONSTRAINT fk_project_owner foreign key (owner_id) references users(id) on delete restrict
);

create table tasks(
    id bigserial primary key ,
    title varchar(255) not null ,
    description text,
    status varchar(50) not null default 'TODO',
    priority varchar(50) not null default 'MEDIUM',
    deadline timestamp,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp,
    assigned_user_id bigint,
    project_id bigint not null ,
    CONSTRAINT fk_task_assigned_user foreign key (assigned_user_id) references users(id) on delete set null ,
    constraint fk_task_project foreign key (project_id) references projects(id) on delete cascade
);

create table comments(
    id bigserial primary key ,
    context text not null ,
    task_id bigint not null ,
    user_id bigint not null ,
    created_at timestamp default current_timestamp,
    updated_at timestamp default current_timestamp,
    constraint fk_comment_task foreign key (task_id) references tasks(id) on delete cascade ,
    constraint fk_comment_user foreign key (user_id) references users(id) on delete cascade
);

create table project_users(
    project_id bigint not null ,
    user_id bigint not null ,
    primary key (project_id, user_id),
    constraint fk_project_users_project foreign key (project_id) references projects(id) on delete cascade ,
    constraint fk_project_users_user foreign key (user_id) references users(id) on delete cascade
);

create index idx_tasks_status on tasks(status);
create index idx_tasks_priority on tasks(priority);
create index idx_tasks_project_id on tasks(project_id);
create index idx_tasks_assigned_user_id on tasks(assigned_user_id);
create index idx_comments_task_id on comments(task_id);
create index idx_comments_user_id on comments(user_id);
create index idx_projects_owner_id on projects(owner_id);
create index idx_users_email on users(email);