-- 팀 ERD 원본 (A 담당). 수정은 migrations/ 에 새 파일로 추가
CREATE DATABASE IF NOT EXISTS ai_compass DEFAULT CHARSET utf8mb4;
USE ai_compass;

CREATE TABLE `Favorites` (
	`favorite_id`	INT	NOT NULL,
	`user_id`	INT	NOT NULL,
	`model_id`	INT	NOT NULL,
	`created_at`	TIMESTAMP	NULL	DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `AI_Models` (
	`model_id`	INT	NOT NULL,
	`name`	VARCHAR(100)	NOT NULL,
	`category`	VARCHAR(50)	NOT NULL,
	`description`	TEXT	NULL,
	`pricing_type`	VARCHAR(30)	NULL,
	`ranking_score`	INT	NULL	DEFAULT 0,
	`created_at`	TIMESTAMP	NULL	DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `Users` (
	`user_id`	INT	NOT NULL,
	`username`	VARCHAR(50)	NOT NULL,
	`email`	VARCHAR(100)	NOT NULL,
	`password`	VARCHAR(255)	NOT NULL,
	`created_at`	TIMESTAMP	NULL	DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `Reviews` (
	`review_id`	INT	NOT NULL,
	`model_id`	INT	NOT NULL,
	`user_id`	INT	NOT NULL,
	`rating`	INT	NULL,
	`content`	TEXT	NOT NULL,
	`created_at`	TIMESTAMP	NULL	DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE `Pipelines` (
	`pipeline_id`	INT	NOT NULL,
	`user_id`	INT	NOT NULL,
	`title`	VARCHAR(100)	NOT NULL,
	`block_data`	TEXT	NOT NULL,
	`created_at`	TIMESTAMP	NULL	DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE `Favorites` ADD CONSTRAINT `PK_FAVORITES` PRIMARY KEY (
	`favorite_id`
);

ALTER TABLE `AI_Models` ADD CONSTRAINT `PK_AI_MODELS` PRIMARY KEY (
	`model_id`
);

ALTER TABLE `Users` ADD CONSTRAINT `PK_USERS` PRIMARY KEY (
	`user_id`
);

ALTER TABLE `Reviews` ADD CONSTRAINT `PK_REVIEWS` PRIMARY KEY (
	`review_id`
);

ALTER TABLE `Pipelines` ADD CONSTRAINT `PK_PIPELINES` PRIMARY KEY (
	`pipeline_id`
);

