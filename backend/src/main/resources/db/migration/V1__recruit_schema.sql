-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2

CREATE TABLE department (
  id bigint AUTO_INCREMENT PRIMARY KEY,
  name varchar(120) NOT NULL UNIQUE
);

CREATE TABLE access_role (
  id bigint AUTO_INCREMENT PRIMARY KEY,
  name varchar(120) NOT NULL UNIQUE,
  scope varchar(20) NOT NULL
);

CREATE TABLE permission (
  id bigint AUTO_INCREMENT PRIMARY KEY,
  code varchar(60) NOT NULL UNIQUE,
  name varchar(120) NOT NULL
);

CREATE TABLE role_permission (role_id bigint NOT NULL, permission_code varchar(60) NOT NULL, PRIMARY KEY(role_id, permission_code), FOREIGN KEY(role_id) REFERENCES access_role(id), FOREIGN KEY(permission_code) REFERENCES permission(code));

CREATE TABLE nav_menu (
  id bigint AUTO_INCREMENT PRIMARY KEY,
  code varchar(60) NOT NULL UNIQUE,
  name varchar(120) NOT NULL,
  name_en varchar(120) NOT NULL,
  permission_code varchar(60) NOT NULL,
  position int NOT NULL,
  enabled boolean NOT NULL
);

CREATE TABLE account (
  id bigint AUTO_INCREMENT PRIMARY KEY,
  username varchar(60) NOT NULL UNIQUE,
  display_name varchar(120) NOT NULL,
  password_hash varchar(100) NOT NULL,
  role_id bigint NOT NULL,
  department_id bigint NOT NULL,
  enabled boolean NOT NULL,
  FOREIGN KEY (role_id) REFERENCES access_role(id),
  FOREIGN KEY (department_id) REFERENCES department(id)
);

CREATE TABLE audit_event (
  id bigint AUTO_INCREMENT PRIMARY KEY,
  actor varchar(60) NOT NULL,
  action varchar(120) NOT NULL,
  object_id varchar(80) NOT NULL,
  department_id bigint NOT NULL,
  created_at timestamp(6) NOT NULL
);

CREATE TABLE system_setting (
  id bigint AUTO_INCREMENT PRIMARY KEY,
  code varchar(60) NOT NULL UNIQUE,
  parameter_value varchar(6000) NOT NULL
);

CREATE TABLE dictionary_entry (
  id bigint AUTO_INCREMENT PRIMARY KEY,
  type varchar(60) NOT NULL,
  code varchar(60) NOT NULL,
  name varchar(120) NOT NULL,
  name_en varchar(120) NOT NULL,
enabled boolean NOT NULL DEFAULT TRUE,
  UNIQUE (type, code)
);





CREATE TABLE job_opening (
 id bigint AUTO_INCREMENT PRIMARY KEY,
 code varchar(60) NOT NULL UNIQUE,
 title varchar(160) NOT NULL,
 location varchar(160) NOT NULL,
 employment_type varchar(60) NOT NULL,
 description varchar(6000) NOT NULL,
 department_id bigint NOT NULL,
 owner_id bigint NOT NULL,
 manager_id bigint NOT NULL,
 submitted_by_id bigint NULL,
 FOREIGN KEY(submitted_by_id) REFERENCES account(id),
 headcount int NOT NULL,
 status varchar(20) NOT NULL,
 revision bigint NOT NULL,
 created_at timestamp(6) NOT NULL,
 published_at timestamp(6) NULL,
 FOREIGN KEY(department_id) REFERENCES department(id),
 FOREIGN KEY(owner_id) REFERENCES account(id),
 FOREIGN KEY(manager_id) REFERENCES account(id),
 CHECK(headcount BETWEEN 1 AND 1000),
 INDEX ix_job_status(status,department_id)
);

CREATE TABLE applicant (
 id bigint AUTO_INCREMENT PRIMARY KEY,
 reference varchar(60) NOT NULL UNIQUE,
 job_id bigint NOT NULL,
 owner_id bigint NOT NULL,
 name varchar(160) NOT NULL,
 email varchar(254) NOT NULL,
 phone varchar(80) NOT NULL,
 introduction varchar(4000) NOT NULL,
 source varchar(60) NOT NULL,
 consent_reference varchar(1000) NOT NULL,
 consent_hash varchar(64) NOT NULL,
 consent_at timestamp(6) NOT NULL,
 stage varchar(20) NOT NULL,
 revision bigint NOT NULL,
 created_at timestamp(6) NOT NULL,
 employee_code varchar(80) NULL,
 joined_date date NULL,
 redacted_at timestamp(6) NULL,
 FOREIGN KEY(job_id) REFERENCES job_opening(id),
 FOREIGN KEY(owner_id) REFERENCES account(id),
 UNIQUE(job_id,email),
 UNIQUE(employee_code),
 INDEX ix_app_stage(job_id,stage),
 INDEX ix_app_owner(owner_id)
);

CREATE TABLE interview (
 id bigint AUTO_INCREMENT PRIMARY KEY,
 applicant_id bigint NOT NULL,
 interviewer_id bigint NOT NULL,
 title varchar(160) NOT NULL,
 location varchar(300) NOT NULL,
 starts_at timestamp(6) NOT NULL,
 ends_at timestamp(6) NOT NULL,
 status varchar(20) NOT NULL,
 recommendation varchar(30) NOT NULL,
 feedback varchar(4000) NOT NULL,
 creator_id bigint NOT NULL,
 completed_at timestamp(6) NULL,
 FOREIGN KEY(applicant_id) REFERENCES applicant(id),
 FOREIGN KEY(interviewer_id) REFERENCES account(id),
 FOREIGN KEY(creator_id) REFERENCES account(id),
 CHECK(ends_at > starts_at),
 INDEX ix_interview_time(interviewer_id,status,starts_at,ends_at)
);

CREATE TABLE recruit_offer (
 id bigint AUTO_INCREMENT PRIMARY KEY,
 applicant_id bigint NOT NULL,
 version int NOT NULL,
 salary decimal(18,2) NOT NULL,
 currency varchar(3) NOT NULL,
 pay_period varchar(20) NOT NULL,
 start_date date NOT NULL,
 expires_at timestamp(6) NOT NULL,
 terms varchar(4000) NOT NULL,
 status varchar(20) NOT NULL,
 creator_id bigint NOT NULL,
 submitted_by_id bigint NULL,
 FOREIGN KEY(submitted_by_id) REFERENCES account(id),
 reviewer_id bigint NULL,
 review_note varchar(1000) NOT NULL,
 delivery_reference varchar(300) NOT NULL,
 response_reference varchar(300) NOT NULL,
 created_at timestamp(6) NOT NULL,
 FOREIGN KEY(applicant_id) REFERENCES applicant(id),
 FOREIGN KEY(creator_id) REFERENCES account(id),
 FOREIGN KEY(reviewer_id) REFERENCES account(id),
 UNIQUE(applicant_id,version),
 CHECK(salary >= 0),
 INDEX ix_offer_state(applicant_id,status)
);

CREATE TABLE applicant_note (
 id bigint AUTO_INCREMENT PRIMARY KEY,
 applicant_id bigint NOT NULL,
 actor_id bigint NOT NULL,
 note varchar(4000) NOT NULL,
 created_at timestamp(6) NOT NULL,
 FOREIGN KEY(applicant_id) REFERENCES applicant(id),
 FOREIGN KEY(actor_id) REFERENCES account(id)
);

CREATE TABLE recruit_event (
 id bigint AUTO_INCREMENT PRIMARY KEY,
 job_id bigint NOT NULL,
 applicant_id bigint NULL,
 action varchar(60) NOT NULL,
 actor varchar(60) NOT NULL,
 note varchar(1000) NOT NULL,
 created_at timestamp(6) NOT NULL,
 FOREIGN KEY(job_id) REFERENCES job_opening(id),
 FOREIGN KEY(applicant_id) REFERENCES applicant(id)
);

CREATE TABLE resume_file (
 id bigint AUTO_INCREMENT PRIMARY KEY,
 applicant_id bigint NOT NULL,
 filename varchar(160) NOT NULL,
 digest varchar(64) NOT NULL,
 created_at timestamp(6) NOT NULL,
 content mediumblob NOT NULL,
 FOREIGN KEY(applicant_id) REFERENCES applicant(id),
 UNIQUE(applicant_id)
);
