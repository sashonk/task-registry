import { showError, initMessageModal } from "./messages.js";
import { initPagination } from "./pagination.js";
import { loadTaskTypes } from "./task-types.js";
import { loadTasks, initTasksToolbar } from "./tasks.js";
import { loadAllLogs, initLogModal } from "./logs.js";
import { initExecutorsToolbar } from "./executors.js";
import { initScheduler } from "./scheduler.js";
import { initTaskModal } from "./task-modal.js";
import { initNavigation, initEscapeHandler, showPage } from "./navigation.js";

document.addEventListener("DOMContentLoaded", () => {
  loadTaskTypes().catch(showError).finally(() => {
    initNavigation();
    initTasksToolbar();
    initExecutorsToolbar();
    initScheduler();
    initTaskModal();
    initLogModal();
    initMessageModal();
    initPagination(loadTasks, loadAllLogs);
    initEscapeHandler();
    showPage("tasks");
  });
});
