import { apiFetch } from "./api.js";
import { state } from "./state.js";
import { escapeHtml } from "./utils.js";
import { populateTaskTypeSelect } from "./parameters.js";

export function renderTaskTypes(catalog) {
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

export async function loadTaskTypes() {
  const response = await apiFetch("/api/task-types");
  if (!response.ok) {
    throw new Error("Не удалось загрузить справочник типов задач");
  }

  const data = await response.json();
  state.taskTypes = data.map(type => ({
    value: type.code,
    label: type.name
  }));

  state.taskTypeHints = Object.fromEntries(
    data.filter(type => type.example).map(type => [type.code, type.example])
  );

  populateTaskTypeSelect("edit-type");
  populateTaskTypeSelect("schedule-type");
  renderTaskTypes(data);
}
