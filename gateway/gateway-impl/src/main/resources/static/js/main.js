import { showError, initMessageModal } from "./messages.js";
import { initPagination } from "./pagination.js";
import { loadTaskTypes } from "./task-types.js";
import { loadTasks, initTasksToolbar } from "./tasks.js";
import { loadAllLogs, initLogModal } from "./logs.js";
import { initAuditFilters } from "./audit.js";
import { initExecutorsToolbar } from "./executors.js";
import { initScheduler } from "./scheduler.js";
import { initTaskModal } from "./task-modal.js";
import { initMinesweeper, initStarMinesweeper } from "./minesweeper.js";
import { initColorLines } from "./color-lines.js";
import { initKlondike } from "./klondike.js";
import { initTanks } from "./tanks.js";
import { initNavigation, initEscapeHandler, showPage } from "./navigation.js";
import { ensureAuthenticated, initAuthUi, applyRoleRestrictions, getDefaultPage, isPlay } from "./auth.js";
import { state } from "./state.js";

document.addEventListener("DOMContentLoaded", () => {
  ensureAuthenticated()
    .then(() => {
      initAuthUi();
      applyRoleRestrictions();
      if (isPlay()) {
        return null;
      }
      return loadTaskTypes();
    })
    .then(() => {
      initNavigation();
      if (!isPlay()) {
        initTasksToolbar();
        initExecutorsToolbar();
        initScheduler();
        initTaskModal();
        initLogModal();
        initAuditFilters();
        initPagination(loadTasks, loadAllLogs);
      }
      initMessageModal();
      if (isPlay()) {
        initMinesweeper();
        initStarMinesweeper();
        initColorLines();
        initKlondike();
        initTanks();
      }
      initEscapeHandler();
      showPage(getDefaultPage());
    })
    .catch(error => {
      if (state.currentUser) {
        showError(error);
      }
    });
});
