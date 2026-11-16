-- AI Compass 시드 데이터: 주요 AI 툴 20개 초기 데이터 (A 담당)
-- 실행 순서: 1) db/schema.sql -> 2) db/migrations/001_add_model_columns.sql -> 3) 본 파일 실행
USE ai_compass;

INSERT INTO `AI_Models` (
    `name`, `category`, `description`, `pricing_type`, `ranking_score`,
    `site_url`, `logo_url`, `tags`, `price_text`,
    `price_min_usd`, `price_max_usd`, `target_scales`,
    `input_types`, `korean_support`, `strengths`, `limitations`, `search_count`
) VALUES
(
    'ChatGPT', '텍스트/LLM',
    'OpenAI의 대화형 생성 AI로 문서 작성, 코딩, 데이터 분석, 음성 대화까지 폭넓은 작업을 지원합니다.',
    'FREEMIUM', 98,
    'https://chatgpt.com', 'https://cdn.simpleicons.org/openai',
    '대화,글쓰기,번역,코딩,데이터분석', '무료 / Plus 월 $20 / Pro 월 $200',
    0.00, 200.00, 'PERSONAL,TEAM,ENTERPRISE',
    '텍스트, 이미지, 음성, 파일', TRUE,
    '방대한 플러그인·GPTs 생태계와 뛰어난 범용 추론 및 멀티모달 처리 능력',
    '무료 플랜은 최신 모델 호출 횟수에 제한이 있으며 간혹 환각(Hallucination)이 발생할 수 있음', 150
),
(
    'Claude', '텍스트/LLM',
    'Anthropic이 개발한 AI 어시스턴트로 자연스러운 한국어 문장력과 긴 문맥(Context) 분석, 정교한 코딩에 강점이 있습니다.',
    'FREEMIUM', 96,
    'https://claude.ai', 'https://cdn.simpleicons.org/anthropic',
    '장문요약,코딩,글쓰기,아티팩트,문서분석', '무료 / Pro 월 $20 / Team 월 $30',
    0.00, 30.00, 'PERSONAL,TEAM,ENTERPRISE',
    '텍스트, 이미지, 문서(PDF/코드)', TRUE,
    '최대 200K 토큰의 긴 문맥 처리와 자연스러운 한국어 작성, 실시간 코드 프리뷰(Artifacts) 제공',
    '실시간 웹 이미지 생성 기능이 없으며 무료 사용량 제한이 빠르게 소진될 수 있음', 142
),
(
    'Gemini', '텍스트/LLM',
    'Google의 네이티브 멀티모달 AI로 Google Workspace 연동과 실시간 검색, 대용량 영상·문서 분석을 지원합니다.',
    'FREEMIUM', 94,
    'https://gemini.google.com', 'https://cdn.simpleicons.org/googlegemini',
    '구글연동,멀티모달,검색,요약,번역', '무료 / Advanced 월 $19.99',
    0.00, 19.99, 'PERSONAL,TEAM,ENTERPRISE',
    '텍스트, 이미지, 오디오, 비디오, 문서', TRUE,
    'Google Docs·Gmail·Drive와 매끄러운 연동 및 최대 1M~2M 토큰의 초대형 컨텍스트 창 지원',
    '창의적 소설 작성이나 특수 도메인에서는 프롬프트에 따라 응답 편차가 발생할 수 있음', 135
),
(
    'Perplexity', '검색/리서치',
    '실시간 웹 검색과 LLM을 결합해 모든 답변에 정확한 출처 링크와 인용을 함께 제공하는 AI 검색 엔진입니다.',
    'FREEMIUM', 93,
    'https://www.perplexity.ai', 'https://cdn.simpleicons.org/perplexity',
    '실시간검색,출처제공,논문리서치,팩트체크', '무료 / Pro 월 $20 / Enterprise 월 $40',
    0.00, 40.00, 'PERSONAL,TEAM,ENTERPRISE',
    '텍스트, 파일, 이미지', TRUE,
    '실시간 웹 페이지 및 학술 논문 기반의 명확한 출처 표기로 환각 현상을 최소화함',
    '창작 글쓰기나 긴 스토리텔링보다는 정보 탐색 및 요약에 특화되어 있음', 128
),
(
    'Midjourney', '이미지 생성',
    '텍스트 프롬프트만으로 예술적이고 극사실적인 고해상도 이미지를 생성하는 대표적인 이미지 생성 AI입니다.',
    'PAID', 95,
    'https://www.midjourney.com', 'https://www.midjourney.com/apple-touch-icon.png',
    '아트워크,실사풍,컨셉아트,디자인,일러스트', 'Basic 월 $10 ~ Mega 월 $120',
    10.00, 120.00, 'PERSONAL,TEAM',
    '텍스트, 참조 이미지', FALSE,
    '독보적인 예술적 디테일과 조명·질감 표현력, 스타일 일관성(--sref, --cref) 기능 지원',
    '무료 체험이 기본적으로 제공되지 않으며 한글 프롬프트 인식률이 낮아 영문 입력이 권장됨', 140
),
(
    'DALL-E 3', '이미지 생성',
    'OpenAI의 이미지 생성 모델로 복잡한 자연어 지시사항과 이미지 내 영문 텍스트 렌더링을 정확하게 반영합니다.',
    'FREEMIUM', 88,
    'https://openai.com/dall-e-3', 'https://cdn.simpleicons.org/openai',
    '일러스트,로고,포스터,대화형수정', '무료(일일 제한) / Plus 월 $20',
    0.00, 20.00, 'PERSONAL,TEAM',
    '텍스트', TRUE,
    'ChatGPT와 대화하며 한국어로도 손쉽게 프롬프트를 다듬고 원하는 구도를 수정할 수 있음',
    '세밀한 포토리얼리즘이나 특정 부위만 정밀하게 제어하는 기능은 전문 툴 대비 제한적임', 95
),
(
    'Stable Diffusion', '이미지 생성',
    'Stability AI의 오픈소스 기반 이미지 생성 모델로 로컬 설치 및 LoRA·ControlNet 커스터마이징이 자유롭습니다.',
    'FREE', 89,
    'https://stability.ai', 'https://stability.ai/favicon.ico',
    '오픈소스,로컬설치,커스텀모델,ControlNet', '오픈소스 무료 ($0)',
    0.00, 0.00, 'PERSONAL,TEAM,ENTERPRISE',
    '텍스트, 이미지, 마스크', FALSE,
    '제한 없는 무료 로컬 생성과 방대한 커뮤니티 체크포인트·LoRA·포즈 제어 확장성',
    '고성능 GPU(VRAM 8GB 이상)가 필요하며 초기 WebUI/ComfyUI 환경 구축 난이도가 높음', 102
),
(
    'Leonardo AI', '이미지 생성',
    '게임 에셋, 캐릭터 디자인, 일러스트 생성에 특화된 올인원 웹 기반 AI 그래픽 제작 플랫폼입니다.',
    'FREEMIUM', 85,
    'https://leonardo.ai', 'https://leonardo.ai/favicon.ico',
    '게임에셋,캐릭터,텍스처,배경제거,캔버스편집', '무료 / 유료 월 $12 ~ $60',
    0.00, 60.00, 'PERSONAL,TEAM',
    '텍스트, 이미지, 스케치', FALSE,
    '매일 무료 크레딧을 제공하며 실시간 캔버스 편집과 다양한 프리셋 파인튜닝 모델을 지원함',
    '한국어 프롬프트 지원이 미흡하며 무료 플랜에서는 생성된 이미지가 공개 갤러리에 노출됨', 78
),
(
    'Runway Gen-3', '영상 생성',
    '텍스트나 이미지를 입력해 영화 같은 고품질 비디오 클립을 생성하고 모션 브러시로 움직임을 제어하는 영상 AI입니다.',
    'FREEMIUM', 91,
    'https://runwayml.com', 'https://runwayml.com/favicon.ico',
    '텍스트투비디오,이미지투비디오,영상편집,VFX', '무료 체험 / Standard 월 $15 ~ Unlimited 월 $95',
    0.00, 95.00, 'PERSONAL,TEAM,ENTERPRISE',
    '텍스트, 이미지, 비디오', FALSE,
    '카메라 워크 제어, 모션 브러시, 립싱크 등 전문가용 영상 제작 제어 도구를 폭넓게 제공',
    '고해상도 영상 생성 시 크레딧 소모가 빠르며 긴 러닝타임 영상을 한 번에 만들기 어려움', 110
),
(
    'Sora', '영상 생성',
    'OpenAI의 비디오 생성 모델로 물리 법칙과 공간감을 반영한 최대 1080p 고화질 시네마틱 영상을 생성합니다.',
    'PAID', 90,
    'https://sora.com', 'https://cdn.simpleicons.org/openai',
    '시네마틱영상,스토리보드,리믹스,비디오생성', 'Plus 월 $20 / Pro 월 $200',
    20.00, 200.00, 'PERSONAL,TEAM',
    '텍스트, 이미지, 비디오', TRUE,
    '스토리보드 기반 시간축 제어와 자연스러운 배경·피사체 일관성 유지',
    '복잡한 다중 인물 상호작용이나 빠른 물리 동작에서는 부자연스러운 왜곡이 나타날 수 있음', 115
),
(
    'Suno', '오디오/음악',
    '가사와 음악 스타일만 입력하면 보컬과 악기 반주가 포함된 완성도 높은 노래를 1분 만에 작곡해 주는 음악 AI입니다.',
    'FREEMIUM', 92,
    'https://suno.com', 'https://suno.com/favicon.ico',
    'AI작곡,보컬생성,BGM,한국어노래,작사', '무료 / Pro 월 $10 / Premier 월 $30',
    0.00, 30.00, 'PERSONAL,TEAM',
    '텍스트(가사/스타일), 오디오', TRUE,
    '한국어 가사 발음이 매우 자연스러우며 K-Pop, 발라드, 재즈 등 다양한 장르를 고품질로 생성',
    '무료 플랜에서 생성한 곡은 상업적 이용 권한이 없으며 개별 악기 트랙(Stem) 정밀 편집이 제한됨', 120
),
(
    'ElevenLabs', '오디오/음악',
    '억양과 감정선까지 살린 자연스러운 텍스트 음성 변환(TTS)과 다국어 더빙, 보이스 클로닝을 제공하는 오디오 AI입니다.',
    'FREEMIUM', 89,
    'https://elevenlabs.io', 'https://elevenlabs.io/favicon.ico',
    'TTS,성우보이스,더빙,음성복제,효과음', '무료 / Starter 월 $5 ~ Scale 월 $330',
    0.00, 330.00, 'PERSONAL,TEAM,ENTERPRISE',
    '텍스트, 오디오', TRUE,
    '실제 성우와 구별하기 힘든 감정 표현력과 29개 이상 다국어 자동 더빙 및 효과음 생성 지원',
    '한국어 고유명사나 숫자 읽기에서 간헐적으로 억양이 어색할 수 있으며 무료 글자 수가 적음', 88
),
(
    'GitHub Copilot', '코딩/개발',
    'VS Code와 IntelliJ 등 IDE에 직접 통합되어 실시간 코드 자동완성, 리팩토링, 테스트 코드 작성을 돕는 AI 페어 프로그래머입니다.',
    'FREEMIUM', 95,
    'https://github.com/features/copilot', 'https://cdn.simpleicons.org/githubcopilot',
    '코드자동완성,IDE연동,디버깅,테스트생성,학생무료', '무료 티어 / Pro 월 $10 / Enterprise 월 $39',
    0.00, 39.00, 'PERSONAL,TEAM,ENTERPRISE',
    '코드, 텍스트, 터미널 로그', TRUE,
    'IntelliJ·VS Code 완벽 호환 및 GitHub 대학교 학생 인증(Education) 시 Pro 기능 무료 제공',
    '전체 프로젝트 아키텍처 설계보다는 파일 단위 구현에 강하며 보안 취약점 코드는 개발자 검토가 필요함', 145
),
(
    'Cursor', '코딩/개발',
    'VS Code 기반으로 설계된 AI 전용 코드 에디터로 프로젝트 전체 코드베이스를 이해하고 다중 파일을 동시에 수정합니다.',
    'FREEMIUM', 96,
    'https://www.cursor.com', 'https://www.cursor.com/favicon.ico',
    'AI코드에디터,멀티파일수정,코드베이스검색,Composer', '무료(Hobby) / Pro 월 $20 / Business 월 $40',
    0.00, 40.00, 'PERSONAL,TEAM,ENTERPRISE',
    '코드, 텍스트, 이미지, 문서 URL', TRUE,
    '전체 저장소 컨텍스트 인덱싱과 Composer/Agent 모드를 통한 다중 파일 일괄 생성·수정 생산성',
    '기존 IntelliJ 단축키·플러그인 환경에 익숙한 Java 개발자는 에디터 전환 적응 기간이 필요함', 148
),
(
    'v0 by Vercel', '코딩/개발',
    '프롬프트나 화면 캡처를 입력하면 Tailwind CSS와 React/Next.js 기반의 깔끔한 UI 컴포넌트 코드를 즉시 생성해 줍니다.',
    'FREEMIUM', 87,
    'https://v0.dev', 'https://cdn.simpleicons.org/vercel',
    '프론트엔드,UI생성,웹디자인,React,프로토타이핑', '무료 / Premium 월 $20 / Team 월 $30',
    0.00, 30.00, 'PERSONAL,TEAM,ENTERPRISE',
    '텍스트, 디자인 캡처 이미지, Figma', TRUE,
    '실시간 화면 미리보기와 함께 바로 복사해 쓸 수 있는 고품질 프론트엔드 UI 코드를 빠르게 제작',
    'React·Tailwind 생태계 중심이라 순수 HTML/Bootstrap 프로젝트에는 변환 과정이 필요함', 84
),
(
    'Notion AI', '업무/생산성',
    '노션 워크스페이스 내부에서 회의록 요약, 문서 초안 작성, 번역, 데이터베이스 자동 채우기를 수행하는 생산성 AI입니다.',
    'FREEMIUM', 88,
    'https://www.notion.so/product/ai', 'https://cdn.simpleicons.org/notion',
    '문서정리,회의록요약,워크스페이스,번역,일정관리', '무료 체험 / AI 애드온 월 $10',
    0.00, 10.00, 'PERSONAL,TEAM,ENTERPRISE',
    '텍스트, 노션 페이지/데이터베이스', TRUE,
    '기존 노션에 축적된 팀 문서·위키를 바탕으로 질문에 답하는 Q&A 기능과 매끄러운 문서 편집 연동',
    '노션 기본 요금제와 별도로 AI 애드온 비용이 과금되며 노션 외부 자료 검색 능력은 제한적임', 92
),
(
    'Gamma', '업무/생산성',
    '주제나 문서 초안만 입력하면 슬라이드 디자인과 레이아웃, 시각 자료가 포함된 발표 자료(PPT)를 자동으로 완성합니다.',
    'FREEMIUM', 90,
    'https://gamma.app', 'https://gamma.app/favicon.ico',
    'PPT제작,발표자료,문서시각화,슬라이드생성', '무료 / Plus 월 $10 / Pro 월 $20',
    0.00, 20.00, 'PERSONAL,TEAM',
    '텍스트, 워드/PDF 문서, URL', TRUE,
    '레이아웃 정렬 스트레스 없이 몇 초 만에 깔끔한 웹 슬라이드를 만들고 PPTX/PDF로 내보내기 가능',
    'PPTX로 변환해 다운로드할 때 일부 폰트나 세부 레이아웃이 깨져 후가공이 필요할 수 있음', 105
),
(
    'DeepL', '번역/글쓰기',
    '문맥과 뉘앙스를 정교하게 파악해 자연스러운 다국어 번역과 문장 교정(DeepL Write)을 제공하는 AI 번역기입니다.',
    'FREEMIUM', 91,
    'https://www.deepl.com', 'https://www.deepl.com/img/favicon/favicon_96.png',
    '인공지능번역,논문번역,문장교정,PDF번역', '무료 / Starter 월 $8.74 ~ Ultimate 월 $57.49',
    0.00, 57.49, 'PERSONAL,TEAM,ENTERPRISE',
    '텍스트, 문서 파일(PDF, DOCX, PPTX)', TRUE,
    '직역투를 벗어난 매끄러운 한국어·영어·일본어 번역 품질과 원본 서식을 유지하는 문서 통번역 지원',
    '무료 버전은 글자 수 및 파일 업로드 크기 제한이 있으며 전문 용어집(Glossary) 개수가 제한됨', 112
),
(
    'NotebookLM', '검색/리서치',
    'Google이 제공하는 맞춤형 리서치 노트로 내가 업로드한 전공 서적 PDF, 강의록, 웹 링크만을 근거로 요약·질의응답을 수행합니다.',
    'FREE', 92,
    'https://notebooklm.google.com', 'https://cdn.simpleicons.org/google',
    '전공공부,논문요약,시험대비,오디오오버뷰,출처기반', '무료 ($0)',
    0.00, 0.00, 'PERSONAL,TEAM',
    'PDF, 텍스트, Google Docs, 유튜브 URL, 오디오', TRUE,
    '업로드한 자료 안에서만 답변하고 정확한 인용 위치를 짚어주어 환각이 거의 없으며 무료로 제공됨',
    '내가 올리지 않은 외부 최신 정보를 스스로 넓게 검색해 추론하는 기능은 제한적임', 118
),
(
    'Wrtn (뤼튼)', '텍스트/LLM',
    '국내 사용자 환경에 맞춰 다양한 최신 AI 모델과 레포트 작성, 자기소개서 첨삭 등 한국형 AI 도구를 무료로 제공하는 플랫폼입니다.',
    'FREE', 86,
    'https://wrtn.ai', 'https://wrtn.ai/favicon.ico',
    '한국형AI,과제도우미,자기소개서,무료챗봇', '무료 ($0)',
    0.00, 0.00, 'PERSONAL',
    '텍스트, 이미지, 문서', TRUE,
    '한국어 UI와 대학생 과제·자소서·요약 특화 템플릿을 별도 결제 없이 무료로 손쉽게 활용 가능',
    '고난도 프로그래밍이나 대규모 컨텍스트 엔지니어링 작업에서는 원본 유료 모델 대비 제약이 있음', 90
);