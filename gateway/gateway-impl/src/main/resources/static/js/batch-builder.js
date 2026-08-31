import { BATCH_STEP_FIELDS } from "./constants.js";
import { state } from "./state.js";

export function getBatchStepTypes() {
  return state.taskTypes.filter(type => type.value !== "BATCH");
}

export function resetBatchBuilder(prefix) {
  const container = document.getElementById(`${prefix}-batch-steps`);
  container.innerHTML = "";
  addBatchStep(prefix, "RANDOM");
}

export function addBatchStep(prefix, initialType = "CALCULATION") {
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
  } else {
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
    } else {
      values[key] = rawValue;
    }
  });

  if (stepType === "REPORT" && !Object.keys(values).length) {
    return null;
  }

  return values;
}

export function validateBatchBuilder(prefix) {
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

export function serializeBatchBuilder(prefix) {
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
