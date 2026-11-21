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

## 챗봇 AI 키 발급 방법
챗봇(`POST /api/v1/pipelines/generate`)은 Google **Gemini 무료 티어**를 OpenAI 호환 API로 호출합니다. 키가 없으면 챗봇만 `503 "챗봇이 아직 설정되지 않았어요"`가 나고, 설문·디렉토리·보관함 등 나머지 기능은 그대로 동작합니다.

### 1. 키 발급 (무료)
1. https://aistudio.google.com/apikey 에 Google 계정으로 로그인합니다.
2. **키 만들기(Create API key)** 를 누르고, 키 이름은 아무거나, 프로젝트는 기본값 `Default Gemini Project` 를 그대로 둡니다. ("가져온 프로젝트"는 사용량·한도가 묶이는 Google Cloud 프로젝트입니다.)
3. 키 목록에서 **결제 등급이 "무료 등급"** 인지 확인합니다. **"결제 설정"은 누르지 마세요.** (누르면 유료로 전환됩니다.)
4. 만들어진 키를 복사합니다. 키 앞부분은 발급 시점에 따라 `AIza…` 또는 `AQ…` 로 다를 수 있으니 **받은 그대로** 쓰면 됩니다.

### 2. `.env` 설정
저장소 루트의 `.env`(없으면 `.env.example` 복사)에 아래 3줄을 넣습니다. 값에는 따옴표나 앞뒤 공백을 넣지 않습니다.
```
AI_API_KEY=발급받은_키
AI_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai
AI_MODEL=gemini-flash-lite-latest
```
`.env`는 서버를 켤 때 읽으므로 **수정 후 서버를 재시작**해야 합니다.

### 3. 키가 동작하는지 먼저 확인 (PowerShell)
```powershell
$key = "발급받은_키"
$body = '{"model":"gemini-flash-lite-latest","messages":[{"role":"user","content":"hi"}]}'
try {
  $r = Invoke-RestMethod -Uri "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions" -Method Post -Headers @{Authorization="Bearer $key"} -ContentType "application/json" -Body $body
  "OK: " + $r.choices[0].message.content
} catch {
  $_.Exception.Response.StatusCode.value__
  (New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())).ReadToEnd()
}
```
`OK: …` 가 나오면 정상입니다. (`curl`은 PowerShell에서 다른 명령의 별칭이라 위처럼 `Invoke-RestMethod` 를 씁니다.)

### 오류가 났을 때
| 증상 | 원인과 해결 |
| --- | --- |
| 401 / 403 | 키가 틀렸거나 복사가 잘못됨. 앞뒤 공백·따옴표를 확인하고, 안 되면 키를 삭제하고 다시 발급 |
| 402 | 이 프로젝트는 무료로 쓸 수 없는 상태(결제 필요). 새 프로젝트로 키를 다시 발급 |
| 404 | **모델 이름 문제.** 모델은 자주 종료·교체됩니다. 아래 명령으로 지금 쓸 수 있는 이름을 확인해 `AI_MODEL` 을 바꾸세요. `-latest` 가 붙은 별칭은 종료돼도 이름을 바꿀 필요가 없어 권장합니다 |
| 429 | 무료 한도 초과. 잠시 후 다시 시도 |
| 챗봇 화면의 "AI 서버 오류"(502) / "응답이 늦어요"(504) | Google 쪽 일시적 과부하·지연. 잠시 후 "다시 시도". 계속되면 `AI_MODEL` 을 다른 모델로 바꿔 보세요 |

사용 가능한 모델 이름 확인:
```powershell
(Invoke-RestMethod -Uri "https://generativelanguage.googleapis.com/v1beta/models?pageSize=100" -Headers @{"x-goog-api-key"=$key}).models | Where-Object { $_.supportedGenerationMethods -contains "generateContent" } | ForEach-Object { $_.name }
```
출력의 `models/` 를 뺀 이름(예: `gemini-flash-lite-latest`)을 `AI_MODEL` 에 넣습니다.

### 주의
- **키는 비밀번호처럼 다룹니다.** `.env`는 `.gitignore` 에 있어 커밋되지 않습니다. 채팅·PR·이슈·스크린샷에 키를 붙여 넣지 마세요. 노출됐다면 AI Studio에서 그 키를 삭제하고 새로 발급합니다.
- **팀원마다 각자 키가 필요합니다.** `.env` 는 공유되지 않습니다.
- 챗봇은 **로그인한 사용자만** 쓸 수 있습니다(AI 호출 한도 보호).
- 무료 티어는 한도가 있고, 입력한 내용이 Google의 서비스 개선에 쓰일 수 있습니다(정책은 AI Studio 약관 기준). 개인정보·민감한 내용은 챗봇에 입력하지 마세요.

### 다른 AI로 바꾸려면
OpenAI 호환(`/chat/completions`) API라면 코드 수정 없이 `.env` 의 `AI_API_KEY`, `AI_BASE_URL`, `AI_MODEL` 3개만 바꾸면 됩니다. (예: Groq는 `AI_BASE_URL=https://api.groq.com/openai/v1`, 모델 이름은 Groq 콘솔에서 확인)


## 담당
| 담당 | 영역 |
|---|---|
| A | db/, 로그인·가입(auth), AI 디렉토리·상세·리뷰 |
| B | 챗봇, 파이프라인 시각화, 마이페이지 보관함 |
| C | 트렌드 피드, 마이페이지 즐겨찾기 비교표, 공용 코드(`common` 패키지·`api.js`) |

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

## 자동 테스트 (GitHub Actions)

PR 을 올리거나 push 하면 `.github/workflows/server-test.yml` 이 서버를 빌드하고 `./gradlew test` 를 돌립니다.
- 머지 버튼 위에 ✅(통과) / ❌(실패) 가 보입니다. ❌ 이면 머지하지 말고 고쳐 주세요.
- 실패하면 오류 요약이 PR 댓글로 달립니다. 전체 기록은 PR 의 Checks 탭에서 볼 수 있습니다.
- DB 없이 도는 테스트만 있어서 MySQL 설정은 필요 없습니다.
