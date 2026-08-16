import { state } from "./state.js";
import { showError } from "./messages.js";

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
    <tr>
      <td>${executor.id}</td>
      <td>${executor.fullName}</td>
      <td>${executor.status}</td>
      <td class="stat-cell stat-success">${executor.successfulTasksCount}</td>
      <td class="stat-cell stat-error">${executor.errorTasksCount}</td>
      <td class="stat-cell stat-aborted">${executor.abortedTasksCount}</td>
    </tr>
  `).join("");
}

export async function loadExecutors() {
  const response = await fetch("/api/executors");
  if (!response.ok) {
    throw new Error("Не удалось загрузить исполнителей");
  }

  state.executors = await response.json();
  renderExecutors();
}

async function addExecutor() {
  const response = await fetch("/api/executors/worker", {
    method: "POST"
  });

  if (!response.ok) {
    showError(new Error("Не удалось добавить исполнителя"));
    return;
  }

  await loadExecutors();
}

export function initExecutorsToolbar() {
  document.getElementById("btn-add-executor").addEventListener("click", () => {
    addExecutor().catch(showError);
  });
}
