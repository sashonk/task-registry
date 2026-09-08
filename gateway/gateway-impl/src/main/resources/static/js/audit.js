import { PAGE_SIZE } from "./constants.js";
import { state } from "./state.js";
import { apiFetch } from "./api.js";
import { showError } from "./messages.js";

const EVENT_TYPE_NAMES = {
  LOGIN_SUCCESSFUL: "Успешный вход",
  TASK_CREATED: "Задача создана",
  TASK_STATUS_CHANGED: "Статус задачи изменён"
};

let currentPage = 1;
let currentEventType = "";
let currentUsername = "";
let totalPages = 1;

export async function loadAuditEvents(page = 1, size = PAGE_SIZE) {
  currentPage = page;
  
  const params = new URLSearchParams({
    page: String(page),
    size: String(size)
  });
  
  if (currentEventType) {
    params.append("eventType", currentEventType);
  }
  if (currentUsername) {
    params.append("username", currentUsername);
  }

  try {
    const response = await apiFetch(`/api/audit/events?${params.toString()}`);
    const data = await response.json();
    
    totalPages = data.totalPages;
    updatePaginationControls();
    
    const countEl = document.getElementById("audit-header-count");
    if (countEl) {
      countEl.textContent = `${data.totalElements} записей`;
    }
    
    renderAuditEvents(data.content);
  } catch (error) {
    showError(error);
  }
}

function renderAuditEvents(events) {
  const tbody = document.getElementById("audit-body");
  if (!tbody) return;
  
  tbody.innerHTML = "";
  
  if (!events || events.length === 0) {
    const tr = document.createElement("tr");
    tr.innerHTML = `<td colspan="6" class="empty-row">Записей не найдено</td>`;
    tbody.appendChild(tr);
    return;
  }
  
  events.forEach((event, index) => {
    const tr = document.createElement("tr");
    tr.className = "clickable-row";
    
    const rawTimestamp = event.timestamp || null;
    const startTime = rawTimestamp
      ? new Date(rawTimestamp.endsWith("Z") ? rawTimestamp : rawTimestamp + "Z")
      : null;
    const timeStr = startTime ? formatTimestamp(startTime) : "—";
    const eventTypeDisplay = EVENT_TYPE_NAMES[event.eventType] || event.eventType;
    const ipStr = event.ipAddress || "—";
    const roleStr = event.userRole || "—";
    
    tr.innerHTML = `
      <td style="text-align: center;">${index + 1}</td>
      <td style="font-family: Consolas, 'Courier New', monospace; font-size: 12px; white-space: nowrap;">${timeStr}</td>
      <td>${eventTypeDisplay}</td>
      <td>${event.username || "—"}</td>
      <td style="font-family: Consolas, 'Courier New', monospace; font-size: 12px; white-space: nowrap;">${ipStr}</td>
      <td style="text-align: center;">${roleStr}</td>
    `;
    
    tr.addEventListener("dblclick", () => {
      openAuditEventModal(event);
    });
    
    tbody.appendChild(tr);
  });
}

function openAuditEventModal(event) {
  const rawTimestamp = event.timestamp || null;
  const startTime = rawTimestamp
    ? new Date(rawTimestamp.endsWith("Z") ? rawTimestamp : rawTimestamp + "Z")
    : null;
  
  setText("audit-modal-id", event.id ?? "—");
  setText("audit-modal-timestamp", startTime ? formatTimestamp(startTime) : "—");
  setText("audit-modal-type", EVENT_TYPE_NAMES[event.eventType] || event.eventType || "—");
  setText("audit-modal-username", event.username || "—");
  setText("audit-modal-ip", event.ipAddress || "—");
  setText("audit-modal-role", event.userRole || "—");
  setText("audit-modal-payload", formatPayload(event.metadata));
  
  document.getElementById("audit-modal-overlay").classList.remove("hidden");
}

export function closeAuditEventModal() {
  document.getElementById("audit-modal-overlay").classList.add("hidden");
}

function setText(id, value) {
  const el = document.getElementById(id);
  if (el) {
    el.textContent = value;
  }
}

function formatPayload(metadata) {
  if (!metadata) {
    return "—";
  }
  try {
    return JSON.stringify(JSON.parse(metadata), null, 2);
  } catch (e) {
    return metadata;
  }
}

function formatTimestamp(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const hours = String(date.getHours()).padStart(2, "0");
  const minutes = String(date.getMinutes()).padStart(2, "0");
  const seconds = String(date.getSeconds()).padStart(2, "0");
  
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

function updatePaginationControls() {
  const pageInput = document.getElementById("audit-page-input");
  const pageTotal = document.getElementById("audit-page-total");
  const prevBtn = document.getElementById("audit-page-prev");
  const nextBtn = document.getElementById("audit-page-next");
  
  if (pageInput) {
    pageInput.value = currentPage;
    pageInput.max = totalPages;
  }
  if (pageTotal) {
    pageTotal.textContent = totalPages;
  }
  if (prevBtn) {
    prevBtn.disabled = currentPage <= 1;
  }
  if (nextBtn) {
    nextBtn.disabled = currentPage >= totalPages;
  }
}

export function initAuditFilters() {
  const applyBtn = document.getElementById("audit-apply-filter");
  const resetBtn = document.getElementById("audit-reset-filter");
  const eventTypeSelect = document.getElementById("audit-event-type");
  const usernameInput = document.getElementById("audit-username");
  const prevBtn = document.getElementById("audit-page-prev");
  const nextBtn = document.getElementById("audit-page-next");
  const pageInput = document.getElementById("audit-page-input");
  const auditModalCloseBtn = document.querySelector(".audit-modal-close-btn");
  const auditModalOverlay = document.getElementById("audit-modal-overlay");
  
  if (auditModalCloseBtn) {
    auditModalCloseBtn.addEventListener("click", closeAuditEventModal);
  }
  
  if (auditModalOverlay) {
    auditModalOverlay.addEventListener("click", event => {
      if (event.target.id === "audit-modal-overlay") {
        closeAuditEventModal();
      }
    });
  }
  
  if (applyBtn) {
    applyBtn.addEventListener("click", () => {
      currentEventType = eventTypeSelect?.value || "";
      currentUsername = usernameInput?.value?.trim() || "";
      loadAuditEvents(1).catch(showError);
    });
  }
  
  if (resetBtn) {
    resetBtn.addEventListener("click", () => {
      currentEventType = "";
      currentUsername = "";
      if (eventTypeSelect) eventTypeSelect.value = "";
      if (usernameInput) usernameInput.value = "";
      loadAuditEvents(1).catch(showError);
    });
  }
  
  if (prevBtn) {
    prevBtn.addEventListener("click", () => {
      if (currentPage > 1) {
        loadAuditEvents(currentPage - 1).catch(showError);
      }
    });
  }
  
  if (nextBtn) {
    nextBtn.addEventListener("click", () => {
      if (currentPage < totalPages) {
        loadAuditEvents(currentPage + 1).catch(showError);
      }
    });
  }
  
  if (pageInput) {
    pageInput.addEventListener("change", () => {
      const page = parseInt(pageInput.value, 10);
      if (page >= 1 && page <= totalPages) {
        loadAuditEvents(page).catch(showError);
      } else {
        pageInput.value = currentPage;
      }
    });
  }
}
