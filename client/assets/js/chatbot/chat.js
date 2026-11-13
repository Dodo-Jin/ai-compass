// 챗봇 모드 화면 (chat.html): 고민 입력 → 추천 블록 → 캔버스에서 편집 → 보관함에 저장.
// chat.html?id=N 으로 열면 저장된 파이프라인을 캔버스에 불러와 수정한다(저장 시 PUT).
import { h, setBusy, validatePipeline, errorMessage } from './util.js';
import { createCanvas } from './canvas.js';
import { sendChat, listTools, getPipeline, createPipeline, updatePipeline, USE_MOCK } from './pipeline-api.js';
import { showMockUser } from './mock-user.js';

const $ = (id) => document.getElementById(id);
const log = $('chat-log');
const form = $('chat-form');
const input = $('chat-input');
const sendBtn = $('chat-send');
const titleInput = $('title-input');
const saveForm = $('save-form');
const saveBtn = $('save-btn');
const saveMsg = $('save-msg');

// 눌러서 바로 물어볼 수 있는 예시 질문 [버튼 글자, 보낼 문장]
const EXAMPLES = [
  ['블로그 글 자동화', '블로그 글을 자동으로 쓰고 싶어요'],
  ['논문 요약 흐름', '논문을 읽고 요약하는 흐름이 필요해요'],
  ['발표자료 제작', '발표자료를 빠르게 만들고 싶어요'],
];

let savedId = null; // 이미 저장했거나 불러온 파이프라인이면 id (이후 저장은 수정으로 처리)
let dirty = false; // 저장하지 않은 변경이 있는지
let busy = false;

showMockUser();
if (USE_MOCK) $('mock-badge').hidden = false;

const canvas = createCanvas($('canvas'), {
  listTools,
  onChange: markDirty,
  emptyText: '챗봇에게 고민을 말하면 추천 조합이 여기에 나타나요. 직접 만들 수도 있어요.',
  footnote: '여러 개의 AI 툴이 순서대로 연결된 ‘파이프라인’을 만드는 화면입니다',
});

// ---------------------------------------------------------------- 챗봇
function addBubble(role, ...content) {
  const bubble = h('div', { class: `chat-msg chat-${role}` }, ...content);
  log.append(bubble);
  log.scrollTop = log.scrollHeight;
  return bubble;
}

const typingDots = () => h('span', { class: 'typing', 'aria-label': '추천을 만드는 중' }, h('i'), h('i'), h('i'));

function setChatBusy(value) {
  busy = value;
  sendBtn.disabled = value;
  for (const b of $('examples').children) b.disabled = value;
}

function ask(text) {
  const message = text.trim();
  if (!message || busy) return;
  input.value = '';
  resizeInput();
  addBubble('user', message);
  request(message);
}

async function request(message) {
  if (busy) return;
  setChatBusy(true);
  const typing = addBubble('bot', typingDots());
  try {
    const res = await sendChat(message);
    typing.remove();
    showRecommendation(res);
  } catch (err) {
    typing.classList.add('chat-error');
    typing.replaceChildren(
      `추천을 불러오지 못했어요. ${errorMessage(err)} `,
      h('button', { type: 'button', class: 'btn btn-link btn-sm p-0 align-baseline', onclick: () => { typing.remove(); request(message); } }, '다시 시도'),
    );
  } finally {
    setChatBusy(false);
  }
}

function showRecommendation(res) {
  const blocks = res.blocks ?? [];
  if (blocks.length === 0) {
    addBubble('bot', res.reply || '조건에 맞는 조합을 찾지 못했어요. 상황을 조금 더 구체적으로 적어 주세요.');
    return;
  }
  const steps = blocks.map((b, i) => `${i + 1}) ${b.modelName ?? '삭제된 툴'}`).join(' → ');
  const lines = [h('div', {}, res.reply || '아래 조합을 추천드려요.'), h('div', { class: 'sub' }, steps)];

  const apply = () => {
    canvas.setBlocks(blocks);
    if (res.title && !titleInput.value.trim()) titleInput.value = res.title;
    markDirty();
  };

  if (canvas.isEmpty()) {
    apply();
    addBubble('bot', ...lines, h('div', { class: 'sub' }, '오른쪽에서 순서를 바꿔보세요.'));
  } else {
    // 이미 만든 블록이 있으면 덮어쓰지 않고 사용자가 고르게 한다
    const button = h('button', { type: 'button', class: 'pl-btn pl-btn-sm mt-2', onclick: () => { apply(); button.disabled = true; button.textContent = '적용했어요'; } }, '캔버스에 적용 (현재 내용 교체)');
    addBubble('bot', ...lines, button);
  }
}

form.addEventListener('submit', (e) => {
  e.preventDefault();
  ask(input.value);
});
// Enter 로 전송, Shift+Enter 로 줄바꿈. 한글 조합 중(isComposing)에는 전송하지 않는다
input.addEventListener('keydown', (e) => {
  if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
    e.preventDefault();
    form.requestSubmit();
  }
});
function resizeInput() {
  input.style.height = 'auto';
  input.style.height = `${Math.min(input.scrollHeight, 120)}px`;
}
input.addEventListener('input', resizeInput);

for (const [label, message] of EXAMPLES) {
  $('examples').append(h('button', { type: 'button', class: 'pl-example', onclick: () => ask(message) }, label));
}

addBubble('bot', h('div', {}, '어떤 작업을 하고 싶으세요?'), h('div', { class: 'sub' }, '고민을 자유롭게 적어주세요.'));

// ---------------------------------------------------------------- 저장
function markDirty() {
  dirty = true;
  if (saveMsg.firstChild?.classList.contains('alert-danger')) saveMsg.replaceChildren();
}

function showSaveMsg(kind, ...content) {
  saveMsg.replaceChildren(h('div', { class: `alert alert-${kind} py-2 mb-0` }, ...content));
}

function updateSaveLabel() {
  saveBtn.querySelector('.save-label').textContent = savedId == null ? '보관함에 저장' : '수정 내용 저장';
}

titleInput.addEventListener('input', markDirty);

saveForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const title = titleInput.value.trim();
  const blocks = canvas.getBlocks();
  const problem = validatePipeline(title, blocks);
  if (problem) return showSaveMsg('danger', problem);

  const wasSaved = savedId != null;
  setBusy(saveBtn, true, '저장 중…');
  try {
    const body = { title, blocks };
    const saved = wasSaved ? await updatePipeline(savedId, body) : await createPipeline(body);
    savedId = saved.pipelineId;
    dirty = false;
    showSaveMsg('success', wasSaved ? '수정 내용을 저장했어요. ' : '보관함에 저장했어요. ', h('a', { class: 'alert-link', href: '/pages/mypage/library.html' }, '보관함 보기 →'));
  } catch (err) {
    showSaveMsg('danger', `저장하지 못했어요. ${errorMessage(err)}`);
  } finally {
    setBusy(saveBtn, false);
    updateSaveLabel();
  }
});

$('reset-btn').addEventListener('click', () => {
  if (dirty && !window.confirm('작성 중인 내용을 모두 지울까요?')) return;
  canvas.setBlocks([]);
  titleInput.value = '';
  saveMsg.replaceChildren();
  savedId = null;
  dirty = false;
  updateSaveLabel();
});

// 저장하지 않은 채 페이지를 떠나려 하면 브라우저 확인 창을 띄운다
window.addEventListener('beforeunload', (e) => {
  if (dirty) {
    e.preventDefault();
    e.returnValue = '';
  }
});

// ---------------------------------------------------------------- 저장된 파이프라인 열기 (?id=N)
const idParam = new URLSearchParams(location.search).get('id');
if (idParam != null) {
  const id = Number(idParam);
  if (!Number.isInteger(id) || id <= 0) {
    showSaveMsg('danger', '잘못된 주소예요. 보관함에서 파이프라인을 다시 선택해 주세요.');
  } else {
    getPipeline(id)
      .then((p) => {
        titleInput.value = p.title;
        canvas.setBlocks(p.blocks);
        savedId = p.pipelineId;
        dirty = false;
        updateSaveLabel();
        addBubble('bot', h('div', {}, `‘${p.title}’ 파이프라인을 불러왔어요.`), h('div', { class: 'sub' }, '오른쪽에서 이어서 수정하거나, 새 조합을 물어볼 수 있어요.'));
      })
      .catch((err) => showSaveMsg('danger', `파이프라인을 불러오지 못했어요. ${errorMessage(err)} `, h('a', { class: 'alert-link', href: '/pages/mypage/library.html' }, '보관함으로')));
  }
}
