import { PAGE_SIZE } from "./constants.js";
import { apiFetch } from "./api.js";
import { state } from "./state.js";
import { showError } from "./messages.js";

export function formatPaginationCount(meta) {
  if (meta.totalElements === 0) {
    return "0 записей";
  }
  if (meta.from === meta.to) {
    return `${meta.from} из ${meta.totalElements}`;
  }
  return `${meta.from}–${meta.to} из ${meta.totalElements}`;
}

export function updatePaginationControls(prefix, meta) {
  const totalPages = Math.max(1, meta.totalPages);
  const page = Math.min(Math.max(1, meta.page), totalPages);

  document.getElementById(`${prefix}-header-count`).textContent = formatPaginationCount(meta);
  document.getElementById(`${prefix}-page-total`).textContent = String(totalPages);
  document.getElementById(`${prefix}-page-input`).value = String(page);
  document.getElementById(`${prefix}-page-input`).max = String(totalPages);
  document.getElementById(`${prefix}-page-prev`).disabled = page <= 1;
  document.getElementById(`${prefix}-page-next`).disabled = page >= totalPages || meta.totalElements === 0;
}

function goToPaginationPage(prefix, loader) {
  const input = document.getElementById(`${prefix}-page-input`);
  const meta = prefix === "tasks" ? state.tasksPagination : state.logsPagination;
  const totalPages = Math.max(1, meta.totalPages);
  const requestedPage = Number.parseInt(input.value, 10);

  if (Number.isNaN(requestedPage)) {
    input.value = String(meta.page);
    return;
  }

  const targetPage = Math.min(Math.max(1, requestedPage), totalPages);
  input.value = String(targetPage);

  if (targetPage !== meta.page) {
    loader(targetPage).catch(showError);
  }
}

function initPaginationSection(prefix, loader) {
  document.getElementById(`${prefix}-page-prev`).addEventListener("click", () => {
    const meta = prefix === "tasks" ? state.tasksPagination : state.logsPagination;
    if (meta.page > 1) {
      loader(meta.page - 1).catch(showError);
    }
  });

  document.getElementById(`${prefix}-page-next`).addEventListener("click", () => {
    const meta = prefix === "tasks" ? state.tasksPagination : state.logsPagination;
    if (meta.page < meta.totalPages) {
      loader(meta.page + 1).catch(showError);
    }
  });

  document.getElementById(`${prefix}-page-input`).addEventListener("keydown", event => {
    if (event.key !== "Enter") {
      return;
    }
    event.preventDefault();
    goToPaginationPage(prefix, loader);
  });

  document.getElementById(`${prefix}-page-input`).addEventListener("change", () => {
    goToPaginationPage(prefix, loader);
  });
}

export function initPagination(loadTasks, loadAllLogs) {
  initPaginationSection("tasks", loadTasks);
  initPaginationSection("logs", loadAllLogs);
}

export async function fetchPaginated(url, page, errorMessage = "Не удалось загрузить данные") {
  const response = await apiFetch(`${url}?page=${page}&size=${PAGE_SIZE}`);
  if (!response.ok) {
    throw new Error(errorMessage);
  }
  return response.json();
}

export function applyPaginationResult(data, paginationKey) {
  if (data.totalPages > 0 && data.page > data.totalPages) {
    return data.totalPages;
  }

  state[paginationKey] = {
    page: data.page,
    size: data.size,
    totalElements: data.totalElements,
    totalPages: Math.max(1, data.totalPages),
    from: data.from,
    to: data.to
  };

  return null;
}
