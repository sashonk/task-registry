import { LOG_REFRESH_MS } from "./constants.js";
import { state } from "./state.js";
import { formatDateTime } from "./utils.js";
import { applyPaginationResult, fetchPaginated, updatePaginationControls } from "./pagination.js";
import { showError } from "./messages.js";

export function renderAllLogs() {
  const tbody = document.getElementById("logs-body");

  if (!state.allLogs.length) {
    tbody.innerHTML = `
      <tr class="empty-row">
        <td colspan="5">Записей пока нет</td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = state.allLogs.map(entry => `
    <tr class="clickable-row" data-task-id="${entry.taskId}">
      <td>${entry.id}</td>
      <td>${entry.taskId}</td>
      <td>${entry.taskName}</td>
      <td>${formatDateTime(entry.createdAt)}</td>
      <td class="log-message-cell">${entry.message}</td>
    </tr>
  `).join("");

  tbody.querySelectorAll("tr.clickable-row").forEach(row => {
    const taskId = Number(row.dataset.taskId);
    row.addEventListener("dblclick", () => {
      openLogModal(taskId).catch(showError);
    });
  });
}

export async function loadAllLogs(page = state.logsPagination.page) {
  const data = await fetchPaginated("/api/logs", page, "Не удалось загрузить журнал логов");
  const retryPage = applyPaginationResult(data, "logsPagination");
  if (retryPage !== null) {
    return loadAllLogs(retryPage);
  }

  state.allLogs = data.content;
  renderAllLogs();
  updatePaginationControls("logs", state.logsPagination);
}

export function startLogsAutoRefresh() {
  stopLogsAutoRefresh();
  state.logsRefreshTimer = window.setInterval(() => {
    if (state.currentPage === "logs" && document.visibilityState === "visible") {
      loadAllLogs().catch(error => console.error(error));
    }
  }, LOG_REFRESH_MS);
}

export function stopLogsAutoRefresh() {
  if (state.logsRefreshTimer !== null) {
    window.clearInterval(state.logsRefreshTimer);
    state.logsRefreshTimer = null;
  }
}

async function loadTaskLogs(taskId) {
  const response = await fetch(`/api/tasks/${taskId}/logs`);
  if (!response.ok) {
    throw new Error("Не удалось загрузить лог задачи");
  }

  return response.json();
}

function renderTaskLogs(logs) {
  const tbody = document.getElementById("log-body");

  if (!logs.length) {
    tbody.innerHTML = `
      <tr class="empty-row">
        <td colspan="3">Записей пока нет</td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = logs.map((entry, index) => `
    <tr>
      <td>${index + 1}</td>
      <td>${formatDateTime(entry.createdAt)}</td>
      <td>${entry.message}</td>
    </tr>
  `).join("");
}

function stopLogModalAutoRefresh() {
  if (state.logModalRefreshTimer !== null) {
    window.clearInterval(state.logModalRefreshTimer);
    state.logModalRefreshTimer = null;
  }
}

function startLogModalAutoRefresh() {
  stopLogModalAutoRefresh();

  state.logModalRefreshTimer = window.setInterval(() => {
    if (state.logTaskId !== null && !document.getElementById("log-modal-overlay").classList.contains("hidden")) {
      loadTaskLogs(state.logTaskId)
        .then(renderTaskLogs)
        .catch(error => console.error(error));
    }
  }, LOG_REFRESH_MS);
}

export async function openLogModal(taskId) {
  state.logTaskId = taskId;
  const task = state.tasks.find(item => item.id === taskId);

  document.getElementById("log-modal-title").textContent =
    task ? `Лог задачи №${task.id} — ${task.name}` : `Лог задачи №${taskId}`;

  const logs = await loadTaskLogs(taskId);
  renderTaskLogs(logs);
  document.getElementById("log-modal-overlay").classList.remove("hidden");
  startLogModalAutoRefresh();
}

export function closeLogModal() {
  state.logTaskId = null;
  stopLogModalAutoRefresh();
  document.getElementById("log-body").innerHTML = "";
  document.getElementById("log-modal-overlay").classList.add("hidden");
}

export function initLogModal() {
  document.querySelector(".log-modal-close-btn").addEventListener("click", closeLogModal);

  document.getElementById("log-modal-overlay").addEventListener("click", event => {
    if (event.target.id === "log-modal-overlay") {
      closeLogModal();
    }
  });
}
