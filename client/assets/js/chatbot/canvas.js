// 파이프라인 캔버스: 블록 목록 + 순서 변경(SortableJS / 숨김 ↑↓ 버튼) + 삭제 + 툴 추가.
// 챗봇 화면(chat.html)에서 쓰고, 저장된 파이프라인을 열 때(chat.html?id=N)도 같은 캔버스를 쓴다.
//
//   const canvas = createCanvas(rootEl, { listTools, onChange });
//   canvas.setBlocks([{ modelId, modelName, category, logoUrl, purpose }])   // 화면에 채우기 (onChange 호출 안 함)
//   canvas.getBlocks()                                                       // [{ modelId, purpose }] — 저장 요청용, 현재 순서대로
import { h, avatar, BLOCKS_MAX, PURPOSE_MAX, errorMessage } from './util.js';

let uidSeq = 0;

export function createCanvas(root, { listTools, onChange = () => {}, emptyText, footnote } = {}) {
  let blocks = []; // { uid, modelId, modelName, category, logoUrl, purpose }
  let toolsCache = null;

  const empty = h(
    'div',
    { class: 'pl-empty' },
    h('i', { class: 'bi bi-diagram-3', 'aria-hidden': 'true' }),
    h('p', { class: 'mb-0' }, emptyText ?? '아직 블록이 없어요. 아래의 “툴 추가”로 직접 만들 수 있어요.'),
  );
  const list = h('ol', { class: 'pl-list list-unstyled mb-0', 'aria-label': '파이프라인 블록 (위에서 아래 순서로 실행)' });
  const hint = h('p', { class: 'pl-hint', 'aria-live': 'polite' });
  const addButton = h(
    'button',
    { type: 'button', class: 'pl-btn', onclick: openPicker },
    h('i', { class: 'bi bi-plus-lg', 'aria-hidden': 'true' }),
    '툴 추가',
  );
  root.replaceChildren(empty, list, hint, addButton, footnote ? h('p', { class: 'pl-footnote' }, footnote) : null);

  // ------------------------------------------------------------ 블록 목록
  function render() {
    list.replaceChildren(...blocks.map(renderBlock));
    empty.hidden = blocks.length > 0;
    list.hidden = blocks.length === 0;
    hint.hidden = blocks.length === 0;
    hint.textContent = `드래그로 순서 변경 · X 로 삭제 · ${blocks.length}/${BLOCKS_MAX}`;
    hint.style.marginTop = '';
    addButton.disabled = blocks.length >= BLOCKS_MAX;
    addButton.style.marginTop = blocks.length === 0 ? '22px' : '';
  }

  function moveButton(label, icon, handler, disabled) {
    return h(
      'button',
      { type: 'button', class: 'pl-move', 'aria-label': label, disabled, onclick: handler },
      h('i', { class: `bi bi-${icon}`, 'aria-hidden': 'true' }),
    );
  }

  function renderBlock(block, index) {
    const name = block.modelName ?? '삭제된 툴';
    const purpose = h('input', {
      type: 'text',
      class: 'pl-purpose',
      placeholder: '용도 입력 (선택)',
      maxlength: PURPOSE_MAX,
      value: block.purpose ?? '',
      'aria-label': `${index + 1}번 ${name} 용도`,
      oninput: (e) => {
        block.purpose = e.target.value;
        onChange();
      },
      // 폼 안에 있으므로 Enter 가 저장을 실행하지 않게 막는다
      onkeydown: (e) => {
        if (e.key === 'Enter') e.preventDefault();
      },
    });
    return h(
      'li',
      { class: 'pl-block', 'data-uid': block.uid },
      h('span', { class: 'pl-handle', title: '드래그해서 순서 변경', 'aria-hidden': 'true' }, h('i', { class: 'bi bi-grip-vertical' })),
      h('span', { class: 'pl-order', 'aria-hidden': 'true' }, String(index + 1)),
      avatar(block.modelName, block.logoUrl, { seed: block.modelId }),
      h(
        'div',
        { class: 'pl-body' },
        h(
          'div',
          { class: 'pl-name' },
          h('span', { class: block.modelName ? '' : 'text-muted' }, name),
        ),
        purpose,
      ),
      h(
        'div',
        { class: 'pl-actions' },
        moveButton('위로 이동', 'chevron-up', () => move(index, index - 1), index === 0),
        moveButton('아래로 이동', 'chevron-down', () => move(index, index + 1), index === blocks.length - 1),
        h(
          'button',
          { type: 'button', class: 'pl-remove', 'aria-label': `${name} 삭제`, title: '삭제', onclick: () => remove(index) },
          h('i', { class: 'bi bi-x-lg', 'aria-hidden': 'true' }),
        ),
      ),
    );
  }

  function move(from, to) {
    if (to < 0 || to >= blocks.length) return;
    const [item] = blocks.splice(from, 1);
    blocks.splice(to, 0, item);
    render();
    onChange();
    // 키보드 사용자가 이어서 누를 수 있게, 옮겨진 블록의 같은 방향 버튼으로 포커스를 돌려준다
    const row = list.children[to];
    const label = to < from ? '위로 이동' : '아래로 이동';
    const same = row.querySelector(`[aria-label="${label}"]`);
    (same && !same.disabled ? same : row.querySelector('.pl-move:not(:disabled)'))?.focus();
  }

  function remove(index) {
    blocks.splice(index, 1);
    render();
    onChange();
    (list.children[Math.min(index, blocks.length - 1)]?.querySelector('.pl-remove') ?? addButton).focus();
  }

  function addTool(tool) {
    if (blocks.length >= BLOCKS_MAX) return false;
    blocks.push({
      uid: ++uidSeq,
      modelId: tool.modelId,
      modelName: tool.name ?? tool.modelName ?? null,
      category: tool.category ?? '',
      logoUrl: tool.logoUrl ?? null,
      purpose: '',
    });
    render();
    onChange();
    list.lastElementChild?.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
    return true;
  }

  // 드래그로 순서 변경. 상태 배열을 먼저 옮기고 목록을 다시 그려서 화면과 데이터가 항상 일치한다
  if (window.Sortable) {
    window.Sortable.create(list, {
      handle: '.pl-handle',
      animation: 150,
      ghostClass: 'pl-ghost',
      chosenClass: 'pl-chosen',
      onEnd(evt) {
        if (evt.oldIndex === evt.newIndex) return;
        const [item] = blocks.splice(evt.oldIndex, 1);
        blocks.splice(evt.newIndex, 0, item);
        render();
        onChange();
      },
    });
  }

  // ------------------------------------------------------------ 툴 추가 모달
  const titleId = `pl-picker-title-${Math.random().toString(36).slice(2)}`;
  const search = h('input', { type: 'search', class: 'form-control mb-3', placeholder: '툴 이름이나 분류로 검색', 'aria-label': '툴 검색', autocomplete: 'off' });
  const status = h('div', { class: 'text-muted small mb-2', role: 'status' });
  const toolList = h('div', { class: 'list-group' });
  const modalEl = h(
    'div',
    { class: 'modal fade', tabindex: '-1', 'aria-labelledby': titleId },
    h(
      'div',
      { class: 'modal-dialog modal-dialog-scrollable' },
      h(
        'div',
        { class: 'modal-content' },
        h(
          'div',
          { class: 'modal-header' },
          h('h2', { class: 'modal-title fs-5', id: titleId }, '툴 추가'),
          h('button', { type: 'button', class: 'btn-close', 'data-bs-dismiss': 'modal', 'aria-label': '닫기' }),
        ),
        h('div', { class: 'modal-body' }, search, status, toolList),
      ),
    ),
  );
  document.body.append(modalEl);
  const modal = new window.bootstrap.Modal(modalEl);
  modalEl.addEventListener('shown.bs.modal', () => search.focus());
  search.addEventListener('input', renderTools);

  function renderTools() {
    const keyword = search.value.trim().toLowerCase();
    const rows = (toolsCache ?? []).filter((t) => !keyword || `${t.name} ${t.category}`.toLowerCase().includes(keyword));
    status.textContent = rows.length === 0 && toolsCache ? '조건에 맞는 툴이 없어요.' : '';
    const used = new Set(blocks.map((b) => b.modelId));
    toolList.replaceChildren(
      ...rows.map((tool) =>
        h(
          'button',
          {
            type: 'button',
            class: 'list-group-item list-group-item-action d-flex align-items-center gap-3',
            onclick: () => {
              if (addTool(tool)) modal.hide();
            },
          },
          avatar(tool.name, tool.logoUrl, { size: 'sm', seed: tool.modelId }),
          h('span', { class: 'flex-grow-1' }, h('strong', {}, tool.name), tool.category ? h('span', { class: 'badge text-bg-light border ms-2 fw-normal' }, tool.category) : null),
          used.has(tool.modelId) ? h('span', { class: 'small text-muted' }, '추가됨') : null,
        ),
      ),
    );
  }

  async function openPicker() {
    search.value = '';
    toolList.replaceChildren();
    modal.show();
    if (toolsCache) return renderTools();
    status.textContent = '툴 목록을 불러오는 중…';
    try {
      toolsCache = await listTools();
      renderTools();
    } catch (err) {
      status.replaceChildren(
        `툴 목록을 불러오지 못했어요. ${errorMessage(err)} `,
        h('button', { type: 'button', class: 'btn btn-link btn-sm p-0 align-baseline', onclick: openPicker }, '다시 시도'),
      );
    }
  }

  render();

  return {
    setBlocks(input) {
      blocks = input.slice(0, BLOCKS_MAX).map((b) => ({
        uid: ++uidSeq,
        modelId: b.modelId,
        modelName: b.modelName ?? null,
        category: b.category ?? '',
        logoUrl: b.logoUrl ?? null,
        purpose: b.purpose ?? '',
      }));
      render();
    },
    getBlocks() {
      return blocks.map((b) => ({ modelId: b.modelId, purpose: (b.purpose ?? '').trim() || null }));
    },
    addTool,
    isEmpty: () => blocks.length === 0,
  };
}
