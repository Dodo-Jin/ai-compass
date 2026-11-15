// fetch 래퍼. 사용: const tools = await api('/models')
export async function api(path, options = {}) {
  const res = await fetch('/api/v1' + path, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
    body: options.body ? JSON.stringify(options.body) : undefined,
  });
  if (!res.ok) throw new Error((await res.json().catch(() => ({}))).message || res.statusText);
  return res.json();
}
