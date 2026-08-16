import { state } from "./state.js";

export function closeMessageModal(result = false) {
  document.getElementById("message-modal-overlay").classList.add("hidden");
  document.getElementById("message-modal-cancel").classList.add("hidden");

  if (state.messageModalResolver) {
    const resolve = state.messageModalResolver;
    state.messageModalResolver = null;
    state.messageModalMode = "alert";
    resolve(result);
  }
}

export function showMessage(message, title = "Сообщение", isError = false) {
  return new Promise(resolve => {
    state.messageModalMode = "alert";
    state.messageModalResolver = () => resolve(true);

    document.getElementById("message-modal-title").textContent = title;
    const textEl = document.getElementById("message-modal-text");
    textEl.textContent = message;
    textEl.classList.toggle("message-modal-error", isError);
    document.getElementById("message-modal-cancel").classList.add("hidden");
    document.getElementById("message-modal-overlay").classList.remove("hidden");
  });
}

export function showConfirm(message, title = "Подтверждение") {
  return new Promise(resolve => {
    state.messageModalMode = "confirm";
    state.messageModalResolver = resolve;

    document.getElementById("message-modal-title").textContent = title;
    const textEl = document.getElementById("message-modal-text");
    textEl.textContent = message;
    textEl.classList.remove("message-modal-error");
    document.getElementById("message-modal-cancel").classList.remove("hidden");
    document.getElementById("message-modal-overlay").classList.remove("hidden");
  });
}

export function showError(error) {
  console.error(error);
  return showMessage(error.message || "Произошла ошибка", "Ошибка", true);
}

export function initMessageModal() {
  document.getElementById("message-modal-ok").addEventListener("click", () => {
    closeMessageModal(true);
  });

  document.getElementById("message-modal-cancel").addEventListener("click", () => {
    closeMessageModal(false);
  });

  document.querySelector(".message-modal-close-btn").addEventListener("click", () => {
    closeMessageModal(state.messageModalMode === "confirm" ? false : true);
  });

  document.getElementById("message-modal-overlay").addEventListener("click", event => {
    if (event.target.id === "message-modal-overlay") {
      closeMessageModal(state.messageModalMode === "confirm" ? false : true);
    }
  });
}
