// 공통 헤더/푸터 삽입. 모든 페이지에서 <div id="header">, <div id="footer"> + 이 스크립트 사용
// 삽입이 끝나면 window.layoutReady(Promise)가 이행된다. 헤더를 건드리는 코드는 `await window.layoutReady` 뒤에 실행할 것.
const parts = ['header', 'footer'].map((id) =>
  fetch(`/partials/${id}.html`)
    .then((r) => r.text())
    .then((html) => {
      const slot = document.getElementById(id);
      if (slot) slot.innerHTML = html;
    }),
);

// 현재 주소가 data-nav-match 로 시작하는 메뉴를 강조한다
window.layoutReady = Promise.all(parts).then(() => {
  for (const link of document.querySelectorAll('#header [data-nav-match]')) {
    if (location.pathname.startsWith(link.dataset.navMatch)) {
      link.classList.add('active');
      link.setAttribute('aria-current', 'page');
    }
  }
});
