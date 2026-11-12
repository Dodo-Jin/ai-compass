// 트렌드 페이지 화면 (담당 C): 실시간 AI 랭킹 Top 100 + 최신 AI 동향
import { fetchRankings, fetchNews, setFavorite } from './trend-api.js';

const CATEGORIES = ['전체', '챗봇', '이미지', '영상', '음성·음악', '코딩'];
const PERIODS = [
  { value: 'realtime', label: '실시간' },
  { value: 'weekly', label: '주간' },
  { value: 'monthly', label: '월간' },
];
const PAGE_SIZE = 10;
const LOGO_COLORS = ['#10a37f', '#c96442', '#4f7be8', '#1f6f78', '#2b2f3a', '#5a4fcf', '#d1495b', '#3c4459', '#b4520f', '#0f766e'];

const state = { period: 'realtime', category: '전체', page: 1, total: 0, loading: false };

const $ = (sel) => document.querySelector(sel);
const els = {
  periods: $('#trend-periods'),
  tabs: $('#trend-tabs'),
  list: $('#trend-list'),
  updated: $('#trend-updated'),
  more: $('#trend-more'),
  status: $('#trend-status'),
  news: $('#trend-news'),
  keywords: $('#trend-keywords'),
  toast: $('#trend-toast'),
};

// ---------- 공통 유틸 ----------
const escapeHtml = (s) =>
  String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]);

function formatCount(n) {
  if (n >= 10000) return `${(n / 10000).toFixed(1).replace(/\.0$/, '')}만`;
  if (n >= 1000) return `${(n / 1000).toFixed(1).replace(/\.0$/, '')}천`;
  return String(n);
}

function timeAgo(iso) {
  const diff = (Date.now() - new Date(iso).getTime()) / 1000;
  if (diff < 3600) return `${Math.max(1, Math.floor(diff / 60))}분 전`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}시간 전`;
  const days = Math.floor(diff / 86400);
  return days === 1 ? '어제' : `${days}일 전`;
}

function formatUpdated(iso) {
  const d = new Date(iso);
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())} 기준 · 매시 정각 갱신`;
}

const logoColor = (name) => LOGO_COLORS[[...name].reduce((a, c) => a + c.charCodeAt(0), 0) % LOGO_COLORS.length];

function showToast(msg) {
  els.toast.textContent = msg;
  els.toast.classList.add('show');
  clearTimeout(showToast.timer);
  showToast.timer = setTimeout(() => els.toast.classList.remove('show'), 2400);
}

// ---------- 랭킹 ----------
function changeBadge(item) {
  if (item.isNew) return '<span class="rank-change new">NEW</span>';
  if (item.rankChange > 0) return `<span class="rank-change up" aria-label="${item.rankChange}계단 상승">▲ ${item.rankChange}</span>`;
  if (item.rankChange < 0) return `<span class="rank-change down" aria-label="${-item.rankChange}계단 하락">▼ ${-item.rankChange}</span>`;
  return '<span class="rank-change same" aria-label="변동 없음">–</span>';
}

function rowHtml(item) {
  const name = escapeHtml(item.name);
  const tags = (item.tags || []).map((t) => `#${escapeHtml(t)}`).join(' ');
  return `
    <li class="rank-row" data-model-id="${item.modelId}">
      <span class="rank-no ${item.rank <= 3 ? 'top' : ''}">${item.rank}</span>
      ${changeBadge(item)}
      <span class="rank-logo" style="background:${logoColor(item.name)}" aria-hidden="true">${name.charAt(0)}</span>
      <div class="rank-info">
        <a class="rank-name" href="/pages/directory/detail.html?modelId=${item.modelId}">${name}</a>
        <div class="rank-tags">${tags}</div>
      </div>
      <div class="rank-score">
        <div class="score-bar" role="img" aria-label="트렌드 점수 ${item.trendScore}점"><span style="width:${item.trendScore}%"></span></div>
        <div class="score-meta">검색 ${formatCount(item.searchCount)} · 찜 ${formatCount(item.favoriteCount)} · 채택 ${item.pipelineRate}%</div>
      </div>
      <button type="button" class="fav-btn ${item.isFavorite ? 'on' : ''}" aria-pressed="${item.isFavorite}"
        aria-label="${name} 즐겨찾기 ${item.isFavorite ? '해제' : '추가'}">
        <i class="bi ${item.isFavorite ? 'bi-bookmark-fill' : 'bi-bookmark'}" aria-hidden="true"></i>
      </button>
    </li>`;
}

async function loadRankings({ append = false } = {}) {
  if (state.loading) return;
  state.loading = true;
  els.more.disabled = true;
  if (!append) els.list.innerHTML = '<li class="rank-empty">불러오는 중…</li>';

  try {
    const res = await fetchRankings({ period: state.period, category: state.category, page: state.page, size: PAGE_SIZE });
    state.total = res.total;
    els.updated.textContent = formatUpdated(res.updatedAt);
    const html = res.items.map(rowHtml).join('');
    if (append) els.list.insertAdjacentHTML('beforeend', html);
    else els.list.innerHTML = html || '<li class="rank-empty">이 카테고리에는 아직 순위가 없어요.</li>';

    const shown = els.list.querySelectorAll('.rank-row').length;
    els.more.hidden = shown >= state.total;
    els.more.textContent = `${shown + 1}위 ~ ${Math.min(shown + PAGE_SIZE, state.total)}위 더 보기`;
    els.status.textContent = `${state.total}개 중 ${shown}위까지 표시`;
  } catch (e) {
    console.error(e);
    els.list.innerHTML = '<li class="rank-empty">랭킹을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.</li>';
    els.more.hidden = true;
  } finally {
    state.loading = false;
    els.more.disabled = false;
  }
}

function renderControls() {
  els.periods.innerHTML = PERIODS.map(
    (p) => `<button type="button" data-period="${p.value}" aria-pressed="${p.value === state.period}"
      class="${p.value === state.period ? 'active' : ''}">${p.label}</button>`,
  ).join('');
  els.tabs.innerHTML = CATEGORIES.map(
    (c) => `<button type="button" role="tab" data-category="${escapeHtml(c)}" aria-selected="${c === state.category}"
      class="${c === state.category ? 'active' : ''}">${escapeHtml(c)}</button>`,
  ).join('');
}

function bindEvents() {
  els.periods.addEventListener('click', (e) => {
    const btn = e.target.closest('button[data-period]');
    if (!btn || btn.dataset.period === state.period) return;
    state.period = btn.dataset.period;
    state.page = 1;
    renderControls();
    loadRankings();
  });

  els.tabs.addEventListener('click', (e) => {
    const btn = e.target.closest('button[data-category]');
    if (!btn || btn.dataset.category === state.category) return;
    state.category = btn.dataset.category;
    state.page = 1;
    renderControls();
    loadRankings();
  });

  els.more.addEventListener('click', () => {
    state.page += 1;
    loadRankings({ append: true });
  });

  els.list.addEventListener('click', async (e) => {
    const btn = e.target.closest('.fav-btn');
    if (!btn || btn.disabled) return;
    const row = btn.closest('.rank-row');
    const modelId = Number(row.dataset.modelId);
    const name = row.querySelector('.rank-name').textContent;
    const next = btn.getAttribute('aria-pressed') !== 'true';

    setFavButton(btn, name, next); // 먼저 화면을 바꾸고, 실패하면 되돌린다
    btn.disabled = true;
    try {
      await setFavorite(modelId, next);
      showToast(next ? `${name}을(를) 즐겨찾기에 추가했어요` : `${name}을(를) 즐겨찾기에서 뺐어요`);
    } catch (err) {
      console.error(err);
      setFavButton(btn, name, !next);
      showToast('즐겨찾기는 로그인 후 사용할 수 있어요');
    } finally {
      btn.disabled = false;
    }
  });
}

function setFavButton(btn, name, on) {
  btn.classList.toggle('on', on);
  btn.setAttribute('aria-pressed', String(on));
  btn.setAttribute('aria-label', `${name} 즐겨찾기 ${on ? '해제' : '추가'}`);
  btn.querySelector('i').className = `bi ${on ? 'bi-bookmark-fill' : 'bi-bookmark'}`;
}

// ---------- 최신 AI 동향 ----------
async function loadNews() {
  try {
    const { items, keywords } = await fetchNews();
    const [featured, ...rest] = items;
    els.news.innerHTML = [
      featured &&
        `<article class="news-card featured">
          <div class="news-thumb" aria-hidden="true">[대표 이미지]</div>
          <div class="news-body">
            <div class="news-meta"><span class="news-tag">${escapeHtml(featured.tag)}</span>${timeAgo(featured.publishedAt)}</div>
            <h3><a href="${escapeHtml(featured.url)}">${escapeHtml(featured.title)}</a></h3>
            ${featured.summary ? `<p>${escapeHtml(featured.summary)}</p>` : ''}
            <div class="news-foot"><span>출처: ${escapeHtml(featured.source)}</span><a href="${escapeHtml(featured.url)}">자세히 보기</a></div>
          </div>
        </article>`,
      ...rest.map(
        (n) => `<article class="news-card">
          <div class="news-thumb small" aria-hidden="true"></div>
          <div class="news-body">
            <div class="news-meta"><span class="news-tag alt">${escapeHtml(n.tag)}</span>${timeAgo(n.publishedAt)}</div>
            <h3><a href="${escapeHtml(n.url)}">${escapeHtml(n.title)}</a></h3>
            <div class="news-related">관련 AI: ${n.relatedModels.map(escapeHtml).join(', ')}</div>
          </div>
        </article>`,
      ),
    ].join('');
    els.keywords.innerHTML = keywords
      .map((k) => `<a class="keyword" href="/pages/directory/list.html?keyword=${encodeURIComponent(k)}">#${escapeHtml(k)}</a>`)
      .join('');
  } catch (e) {
    console.error(e);
    els.news.innerHTML = '<p class="rank-empty">AI 동향을 불러오지 못했어요.</p>';
  }
}

renderControls();
bindEvents();
loadRankings();
loadNews();
