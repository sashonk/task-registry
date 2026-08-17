import { apiFetch } from "./api.js";
import { TASKS_REFRESH_MS } from "./constants.js";
import { state } from "./state.js";
import { mapTaskFromApi } from "./utils.js";
import { applyPaginationResult, fetchPaginated, updatePaginationControls } from "./pagination.js";
import { showError, showConfirm, showMessage } from "./messages.js";
import { openLogModal } from "./logs.js";

export function selectTask(taskId) {
  state.selectedTaskId = taskId;

  document.querySelectorAll("#tasks-body tr").forEach(row => {
    row.classList.toggle("selected", Number(row.dataset.id) === taskId);
  });
}

export function renderTasks() {
  const tbody = document.getElementById("tasks-body");

  if (!state.tasks.length) {
    tbody.innerHTML = `
      <tr class="empty-row">
        <td colspan="6">Нет задач</td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = state.tasks.map(task => `
    <tr class="clickable-row${task.id === state.selectedTaskId ? " selected" : ""}" data-id="${task.id}">
      <td>${task.id}</td>
      <td>${task.name}</td>
      <td>${task.executor}</td>
      <td>${task.datetime}</td>
      <td>
        <div class="duke-bar">
          <div class="duke-bar-fill" style="width: ${task.completionPercent ?? 0}%"></div>
          <span class="duke-bar-label">${task.completionPercent ?? 0}%</span>
        </div>
      </td>
      <td>
        <span class="status-cell">
          <span class="status-badge status-badge-${task.status}">${task.statusText}</span>
        </span>
      </td>
    </tr>
  `).join("");

  tbody.querySelectorAll("tr.clickable-row").forEach(row => {
    const taskId = Number(row.dataset.id);

    row.addEventListener("click", () => selectTask(taskId));
    row.addEventListener("dblclick", () => {
      openLogModal(taskId).catch(showError);
    });
  });
}

export async function loadTasks(page = state.tasksPagination.page) {
  const data = await fetchPaginated("/api/tasks", page, "Не удалось загрузить задачи");
  const retryPage = applyPaginationResult(data, "tasksPagination");
  if (retryPage !== null) {
    return loadTasks(retryPage);
  }

  state.tasks = data.content.map(mapTaskFromApi);

  if (state.selectedTaskId && !state.tasks.some(task => task.id === state.selectedTaskId)) {
    state.selectedTaskId = null;
  }

  renderTasks();
  updatePaginationControls("tasks", state.tasksPagination);
}

export function startTasksAutoRefresh() {
  stopTasksAutoRefresh();
  state.tasksRefreshTimer = window.setInterval(() => {
    if (state.currentPage === "tasks" && document.visibilityState === "visible") {
      loadTasks().catch(error => console.error(error));
    }
  }, TASKS_REFRESH_MS);
}

export function stopTasksAutoRefresh() {
  if (state.tasksRefreshTimer !== null) {
    window.clearInterval(state.tasksRefreshTimer);
    state.tasksRefreshTimer = null;
  }
}

export async function abortSelectedTask() {
  if (!state.selectedTaskId) {
    await showMessage("Выберите задачу в таблице.", "Внимание");
    return;
  }

  const task = state.tasks.find(item => item.id === state.selectedTaskId);
  if (!task) {
    return;
  }

  if (task.apiStatus !== "IN_PROGRESS") {
    await showMessage("Прервать можно только выполняющуюся задачу.", "Внимание");
    return;
  }

  const response = await apiFetch(`/api/tasks/${state.selectedTaskId}/status`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ status: "ABORTED" })
  });

  if (!response.ok) {
    showError(new Error("Не удалось прервать задачу"));
    return;
  }

  await loadTasks();
}

export async function deleteSelectedTask() {
  if (!state.selectedTaskId) {
    await showMessage("Выберите задачу в таблице.", "Внимание");
    return;
  }

  const task = state.tasks.find(item => item.id === state.selectedTaskId);
  if (!task) {
    return;
  }

  const confirmed = await showConfirm(`Удалить задачу «${task.name}»?`, "Удаление задачи");
  if (!confirmed) {
    return;
  }

  await showMessage("Удаление задач через API пока не реализовано.", "Информация");
}

export function initTasksToolbar() {
  document.getElementById("btn-abort-task").addEventListener("click", () => {
    abortSelectedTask().catch(showError);
  });
  document.getElementById("btn-delete-task").addEventListener("click", () => {
    deleteSelectedTask().catch(showError);
  });
}
