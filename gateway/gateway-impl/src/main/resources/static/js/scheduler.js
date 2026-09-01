import { SCHEDULER_REFRESH_MS, TYPES_WITHOUT_PARAMETERS, TYPES_WITH_OPTIONAL_PARAMETERS } from "./constants.js";
import { apiFetch } from "./api.js";
import { state } from "./state.js";
import { formatDateTime, defaultScheduleDateTime, toUtcIsoFromDateTimeLocal } from "./utils.js";
import { validateBatchBuilder } from "./batch-builder.js";
import { readParametersPayload, updateScheduleParametersFieldVisibility } from "./parameters.js";
import { showError, showConfirm, showMessage } from "./messages.js";
import { addBatchStep } from "./batch-builder.js";

function selectSchedule(scheduleId) {
  state.selectedScheduleId = scheduleId;

  document.querySelectorAll("#scheduler-body tr").forEach(row => {
    row.classList.toggle("selected", Number(row.dataset.id) === scheduleId);
  });
}

export function renderSchedules() {
  const tbody = document.getElementById("scheduler-body");

  if (!state.schedules.length) {
    tbody.innerHTML = `
      <tr class="empty-row">
        <td colspan="6">Нет расписаний</td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = state.schedules.map(schedule => `
    <tr class="clickable-row${schedule.id === state.selectedScheduleId ? " selected" : ""}" data-id="${schedule.id}">
      <td>${schedule.id}</td>
      <td>${schedule.taskName}</td>
      <td>${formatDateTime(schedule.nextRunAt)}</td>
      <td>${schedule.repeatText}</td>
      <td>
        <span class="status-badge status-badge-${schedule.enabled ? "done" : "aborted"}">${schedule.statusText}</span>
      </td>
      <td>${formatDateTime(schedule.lastTriggeredAt)}</td>
    </tr>
  `).join("");

  tbody.querySelectorAll("tr.clickable-row").forEach(row => {
    const scheduleId = Number(row.dataset.id);
    row.addEventListener("click", () => selectSchedule(scheduleId));
  });
}

export async function loadSchedules() {
  const response = await apiFetch("/api/schedules");
  if (!response.ok) {
    throw new Error("Не удалось загрузить расписание");
  }

  state.schedules = await response.json();
  renderSchedules();
}

export function startSchedulerAutoRefresh() {
  stopSchedulerAutoRefresh();
  state.schedulerRefreshTimer = window.setInterval(() => {
    if (state.currentPage === "scheduler" && document.visibilityState === "visible") {
      loadSchedules().catch(error => console.error(error));
    }
  }, SCHEDULER_REFRESH_MS);
}

export function stopSchedulerAutoRefresh() {
  if (state.schedulerRefreshTimer !== null) {
    window.clearInterval(state.schedulerRefreshTimer);
    state.schedulerRefreshTimer = null;
  }
}

function openScheduleModal() {
  document.getElementById("schedule-type").value = state.taskTypes[0]?.value || "CALCULATION";
  document.getElementById("schedule-parameters-formula").value = "";
  document.getElementById("schedule-parameters-json").value = "";
  document.getElementById("schedule-batch-steps").innerHTML = "";
  document.getElementById("schedule-next-run").value = defaultScheduleDateTime();
  document.getElementById("schedule-repeat").value = "";
  updateScheduleParametersFieldVisibility();
  document.getElementById("schedule-modal-overlay").classList.remove("hidden");
}

function closeScheduleModal() {
  document.getElementById("schedule-form").reset();
  document.getElementById("schedule-parameters-formula").value = "";
  document.getElementById("schedule-parameters-json").value = "";
  document.getElementById("schedule-parameters-row").classList.add("hidden");
  document.getElementById("schedule-batch-builder").classList.add("hidden");
  document.getElementById("schedule-batch-steps").innerHTML = "";
  document.getElementById("schedule-modal-window").classList.remove("batch-modal-window");
  document.getElementById("schedule-modal-overlay").classList.add("hidden");
}

async function createSchedule(event) {
  event.preventDefault();

  const type = document.getElementById("schedule-type").value;
  const nextRunAt = document.getElementById("schedule-next-run").value;
  const repeatRaw = document.getElementById("schedule-repeat").value.trim();

  if (type === "BATCH") {
    const batchError = validateBatchBuilder("schedule");
    if (batchError) {
      await showMessage(batchError, "Внимание");
      return;
    }
  }

  const parameters = readParametersPayload(type, "schedule");

  if (!nextRunAt) {
    await showMessage("Укажите дату и время запуска.", "Внимание");
    return;
  }

  const nextRunAtUtc = toUtcIsoFromDateTimeLocal(nextRunAt);
  if (!nextRunAtUtc) {
    await showMessage("Некорректная дата и время запуска.", "Внимание");
    return;
  }

  if (!TYPES_WITHOUT_PARAMETERS.has(type) && !TYPES_WITH_OPTIONAL_PARAMETERS.has(type) && !parameters) {
    await showMessage("Укажите параметры расписания.", "Внимание");
    return;
  }

  const payload = {
    taskType: type,
    nextRunAt: nextRunAtUtc
  };

  if (parameters) {
    payload.parameters = parameters;
  }

  if (repeatRaw) {
    const repeatIntervalMinutes = Number(repeatRaw);
    if (!Number.isFinite(repeatIntervalMinutes) || repeatIntervalMinutes <= 0) {
      await showMessage("Интервал повтора должен быть положительным числом.", "Внимание");
      return;
    }
    payload.repeatIntervalMinutes = repeatIntervalMinutes;
  }

  const response = await apiFetch("/api/schedules", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });

  if (!response.ok) {
    const errorBody = await response.json().catch(() => null);
    throw new Error(errorBody?.error || "Не удалось создать расписание");
  }

  const schedule = await response.json();
  state.selectedScheduleId = schedule.id;
  closeScheduleModal();
  await loadSchedules();
}

async function toggleSelectedSchedule() {
  if (!state.selectedScheduleId) {
    await showMessage("Выберите расписание в таблице.", "Внимание");
    return;
  }

  const schedule = state.schedules.find(item => item.id === state.selectedScheduleId);
  if (!schedule) {
    return;
  }

  const response = await apiFetch(`/api/schedules/${state.selectedScheduleId}/enabled`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ enabled: !schedule.enabled })
  });

  if (!response.ok) {
    throw new Error("Не удалось изменить статус расписания");
  }

  await loadSchedules();
}

async function deleteSelectedSchedule() {
  if (!state.selectedScheduleId) {
    await showMessage("Выберите расписание в таблице.", "Внимание");
    return;
  }

  const schedule = state.schedules.find(item => item.id === state.selectedScheduleId);
  if (!schedule) {
    return;
  }

  const confirmed = await showConfirm(`Удалить расписание «${schedule.taskName}»?`, "Удаление расписания");
  if (!confirmed) {
    return;
  }

  const response = await apiFetch(`/api/schedules/${state.selectedScheduleId}`, {
    method: "DELETE"
  });

  if (!response.ok) {
    throw new Error("Не удалось удалить расписание");
  }

  state.selectedScheduleId = null;
  await loadSchedules();
}

export function initScheduler() {
  document.getElementById("btn-new-schedule").addEventListener("click", openScheduleModal);
  document.getElementById("btn-toggle-schedule").addEventListener("click", () => {
    toggleSelectedSchedule().catch(showError);
  });
  document.getElementById("btn-delete-schedule").addEventListener("click", () => {
    deleteSelectedSchedule().catch(showError);
  });
  document.getElementById("schedule-form").addEventListener("submit", event => {
    createSchedule(event).catch(showError);
  });
  document.getElementById("schedule-cancel-btn").addEventListener("click", closeScheduleModal);
  document.querySelector(".schedule-modal-close-btn").addEventListener("click", closeScheduleModal);
  document.getElementById("schedule-type").addEventListener("change", updateScheduleParametersFieldVisibility);
  document.getElementById("schedule-batch-add-step").addEventListener("click", () => addBatchStep("schedule"));

  document.getElementById("schedule-modal-overlay").addEventListener("click", event => {
    if (event.target.id === "schedule-modal-overlay") {
      closeScheduleModal();
    }
  });
}

export { closeScheduleModal };
