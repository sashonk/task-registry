import { apiFetch } from "./api.js";
import { state } from "./state.js";
import { showError, showConfirm, showMessage } from "./messages.js";

function selectExecutor(executorId) {
  state.selectedExecutorId = executorId;

  document.querySelectorAll("#executors-body tr").forEach(row => {
    row.classList.toggle("selected", Number(row.dataset.id) === executorId);
  });
}

export function renderExecutors() {
  const tbody = document.getElementById("executors-body");

  if (!state.executors.length) {
    tbody.innerHTML = `
      <tr class="empty-row">
        <td colspan="6">Нет исполнителей</td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = state.executors.map(executor => `
    <tr class="clickable-row${executor.id === state.selectedExecutorId ? " selected" : ""}" data-id="${executor.id}">
      <td>${executor.id}</td>
      <td>${executor.fullName}</td>
      <td>${executor.status}</td>
      <td class="stat-cell stat-success">${executor.successfulTasksCount}</td>
      <td class="stat-cell stat-error">${executor.errorTasksCount}</td>
      <td class="stat-cell stat-aborted">${executor.abortedTasksCount}</td>
    </tr>
  `).join("");

  tbody.querySelectorAll("tr.clickable-row").forEach(row => {
    const executorId = Number(row.dataset.id);
    row.addEventListener("click", () => selectExecutor(executorId));
  });
}

export async function loadExecutors() {
  const response = await apiFetch("/api/executors");
  if (!response.ok) {
    throw new Error("Не удалось загрузить исполнителей");
  }

  state.executors = await response.json();

  if (state.selectedExecutorId && !state.executors.some(executor => executor.id === state.selectedExecutorId)) {
    state.selectedExecutorId = null;
  }

  renderExecutors();
}

async function addExecutor() {
  const response = await apiFetch("/api/executors/worker", {
    method: "POST"
  });

  if (!response.ok) {
    showError(new Error("Не удалось добавить исполнителя"));
    return;
  }

  await loadExecutors();
}

async function deleteSelectedExecutor() {
  if (!state.selectedExecutorId) {
    await showMessage("Выберите исполнителя в таблице.", "Внимание");
    return;
  }

  const executor = state.executors.find(item => item.id === state.selectedExecutorId);
  if (!executor) {
    return;
  }

  const confirmed = await showConfirm(`Удалить исполнителя «${executor.fullName}»?`, "Удаление исполнителя");
  if (!confirmed) {
    return;
  }

  const response = await apiFetch(`/api/executors/${state.selectedExecutorId}`, {
    method: "DELETE"
  });

  if (!response.ok) {
    const errorBody = await response.json().catch(() => null);
    throw new Error(errorBody?.error || "Не удалось удалить исполнителя");
  }

  state.selectedExecutorId = null;
  await loadExecutors();
}

export function initExecutorsToolbar() {
  document.getElementById("btn-add-executor").addEventListener("click", () => {
    addExecutor().catch(showError);
  });
  document.getElementById("btn-delete-executor").addEventListener("click", () => {
    deleteSelectedExecutor().catch(showError);
  });
}
