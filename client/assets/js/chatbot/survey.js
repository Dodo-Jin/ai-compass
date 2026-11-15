// 설문 모드 화면 (survey.html): 직업·사용량·예산 선택 → 조건에 맞는 툴 1개 추천 → 보관함 저장.
// LLM 없이 서버가 DB 를 필터링해서 한 개를 돌려준다. 파이프라인(블록 조합)이 아니라 단일 툴 추천이다.
import { h, avatar, setBusy, errorMessage } from './util.js';
import { SURVEY, labelOf } from './survey-options.js';
import { recommendBySurvey, createPipeline, USE_MOCK } from './pipeline-api.js';
import { showMockUser } from './mock-user.js';

const $ = (id) => document.getElementById(id);
const form = $('survey-form');
const recommendBtn = $('recommend-btn');
const result = $('result');
const saveBtn = $('save-btn');

let current = null; // { result, selection, saved } — 지금 보이는 추천 결과
let busy = false;

showMockUser();
if (USE_MOCK) $('mock-badge').hidden = false;

// ---------------------------------------------------------------- 선택지
for (const [group, def] of Object.entries(SURVEY)) {
  $('survey-groups').append(
    h(
      'fieldset',
      { class: 'border-0 p-0 m-0' },
      h('legend', { class: 'pl-field-label' }, def.label),
      h(
        'div',
        { class: 'pl-choices' },
        def.options.flatMap(([code, label]) => [
          h('input', { type: 'radio', class: 'pl-choice-input', name: group, id: `${group}-${code}`, value: code }),
          h('label', { class: 'pl-choice', for: `${group}-${code}` }, label),
        ]),
      ),
    ),
  );
}

function readSelection() {
  const selection = {};
  for (const group of Object.keys(SURVEY)) {
    selection[group] = form.querySelector(`input[name="${group}"]:checked`)?.value ?? null;
  }
  return selection;
}

function showMsg(target, kind, ...content) {
  target.replaceChildren(h('div', { class: `alert alert-${kind} py-2 mb-0` }, ...content));
}

// ---------------------------------------------------------------- 추천
async function recommend() {
  if (busy) return;
  const selection = readSelection();
  const missing = Object.entries(SURVEY).filter(([group]) => !selection[group]).map(([, def]) => def.label.replace(' 선택', ''));
  if (missing.length) return showMsg($('survey-msg'), 'danger', `다음 항목을 선택해 주세요: ${missing.join(', ')}`);

  $('survey-msg').replaceChildren();
  $('save-msg').replaceChildren();
  busy = true;
  setBusy(recommendBtn, true, '찾는 중…');
  renderLoading();
  try {
    current = { result: await recommendBySurvey(selection), selection, saved: false };
    renderResult();
  } catch (err) {
    current = null;
    renderEmpty();
    showMsg($('survey-msg'), 'danger', `추천을 불러오지 못했어요. ${errorMessage(err)}`);
  } finally {
    busy = false;
    setBusy(recommendBtn, false);
    updateSaveButton();
  }
}

form.addEventListener('submit', (e) => {
  e.preventDefault();
  recommend();
});

// ---------------------------------------------------------------- 결과 화면
function renderEmpty() {
  result.replaceChildren(
    h('div', { class: 'pl-result-empty' }, h('i', { class: 'bi bi-ui-checks', 'aria-hidden': 'true' }), h('p', { class: 'mb-0' }, '조건을 선택하고 ‘추천받기’를 눌러보세요.')),
  );
}

function renderLoading() {
  result.replaceChildren(h('div', { class: 'pl-result-empty' }, h('div', { class: 'spinner-border text-primary mb-3', 'aria-hidden': 'true' }), h('p', { class: 'mb-0' }, '조건에 맞는 툴을 찾는 중이에요…')));
}

function renderResult() {
  const r = current.result;
  const rating = Number(r.rating);
  const stars = Number.isFinite(rating) && r.rating != null
    ? h(
        'div',
        { class: 'pl-stars', role: 'img', 'aria-label': `평점 ${rating.toFixed(1)}점` + (r.reviewCount != null ? `, 리뷰 ${r.reviewCount}개` : '') },
        [1, 2, 3, 4, 5].map((i) => h('i', { class: `bi bi-star-fill${i <= Math.round(rating) ? '' : ' off'}`, 'aria-hidden': 'true' })),
        h('span', { class: 'score', 'aria-hidden': 'true' }, `${rating.toFixed(1)}${r.reviewCount != null ? ` (리뷰 ${r.reviewCount}개)` : ''}`),
      )
    : null;

  const card = h(
    'article',
    { class: 'pl-result' },
    h(
      'div',
      { class: 'pl-result-head' },
      avatar(r.name, r.logoUrl, { size: 'lg', seed: r.modelId }),
      h('div', {}, h('h3', {}, r.name), h('p', { class: 'pl-result-sub' }, r.subtitle ?? r.category ?? ''), stars),
    ),
    // 값이 있는 항목만 보여준다 (DB 에 없는 항목은 서버가 생략해도 화면이 깨지지 않는다)
    r.reason ? [h('h3', { class: 'pl-sec' }, '추천 이유'), h('p', { class: 'pl-reason' }, r.reason)] : null,
    r.features?.length ? [h('h3', { class: 'pl-sec' }, '주요 특징'), h('ul', { class: 'pl-features' }, r.features.map((f) => h('li', {}, f)))] : null,
    r.priceText ? [h('h3', { class: 'pl-sec' }, '가격'), h('span', { class: 'pl-price' }, r.priceText)] : null,
    h(
      'div',
      { class: 'pl-result-actions' },
      h('button', { type: 'button', class: 'pl-btn', onclick: recommend }, h('i', { class: 'bi bi-arrow-repeat', 'aria-hidden': 'true' }), '다시 추천받기'),
      h('a', { class: 'pl-btn pl-btn-primary', href: `/pages/directory/detail.html?id=${encodeURIComponent(r.modelId)}` }, '상세 페이지 보기', h('i', { class: 'bi bi-arrow-right', 'aria-hidden': 'true' })),
    ),
  );
  result.replaceChildren(card, h('p', { class: 'pl-footnote text-center' }, '※ 설문 모드는 파이프라인(블록 조합)이 아닌, 조건에 가장 맞는 ‘단일 AI 툴’을 추천합니다'));
}

// ---------------------------------------------------------------- 보관함 저장 (블록 1개짜리 파이프라인)
function updateSaveButton() {
  saveBtn.disabled = !current || current.saved;
  saveBtn.textContent = current?.saved ? '저장됨' : '보관함에 저장';
}

saveBtn.addEventListener('click', async () => {
  if (!current || current.saved) return;
  const { result: r, selection } = current;
  const job = labelOf('job', selection.job);
  const body = {
    title: `${r.name} 추천 · ${job}`.slice(0, 100),
    blocks: [{ modelId: r.modelId, purpose: `설문 추천 · ${job} / ${labelOf('usage', selection.usage)} / ${labelOf('budget', selection.budget)}` }],
  };
  setBusy(saveBtn, true, '저장 중…');
  try {
    await createPipeline(body);
    current.saved = true;
    $('save-msg').replaceChildren('보관함에 저장했어요. ', h('a', { href: '/pages/mypage/library.html' }, '보관함 보기 →'));
  } catch (err) {
    showMsg($('save-msg'), 'danger', `저장하지 못했어요. ${errorMessage(err)}`);
  } finally {
    setBusy(saveBtn, false);
    updateSaveButton();
  }
});

renderEmpty();
updateSaveButton();
