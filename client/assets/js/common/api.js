// fetch 래퍼 (공용, 담당 C). 사용: const tools = await api('/models')
//
// - 주소 앞에 /api/v1 을 붙인다. 화면 코드는 '/models' 처럼 뒷부분만 쓴다.
// - 서버가 api-spec 형식 { success, data } 로 주면 data 만 돌려준다.
//   (그 형식이 아닌 응답은 받은 JSON 을 그대로 돌려준다.)
// - 204(본문 없음)는 null 을 돌려준다.
// - 실패하면 ApiError 를 던진다. status(401 등), code(LOGIN_REQUIRED 등), message 를 담고 있다.
//     try { await api('/favorites', { method: 'POST', body: { modelId } }); }
//     catch (e) { if (e.status === 401) location.href = '/pages/auth/login.html'; else alert(e.message); }

export const API_BASE = '/api/v1';

export class ApiError extends Error {
  constructor(message, { status = 0, code = null, body = null } = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.body = body;
  }
}

export async function api(path, options = {}) {
  const { body, headers = {}, ...rest } = options;
  const isForm = typeof FormData !== 'undefined' && body instanceof FormData;
  const hasBody = body !== undefined && body !== null;

  let res;
  try {
    res = await fetch(API_BASE + path, {
      ...rest,
      headers: {
        Accept: 'application/json',
        ...(hasBody && !isForm ? { 'Content-Type': 'application/json' } : {}),
        ...headers,
      },
      body: hasBody ? (isForm ? body : JSON.stringify(body)) : undefined,
    });
  } catch (e) {
    throw new ApiError('서버에 연결할 수 없습니다. 서버가 켜져 있는지 확인해 주세요.', { code: 'NETWORK_ERROR' });
  }

  if (res.status === 204) return null;

  const text = await res.text();
  let json = null;
  if (text) {
    try {
      json = JSON.parse(text);
    } catch {
      json = null;
    }
  }

  const failed = !res.ok || (json !== null && typeof json === 'object' && json.success === false);
  if (failed) {
    // api-spec 형식 { error: { code, message } } 와 평평한 { message } 를 모두 읽는다
    const err = (json && (json.error ?? json)) || {};
    throw new ApiError(err.message || res.statusText || '요청을 처리하지 못했습니다.', {
      status: res.status,
      code: err.code ?? null,
      body: json,
    });
  }

  if (json !== null && typeof json === 'object' && json.success === true && 'data' in json) {
    return json.data;
  }
  return json;
}
