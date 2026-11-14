// B 담당 화면(챗봇 · 파이프라인 캔버스 · 보관함)이 함께 쓰는 도우미

// 서버 검증 규칙과 같은 값 (docs/api-spec.md 의 Pipelines 참고)
export const TITLE_MAX = 100;
export const BLOCKS_MAX = 20;
export const PURPOSE_MAX = 200;

/**
 * DOM 생성 도우미. 문자열 자식은 항상 텍스트 노드로 들어가므로 LLM 응답이나 사용자 입력을 넣어도 안전하다.
 *   h('button', { class: 'btn', onclick: fn, 'aria-label': '삭제' }, '삭제')
 * on* 은 소문자 이벤트명(onclick, oninput)을 쓴다. 값이 null/false 인 속성은 건너뛴다.
 */
export function h(tag, props = {}, ...children) {
  const node = document.createElement(tag);
  for (const [key, value] of Object.entries(props || {})) {
    if (value == null || value === false) continue;
    if (key.startsWith('on') && typeof value === 'function') node.addEventListener(key.slice(2), value);
    else if (key === 'class') node.className = value;
    else node.setAttribute(key, value === true ? '' : value);
  }
  for (const child of children.flat()) {
    if (child == null || child === false) continue;
    node.append(child instanceof Node ? child : document.createTextNode(String(child)));
  }
  return node;
}

export function initial(name) {
  const t = (name || '').trim();
  return t ? [...t][0].toUpperCase() : '?';
}

/**
 * 툴 로고. logoUrl 이 없거나 불러오지 못하면 이름 첫 글자를 보여준다.
 * 로고가 없을 때의 배경색은 seed(보통 modelId)로 정해서 같은 툴은 항상 같은 색이 된다.
 * size: '' | 'sm' | 'lg'
 */
export function avatar(name, logoUrl, { size = '', seed = 0 } = {}) {
  const tone = (Math.abs(Number(seed) || 0) + 7) % 8; // +7: 1번 툴이 첫 번째 색(초록)이 되도록 맞춘 순서
  const box = h('span', { class: `pl-avatar pl-tone-${tone}` + (size ? ` pl-avatar-${size}` : ''), 'aria-hidden': 'true' });
  if (logoUrl) {
    const img = h('img', { src: logoUrl, alt: '', loading: 'lazy' });
    img.addEventListener('error', () => {
      img.remove();
      box.textContent = initial(name);
    });
    box.append(img);
  } else {
    box.textContent = initial(name);
  }
  return box;
}

/** "2026-11-03T19:30:00" → "2026.11.03" */
export function formatDate(iso) {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '';
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())}`;
}

export function debounce(fn, ms) {
  let timer;
  return (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), ms);
  };
}

export function errorMessage(err) {
  return (err && err.message) || '알 수 없는 오류가 발생했어요.';
}

/** 저장 전 검사. 문제가 있으면 안내 문구, 없으면 null (서버도 같은 규칙으로 다시 검사한다) */
export function validatePipeline(title, blocks) {
  if (!title.trim()) return '제목을 입력해 주세요.';
  if (title.trim().length > TITLE_MAX) return `제목은 ${TITLE_MAX}자 이하여야 해요.`;
  if (blocks.length === 0) return '블록을 1개 이상 추가해 주세요.';
  if (blocks.length > BLOCKS_MAX) return `블록은 최대 ${BLOCKS_MAX}개까지 추가할 수 있어요.`;
  return null;
}

/** 버튼을 처리 중 상태(스피너 + 비활성)로 바꾸고, 해제하면 원래 내용으로 되돌린다 */
export function setBusy(button, busy, busyText = '처리 중…') {
  if (busy) {
    button._saved = [...button.childNodes];
    button.disabled = true;
    button.replaceChildren(h('span', { class: 'spinner-border spinner-border-sm me-2', 'aria-hidden': 'true' }), busyText);
  } else {
    if (button._saved) button.replaceChildren(...button._saved);
    button._saved = null;
    button.disabled = false;
  }
}
