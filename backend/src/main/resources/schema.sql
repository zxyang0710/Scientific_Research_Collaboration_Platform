CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(30),
    avatar VARCHAR(255),
    department VARCHAR(100),
    title VARCHAR(50),
    role VARCHAR(20) NOT NULL DEFAULT 'RESEARCHER',
    status INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS research_project (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description CLOB,
    category VARCHAR(50),
    source VARCHAR(100),
    leader_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNING',
    progress INT NOT NULL DEFAULT 0,
    start_date DATE,
    end_date DATE,
    budget DECIMAL(14,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS project_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    project_role VARCHAR(30) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(project_id, user_id)
);

CREATE TABLE IF NOT EXISTS research_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    description CLOB,
    assignee_id BIGINT,
    creator_id BIGINT NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'TODO',
    deadline DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS project_file (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    uploader_id BIGINT NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(120),
    category VARCHAR(30) NOT NULL DEFAULT 'DOCUMENT',
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS research_paper (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    title VARCHAR(250) NOT NULL,
    authors VARCHAR(500),
    journal VARCHAR(200),
    publication_date DATE,
    doi VARCHAR(120),
    url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    abstract_text CLOB,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS project_discussion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    parent_id BIGINT,
    content CLOB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    receiver_id BIGINT NOT NULL,
    sender_id BIGINT,
    title VARCHAR(150) NOT NULL,
    content VARCHAR(500),
    type VARCHAR(30) NOT NULL DEFAULT 'SYSTEM',
    business_id BIGINT,
    link VARCHAR(255),
    is_read INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_member_project ON project_member(project_id);
CREATE INDEX IF NOT EXISTS idx_member_user ON project_member(user_id);
CREATE INDEX IF NOT EXISTS idx_task_project ON research_task(project_id);
CREATE INDEX IF NOT EXISTS idx_task_assignee ON research_task(assignee_id);
CREATE INDEX IF NOT EXISTS idx_file_project ON project_file(project_id);
CREATE INDEX IF NOT EXISTS idx_paper_project ON research_paper(project_id);
CREATE INDEX IF NOT EXISTS idx_discussion_project ON project_discussion(project_id);
CREATE INDEX IF NOT EXISTS idx_notification_receiver ON notification(receiver_id);
