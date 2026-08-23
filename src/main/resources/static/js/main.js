import { showError, initMessageModal } from "./messages.js";
import { initPagination } from "./pagination.js";
import { loadTaskTypes } from "./task-types.js";
import { loadTasks, initTasksToolbar } from "./tasks.js";
import { loadAllLogs, initLogModal } from "./logs.js";
import { initExecutorsToolbar } from "./executors.js";
import { initScheduler } from "./scheduler.js";
import { initTaskModal } from "./task-modal.js";
import { initMinesweeper } from "./minesweeper.js";
import { initNavigation, initEscapeHandler, showPage } from "./navigation.js";
import { ensureAuthenticated, initAuthUi, applyRoleRestrictions } from "./auth.js";
import { state } from "./state.js";

document.addEventListener("DOMContentLoaded", () => {
  ensureAuthenticated()
    .then(() => {
      initAuthUi();
      applyRoleRestrictions();
      return loadTaskTypes();
    })
    .then(() => {
      initNavigation();
      initTasksToolbar();
      initExecutorsToolbar();
      initScheduler();
      initTaskModal();
      initLogModal();
      initMessageModal();
      initMinesweeper();
      initPagination(loadTasks, loadAllLogs);
      initEscapeHandler();
      showPage("tasks");
    })
    .catch(error => {
      if (state.currentUser) {
        showError(error);
      }
    });
});
