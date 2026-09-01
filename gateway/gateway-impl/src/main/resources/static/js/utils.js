import { STATUS_MAP } from "./constants.js";

export function formatDateTime(value) {
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

export function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

export function mapTaskFromApi(task) {
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

export function defaultScheduleDateTime() {
  const date = new Date(Date.now() + 5 * 60 * 1000);
  const pad = part => String(part).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export function toUtcIsoFromDateTimeLocal(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return null;
  }

  return date.toISOString();
}
