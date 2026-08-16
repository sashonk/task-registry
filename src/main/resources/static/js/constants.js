export const STATUS_MAP = {
  NEW: { css: "pending", text: "Ожидает" },
  IN_PROGRESS: { css: "running", text: "Выполняется" },
  DONE: { css: "done", text: "Выполнено" },
  ERROR: { css: "error", text: "Ошибка" },
  ABORTED: { css: "aborted", text: "Прервано" }
};

export const PAGE_TITLES = {
  tasks: "Task Registry — Задачи",
  executors: "Task Registry — Исполнители",
  logs: "Task Registry — Логи",
  scheduler: "Task Registry — Планировщик",
  "task-types": "Task Registry — Типы задач",
  empty: "Task Registry — Пустой раздел",
  help: "Task Registry — Справка"
};

export const TASKS_REFRESH_MS = 2000;
export const SCHEDULER_REFRESH_MS = 2000;
export const LOG_REFRESH_MS = 2000;
export const PAGE_SIZE = 10;

export const TASK_TYPE_HINTS = {
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

export const TYPES_WITHOUT_PARAMETERS = new Set();
export const TYPES_WITH_OPTIONAL_PARAMETERS = new Set(["REPORT"]);

export const BATCH_STEP_FIELDS = {
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
