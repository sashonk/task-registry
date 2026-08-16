import { TYPES_WITHOUT_PARAMETERS, TYPES_WITH_OPTIONAL_PARAMETERS } from "./constants.js";
import { state } from "./state.js";
import { formatDateTime } from "./utils.js";
import { validateBatchBuilder } from "./batch-builder.js";
import { addBatchStep } from "./batch-builder.js";
import {
  readParametersPayload,
  setModalMode,
  updateParametersFieldVisibility
} from "./parameters.js";
import { showError, showMessage } from "./messages.js";
import { loadTasks } from "./tasks.js";

export function closeModal() {
  state.editingTaskId = null;
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

async function openModal(taskId) {
  const response = await fetch(`/api/tasks/${taskId}`);
  if (!response.ok) {
    showError(new Error("Не удалось загрузить задачу"));
    return;
  }

  const task = await response.json();
  state.editingTaskId = taskId;
  setModalMode("edit");

  document.getElementById("modal-title").textContent = "Редактирование задачи";
  document.getElementById("edit-id").value = task.id;
  document.getElementById("edit-type").value = task.type;
  document.getElementById("edit-executor").value = task.executorName || "—";
  document.getElementById("edit-datetime").value = formatDateTime(task.startedAt || task.createdAt);
  document.getElementById("edit-status").value = task.status;
  document.getElementById("modal-overlay").classList.remove("hidden");
}

function openNewTaskModal() {
  state.editingTaskId = "new";
  setModalMode("create");

  document.getElementById("modal-title").textContent = "Новая задача";
  document.getElementById("edit-type").value = state.taskTypes[0]?.value || "CALCULATION";
  document.getElementById("edit-parameters-formula").value = "";
  document.getElementById("edit-parameters-json").value = "";
  document.getElementById("edit-batch-steps").innerHTML = "";
  updateParametersFieldVisibility();
  document.getElementById("modal-overlay").classList.remove("hidden");
}

export function createNewTask() {
  openNewTaskModal();
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
  state.selectedTaskId = task.id;
  closeModal();
  await loadTasks(1);
}

async function saveTask(event) {
  event.preventDefault();

  if (state.editingTaskId === "new") {
    await createNewTaskRequest();
    return;
  }

  const status = document.getElementById("edit-status").value;
  const response = await fetch(`/api/tasks/${state.editingTaskId}/status`, {
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

export function initTaskModal() {
  document.getElementById("btn-new-task").addEventListener("click", createNewTask);
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
}

export { openModal };
