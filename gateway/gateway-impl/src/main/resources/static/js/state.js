import { PAGE_SIZE } from "./constants.js";

export function createEmptyPagination() {
  return {
    page: 1,
    size: PAGE_SIZE,
    totalElements: 0,
    totalPages: 1,
    from: 0,
    to: 0
  };
}

export const state = {
  taskTypes: [],
  taskTypeHints: {},
  tasks: [],
  executors: [],
  allLogs: [],
  schedules: [],
  tasksPagination: createEmptyPagination(),
  logsPagination: createEmptyPagination(),
  editingTaskId: null,
  selectedTaskId: null,
  selectedScheduleId: null,
  selectedExecutorId: null,
  currentPage: "tasks",
  tasksRefreshTimer: null,
  logsRefreshTimer: null,
  schedulerRefreshTimer: null,
  logTaskId: null,
  logModalRefreshTimer: null,
  messageModalResolver: null,
  messageModalMode: "alert",
  currentUser: null
};
