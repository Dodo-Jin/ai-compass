# 🧭 AI Compass
AI 툴 디렉토리 + 챗봇 파이프라인 추천 플랫폼 (Bootstrap 5 / FlexStart, Spring Boot(Java 21) + MySQL)

## 사전 설치

| 도구 | 버전 |
| --- | --- |
| JDK | 21 (Eclipse Temurin) |
| MySQL | 8.0.x (포트 3306) |
| IntelliJ IDEA | 최신 버전 |
| Git | 최신 버전 |

설치부터 첫 실행까지의 자세한 순서는 [환경 세팅 가이드](https://claude.ai/artifact/QmsVoFDsCTjoNGhbAQ2tSu#b178d554-7514)를 따라 주세요.

## 처음 한 번만 할 일

1. `ai_compass` DB를 만들고 `db/`의 SQL을 실행합니다. (`utf8mb4`)
2. 저장소 루트에서 `.env.example`을 `.env`로 복사하고 `DB_PASSWORD`에 본인 MySQL root 비밀번호를 입력합니다.
3. IntelliJ에서 저장소 루트가 아니라 `server` 폴더를 열고 `AiCompassApplication`을 실행합니다.

## 실행
```bash
cp .env.example .env      # 값 채우기
cd server && gradle bootRun     # gradle 미설치 시 IntelliJ에서 server 폴더를 Gradle 프로젝트로 열고 AiCompassApplication 실행
```
`http://localhost:3000` 에서 `client/` 가 정적으로 서빙됩니다.

## 담당
| 담당 | 영역 |
|---|---|
| A | db/, 로그인·가입(auth), AI 디렉토리·상세·리뷰 |
| B | 챗봇, 파이프라인 시각화, 마이페이지 보관함 |
| C | 트렌드 피드, 마이페이지 즐겨찾기 비교표, 홈, 공용 코드(`common` 패키지·`api.js`) |

## 규칙
- `main` 직접 push 금지, `develop` 에서 `feature/<이름>-<기능>` 브랜치로 작업 후 PR
- 내 폴더만 수정, 공용 파일(`partials/`, `common.*`, `config/`)은 작은 PR로 먼저 머지
- 새 API: `server/src/main/java/com/aicompass/<기능>/` 패키지 안에 Controller/Service/Repository 를 추가 (기능 패키지 밖은 수정 금지)
- FlexStart 원본은 `client/assets/vendor/` 에 넣고 수정하지 않기

## 공용 코드 사용법

**서버** (`com.aicompass.common`)
```java
// 성공 응답: { "success": true, "data": ... }
return ApiResponse.ok(data);
return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));

// 실패: 던지기만 하면 GlobalExceptionHandler 가 { "success": false, "error": { code, message } } 로 응답
throw ApiException.notFound("MODEL_NOT_FOUND", "AI 툴을 찾을 수 없습니다.");

// 로그인 사용자 (세션 키는 SessionKeys.USER_ID = "userId")
Integer userId = LoginUser.require(session);  // 비로그인이면 401 LOGIN_REQUIRED
Integer userId = LoginUser.find(session);     // 비로그인이면 null
```

**화면** (`client/assets/js/common/api.js`)
```js
import { api, ApiError } from '/assets/js/common/api.js';
const page = await api('/models?page=1');      // /api/v1 은 자동, { success, data } 면 data 만 돌려줌
await api('/favorites/3', { method: 'DELETE' }); // 204 면 null
try { await api('/favorites', { method: 'POST', body: { modelId: 3 } }); }
catch (e) { if (e.status === 401) location.href = '/pages/auth/login.html'; } // e.code, e.message 도 있음
```
