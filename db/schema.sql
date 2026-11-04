-- AI Compass DB 스키마 (팀 ERD 원본, A 담당). 이 파일 하나만 실행하면 DB가 완성됩니다.
-- 개발 시작 전: 구조 변경은 이 파일을 직접 수정합니다.
-- 개발 시작 후: 이 파일은 그대로 두고 db/migrations/ 에 번호 붙은 새 파일(예: 001_xxx.sql)로 추가합니다.
CREATE DATABASE IF NOT EXISTS ai_compass DEFAULT CHARSET utf8mb4;
USE ai_compass;

CREATE TABLE `Favorites` (
	`favorite_id`	INT	NOT NULL	AUTO_INCREMENT,
	`user_id`	INT	NOT NULL,
	`model_id`	INT	NOT NULL,
	`created_at`	TIMESTAMP	NULL	DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT `PK_FAVORITES` PRIMARY KEY (`favorite_id`)
);

CREATE TABLE `AI_Models` (
	`model_id`	INT	NOT NULL	AUTO_INCREMENT,
	`name`	VARCHAR(100)	NOT NULL,
	`category`	VARCHAR(50)	NOT NULL,
	`description`	TEXT	NULL,
	`pricing_type`	VARCHAR(30)	NULL,
	`ranking_score`	INT	NULL	DEFAULT 0,
	`created_at`	TIMESTAMP	NULL	DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT `PK_AI_MODELS` PRIMARY KEY (`model_id`)
);

CREATE TABLE `Users` (
	`user_id`	INT	NOT NULL	AUTO_INCREMENT,
	`username`	VARCHAR(50)	NOT NULL,
	`email`	VARCHAR(100)	NOT NULL,
	`password`	VARCHAR(255)	NOT NULL,
	`created_at`	TIMESTAMP	NULL	DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT `PK_USERS` PRIMARY KEY (`user_id`)
);

CREATE TABLE `Reviews` (
	`review_id`	INT	NOT NULL	AUTO_INCREMENT,
	`model_id`	INT	NOT NULL,
	`user_id`	INT	NOT NULL,
	`rating`	INT	NULL,
	`content`	TEXT	NOT NULL,
	`created_at`	TIMESTAMP	NULL	DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT `PK_REVIEWS` PRIMARY KEY (`review_id`)
);

CREATE TABLE `Pipelines` (
	`pipeline_id`	INT	NOT NULL	AUTO_INCREMENT,
	`user_id`	INT	NOT NULL,
	`title`	VARCHAR(100)	NOT NULL,
	`block_data`	TEXT	NOT NULL,
	`created_at`	TIMESTAMP	NULL	DEFAULT CURRENT_TIMESTAMP,
	CONSTRAINT `PK_PIPELINES` PRIMARY KEY (`pipeline_id`)
);

-- 중복 방지 (유니크)
ALTER TABLE `Users`     ADD CONSTRAINT `UQ_USERS_EMAIL`        UNIQUE (`email`);
ALTER TABLE `Users`     ADD CONSTRAINT `UQ_USERS_USERNAME`     UNIQUE (`username`);
ALTER TABLE `Favorites` ADD CONSTRAINT `UQ_FAVORITES`          UNIQUE (`user_id`, `model_id`);
ALTER TABLE `Reviews`   ADD CONSTRAINT `UQ_REVIEWS_USER_MODEL` UNIQUE (`user_id`, `model_id`);

-- 외래키 (부모 행이 삭제되면 자식 행도 함께 삭제)
ALTER TABLE `Reviews`   ADD CONSTRAINT `FK_REVIEWS_MODEL`   FOREIGN KEY (`model_id`) REFERENCES `AI_Models` (`model_id`) ON DELETE CASCADE;
ALTER TABLE `Reviews`   ADD CONSTRAINT `FK_REVIEWS_USER`    FOREIGN KEY (`user_id`)  REFERENCES `Users` (`user_id`)      ON DELETE CASCADE;
ALTER TABLE `Favorites` ADD CONSTRAINT `FK_FAVORITES_USER`  FOREIGN KEY (`user_id`)  REFERENCES `Users` (`user_id`)      ON DELETE CASCADE;
ALTER TABLE `Favorites` ADD CONSTRAINT `FK_FAVORITES_MODEL` FOREIGN KEY (`model_id`) REFERENCES `AI_Models` (`model_id`) ON DELETE CASCADE;
ALTER TABLE `Pipelines` ADD CONSTRAINT `FK_PIPELINES_USER`  FOREIGN KEY (`user_id`)  REFERENCES `Users` (`user_id`)      ON DELETE CASCADE;
