const STATUS_MAP = {
  NEW: { css: "pending", text: "Ожидает" },
  IN_PROGRESS: { css: "running", text: "Выполняется" },
  DONE: { css: "done", text: "Выполнено" },
  ERROR: { css: "error", text: "Ошибка" },
  ABORTED: { css: "aborted", text: "Прервано" }
};

const PAGE_TITLES = {
  tasks: "Task Registry — Задачи",
  executors: "Task Registry — Исполнители",
  logs: "Task Registry — Логи",
  scheduler: "Task Registry — Планировщик",
  "task-types": "Task Registry — Типы задач",
  empty: "Task Registry — Пустой раздел",
  help: "Task Registry — Справка"
};

const TASKS_REFRESH_MS = 2000;
const SCHEDULER_REFRESH_MS = 2000;

let taskTypes = [];
let taskTypeHints = {};

const TASK_TYPE_HINTS = {
  CALCULATION: "Формула, например: 2 + 3 * (4 - 1)",
  DELAY: '{"durationSeconds": 30}',
  RANDOM: '{"min": 1, "max": 100}',
  TEXT_TRANSFORM: '{"text": "Hello", "operation": "upper"}',
  HASH: '{"text": "hello", "algorithm": "SHA-256"}',
  HTTP_REQUEST: '{"url": "http://localhost:8080/api/tasks", "method": "GET", "timeoutSeconds": 10}',
  FILE_CHECK: '{"path": ".", "check": "exists"}',
  SQL_QUERY: '{"sql": "SELECT COUNT(*) AS cnt FROM tasks"}',
  REPORT: '{"days": 7}',
  CLEANUP: '{"target": "logs", "daysOld": 30}',
  BATCH: '{"steps": [{"type": "RANDOM", "parameters": {"min": 1, "max": 10}}, {"type": "CALCULATION", "parameters": "2+2"}]}',
  SIMULATION: '{"durationSeconds": 15, "intensity": 50}',
  SCRIPT: '{"script": "log(\\"Hello from Groovy\\"); 2 + 2"}'
};

const TYPES_WITHOUT_PARAMETERS = new Set();
const TYPES_WITH_OPTIONAL_PARAMETERS = new Set(["REPORT"]);

function getBatchStepTypes() {
  return taskTypes.filter(type => type.value !== "BATCH");
}

const BATCH_STEP_FIELDS = {
  CALCULATION: [
    { key: "formula", label: "Формула", type: "text", wide: true, required: true, placeholder: "2 + 3 * (4 - 1)" }
  ],
  DELAY: [
    { key: "durationSeconds", label: "Длительность (сек.)", type: "number", required: true, min: 1, defaultValue: "30" }
  ],
  RANDOM: [
    { key: "min", label: "Минимум", type: "number", required: true, defaultValue: "1" },
    { key: "max", label: "Максимум", type: "number", required: true, defaultValue: "100" }
  ],
  TEXT_TRANSFORM: [
    { key: "text", label: "Текст", type: "text", wide: true, required: true, placeholder: "Hello" },
    {
      key: "operation",
      label: "Операция",
      type: "select",
      required: true,
      options: [
        { value: "upper", label: "В верхний регистр" },
        { value: "lower", label: "В нижний регистр" },
        { value: "reverse", label: "Развернуть" },
        { value: "trim", label: "Обрезить пробелы" }
      ],
      defaultValue: "upper"
    }
  ],
  HASH: [
    { key: "text", label: "Текст", type: "text", wide: true, required: true, placeholder: "hello" },
    {
      key: "algorithm",
      label: "Алгоритм",
      type: "select",
      required: true,
      options: [
        { value: "MD5", label: "MD5" },
        { value: "SHA-256", label: "SHA-256" }
      ],
      defaultValue: "SHA-256"
    }
  ],
  HTTP_REQUEST: [
    { key: "url", label: "URL", type: "text", wide: true, required: true, placeholder: "http://localhost:8080/api/tasks" },
    {
      key: "method",
      label: "Метод",
      type: "select",
      required: true,
      options: [
        { value: "GET", label: "GET" },
        { value: "POST", label: "POST" },
        { value: "PUT", label: "PUT" },
        { value: "PATCH", label: "PATCH" },
        { value: "DELETE", label: "DELETE" },
        { value: "HEAD", label: "HEAD" }
      ],
      defaultValue: "GET"
    },
    { key: "timeoutSeconds", label: "Таймаут (сек.)", type: "number", min: 1, defaultValue: "10" },
    { key: "body", label: "Тело запроса", type: "textarea", wide: true, rows: 2, placeholder: "Необязательно" }
  ],
  FILE_CHECK: [
    { key: "path", label: "Путь (в ./data)", type: "text", wide: true, required: true, placeholder: ".", defaultValue: "." },
    {
      key: "check",
      label: "Проверка",
      type: "select",
      required: true,
      options: [
        { value: "exists", label: "Существует" },
        { value: "size", label: "Размер" },
        { value: "isDirectory", label: "Это каталог" }
      ],
      defaultValue: "exists"
    }
  ],
  SQL_QUERY: [
    { key: "sql", label: "SQL (только SELECT)", type: "textarea", wide: true, required: true, rows: 3, defaultValue: "SELECT COUNT(*) AS cnt FROM tasks" }
  ],
  REPORT: [
    { key: "days", label: "За последние N дней", type: "number", min: 1, placeholder: "Все задачи, если пусто" }
  ],
  CLEANUP: [
    {
      key: "target",
      label: "Что очищать",
      type: "select",
      required: true,
      options: [
        { value: "logs", label: "Логи" },
        { value: "tasks", label: "Завершённые задачи" }
      ],
      defaultValue: "logs"
    },
    { key: "daysOld", label: "Старше (дней)", type: "number", required: true, min: 1, defaultValue: "30" }
  ],
  SIMULATION: [
    { key: "durationSeconds", label: "Длительность (сек.)", type: "number", required: true, min: 1, defaultValue: "15" },
    { key: "intensity", label: "Интенсивность (1–100)", type: "number", min: 1, max: 100, defaultValue: "50" }
  ],
  SCRIPT: [
    { key: "script", label: "Groovy-скрипт", type: "textarea", wide: true, required: true, rows: 4, defaultValue: 'log("step"); 1 + 1' }
  ]
};

let tasks = [];
let executors = [];
let allLogs = [];
let schedules = [];
let editingTaskId = null;
let selectedTaskId = null;
let selectedScheduleId = null;
let currentPage = "tasks";
let tasksRefreshTimer = null;
let logsRefreshTimer = null;
let schedulerRefreshTimer = null;
let logTaskId = null;
let logModalRefreshTimer = null;
const LOG_REFRESH_MS = 2000;

let messageModalResolver = null;
let messageModalMode = "alert";

function formatDateTime(value) {
  if (!value) {
    return "—";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  const pad = part => String(part).padStart(2, "0");
  return `${pad(date.getDate())}.${pad(date.getMonth() + 1)}.${date.getFullYear()} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function mapTaskFromApi(task) {
  const status = STATUS_MAP[task.status] || { css: "pending", text: task.status };
  return {
    id: task.id,
    name: task.name,
    executor: task.executor || "—",
    datetime: formatDateTime(task.launchDateTime),
    status: status.css,
    statusText: status.text,
    apiStatus: task.status,
    completionPercent: task.completionPercent
  };
}

async function loadTasks() {
  const response = await fetch("/api/tasks");
  if (!response.ok) {
    throw new Error("Не удалось загрузить задачи");
  }

  const data = await response.json();
  tasks = data.map(mapTaskFromApi);
  renderTasks();
}

function startTasksAutoRefresh() {
  stopTasksAutoRefresh();
  tasksRefreshTimer = window.setInterval(() => {
    if (currentPage === "tasks" && document.visibilityState === "visible") {
      loadTasks().catch(error => console.error(error));
    }
  }, TASKS_REFRESH_MS);
}

function stopTasksAutoRefresh() {
  if (tasksRefreshTimer !== null) {
    window.clearInterval(tasksRefreshTimer);
    tasksRefreshTimer = null;
  }
}

function startLogsAutoRefresh() {
  stopLogsAutoRefresh();
  logsRefreshTimer = window.setInterval(() => {
    if (currentPage === "logs" && document.visibilityState === "visible") {
      loadAllLogs().catch(error => console.error(error));
    }
  }, LOG_REFRESH_MS);
}

function stopLogsAutoRefresh() {
  if (logsRefreshTimer !== null) {
    window.clearInterval(logsRefreshTimer);
    logsRefreshTimer = null;
  }
}

async function loadAllLogs() {
  const response = await fetch("/api/logs");
  if (!response.ok) {
    throw new Error("Не удалось загрузить журнал логов");
  }

  allLogs = await response.json();
  renderAllLogs();
}

function renderAllLogs() {
  const tbody = document.getElementById("logs-body");

  if (!allLogs.length) {
    tbody.innerHTML = `
      <tr class="empty-row">
        <td colspan="5">Записей пока нет</td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = allLogs.map(entry => `
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

async function loadExecutors() {
  const response = await fetch("/api/executors");
  if (!response.ok) {
    throw new Error("Не удалось загрузить исполнителей");
  }

  executors = await response.json();
  renderExecutors();
}

async function loadSchedules() {
  const response = await fetch("/api/schedules");
  if (!response.ok) {
    throw new Error("Не удалось загрузить расписание");
  }

  schedules = await response.json();
  renderSchedules();
}

function startSchedulerAutoRefresh() {
  stopSchedulerAutoRefresh();
  schedulerRefreshTimer = window.setInterval(() => {
    if (currentPage === "scheduler" && document.visibilityState === "visible") {
      loadSchedules().catch(error => console.error(error));
    }
  }, SCHEDULER_REFRESH_MS);
}

function stopSchedulerAutoRefresh() {
  if (schedulerRefreshTimer !== null) {
    window.clearInterval(schedulerRefreshTimer);
    schedulerRefreshTimer = null;
  }
}

function selectSchedule(scheduleId) {
  selectedScheduleId = scheduleId;

  document.querySelectorAll("#scheduler-body tr").forEach(row => {
    row.classList.toggle("selected", Number(row.dataset.id) === scheduleId);
  });
}

function renderSchedules() {
  const tbody = document.getElementById("scheduler-body");

  if (!schedules.length) {
    tbody.innerHTML = `
      <tr class="empty-row">
        <td colspan="6">Нет расписаний</td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = schedules.map(schedule => `
    <tr class="clickable-row${schedule.id === selectedScheduleId ? " selected" : ""}" data-id="${schedule.id}">
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

function showPage(page) {
  currentPage = page;

  document.querySelectorAll(".page").forEach(section => {
    section.classList.add("hidden");
  });
  document.getElementById(`page-${page}`).classList.remove("hidden");

  document.querySelectorAll(".nav-btn").forEach(btn => {
    btn.classList.toggle("active", btn.dataset.page === page);
  });

  document.getElementById("tasks-toolbar").classList.toggle("hidden", page !== "tasks");
  document.getElementById("executors-toolbar").classList.toggle("hidden", page !== "executors");
  document.getElementById("scheduler-toolbar").classList.toggle("hidden", page !== "scheduler");
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
  }
}

function selectTask(taskId) {
  selectedTaskId = taskId;

  document.querySelectorAll("#tasks-body tr").forEach(row => {
    row.classList.toggle("selected", Number(row.dataset.id) === taskId);
  });
}

function renderTasks() {
  const tbody = document.getElementById("tasks-body");

  if (!tasks.length) {
    tbody.innerHTML = `
      <tr class="empty-row">
        <td colspan="6">Нет задач</td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = tasks.map(task => `
    <tr class="clickable-row${task.id === selectedTaskId ? " selected" : ""}" data-id="${task.id}">
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
  if (logModalRefreshTimer !== null) {
    window.clearInterval(logModalRefreshTimer);
    logModalRefreshTimer = null;
  }
}

function startLogModalAutoRefresh() {
  stopLogModalAutoRefresh();

  logModalRefreshTimer = window.setInterval(() => {
    if (logTaskId !== null && !document.getElementById("log-modal-overlay").classList.contains("hidden")) {
      loadTaskLogs(logTaskId)
        .then(renderTaskLogs)
        .catch(error => console.error(error));
    }
  }, LOG_REFRESH_MS);
}

async function openLogModal(taskId) {
  logTaskId = taskId;
  const task = tasks.find(item => item.id === taskId);

  document.getElementById("log-modal-title").textContent =
    task ? `Лог задачи №${task.id} — ${task.name}` : `Лог задачи №${taskId}`;

  const logs = await loadTaskLogs(taskId);
  renderTaskLogs(logs);
  document.getElementById("log-modal-overlay").classList.remove("hidden");
  startLogModalAutoRefresh();
}

function closeLogModal() {
  logTaskId = null;
  stopLogModalAutoRefresh();
  document.getElementById("log-body").innerHTML = "";
  document.getElementById("log-modal-overlay").classList.add("hidden");
}

function renderExecutors() {
  const tbody = document.getElementById("executors-body");

  if (!executors.length) {
    tbody.innerHTML = `
      <tr class="empty-row">
        <td colspan="6">Нет исполнителей</td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = executors.map(executor => `
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

function setModalMode(mode) {
  document.querySelectorAll(".edit-only").forEach(row => {
    row.classList.toggle("hidden", mode === "create");
  });
  updateParametersFieldVisibility();
}

function populateTaskTypeSelect(selectId) {
  const select = document.getElementById(selectId);
  select.innerHTML = taskTypes.map(type => `<option value="${type.value}">${type.label}</option>`).join("");
}

async function loadTaskTypes() {
  const response = await fetch("/api/task-types");
  if (!response.ok) {
    throw new Error("Не удалось загрузить справочник типов задач");
  }

  const data = await response.json();
  taskTypes = data.map(type => ({
    value: type.code,
    label: type.name
  }));

  taskTypeHints = Object.fromEntries(
    data.filter(type => type.example).map(type => [type.code, type.example])
  );

  populateTaskTypeSelect("edit-type");
  populateTaskTypeSelect("schedule-type");
  renderTaskTypes(data);
}

function renderTaskTypes(catalog) {
  const tbody = document.getElementById("task-types-body");
  if (!tbody) {
    return;
  }

  if (!catalog.length) {
    tbody.innerHTML = `
      <tr class="empty-row">
        <td colspan="5">Нет данных</td>
      </tr>
    `;
    return;
  }

  tbody.innerHTML = catalog.map(type => `
    <tr>
      <td class="type-code">${type.code}</td>
      <td>${type.name}</td>
      <td class="type-description">${type.description}</td>
      <td class="type-parameters">${type.parametersDescription}${type.requiresParameters ? "" : " (необязательно)"}</td>
      <td class="type-example"><code>${escapeHtml(type.example || "—")}</code></td>
    </tr>
  `).join("");
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function getTaskTypeHint(type) {
  return taskTypeHints[type] || TASK_TYPE_HINTS[type] || "";
}

function configureParametersField(prefix, type, isCreate) {
  const row = document.getElementById(`${prefix}-parameters-row`);
  const batchBuilder = document.getElementById(`${prefix}-batch-builder`);
  const formulaInput = document.getElementById(`${prefix}-parameters-formula`);
  const jsonInput = document.getElementById(`${prefix}-parameters-json`);
  const hint = document.getElementById(`${prefix}-parameters-hint`);
  const label = document.getElementById(`${prefix}-parameters-label`);
  const showRow = isCreate && !TYPES_WITHOUT_PARAMETERS.has(type);
  const isBatch = type === "BATCH";

  if (prefix === "edit") {
    document.getElementById("task-modal-window").classList.toggle("batch-modal-window", isBatch && isCreate);
  } else if (prefix === "schedule") {
    document.getElementById("schedule-modal-window").classList.toggle("batch-modal-window", isBatch);
  }

  if (batchBuilder) {
    batchBuilder.classList.toggle("hidden", !showRow || !isBatch);
  }

  row.classList.toggle("hidden", !showRow || isBatch);
  formulaInput.classList.add("hidden");
  jsonInput.classList.add("hidden");
  formulaInput.required = false;
  jsonInput.required = false;

  if (!showRow) {
    if (hint) {
      hint.textContent = "";
    }
    return;
  }

  if (isBatch) {
    if (batchBuilder && !document.getElementById(`${prefix}-batch-steps`).children.length) {
      resetBatchBuilder(prefix);
    }
    return;
  }

  if (type === "CALCULATION") {
    label.textContent = "Формула";
    formulaInput.classList.remove("hidden");
    formulaInput.required = true;
  }
  else {
    label.textContent = "Параметры (JSON)";
    jsonInput.classList.remove("hidden");
    jsonInput.required = !TYPES_WITH_OPTIONAL_PARAMETERS.has(type);
    if (!jsonInput.value.trim()) {
      const hint = getTaskTypeHint(type);
      if (hint) {
        jsonInput.value = hint;
      }
    }
  }

  hint.textContent = getTaskTypeHint(type) || "";
}

function resetBatchBuilder(prefix) {
  const container = document.getElementById(`${prefix}-batch-steps`);
  container.innerHTML = "";
  addBatchStep(prefix, "RANDOM");
}

function addBatchStep(prefix, initialType = "CALCULATION") {
  const container = document.getElementById(`${prefix}-batch-steps`);
  const stepElement = document.createElement("div");
  stepElement.className = "batch-step";
  stepElement.innerHTML = `
    <div class="batch-step-header">
      <span class="batch-step-index">Шаг</span>
      <select class="batch-step-type"></select>
      <div class="batch-step-actions">
        <button type="button" class="batch-step-action batch-step-up" title="Выше">▲</button>
        <button type="button" class="batch-step-action batch-step-down" title="Ниже">▼</button>
        <button type="button" class="batch-step-action batch-step-remove" title="Удалить">×</button>
      </div>
    </div>
    <div class="batch-step-fields"></div>
  `;

  const typeSelect = stepElement.querySelector(".batch-step-type");
  typeSelect.innerHTML = getBatchStepTypes().map(type => `<option value="${type.value}">${type.label}</option>`).join("");
  typeSelect.value = initialType;
  typeSelect.addEventListener("change", () => renderBatchStepFields(stepElement));

  stepElement.querySelector(".batch-step-up").addEventListener("click", () => moveBatchStep(prefix, stepElement, -1));
  stepElement.querySelector(".batch-step-down").addEventListener("click", () => moveBatchStep(prefix, stepElement, 1));
  stepElement.querySelector(".batch-step-remove").addEventListener("click", () => removeBatchStep(prefix, stepElement));

  container.appendChild(stepElement);
  renderBatchStepFields(stepElement);
  reindexBatchSteps(prefix);
}

function renderBatchStepFields(stepElement) {
  const type = stepElement.querySelector(".batch-step-type").value;
  const fieldsContainer = stepElement.querySelector(".batch-step-fields");
  const fields = BATCH_STEP_FIELDS[type] || [];

  if (!fields.length) {
    fieldsContainer.innerHTML = `<div class="batch-step-empty">Параметры не требуются</div>`;
    return;
  }

  fieldsContainer.innerHTML = fields.map(field => {
    const fieldClass = field.wide ? "batch-step-field batch-step-field-wide" : "batch-step-field";
    if (field.type === "select") {
      const options = (field.options || []).map(option =>
        `<option value="${option.value}">${option.label}</option>`
      ).join("");
      return `
        <div class="${fieldClass}">
          <label>${field.label}</label>
          <select class="batch-field-input" data-key="${field.key}" data-field-type="select" ${field.required ? "required" : ""}>${options}</select>
        </div>
      `;
    }

    if (field.type === "textarea") {
      return `
        <div class="${fieldClass}">
          <label>${field.label}</label>
          <textarea class="batch-field-input" data-key="${field.key}" data-field-type="textarea" rows="${field.rows || 3}" ${field.required ? "required" : ""} placeholder="${field.placeholder || ""}">${field.defaultValue || ""}</textarea>
        </div>
      `;
    }

    return `
      <div class="${fieldClass}">
        <label>${field.label}</label>
        <input
          class="batch-field-input"
          data-key="${field.key}"
          data-field-type="${field.type}"
          type="${field.type}"
          value="${field.defaultValue || ""}"
          ${field.required ? "required" : ""}
          ${field.min !== undefined ? `min="${field.min}"` : ""}
          ${field.max !== undefined ? `max="${field.max}"` : ""}
          placeholder="${field.placeholder || ""}"
        >
      </div>
    `;
  }).join("");

  fields.forEach(field => {
    if (field.type !== "select") {
      return;
    }
    const select = fieldsContainer.querySelector(`[data-key="${field.key}"]`);
    if (select && field.defaultValue) {
      select.value = field.defaultValue;
    }
  });
}

function reindexBatchSteps(prefix) {
  const steps = document.querySelectorAll(`#${prefix}-batch-steps .batch-step`);
  steps.forEach((step, index) => {
    step.querySelector(".batch-step-index").textContent = `Шаг ${index + 1}`;
    step.querySelector(".batch-step-up").disabled = index === 0;
    step.querySelector(".batch-step-down").disabled = index === steps.length - 1;
    step.querySelector(".batch-step-remove").disabled = steps.length === 1;
  });
}

function moveBatchStep(prefix, stepElement, direction) {
  const container = document.getElementById(`${prefix}-batch-steps`);
  const sibling = direction < 0 ? stepElement.previousElementSibling : stepElement.nextElementSibling;
  if (!sibling) {
    return;
  }

  if (direction < 0) {
    container.insertBefore(stepElement, sibling);
  }
  else {
    container.insertBefore(sibling, stepElement);
  }

  reindexBatchSteps(prefix);
}

function removeBatchStep(prefix, stepElement) {
  const container = document.getElementById(`${prefix}-batch-steps`);
  if (container.children.length <= 1) {
    return;
  }

  stepElement.remove();
  reindexBatchSteps(prefix);
}

function readBatchStepParameters(stepElement, stepType) {
  if (stepType === "CALCULATION") {
    const formula = stepElement.querySelector('[data-key="formula"]')?.value.trim();
    return formula || null;
  }

  const values = {};
  stepElement.querySelectorAll(".batch-field-input").forEach(input => {
    const key = input.dataset.key;
    const fieldType = input.dataset.fieldType;
    const rawValue = input.value.trim();
    if (!rawValue) {
      return;
    }

    if (fieldType === "number") {
      values[key] = Number(rawValue);
    }
    else {
      values[key] = rawValue;
    }
  });

  if (stepType === "REPORT" && !Object.keys(values).length) {
    return null;
  }

  return values;
}

function validateBatchBuilder(prefix) {
  const steps = document.querySelectorAll(`#${prefix}-batch-steps .batch-step`);
  if (!steps.length) {
    return "Добавьте хотя бы один шаг в пакет.";
  }

  for (const stepElement of steps) {
    const stepType = stepElement.querySelector(".batch-step-type").value;
    const fields = BATCH_STEP_FIELDS[stepType] || [];

    for (const field of fields) {
      if (!field.required) {
        continue;
      }

      const input = stepElement.querySelector(`[data-key="${field.key}"]`);
      if (!input || !input.value.trim()) {
        const stepIndex = stepElement.querySelector(".batch-step-index").textContent;
        return `${stepIndex}: заполните поле «${field.label}».`;
      }
    }

    if (stepType === "RANDOM") {
      const min = Number(stepElement.querySelector('[data-key="min"]')?.value);
      const max = Number(stepElement.querySelector('[data-key="max"]')?.value);
      if (min > max) {
        return `${stepElement.querySelector(".batch-step-index").textContent}: минимум не может быть больше максимума.`;
      }
    }
  }

  return null;
}

function serializeBatchBuilder(prefix) {
  const steps = [...document.querySelectorAll(`#${prefix}-batch-steps .batch-step`)].map(stepElement => {
    const type = stepElement.querySelector(".batch-step-type").value;
    const parameters = readBatchStepParameters(stepElement, type);
    const step = { type };
    if (parameters !== null && parameters !== undefined && parameters !== "") {
      step.parameters = parameters;
    }
    return step;
  });

  return JSON.stringify({ steps });
}

function updateParametersFieldVisibility() {
  const type = document.getElementById("edit-type").value;
  configureParametersField("edit", type, editingTaskId === "new");
}

function updateScheduleParametersFieldVisibility() {
  const type = document.getElementById("schedule-type").value;
  configureParametersField("schedule", type, true);
}

function readParametersPayload(type, prefix) {
  if (TYPES_WITHOUT_PARAMETERS.has(type)) {
    return null;
  }

  if (type === "BATCH") {
    return serializeBatchBuilder(prefix);
  }

  if (type === "CALCULATION") {
    return document.getElementById(`${prefix}-parameters-formula`).value.trim();
  }

  const jsonValue = document.getElementById(`${prefix}-parameters-json`).value.trim();
  return jsonValue || null;
}

async function openModal(taskId) {
  const response = await fetch(`/api/tasks/${taskId}`);
  if (!response.ok) {
    showError(new Error("Не удалось загрузить задачу"));
    return;
  }

  const task = await response.json();
  editingTaskId = taskId;
  setModalMode("edit");

  document.getElementById("modal-title").textContent = "Редактирование задачи";
  document.getElementById("edit-id").value = task.id;
  document.getElementById("edit-type").value = task.type;
  document.getElementById("edit-executor").value = task.executorName || "—";
  document.getElementById("edit-datetime").value = formatDateTime(task.startedAt || task.createdAt);
  document.getElementById("edit-status").value = task.status;
  document.getElementById("modal-overlay").classList.remove("hidden");
}

function closeModal() {
  editingTaskId = null;
  document.getElementById("edit-form").reset();
  document.getElementById("edit-parameters-formula").value = "";
  document.getElementById("edit-parameters-json").value = "";
  document.getElementById("edit-parameters-row").classList.add("hidden");
  document.getElementById("edit-batch-builder").classList.add("hidden");
  document.getElementById("edit-batch-steps").innerHTML = "";
  document.getElementById("edit-parameters-formula").required = false;
  document.getElementById("edit-parameters-json").required = false;
  document.getElementById("task-modal-window").classList.remove("batch-modal-window");
  document.getElementById("modal-overlay").classList.add("hidden");
}

async function saveTask(event) {
  event.preventDefault();

  if (editingTaskId === "new") {
    await createNewTaskRequest();
    return;
  }

  const status = document.getElementById("edit-status").value;
  const response = await fetch(`/api/tasks/${editingTaskId}/status`, {
    method: "PATCH",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ status })
  });

  if (!response.ok) {
    showError(new Error("Не удалось сохранить задачу"));
    return;
  }

  closeModal();
  await loadTasks();
}

async function createNewTaskRequest() {
  const type = document.getElementById("edit-type").value;

  if (type === "BATCH") {
    const batchError = validateBatchBuilder("edit");
    if (batchError) {
      await showMessage(batchError, "Внимание");
      return;
    }
  }

  const parameters = readParametersPayload(type, "edit");

  if (!TYPES_WITHOUT_PARAMETERS.has(type) && !TYPES_WITH_OPTIONAL_PARAMETERS.has(type) && !parameters) {
    await showMessage("Укажите параметры задачи.", "Внимание");
    return;
  }

  const payload = { type };
  if (parameters) {
    payload.parameters = parameters;
  }

  const response = await fetch("/api/tasks", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });

  if (!response.ok) {
    const errorBody = await response.json().catch(() => null);
    throw new Error(errorBody?.error || "Не удалось создать задачу");
  }

  const task = await response.json();
  selectedTaskId = task.id;
  closeModal();
  await loadTasks();
}

function openNewTaskModal() {
  editingTaskId = "new";
  setModalMode("create");

  document.getElementById("modal-title").textContent = "Новая задача";
  document.getElementById("edit-type").value = taskTypes[0]?.value || "CALCULATION";
  document.getElementById("edit-parameters-formula").value = "";
  document.getElementById("edit-parameters-json").value = "";
  document.getElementById("edit-batch-steps").innerHTML = "";
  updateParametersFieldVisibility();
  document.getElementById("modal-overlay").classList.remove("hidden");
}

function createNewTask() {
  openNewTaskModal();
}

function updateScheduleFormulaVisibility() {
  updateScheduleParametersFieldVisibility();
}

function defaultScheduleDateTime() {
  const date = new Date(Date.now() + 5 * 60 * 1000);
  const pad = part => String(part).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function openScheduleModal() {
  document.getElementById("schedule-type").value = taskTypes[0]?.value || "CALCULATION";
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

  if (!TYPES_WITHOUT_PARAMETERS.has(type) && !TYPES_WITH_OPTIONAL_PARAMETERS.has(type) && !parameters) {
    await showMessage("Укажите параметры расписания.", "Внимание");
    return;
  }

  const payload = {
    taskType: type,
    nextRunAt
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

  const response = await fetch("/api/schedules", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });

  if (!response.ok) {
    const errorBody = await response.json().catch(() => null);
    throw new Error(errorBody?.error || "Не удалось создать расписание");
  }

  const schedule = await response.json();
  selectedScheduleId = schedule.id;
  closeScheduleModal();
  await loadSchedules();
}

async function toggleSelectedSchedule() {
  if (!selectedScheduleId) {
    await showMessage("Выберите расписание в таблице.", "Внимание");
    return;
  }

  const schedule = schedules.find(item => item.id === selectedScheduleId);
  if (!schedule) {
    return;
  }

  const response = await fetch(`/api/schedules/${selectedScheduleId}/enabled`, {
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
  if (!selectedScheduleId) {
    await showMessage("Выберите расписание в таблице.", "Внимание");
    return;
  }

  const schedule = schedules.find(item => item.id === selectedScheduleId);
  if (!schedule) {
    return;
  }

  const confirmed = await showConfirm(`Удалить расписание «${schedule.taskName}»?`, "Удаление расписания");
  if (!confirmed) {
    return;
  }

  const response = await fetch(`/api/schedules/${selectedScheduleId}`, {
    method: "DELETE"
  });

  if (!response.ok) {
    throw new Error("Не удалось удалить расписание");
  }

  selectedScheduleId = null;
  await loadSchedules();
}

async function abortSelectedTask() {
  if (!selectedTaskId) {
    await showMessage("Выберите задачу в таблице.", "Внимание");
    return;
  }

  const task = tasks.find(item => item.id === selectedTaskId);
  if (!task) {
    return;
  }

  if (task.apiStatus !== "IN_PROGRESS") {
    await showMessage("Прервать можно только выполняющуюся задачу.", "Внимание");
    return;
  }

  const response = await fetch(`/api/tasks/${selectedTaskId}/status`, {
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

async function deleteSelectedTask() {
  if (!selectedTaskId) {
    await showMessage("Выберите задачу в таблице.", "Внимание");
    return;
  }

  const task = tasks.find(item => item.id === selectedTaskId);
  if (!task) {
    return;
  }

  const confirmed = await showConfirm(`Удалить задачу «${task.name}»?`, "Удаление задачи");
  if (!confirmed) {
    return;
  }

  await showMessage("Удаление задач через API пока не реализовано.", "Информация");
}

function closeMessageModal(result = false) {
  document.getElementById("message-modal-overlay").classList.add("hidden");
  document.getElementById("message-modal-cancel").classList.add("hidden");

  if (messageModalResolver) {
    const resolve = messageModalResolver;
    messageModalResolver = null;
    messageModalMode = "alert";
    resolve(result);
  }
}

function showMessage(message, title = "Сообщение", isError = false) {
  return new Promise(resolve => {
    messageModalMode = "alert";
    messageModalResolver = () => resolve(true);

    document.getElementById("message-modal-title").textContent = title;
    const textEl = document.getElementById("message-modal-text");
    textEl.textContent = message;
    textEl.classList.toggle("message-modal-error", isError);
    document.getElementById("message-modal-cancel").classList.add("hidden");
    document.getElementById("message-modal-overlay").classList.remove("hidden");
  });
}

function showConfirm(message, title = "Подтверждение") {
  return new Promise(resolve => {
    messageModalMode = "confirm";
    messageModalResolver = resolve;

    document.getElementById("message-modal-title").textContent = title;
    const textEl = document.getElementById("message-modal-text");
    textEl.textContent = message;
    textEl.classList.remove("message-modal-error");
    document.getElementById("message-modal-cancel").classList.remove("hidden");
    document.getElementById("message-modal-overlay").classList.remove("hidden");
  });
}

function showError(error) {
  console.error(error);
  return showMessage(error.message || "Произошла ошибка", "Ошибка", true);
}

function initNavigation() {
  document.querySelectorAll(".nav-btn").forEach(btn => {
    btn.addEventListener("click", () => showPage(btn.dataset.page));
  });
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

function initToolbar() {
  document.getElementById("btn-new-task").addEventListener("click", createNewTask);
  document.getElementById("btn-abort-task").addEventListener("click", () => {
    abortSelectedTask().catch(showError);
  });
  document.getElementById("btn-delete-task").addEventListener("click", () => {
    deleteSelectedTask().catch(showError);
  });
  document.getElementById("btn-add-executor").addEventListener("click", () => {
    addExecutor().catch(showError);
  });
  document.getElementById("btn-new-schedule").addEventListener("click", openScheduleModal);
  document.getElementById("btn-toggle-schedule").addEventListener("click", () => {
    toggleSelectedSchedule().catch(showError);
  });
  document.getElementById("btn-delete-schedule").addEventListener("click", () => {
    deleteSelectedSchedule().catch(showError);
  });
}

function initMessageModal() {
  document.getElementById("message-modal-ok").addEventListener("click", () => {
    closeMessageModal(true);
  });

  document.getElementById("message-modal-cancel").addEventListener("click", () => {
    closeMessageModal(false);
  });

  document.querySelector(".message-modal-close-btn").addEventListener("click", () => {
    closeMessageModal(messageModalMode === "confirm" ? false : true);
  });

  document.getElementById("message-modal-overlay").addEventListener("click", event => {
    if (event.target.id === "message-modal-overlay") {
      closeMessageModal(messageModalMode === "confirm" ? false : true);
    }
  });
}

function initScheduleModal() {
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

function initModal() {
  document.getElementById("edit-form").addEventListener("submit", event => {
    saveTask(event).catch(showError);
  });
  document.getElementById("cancel-btn").addEventListener("click", closeModal);
  document.querySelector(".modal-close-btn").addEventListener("click", closeModal);
  document.getElementById("edit-type").addEventListener("change", updateParametersFieldVisibility);
  document.getElementById("edit-batch-add-step").addEventListener("click", () => addBatchStep("edit"));

  document.getElementById("modal-overlay").addEventListener("click", event => {
    if (event.target.id === "modal-overlay") {
      closeModal();
    }
  });

  document.querySelector(".log-modal-close-btn").addEventListener("click", closeLogModal);

  document.getElementById("log-modal-overlay").addEventListener("click", event => {
    if (event.target.id === "log-modal-overlay") {
      closeLogModal();
    }
  });

  document.addEventListener("keydown", event => {
    if (event.key !== "Escape") {
      return;
    }

    if (!document.getElementById("message-modal-overlay").classList.contains("hidden")) {
      closeMessageModal(messageModalMode === "confirm" ? false : true);
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

    if (!document.getElementById("modal-overlay").classList.contains("hidden")) {
      closeModal();
    }
  });
}

document.addEventListener("DOMContentLoaded", () => {
  loadTaskTypes().catch(showError).finally(() => {
    initNavigation();
    initToolbar();
    initMessageModal();
    initScheduleModal();
    initModal();
    showPage("tasks");
  });
});
