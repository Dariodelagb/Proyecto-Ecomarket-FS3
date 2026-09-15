import { getSession } from "./auth-loader";

const TRANSITIONS = {
  CREADO: ["ACEPTADO", "CANCELADO"],
  ACEPTADO: ["EN_PREPARACION", "CANCELADO"],
  EN_PREPARACION: ["DESPACHADO", "CANCELADO"],
  DESPACHADO: ["ENTREGADO"],
  ENTREGADO: [],
  CANCELADO: [],
};

const STATUS_LABELS = {
  CREADO: "Creado",
  ACEPTADO: "Aceptado",
  EN_PREPARACION: "En preparacion",
  DESPACHADO: "Despachado",
  ENTREGADO: "Entregado",
  CANCELADO: "Cancelado",
};

const STATUS_CLASSES = {
  CREADO: "bg-gray-100 text-gray-700 dark:bg-gray-800 dark:text-gray-300",
  ACEPTADO: "bg-blue-light-50 text-blue-light-700 dark:bg-blue-light-500/15 dark:text-blue-light-300",
  EN_PREPARACION: "bg-warning-50 text-warning-700 dark:bg-warning-500/15 dark:text-warning-300",
  DESPACHADO: "bg-brand-50 text-brand-700 dark:bg-brand-500/15 dark:text-brand-300",
  ENTREGADO: "bg-success-50 text-success-700 dark:bg-success-500/15 dark:text-success-300",
  CANCELADO: "bg-error-50 text-error-700 dark:bg-error-500/15 dark:text-error-300",
};

const escapeHtml = (value) =>
  String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");

const currency = new Intl.NumberFormat("es-CL", {
  style: "currency",
  currency: "CLP",
  maximumFractionDigits: 0,
});

const formatDate = (value) => {
  if (!value) return "Sin fecha";
  return new Intl.DateTimeFormat("es-CL").format(new Date(`${value}T00:00:00`));
};

const clientName = (order) =>
  [order.cliente?.nombres, order.cliente?.apellidos].filter(Boolean).join(" ") || "Sin cliente";

const getErrorMessage = async (response) => {
  try {
    const data = await response.json();
    return data.detail || data.message || data.error || "No se pudo completar la operacion";
  } catch {
    return "No se pudo completar la operacion";
  }
};

const statusControl = (order, canEdit) => {
  const state = order.estado || "CREADO";
  const nextStates = TRANSITIONS[state] || [];
  if (!canEdit || !nextStates.length) {
    return `<span class="rounded-full px-2.5 py-1 text-theme-xs font-medium ${STATUS_CLASSES[state] || STATUS_CLASSES.CREADO}">${escapeHtml(STATUS_LABELS[state] || state)}</span>`;
  }

  const options = nextStates
    .map((next) => `<option value="${next}">${escapeHtml(STATUS_LABELS[next])}</option>`)
    .join("");
  return `
    <label class="sr-only" for="estado-pedido-${order.id}">Cambiar estado</label>
    <select id="estado-pedido-${order.id}" class="orders-status-select" data-order-state data-order-id="${order.id}">
      <option value="">${escapeHtml(STATUS_LABELS[state] || state)}</option>
      ${options}
    </select>
  `;
};

const createRow = (order, canEdit) => {
  const row = document.createElement("tr");
  row.className = "border-b border-gray-100 dark:border-gray-800";
  row.innerHTML = `
    <td class="py-4 pr-4 text-theme-sm font-medium text-gray-800 dark:text-white/90">#${escapeHtml(order.id)}</td>
    <td class="py-4 pr-4 text-theme-sm text-gray-500 dark:text-gray-400">${escapeHtml(formatDate(order.fecha))}</td>
    <td class="py-4 pr-4">
      <p class="text-theme-sm font-medium text-gray-800 dark:text-white/90">${escapeHtml(clientName(order))}</p>
      <span class="text-theme-xs text-gray-500 dark:text-gray-400">ID ${escapeHtml(order.cliente?.id || "-")}</span>
    </td>
    <td class="py-4 pr-4">${statusControl(order, canEdit)}</td>
    <td class="py-4 text-theme-sm font-medium text-gray-800 dark:text-white/90">${escapeHtml(currency.format(order.monto || 0))}</td>
  `;
  return row;
};

const initializeOrders = () => {
  const container = document.querySelector("[data-orders-mode]");
  const tableBody = document.getElementById("orders-table-body");
  if (!container || !tableBody) return;

  const session = getSession();
  const token = session?.token;
  const role = session?.cliente?.rol;
  const requestedMode = container.dataset.ordersMode;
  const mode = requestedMode === "auto" ? (role === "ADMIN" ? "admin" : "client") : requestedMode;
  const canEdit = role === "ADMIN" || role === "OPERADOR";

  const invalidAutoRole = requestedMode === "auto" && !["ADMIN", "CLIENTE"].includes(role);
  if (!token || invalidAutoRole || (mode === "admin" && role !== "ADMIN") || (mode === "operator" && !canEdit)) {
    window.location.href = token ? "index.html" : "login.html";
    return;
  }

  const reportButton = document.querySelector("[data-report-download]");
  if (reportButton) {
    reportButton.addEventListener("click", async () => {
      const originalText = reportButton.textContent;
      reportButton.disabled = true;
      reportButton.textContent = "Generando reporte...";
      try {
        const response = await fetch("/reportes/get-reporte", {
          headers: { "X-Session-Token": token },
        });
        if (!response.ok) throw new Error(await getErrorMessage(response));

        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = "reporte-pedidos360.xlsx";
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.setTimeout(() => URL.revokeObjectURL(url), 1000);
      } catch (error) {
        console.error("Error descargando reporte:", error);
        setStatus(error.message || "No se pudo generar el reporte");
      } finally {
        reportButton.disabled = false;
        reportButton.textContent = originalText;
      }
    });
  }

  let page = 0;
  let loading = false;
  let finished = false;

  const setStatus = (message) => {
    const element = document.getElementById("orders-load-status");
    if (element) element.textContent = message;
  };

  const loadNextPage = async () => {
    if (loading || finished) return;
    loading = true;
    setStatus("Cargando pedidos...");

    const endpoint = mode === "client"
      ? "/api/pedidos/recientes?limit=10"
      : `/api/pedidos?page=${page}&size=12`;

    try {
      const response = await fetch(endpoint, { headers: { "X-Session-Token": token } });
      if (!response.ok) throw new Error(await getErrorMessage(response));

      const data = await response.json();
      const orders = Array.isArray(data) ? data : data.content || [];
      orders.forEach((order) => tableBody.appendChild(createRow(order, canEdit && mode !== "client")));

      if (Array.isArray(data) || data.last || !orders.length) {
        finished = true;
      } else {
        page += 1;
      }
      setStatus(tableBody.children.length ? (finished ? "Todos los pedidos cargados" : "") : "No hay pedidos para mostrar");
    } catch (error) {
      console.error("Error cargando pedidos:", error);
      setStatus(error.message || "No se pudieron cargar los pedidos");
    } finally {
      loading = false;
    }
  };

  window.addEventListener("pedidos360:orders-updated", () => {
    tableBody.innerHTML = "";
    page = 0;
    finished = false;
    loadNextPage();
  });

  tableBody.addEventListener("change", async (event) => {
    const select = event.target.closest("[data-order-state]");
    if (!select || !select.value) return;

    select.disabled = true;
    try {
      const response = await fetch(`/api/pedidos/${select.dataset.orderId}/estado`, {
        method: "PATCH",
        headers: {
          "Content-Type": "application/json",
          "X-Session-Token": token,
        },
        body: JSON.stringify({ estado: select.value }),
      });
      if (!response.ok) throw new Error(await getErrorMessage(response));

      const updated = await response.json();
      const replacement = createRow(updated, true);
      select.closest("tr").replaceWith(replacement);
    } catch (error) {
      console.error("Error actualizando pedido:", error);
      setStatus(error.message || "No se pudo actualizar el pedido");
      select.value = "";
      select.disabled = false;
    }
  });

  const sentinel = document.getElementById("orders-scroll-sentinel");
  if (sentinel && mode !== "client") {
    const observer = new IntersectionObserver((entries) => {
      if (entries.some((entry) => entry.isIntersecting)) loadNextPage();
    }, { rootMargin: "300px" });
    observer.observe(sentinel);
  }

  loadNextPage();
};

document.addEventListener("DOMContentLoaded", initializeOrders);

export default initializeOrders;
