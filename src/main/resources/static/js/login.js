document.addEventListener("DOMContentLoaded", () => {
  const form = document.getElementById("login-form");
  const errorEl = document.getElementById("login-error");

  form.addEventListener("submit", async event => {
    event.preventDefault();
    errorEl.textContent = "";
    errorEl.classList.add("hidden");

    const username = document.getElementById("login-username").value.trim();
    const password = document.getElementById("login-password").value;

    if (!username || !password) {
      errorEl.textContent = "Введите имя пользователя и пароль.";
      errorEl.classList.remove("hidden");
      return;
    }

    try {
      const response = await fetch("/api/auth/login", {
        method: "POST",
        credentials: "same-origin",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password })
      });

      if (!response.ok) {
        const body = await response.json().catch(() => null);
        errorEl.textContent = body?.error || "Неверное имя пользователя или пароль.";
        errorEl.classList.remove("hidden");
        return;
      }

      window.location.href = "/";
    } catch {
      errorEl.textContent = "Не удалось выполнить вход. Проверьте соединение.";
      errorEl.classList.remove("hidden");
    }
  });
});
