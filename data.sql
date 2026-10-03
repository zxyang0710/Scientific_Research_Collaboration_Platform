CREATE DATABASE IF NOT EXISTS research_collaboration
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE research_collaboration;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
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
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    KEY idx_user_role_status (role, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS research_project (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(120) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    source VARCHAR(100),
    leader_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNING',
    progress INT NOT NULL DEFAULT 0,
    start_date DATE,
    end_date DATE,
    budget DECIMAL(14,2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_code (code),
    KEY idx_project_leader (leader_id),
    KEY idx_project_status (status),
    CONSTRAINT fk_project_leader FOREIGN KEY (leader_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS project_member (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    project_role VARCHAR(30) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_member (project_id, user_id),
    KEY idx_member_project (project_id),
    KEY idx_member_user (user_id),
    CONSTRAINT fk_member_project FOREIGN KEY (project_id) REFERENCES research_project (id) ON DELETE CASCADE,
    CONSTRAINT fk_member_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS research_task (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    assignee_id BIGINT,
    creator_id BIGINT NOT NULL,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'TODO',
    deadline DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_task_project (project_id),
    KEY idx_task_assignee (assignee_id),
    KEY idx_task_creator (creator_id),
    KEY idx_task_status_deadline (status, deadline),
    CONSTRAINT fk_task_project FOREIGN KEY (project_id) REFERENCES research_project (id) ON DELETE CASCADE,
    CONSTRAINT fk_task_assignee FOREIGN KEY (assignee_id) REFERENCES sys_user (id) ON DELETE SET NULL,
    CONSTRAINT fk_task_creator FOREIGN KEY (creator_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS project_file (
    id BIGINT NOT NULL AUTO_INCREMENT,
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
    deleted INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_file_project (project_id),
    KEY idx_file_uploader (uploader_id),
    KEY idx_file_category (category),
    CONSTRAINT fk_file_project FOREIGN KEY (project_id) REFERENCES research_project (id) ON DELETE CASCADE,
    CONSTRAINT fk_file_uploader FOREIGN KEY (uploader_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS research_paper (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    title VARCHAR(250) NOT NULL,
    authors VARCHAR(500),
    journal VARCHAR(200),
    publication_date DATE,
    doi VARCHAR(120),
    url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    abstract_text TEXT,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_paper_project (project_id),
    KEY idx_paper_creator (created_by),
    KEY idx_paper_status (status),
    CONSTRAINT fk_paper_project FOREIGN KEY (project_id) REFERENCES research_project (id) ON DELETE CASCADE,
    CONSTRAINT fk_paper_creator FOREIGN KEY (created_by) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS project_discussion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    parent_id BIGINT,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_discussion_project (project_id, created_at),
    KEY idx_discussion_author (author_id),
    KEY idx_discussion_parent (parent_id),
    CONSTRAINT fk_discussion_project FOREIGN KEY (project_id) REFERENCES research_project (id) ON DELETE CASCADE,
    CONSTRAINT fk_discussion_author FOREIGN KEY (author_id) REFERENCES sys_user (id),
    CONSTRAINT fk_discussion_parent FOREIGN KEY (parent_id) REFERENCES project_discussion (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notification (
    id BIGINT NOT NULL AUTO_INCREMENT,
    receiver_id BIGINT NOT NULL,
    sender_id BIGINT,
    title VARCHAR(150) NOT NULL,
    content VARCHAR(500),
    type VARCHAR(30) NOT NULL DEFAULT 'SYSTEM',
    business_id BIGINT,
    link VARCHAR(255),
    is_read INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_notification_receiver (receiver_id, is_read, created_at),
    KEY idx_notification_sender (sender_id),
    CONSTRAINT fk_notification_receiver FOREIGN KEY (receiver_id) REFERENCES sys_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_notification_sender FOREIGN KEY (sender_id) REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
