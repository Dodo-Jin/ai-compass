-- AI Compass 시연용 활동 시드 데이터 (A 담당)
-- 목적: 트렌드 랭킹(검색량 + 즐겨찾기 수 + 파이프라인 사용 수) 및 AI 툴 별점·리뷰 시연 화면을 풍성하게 구성
-- 실행 순서: 1) schema.sql -> 2) 001_add_model_columns.sql -> 3) 002_seed_models.sql -> 4) 본 파일 실행
USE ai_compass;

-- 1. 시연용 가짜 사용자 5명 추가 (이미 존재하면 건너뜀)
INSERT IGNORE INTO `Users` (`username`, `email`, `password`) VALUES
('민수', 'demo1@aicompass.kr', 'demo_only_not_for_login'),
('서연', 'demo2@aicompass.kr', 'demo_only_not_for_login'),
('지훈', 'demo3@aicompass.kr', 'demo_only_not_for_login'),
('하은', 'demo4@aicompass.kr', 'demo_only_not_for_login'),
('도윤', 'demo5@aicompass.kr', 'demo_only_not_for_login');

-- 생성된 시연용 유저 ID를 변수에 저장 (기존 유저가 있어도 외래키 오류 방지)
SELECT `user_id` INTO @u1 FROM `Users` WHERE `email` = 'demo1@aicompass.kr' LIMIT 1;
SELECT `user_id` INTO @u2 FROM `Users` WHERE `email` = 'demo2@aicompass.kr' LIMIT 1;
SELECT `user_id` INTO @u3 FROM `Users` WHERE `email` = 'demo3@aicompass.kr' LIMIT 1;
SELECT `user_id` INTO @u4 FROM `Users` WHERE `email` = 'demo4@aicompass.kr' LIMIT 1;
SELECT `user_id` INTO @u5 FROM `Users` WHERE `email` = 'demo5@aicompass.kr' LIMIT 1;

-- 2. 즐겨찾기 시연 데이터 24건 (인기 툴에 자연스럽게 집중 분포)
-- model_id 기준: 1:ChatGPT, 2:Claude, 3:Gemini, 4:Perplexity, 5:Midjourney, 9:Runway, 11:Suno, 13:Copilot, 14:Cursor, 17:Gamma, 18:DeepL, 19:NotebookLM
INSERT IGNORE INTO `Favorites` (`user_id`, `model_id`) VALUES
-- ChatGPT (5명)
(@u1, 1), (@u2, 1), (@u3, 1), (@u4, 1), (@u5, 1),
-- Cursor (4명)
(@u1, 14), (@u2, 14), (@u3, 14), (@u5, 14),
-- Claude (4명)
(@u1, 2), (@u2, 2), (@u4, 2), (@u5, 2),
-- GitHub Copilot (3명)
(@u1, 13), (@u3, 13), (@u5, 13),
-- Perplexity (2명)
(@u2, 4), (@u4, 4),
-- Midjourney (2명)
(@u2, 5), (@u4, 5),
-- 기타 인기 툴 각 1명
(@u3, 3),  -- Gemini
(@u4, 17), -- Gamma
(@u1, 19), -- NotebookLM
(@u2, 11); -- Suno

-- 3. 파이프라인 시연 데이터 6건 (block_data에 modelId와 툴 이름 함께 저장 -> 트렌드 점수 반영)
INSERT INTO `Pipelines` (`user_id`, `title`, `block_data`) VALUES
(
    @u1,
    '전공 발표 PPT 원스톱 제작 파이프라인',
    '[{"step":1,"role":"자료 조사","modelId":4,"name":"Perplexity"},{"step":2,"role":"대본·초안 작성","modelId":1,"name":"ChatGPT"},{"step":3,"role":"슬라이드 자동 생성","modelId":17,"name":"Gamma"}]'
),
(
    @u1,
    '풀스택 웹 개발 생산성 파이프라인',
    '[{"step":1,"role":"UI 프로토타입","modelId":15,"name":"v0 by Vercel"},{"step":2,"role":"아키텍처·로직 설계","modelId":2,"name":"Claude"},{"step":3,"role":"멀티파일 코드 구현","modelId":14,"name":"Cursor"},{"step":4,"role":"테스트·자동완성","modelId":13,"name":"GitHub Copilot"}]'
),
(
    @u2,
    '유튜브 쇼츠·홍보 영상 제작 파이프라인',
    '[{"step":1,"role":"기획·스크립트","modelId":1,"name":"ChatGPT"},{"step":2,"role":"키비주얼 생성","modelId":5,"name":"Midjourney"},{"step":3,"role":"영상 모션 생성","modelId":9,"name":"Runway Gen-3"},{"step":4,"role":"배경음악 작곡","modelId":11,"name":"Suno"}]'
),
(
    @u3,
    '영문 논문 리서치 및 시험 대비 파이프라인',
    '[{"step":1,"role":"관련 논문 탐색","modelId":4,"name":"Perplexity"},{"step":2,"role":"PDF 정밀 번역","modelId":18,"name":"DeepL"},{"step":3,"role":"핵심 요약·예상문제","modelId":19,"name":"NotebookLM"}]'
),
(
    @u4,
    '팀 프로젝트 회의록 및 문서화 파이프라인',
    '[{"step":1,"role":"멀티모달 자료 정리","modelId":3,"name":"Gemini"},{"step":2,"role":"장문 보고서 작성","modelId":2,"name":"Claude"},{"step":3,"role":"위키·회의록 아카이빙","modelId":16,"name":"Notion AI"}]'
),
(
    @u5,
    '백엔드 코딩 테스트 및 리팩토링 파이프라인',
    '[{"step":1,"role":"알고리즘 풀이 검증","modelId":1,"name":"ChatGPT"},{"step":2,"role":"코드 리팩토링","modelId":14,"name":"Cursor"},{"step":3,"role":"IDE 실시간 보완","modelId":13,"name":"GitHub Copilot"}]'
);

-- 4. 시연용 리뷰 데이터 15건 (별점 1~5, 200자 이내 제약 준수)
INSERT IGNORE INTO `Reviews` (`model_id`, `user_id`, `rating`, `content`) VALUES
(1,  @u1, 5, '과제 초안 작성부터 디버깅까지 가장 무난하고 빠르게 답해줘서 매일 쓰고 있습니다.'),
(1,  @u2, 4, '범용성이 뛰어나지만 무료 버전은 사용량 제한이 금방 걸리는 점이 조금 아쉽습니다.'),
(2,  @u1, 5, '한국어 문장이 정말 자연스럽고 긴 코드 맥락을 잘 이해해서 팀 프로젝트에 큰 도움이 됐습니다.'),
(2,  @u4, 5, '보고서 다듬을 때 말투가 어색하지 않고 Artifacts 미리보기 기능이 특히 편합니다.'),
(3,  @u3, 4, '구글 드라이브랑 바로 연동되고 긴 PDF 강의록을 한 번에 요약할 수 있어 유용합니다.'),
(4,  @u2, 5, '모든 답변에 출처 링크가 바로 붙어서 전공 리서치할 때 팩트체크 시간이 절반으로 줄었어요.'),
(5,  @u4, 5, '발표 자료용 일러스트 퀄리티가 압도적입니다. 다만 영문 프롬프트를 써야 잘 나옵니다.'),
(9,  @u2, 4, '이미지 한 장으로 자연스러운 움직임을 만들 수 있어서 홍보 영상 시안 작업에 좋습니다.'),
(11, @u2, 5, '한국어 가사로 노래를 만들어도 발음이 자연스럽고 BGM 퀄리티가 기대 이상입니다.'),
(13, @u3, 5, '대학생 GitHub Education 인증으로 무료 사용 중인데 IntelliJ 자동완성이 정말 편합니다.'),
(14, @u1, 5, '프로젝트 전체 폴더를 이해하고 여러 파일을 한 번에 수정해 줘서 개발 속도가 확 빨라졌습니다.'),
(14, @u5, 5, '프론트엔드랑 백엔드 연결할 때 컨텍스트를 잘 잡아줘서 디버깅 시간이 크게 줄었습니다.'),
(17, @u4, 4, '주제만 넣어도 깔끔한 발표 슬라이드 초안이 바로 나와서 팀플 발표 준비할 때 최고입니다.'),
(18, @u3, 5, '영문 전공 서적이나 논문 PDF 번역할 때 직역투가 적고 문맥이 매끄럽습니다.'),
(19, @u1, 5, '업로드한 강의 PDF 안에서만 답변해 줘서 시험 기간에 환각 걱정 없이 복습하기 좋습니다.');