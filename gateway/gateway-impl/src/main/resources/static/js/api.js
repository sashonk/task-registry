export async function apiFetch(url, options = {}) {
  const headers = { ...options.headers };

  if (options.body !== undefined && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json";
  }

  const response = await fetch(url, {
    credentials: "same-origin",
    ...options,
    headers
  });

  if (response.status === 401) {
    window.location.href = "/login.html";
    throw new Error("Требуется вход в систему");
  }

  return response;
}
