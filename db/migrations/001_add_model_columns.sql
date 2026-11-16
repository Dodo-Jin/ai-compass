-- AI Compass 마이그레이션: AI_Models 테이블 확장 컬럼 9개 추가 (A 담당)
-- 실행 위치: db/schema.sql 실행 이후 이 파일을 실행합니다.
USE ai_compass;

ALTER TABLE `AI_Models`
    ADD COLUMN `site_url`       VARCHAR(500) NULL                      COMMENT '공식 사이트 URL',
    ADD COLUMN `logo_url`       VARCHAR(500) NULL                      COMMENT '로고 이미지 URL',
    ADD COLUMN `tags`           VARCHAR(255) NULL                      COMMENT '검색/분류 태그 (쉼표 구분)',
    ADD COLUMN `price_text`     VARCHAR(100) NULL                      COMMENT '상세 요금 안내 텍스트 (예: 무료 / 월 $20)',
    ADD COLUMN `input_types`    VARCHAR(100) NULL                      COMMENT '지원 입력 형태 (예: 텍스트, 이미지, 음성)',
    ADD COLUMN `korean_support` BOOLEAN      NOT NULL DEFAULT FALSE    COMMENT '한국어 지원 여부 (1: 지원, 0: 미지원)',
    ADD COLUMN `strengths`      TEXT         NULL                      COMMENT '주요 강점 및 특징',
    ADD COLUMN `limitations`    TEXT         NULL                      COMMENT '한계점 및 주의사항',
    ADD COLUMN `search_count`   INT          NOT NULL DEFAULT 0        COMMENT '검색/조회 횟수 (트렌드 점수 계산용)';