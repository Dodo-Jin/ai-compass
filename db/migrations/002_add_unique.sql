-- 001 보완: 이름 중복 방지, 툴당 리뷰 1개 제한
-- 001_autoinc_fk.sql 실행 후 이어서 실행
USE ai_compass;

ALTER TABLE Users   ADD CONSTRAINT UQ_USERS_USERNAME UNIQUE (username);
ALTER TABLE Reviews ADD CONSTRAINT UQ_REVIEWS_USER_MODEL UNIQUE (user_id, model_id);
