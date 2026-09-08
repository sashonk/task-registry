import { PAGE_TITLES, GAME_PAGES } from "./constants.js";
import { state } from "./state.js";
import { getDefaultPage, isAdmin, isPlay } from "./auth.js";
import { showError, closeMessageModal } from "./messages.js";
import { loadTasks, startTasksAutoRefresh, stopTasksAutoRefresh } from "./tasks.js";
import { loadAllLogs, startLogsAutoRefresh, stopLogsAutoRefresh } from "./logs.js";
import { loadExecutors } from "./executors.js";
import { loadSchedules, startSchedulerAutoRefresh, stopSchedulerAutoRefresh, closeScheduleModal } from "./scheduler.js";
import { loadTaskTypes } from "./task-types.js";
import { loadAuditEvents, closeAuditEventModal } from "./audit.js";
import { closeModal } from "./task-modal.js";
import { closeLogModal } from "./logs.js";

export function showPage(page) {
  const isGamePage = GAME_PAGES.includes(page);
  if ((isPlay() && !isGamePage) || (!isPlay() && isGamePage)) {
    page = getDefaultPage();
  }

  state.currentPage = page;

  document.querySelectorAll(".page").forEach(section => {
    section.classList.add("hidden");
  });
  document.getElementById(`page-${page}`).classList.remove("hidden");

  document.querySelectorAll(".nav-btn").forEach(btn => {
    btn.classList.toggle("active", btn.dataset.page === page);
  });

  document.getElementById("tasks-toolbar").classList.toggle("hidden", page !== "tasks" || !isAdmin());
  document.getElementById("executors-toolbar").classList.toggle("hidden", page !== "executors" || !isAdmin());
  document.getElementById("scheduler-toolbar").classList.toggle("hidden", page !== "scheduler" || !isAdmin());
  document.title = PAGE_TITLES[page];

  stopTasksAutoRefresh();
  stopLogsAutoRefresh();
  stopSchedulerAutoRefresh();

  if (page === "tasks") {
    loadTasks().catch(showError);
    startTasksAutoRefresh();
  } else if (page === "executors") {
    loadExecutors().catch(showError);
  } else if (page === "logs") {
    loadAllLogs().catch(showError);
    startLogsAutoRefresh();
  } else if (page === "scheduler") {
    loadSchedules().catch(showError);
    startSchedulerAutoRefresh();
  } else if (page === "task-types") {
    loadTaskTypes().catch(showError);
  } else if (page === "audit") {
    loadAuditEvents().catch(showError);
  }
}

export function initNavigation() {
  document.querySelectorAll(".nav-btn").forEach(btn => {
    btn.addEventListener("click", () => showPage(btn.dataset.page));
  });
}

export function initEscapeHandler() {
  document.addEventListener("keydown", event => {
    if (event.key !== "Escape") {
      return;
    }

    if (!document.getElementById("message-modal-overlay").classList.contains("hidden")) {
      closeMessageModal(state.messageModalMode === "confirm" ? false : true);
      return;
    }

    if (!document.getElementById("schedule-modal-overlay").classList.contains("hidden")) {
      closeScheduleModal();
      return;
    }

    if (!document.getElementById("log-modal-overlay").classList.contains("hidden")) {
      closeLogModal();
      return;
    }

    if (!document.getElementById("audit-modal-overlay").classList.contains("hidden")) {
      closeAuditEventModal();
      return;
    }

    if (!document.getElementById("modal-overlay").classList.contains("hidden")) {
      closeModal();
    }
  });
}
