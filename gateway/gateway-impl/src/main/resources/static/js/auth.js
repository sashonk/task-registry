import { apiFetch } from "./api.js";
import { state } from "./state.js";

export async function ensureAuthenticated() {
  const response = await apiFetch("/api/auth/me");
  if (!response.ok) {
    throw new Error("Not authenticated");
  }

  const user = await response.json();
  state.currentUser = user;
  return user;
}

export function isAdmin() {
  return state.currentUser?.role === "ADMIN";
}

export function initAuthUi() {
  const usernameEl = document.getElementById("header-username");
  const roleEl = document.getElementById("header-role");

  if (usernameEl) {
    usernameEl.textContent = state.currentUser?.username ?? "";
  }

  if (roleEl) {
    roleEl.textContent = state.currentUser?.roleDisplayName ?? "";
  }

  document.getElementById("btn-logout")?.addEventListener("click", () => {
    logout().catch(() => {
      window.location.href = "/login.html";
    });
  });
}

export async function logout() {
  const response = await apiFetch("/api/auth/logout", { method: "POST" });
  if (!response.ok && response.status !== 204) {
    throw new Error("Не удалось выйти");
  }

  window.location.href = "/login.html";
}

export function applyRoleRestrictions() {
  if (isAdmin()) {
    return;
  }

  document.querySelectorAll(".toolbar").forEach(toolbar => {
    toolbar.classList.add("hidden");
  });
}
