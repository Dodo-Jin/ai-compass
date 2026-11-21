// 마이페이지 > 즐겨찾기 비교 (favorites.html, 담당 C)
// 즐겨찾기 목록에서 2~4개를 골라 스펙을 나란히 비교한다. 선택은 주소(?ids=1,2,3)에 남아 새로고침해도 유지된다.
import { USE_MOCK, loadUser, listFavorites, compareModels, removeFavorite, addFavorite, countOf, loginUrl } from './favorite-api.js';

const MIN = 2;
const MAX = 4;
const DEFAULT_PICK = 3;
const LOGO_COLORS = ['#10a37f', '#c96442', '#4f7be8', '#1f6f78', '#2b2f3a', '#5a4fcf', '#d1495b', '#3c4459', '#b4520f', '#0f766e'];

const $ = (id) => document.getElementById(id);
const state = {
  all: [],            // 내 즐겨찾기 전체
  categories: [],
  category: '',       // 목록 필터 ('' = 전체)
  selected: [],       // 비교할 modelId (고른 순서)
  diffOnly: false,
  compareSeq: 0,      // 느린 응답이 최신 응답을 덮어쓰지 않게
};

// ---------------------------------------------------------------- 작은 도우미

/** h('a', { href, class }, '글자', 자식…). 글자는 항상 textContent 로 넣는다 */
function h(tag, props = {}, ...children) {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(props)) {
    if (v == null || v === false) continue;
    if (k === 'class') el.className = v;
    else if (k.startsWith('on')) el.addEventListener(k.slice(2), v);
    else if (k in el && typeof v !== 'string') el[k] = v;
    else el.setAttribute(k, v === true ? '' : v);
  }
  for (const c of children.flat()) if (c != null && c !== false) el.append(c);
  return el;
}

const logoColor = (name) => LOGO_COLORS[[...(name || '?')].reduce((a, c) => a + c.charCodeAt(0), 0) % LOGO_COLORS.length];

function logo(m, size = 40) {
  if (m.logoUrl) {
    return h('img', { class: 'fv-logo', src: m.logoUrl, alt: '', width: size, height: size, loading: 'lazy' });
  }
  return h('span', { class: 'fv-logo', 'aria-hidden': 'true', style: `background:${logoColor(m.name)};width:${size}px;height:${size}px` },
    (m.name || '?').charAt(0).toUpperCase());
}

const fmtCount = (n) => Number(n || 0).toLocaleString('ko-KR');

function priceLabel(m) {
  if (m.priceText) return m.priceText;
  const lo = m.priceMinUsd, hi = m.priceMaxUsd;
  if (lo == null && hi == null) return null;
  const f = (v) => (Number(v) === 0 ? '무료' : `월 $${Number(v)}`);
  if (lo == null || hi == null || Number(lo) === Number(hi)) return f(lo ?? hi);
  return `${f(lo)} ~ ${f(hi)}`;
}

function toast(message, action) {
  const box = $('fv-toast');
  box.replaceChildren(h('span', {}, message));
  if (action) box.append(h('button', { type: 'button', class: 'fv-toast-btn', onclick: () => { box.classList.remove('show'); action.run(); } }, action.label));
  box.classList.add('show');
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => box.classList.remove('show'), action ? 6000 : 2600);
}

/** 401 이면 로그인 화면으로 보내고 true */
function handleAuth(err) {
  if (err && err.status === 401) {
    location.href = loginUrl();
    return true;
  }
  return false;
}

function syncUrl() {
  const qs = new URLSearchParams(location.search);
  qs.delete('ids');
  qs.delete('mock'); // 목 모드는 sessionStorage 에 남아 있다
  const parts = qs.toString() ? [qs.toString()] : [];
  if (state.selected.length) parts.push('ids=' + state.selected.join(',')); // 쉼표가 %2C 로 바뀌지 않게 직접 만든다
  history.replaceState(null, '', location.pathname + (parts.length ? '?' + parts.join('&') : ''));
}

// ---------------------------------------------------------------- 프로필 카드 (공용 partial)

async function fillProfile() {
  let user;
  try {
    user = await loadUser();
  } catch (err) {
    if (handleAuth(err)) return false;
    user = null;
  }
  if (user) {
    $('profile-name').textContent = user.username;
    $('profile-email').textContent = user.email;
    $('profile-email').hidden = false;
    const link = document.getElementById('header-user');
    if (link) {
      link.querySelector('.pl-user-name').textContent = user.username;
      link.setAttribute('href', '/pages/mypage/index.html');
    }
  }
  countOf('/pipelines?page=1&size=1').then((n) => { if (n != null) $('stat-pipelines').textContent = String(n); });
  countOf('/users/me/reviews?page=1&size=1').then((n) => { if (n != null) $('stat-reviews').textContent = String(n); });
  return true;
}

// ---------------------------------------------------------------- 즐겨찾기 목록

async function loadFavorites() {
  const res = await listFavorites();
  state.all = res.items ?? [];
  state.categories = res.categories ?? [...new Set(state.all.map((m) => m.category))];
  $('stat-favorites').textContent = String(res.totalElements ?? state.all.length);
  if (state.category && !state.categories.includes(state.category)) state.category = '';

  const ids = new Set(state.all.map((m) => m.modelId));
  state.selected = state.selected.filter((id) => ids.has(id)).slice(0, MAX);
}

function renderFilter() {
  const select = $('fv-category');
  select.replaceChildren(
    h('option', { value: '' }, '전체'),
    ...state.categories.map((c) => h('option', { value: c }, c)),
  );
  select.value = state.category;
}

function renderChips() {
  const box = $('fv-chips');
  $('fv-count').textContent = String(state.all.length);
  $('fv-hint').textContent = state.all.length < MIN
    ? `비교하려면 AI 툴을 ${MIN}개 이상 즐겨찾기해 주세요.`
    : `비교할 모델을 ${MIN}~${MAX}개 선택하세요 · 현재 ${state.selected.length}개 선택됨`;

  if (!state.all.length) {
    box.replaceChildren(
      h('div', { class: 'pl-empty text-center w-100' },
        h('i', { class: 'bi bi-bookmark-star', 'aria-hidden': 'true' }),
        h('p', { class: 'mb-3' }, '아직 즐겨찾기한 AI 툴이 없어요.'),
        h('a', { class: 'pl-btn pl-btn-primary', href: '/pages/directory/list.html' }, 'AI 툴 둘러보기')),
    );
    return;
  }

  const shown = state.all.filter((m) => !state.category || m.category === state.category);
  const full = state.selected.length >= MAX;
  box.replaceChildren(...shown.map((m) => {
    const on = state.selected.includes(m.modelId);
    const input = h('input', {
      type: 'checkbox', class: 'form-check-input', checked: on, disabled: !on && full,
      onchange: () => toggle(m.modelId),
    });
    return h('div', { class: `fv-chip${on ? ' on' : ''}${!on && full ? ' locked' : ''}` },
      h('label', { class: 'fv-chip-main', title: !on && full ? `최대 ${MAX}개까지 비교할 수 있어요` : null },
        input, logo(m, 36),
        h('span', { class: 'fv-chip-text' }, h('strong', {}, m.name), h('small', {}, m.category))),
      h('button', {
        type: 'button', class: 'fv-chip-remove', 'aria-label': `${m.name} 즐겨찾기 해제`, title: '즐겨찾기 해제',
        onclick: () => unfavorite(m),
      }, h('i', { class: 'bi bi-x-lg', 'aria-hidden': 'true' })));
  }));
}

function toggle(modelId) {
  const i = state.selected.indexOf(modelId);
  if (i >= 0) state.selected.splice(i, 1);
  else if (state.selected.length < MAX) state.selected.push(modelId);
  syncUrl();
  renderChips();
  renderCompare();
}

async function unfavorite(m) {
  try {
    await removeFavorite(m.modelId);
  } catch (err) {
    if (!handleAuth(err)) toast(err.message || '즐겨찾기를 해제하지 못했어요.');
    return;
  }
  const wasSelected = state.selected.includes(m.modelId);
  state.all = state.all.filter((x) => x.modelId !== m.modelId);
  state.selected = state.selected.filter((id) => id !== m.modelId);
  $('stat-favorites').textContent = String(state.all.length);
  syncUrl();
  renderChips();
  renderCompare();
  toast(`${m.name}을(를) 즐겨찾기에서 뺐어요.`, {
    label: '되돌리기',
    run: async () => {
      try {
        await addFavorite(m.modelId);
        await loadFavorites();
        if (wasSelected && state.selected.length < MAX) state.selected.push(m.modelId);
        renderFilter();
        syncUrl();
        renderChips();
        renderCompare();
      } catch (err) {
        if (!handleAuth(err)) toast(err.message || '되돌리지 못했어요.');
      }
    },
  });
}

// ---------------------------------------------------------------- 비교표

/**
 * 행 정의. text: 칸에 보일 글자 (없으면 '–'), score: 클수록 좋은 값 (가장 좋은 칸에 표시), best: 표시 문구
 */
const ROWS = [
  { label: '카테고리', text: (m) => m.category },
  { label: '요금 형태', text: (m) => m.pricingType },
  { label: '가격', text: priceLabel, score: (m) => (m.priceMinUsd == null ? null : -Number(m.priceMinUsd)), best: '가장 저렴' },
  {
    label: '평균 별점',
    text: (m) => (m.reviewCount > 0 ? `★ ${Number(m.averageRating).toFixed(1)} (리뷰 ${fmtCount(m.reviewCount)}개)` : '리뷰 없음'),
    score: (m) => (m.reviewCount > 0 ? Number(m.averageRating) : null),
    best: '최고 별점',
  },
  { label: '입력 형식', text: (m) => (m.inputTypes ?? []).join(' · '), score: (m) => (m.inputTypes?.length ? m.inputTypes.length : null), best: '가장 다양' },
  { label: '한국어 지원', text: (m) => (m.koreanSupport ? '지원' : '미지원'), score: (m) => (m.koreanSupport ? 1 : 0), best: '지원' },
  { label: '주요 강점', text: (m) => m.strengths, long: true },
  { label: '한계점', text: (m) => m.limitations, long: true },
  { label: '즐겨찾기 수', text: (m) => `${fmtCount(m.favoriteCount)}명`, score: (m) => Number(m.favoriteCount ?? 0), best: '가장 인기' },
];

/** 가장 좋은 칸의 index 목록. 모두 같으면 [] */
function bestIndexes(row, items) {
  if (!row.score) return [];
  const scores = items.map(row.score);
  const valid = scores.filter((s) => s != null);
  if (valid.length < 2) return [];
  const top = Math.max(...valid);
  if (valid.every((s) => s === top)) return [];
  return scores.map((s, i) => (s === top ? i : -1)).filter((i) => i >= 0);
}

async function renderCompare() {
  const box = $('fv-compare');
  const seq = ++state.compareSeq;
  $('fv-diff').disabled = state.selected.length < MIN;

  if (state.selected.length < MIN) {
    box.replaceChildren(h('div', { class: 'pl-empty text-center' },
      h('i', { class: 'bi bi-layout-three-columns', 'aria-hidden': 'true' }),
      h('p', { class: 'mb-0' }, state.all.length < MIN
        ? '즐겨찾기한 AI 툴이 2개 이상이면 여기에서 나란히 비교할 수 있어요.'
        : `위에서 비교할 AI 툴을 ${MIN}개 이상 골라 주세요.`)));
    return;
  }

  box.setAttribute('aria-busy', 'true');
  let items;
  try {
    items = (await compareModels(state.selected)).items;
  } catch (err) {
    if (handleAuth(err) || seq !== state.compareSeq) return;
    box.replaceChildren(h('div', { class: 'alert alert-warning mb-0', role: 'alert' }, err.message || '비교표를 불러오지 못했어요.'));
    return;
  } finally {
    if (seq === state.compareSeq) box.removeAttribute('aria-busy');
  }
  if (seq !== state.compareSeq) return;

  const head = h('tr', {},
    h('th', { scope: 'col', class: 'fv-label-col' }, '항목'),
    ...items.map((m) => h('th', { scope: 'col' },
      h('div', { class: 'fv-head' },
        logo(m, 44),
        h('div', { class: 'fv-head-text' },
          h('strong', {}, m.name),
          h('a', { href: `/pages/directory/detail.html?modelId=${m.modelId}` }, '상세 보기')),
        h('button', {
          type: 'button', class: 'fv-head-remove', 'aria-label': `${m.name} 비교에서 빼기`, title: '비교에서 빼기',
          onclick: () => toggle(m.modelId),
        }, h('i', { class: 'bi bi-x-lg', 'aria-hidden': 'true' }))))),
    items.length < MAX
      ? h('th', { scope: 'col', class: 'fv-add-col' },
        h('button', { type: 'button', class: 'fv-add', onclick: () => $('fv-chips').scrollIntoView({ behavior: 'smooth', block: 'center' }) },
          '+ 모델 추가', h('br'), `(최대 ${MAX}개)`))
      : null,
  );

  const body = ROWS.map((row) => {
    const texts = items.map((m) => row.text(m) || '–');
    const same = texts.every((t) => t === texts[0]);
    const best = bestIndexes(row, items);
    return h('tr', { class: same ? 'fv-same' : null },
      h('th', { scope: 'row' }, row.label),
      ...texts.map((t, i) => h('td', { class: [best.includes(i) ? 'fv-best' : '', row.long ? 'fv-long' : ''].join(' ').trim() || null },
        t, best.includes(i) ? h('span', { class: 'fv-best-tag' }, row.best) : null)),
      items.length < MAX ? h('td', { class: 'fv-add-col' }) : null);
  });

  const site = h('tr', {},
    h('th', { scope: 'row' }, '공식 사이트'),
    ...items.map((m) => h('td', {}, m.siteUrl
      ? h('a', { class: 'pl-btn pl-btn-primary fv-visit', href: m.siteUrl, target: '_blank', rel: 'noopener noreferrer' },
        'Visit Site ', h('i', { class: 'bi bi-box-arrow-up-right', 'aria-hidden': 'true' }))
      : '–')),
    items.length < MAX ? h('td', { class: 'fv-add-col' }) : null);

  const table = h('table', { class: `fv-table${state.diffOnly ? ' diff-only' : ''}` },
    h('caption', { class: 'visually-hidden' }, `${items.map((m) => m.name).join(', ')} 비교`),
    h('thead', {}, head), h('tbody', {}, ...body, site));

  box.replaceChildren(h('div', { class: 'fv-table-wrap' }, table));
}

// ---------------------------------------------------------------- 시작

await window.layoutReady; // 프로필·탭 partial 이 들어온 뒤에 채운다
if (USE_MOCK) $('mock-badge').hidden = false;

const fromUrl = (new URLSearchParams(location.search).get('ids') ?? '')
  .split(',').map((s) => parseInt(s, 10)).filter((n) => Number.isInteger(n) && n > 0);
state.selected = [...new Set(fromUrl)];

$('fv-category').addEventListener('change', (e) => {
  state.category = e.target.value;
  renderChips();
});
$('fv-diff').addEventListener('change', (e) => {
  state.diffOnly = e.target.checked;
  document.querySelector('.fv-table')?.classList.toggle('diff-only', state.diffOnly);
});

if (await fillProfile()) {
  try {
    await loadFavorites();
    if (!fromUrl.length) state.selected = state.all.slice(0, DEFAULT_PICK).map((m) => m.modelId);
    syncUrl();
    renderFilter();
    renderChips();
    renderCompare();
  } catch (err) {
    if (!handleAuth(err)) {
      $('fv-chips').replaceChildren(h('div', { class: 'alert alert-warning w-100 mb-0', role: 'alert' },
        err.message || '즐겨찾기를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.'));
      $('fv-compare').replaceChildren();
    }
  }
}
