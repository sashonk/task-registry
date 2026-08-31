import {
  TASK_TYPE_HINTS,
  TYPES_WITHOUT_PARAMETERS,
  TYPES_WITH_OPTIONAL_PARAMETERS
} from "./constants.js";
import { state } from "./state.js";
import { addBatchStep, resetBatchBuilder, serializeBatchBuilder } from "./batch-builder.js";

export function populateTaskTypeSelect(selectId) {
  const select = document.getElementById(selectId);
  select.innerHTML = state.taskTypes.map(type => `<option value="${type.value}">${type.label}</option>`).join("");
}

export function getTaskTypeHint(type) {
  return state.taskTypeHints[type] || TASK_TYPE_HINTS[type] || "";
}

export function configureParametersField(prefix, type, isCreate) {
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
  } else {
    label.textContent = "Параметры (JSON)";
    jsonInput.classList.remove("hidden");
    jsonInput.required = !TYPES_WITH_OPTIONAL_PARAMETERS.has(type);
    if (!jsonInput.value.trim()) {
      const typeHint = getTaskTypeHint(type);
      if (typeHint) {
        jsonInput.value = typeHint;
      }
    }
  }

  hint.textContent = getTaskTypeHint(type) || "";
}

export function updateParametersFieldVisibility() {
  const type = document.getElementById("edit-type").value;
  configureParametersField("edit", type, state.editingTaskId === "new");
}

export function updateScheduleParametersFieldVisibility() {
  const type = document.getElementById("schedule-type").value;
  configureParametersField("schedule", type, true);
}

export function readParametersPayload(type, prefix) {
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

export function setModalMode(mode) {
  document.querySelectorAll(".edit-only").forEach(row => {
    row.classList.toggle("hidden", mode === "create");
  });
  updateParametersFieldVisibility();
}
