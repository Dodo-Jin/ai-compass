# API 명세

## 공통 규칙

- Base URL: `/api/v1`, 요청/응답은 모두 JSON(UTF-8). 컨트롤러 `@RequestMapping`도 `/api/v1/...`로 시작한다
- 새 API는 이 문서에 먼저 쓰고 PR에 같이 올린다
- 인증: 세션 쿠키(`JSESSIONID`). 로그인이 필요한 API는 비로그인 시 401 JSON을 반환
- 세션 키: `userId` (값은 `Users.user_id`, Integer). 서버 코드는 `SessionKeys.USER_ID` 상수를 쓴다
- 네이밍: URL은 소문자 복수형, JSON 필드는 camelCase (`model_id` → `modelId`)
- 목록 API는 `page`(1부터), `size`(기본 12, 최대 50)
- 성공 응답: `{ "success": true, "data": { ... } }`
- 실패 응답: `{ "success": false, "error": { "code": "MODEL_NOT_FOUND", "message": "..." } }`
- 상태 코드: 200 조회/수정, 201 생성, 204 삭제, 400 값 오류, 401 로그인 필요, 403 권한 없음, 404 없음, 409 중복

## 목록

| 메서드 | 경로 | 설명 | 담당 |
| --- | --- | --- | --- |
| POST | /api/v1/auth/signup | 회원가입 (email, password, username) | A |
| POST | /api/v1/auth/login | 로그인, 세션 쿠키 발급 | A |
| POST | /api/v1/auth/logout | 로그아웃, 세션 무효화 | A |
| GET | /api/v1/auth/check | 이메일/이름 중복 확인 (`?email=` 또는 `?username=`) | A |
| GET | /api/v1/users/me | 내 정보 조회 | A |
| GET | /api/v1/models | AI 툴 목록 (keyword, category, pricingType, sort, page, size) | A |
| GET | /api/v1/models/categories | 카테고리 목록 | A |
| GET | /api/v1/models/{modelId} | AI 툴 상세 (평균 별점, 리뷰 수, 내 리뷰, 즐겨찾기 여부 포함) | A |
| GET | /api/v1/models/{modelId}/reviews | 리뷰 목록 | A |
| POST | /api/v1/models/{modelId}/reviews | 리뷰 등록 (rating 1~5, content 200자 이내) | A |
| PUT | /api/v1/reviews/{reviewId} | 리뷰 수정 (작성자만) | A |
| DELETE | /api/v1/reviews/{reviewId} | 리뷰 삭제 (작성자만) | A |
| GET | /api/v1/users/me/reviews | 내가 쓴 리뷰 목록 | A |
| POST | /api/v1/pipelines/recommend-by-survey | 설문으로 AI 툴 1개 추천 (저장 안 함). 아래 메모 참고 | B |
| POST | /api/v1/pipelines/generate | 챗봇 요청으로 파이프라인 결과 생성 (저장 안 함). 아래 메모 참고 | B |
| POST | /api/v1/pipelines | 생성한 파이프라인 저장 (title, blocks) | B |
| GET | /api/v1/pipelines | 파이프라인 보관함 목록 | B |
| GET | /api/v1/pipelines/{pipelineId} | 파이프라인 상세 (blocks에 툴 이름 포함) | B |
| PATCH | /api/v1/pipelines/{pipelineId} | 파이프라인 제목 수정 | B |
| DELETE | /api/v1/pipelines/{pipelineId} | 파이프라인 삭제 | B |
| POST | /api/v1/favorites | 즐겨찾기 추가 (modelId) | C |
| DELETE | /api/v1/favorites/{modelId} | 즐겨찾기 삭제 | C |
| GET | /api/v1/favorites | 내 즐겨찾기 목록 | C |
| GET | /api/v1/favorites/compare | 즐겨찾기 비교표 데이터 (`?modelIds=1,2,3`, 2~4개) | C |

## 메모

- 파이프라인은 `Pipelines.block_data`(TEXT)에 `blocks` 배열을 JSON 문자열로 저장하고, API에서는 배열로 주고받음
- 평균 별점(`avgRating`)과 리뷰 수(`reviewCount`)는 컬럼이 없어 `Reviews`에서 계산해서 응답
- 요청/응답 예시와 에러 코드는 구현하면서 이 문서에 추가
- `recommend-by-survey` 요청: `{ "job": "marketer|developer|creator|other", "usage": "personal|team|enterprise", "budget": "free|under30k|30k-150k|over150k" }`
- `recommend-by-survey` 응답(200): `{ modelId, name, category, logoUrl, reason, priceText }`. 값 누락 400, 일치하는 툴 없음 404 (`{ message }`)
- 예산 구간은 월 가격(달러, `price_min`/`price_max`) 기준: 무료 `price_min = 0`, 3만원 이하 `price_min <= 20`, 3~15만원 `20~100`과 겹침, 15만원 이상 `price_max >= 100`
- 사용량은 `AI_Models.target_scales`(콤마 구분)에 포함되면 통과, 비어 있으면 전체 대상. 일치하는 툴이 없으면 직업 → 사용량 순으로 조건을 풀어 다시 찾음(예산은 유지)
- `generate` 요청: `{ "message": "..." }` (최대 500자). 응답(200): `{ title, reply, blocks: [{ modelId, order, purpose, modelName, category, logoUrl }] }`. 요청에 맞는 툴이 없으면 `blocks`는 빈 배열
- `generate` 오류(`{ message }`): 400 메시지 비어 있음/너무 김, 429 AI 요청 한도, 502 AI 오류·응답 해석 실패, 503 AI 키 미설정/등록된 툴 없음, 504 AI 응답 지연
- 챗봇 AI는 OpenAI 호환 API이고 `.env`의 `AI_API_KEY`, `AI_BASE_URL`, `AI_MODEL`로 설정. 응답의 modelId는 `AI_Models`에 있는 것만 통과
