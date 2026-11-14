// 마이페이지 > 파이프라인 보관함 (library.html)
// 검색 · 정렬 · 페이지네이션 · 카드/리스트 보기 · 열기(chat.html?id=N) · 삭제(확인 모달)
import { h, formatDate, debounce, errorMessage, setBusy } from './util.js';
import { listPipelines, deletePipeline, USE_MOCK } from './pipeline-api.js';
import { showMockUser, MOCK_USER } from './mock-user.js';

const SIZE = 12;
const SORTS = ['latest', 'oldest', 'title'];
const VIEW_KEY = 'aicompass.library.view';
const MAX_CHIPS = 4; // 카드에는 앞의 4개만 보여주고 나머지는 +N 으로 줄인다

const $ = (id) => document.getElementById(id);
const content = $('content');
const pager = $('pager');

const params = new URLSearchParams(location.search);
const state = {
  q: params.get('q') ?? '',
  sort: SORTS.includes(params.get('sort')) ? params.get('sort') : 'latest',
  page: Math.max(parseInt(params.get('page') ?? '0', 10) || 0, 0),
  view: readView(),
};
let lastRes = null; // 마지막으로 받은 목록 (보기 방식만 바꿀 때 다시 요청하지 않으려고 보관)
let totalAll = null; // 검색어 없이 본 전체 개수 (프로필의 "저장한 파이프라인")
let requestSeq = 0; // 느린 응답이 최신 응답을 덮어쓰지 않게 하는 번호
let pendingDelete = null;

function readView() {
  try {
    return localStorage.getItem(VIEW_KEY) === 'list' ? 'list' : 'card';
  } catch {
    return 'card';
  }
}

// ---------------------------------------------------------------- 프로필 (로그인 연동 전)
await window.layoutReady; // 프로필 카드가 공용 partial 이라 삽입이 끝난 뒤에 채운다
showMockUser();
if (USE_MOCK) {
  $('mock-badge').hidden = false;
  $('profile-name').textContent = MOCK_USER.username;
  $('profile-email').textContent = MOCK_USER.email;
  $('profile-email').hidden = false;
  // 즐겨찾기·리뷰는 다른 담당자의 API 라서 목 모드에서만 임시 숫자를 보여준다
  $('stat-favorites').textContent = '8';
  $('stat-reviews').textContent = '3';
}

// ---------------------------------------------------------------- 컨트롤
$('q').value = state.q;
$('sort').value = state.sort;
syncViewButtons();

$('q').addEventListener(
  'input',
  debounce(() => {
    state.q = $('q').value.trim();
    state.page = 0;
    load();
  }, 300),
);
$('sort').addEventListener('change', () => {
  state.sort = $('sort').value;
  state.page = 0;
  load();
});
for (const [id, view] of [['view-card', 'card'], ['view-list', 'list']]) {
  $(id).addEventListener('click', () => {
    state.view = view;
    try {
      localStorage.setItem(VIEW_KEY, view);
    } catch {
      // 저장소를 못 쓰면 이번 방문에서만 유지된다
    }
    syncViewButtons();
    if (lastRes) render();
  });
}

function syncViewButtons() {
  for (const [id, view] of [['view-card', 'card'], ['view-list', 'list']]) {
    $(id).classList.toggle('active', state.view === view);
    $(id).setAttribute('aria-pressed', String(state.view === view));
  }
}

function syncUrl() {
  const next = new URLSearchParams();
  if (state.q) next.set('q', state.q);
  if (state.sort !== 'latest') next.set('sort', state.sort);
  if (state.page > 0) next.set('page', String(state.page));
  const qs = next.toString();
  history.replaceState(null, '', qs ? `?${qs}` : location.pathname);
}

// ---------------------------------------------------------------- 불러오기
async function load() {
  const mine = ++requestSeq;
  syncUrl();
  content.setAttribute('aria-busy', 'true');
  if (!lastRes) renderState('loading');
  try {
    const res = await listPipelines({ q: state.q, sort: state.sort, page: state.page, size: SIZE });
    if (mine !== requestSeq) return;
    // 삭제 등으로 현재 페이지가 비었으면 이전 페이지로 물러난다
    if (res.items.length === 0 && state.page > 0) {
      state.page = Math.max(Math.min(res.totalPages - 1, state.page - 1), 0);
      return load();
    }
    lastRes = res;
    if (!state.q) totalAll = res.totalElements;
    render();
  } catch (err) {
    if (mine !== requestSeq) return;
    renderState('error', err);
  } finally {
    if (mine === requestSeq) content.removeAttribute('aria-busy');
  }
}

// ---------------------------------------------------------------- 그리기
function render() {
  const res = lastRes;
  $('stat-pipelines').textContent = String(totalAll ?? res.totalElements);
  $('summary').textContent = `총 ${res.totalElements}개` + (state.q ? ` · ‘${state.q}’ 검색 결과` : '');

  if (res.totalElements === 0) {
    pager.replaceChildren();
    return renderState(state.q ? 'no-result' : 'empty');
  }

  const onLastPage = res.page >= res.totalPages - 1;
  if (state.view === 'card') {
    content.replaceChildren(
      // 카드 최소 폭을 기준으로 칸 수가 자동으로 정해진다 (노트북 1440px 에서는 2열, 더 넓으면 3열)
      h(
        'div',
        { class: 'pl-grid' },
        res.items.map(renderCard),
        onLastPage
          ? h('a', { class: 'pl-new-tile', href: '/pages/chatbot/chat.html' }, h('i', { class: 'bi bi-plus-lg', 'aria-hidden': 'true' }), h('strong', {}, '새 파이프라인 만들기'), h('small', {}, '파이프라인 설계 화면으로 이동'))
          : null,
      ),
    );
  } else {
    content.replaceChildren(...res.items.map(renderRow));
  }
  renderPager(res);
}

function chips(blocks) {
  if (blocks.length === 0) return h('div', { class: 'pl-chips' }, h('span', { class: 'pl-chip text-muted' }, '블록 없음'));
  const shown = blocks.slice(0, MAX_CHIPS);
  const more = blocks.length - shown.length;
  const arrow = () => h('i', { class: 'bi bi-arrow-right pl-chip-arrow', 'aria-hidden': 'true' });
  // 화살표는 앞의 칩에 붙여서 한 덩어리로 줄바꿈된다 (화살표만 줄 끝/처음에 남지 않게)
  const steps = shown.map((b, i) => h('span', { class: 'pl-step' }, h('span', { class: 'pl-chip' }, b.modelName ?? '삭제된 툴'), i < shown.length - 1 || more > 0 ? arrow() : null));
  if (more > 0) steps.push(h('span', { class: 'pl-step' }, h('span', { class: 'pl-chip text-muted' }, `+${more}`)));
  return h('div', { class: 'pl-chips', 'aria-label': `사용 툴 ${blocks.length}개` }, steps);
}

const meta = (p) => `${p.blocks.length}단계 · ${formatDate(p.createdAt)}`;

function actions(p) {
  return h(
    'div',
    { class: 'pl-card-actions' },
    h('a', { class: 'pl-btn pl-btn-sm', href: `/pages/chatbot/chat.html?id=${p.pipelineId}`, 'aria-label': `${p.title} 열기 또는 수정` }, '열기 / 수정'),
    h('button', { type: 'button', class: 'pl-btn pl-btn-sm pl-btn-danger', 'aria-label': `${p.title} 삭제`, onclick: () => askDelete(p) }, '삭제'),
  );
}

function renderCard(p) {
  return h('article', { class: 'pl-card' }, h('h3', { title: p.title }, p.title), chips(p.blocks), h('p', { class: 'pl-meta' }, meta(p)), actions(p));
}

function renderRow(p) {
  return h('article', { class: 'pl-row' }, h('h3', { title: p.title }, p.title), chips(p.blocks), h('span', { class: 'pl-meta' }, meta(p)), actions(p));
}

function renderPager(res) {
  if (res.totalPages <= 1) return pager.replaceChildren();
  const current = res.page;
  const last = res.totalPages - 1;
  const start = Math.max(0, Math.min(current - 2, last - 4)); // 번호는 최대 5개만
  const end = Math.min(last, start + 4);
  const go = (page) => {
    state.page = page;
    load();
    $('summary').scrollIntoView({ block: 'start', behavior: 'smooth' });
  };
  const item = (label, ariaLabel, page, { disabled = false, active = false } = {}) =>
    h('li', { class: active ? 'active' : '' }, h('button', { type: 'button', 'aria-label': ariaLabel, 'aria-current': active ? 'page' : null, disabled, onclick: () => go(page) }, label));

  const pages = [];
  for (let i = start; i <= end; i++) pages.push(item(String(i + 1), `${i + 1}페이지`, i, { active: i === current }));
  pager.replaceChildren(
    item(h('i', { class: 'bi bi-chevron-left', 'aria-hidden': 'true' }), '이전 페이지', current - 1, { disabled: current === 0 }),
    ...pages,
    item(h('i', { class: 'bi bi-chevron-right', 'aria-hidden': 'true' }), '다음 페이지', current + 1, { disabled: current >= last }),
  );
}

function renderState(kind, err) {
  pager.replaceChildren();
  const box = (icon, title, ...rest) => h('div', { class: 'pl-state' }, h('i', { class: `bi bi-${icon}`, 'aria-hidden': 'true' }), h('strong', {}, title), ...rest);
  if (kind === 'loading') {
    content.replaceChildren(h('div', { class: 'pl-state' }, h('div', { class: 'spinner-border text-primary mb-3', 'aria-hidden': 'true' }), h('div', {}, '불러오는 중…')));
  } else if (kind === 'error') {
    $('summary').textContent = '';
    content.replaceChildren(
      box('exclamation-circle', '보관함을 불러오지 못했어요', h('p', { class: 'mb-3' }, errorMessage(err)), h('button', { type: 'button', class: 'pl-btn pl-btn-sm', onclick: () => { lastRes = null; load(); } }, '다시 시도')),
    );
  } else if (kind === 'no-result') {
    content.replaceChildren(box('search', '검색 결과가 없어요', h('p', { class: 'mb-0' }, '다른 제목으로 검색해 보세요.')));
  } else {
    content.replaceChildren(
      box('diagram-3', '아직 저장된 파이프라인이 없어요', h('p', { class: 'mb-3' }, '챗봇에게 고민을 말하면 AI 툴 조합을 만들어 줘요.'), h('a', { class: 'pl-btn pl-btn-primary', href: '/pages/chatbot/chat.html' }, '챗봇으로 만들어보기', h('i', { class: 'bi bi-arrow-right', 'aria-hidden': 'true' }))),
    );
  }
}

// ---------------------------------------------------------------- 삭제
const deleteModal = new window.bootstrap.Modal($('delete-modal'));

function askDelete(p) {
  pendingDelete = p;
  $('delete-name').textContent = p.title;
  $('delete-error').replaceChildren();
  deleteModal.show();
}

$('delete-confirm').addEventListener('click', async () => {
  if (!pendingDelete) return;
  const button = $('delete-confirm');
  setBusy(button, true, '삭제 중…');
  try {
    await deletePipeline(pendingDelete.pipelineId);
    pendingDelete = null;
    if (totalAll != null) totalAll -= 1;
    deleteModal.hide();
    await load();
    $('summary').focus(); // 삭제한 버튼이 사라졌으니 포커스를 목록 위로 옮긴다
  } catch (err) {
    $('delete-error').replaceChildren(h('div', { class: 'alert alert-danger py-2 mb-0' }, `삭제하지 못했어요. ${errorMessage(err)}`));
  } finally {
    setBusy(button, false);
  }
});

load();
