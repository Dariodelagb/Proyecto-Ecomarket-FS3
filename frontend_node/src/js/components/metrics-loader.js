function formatNumber(num) {
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

function formatCurrency(value) {
  return new Intl.NumberFormat("es-CL", {
    style: "currency",
    currency: "CLP",
    maximumFractionDigits: 0,
  }).format(value || 0);
}

async function loadClientCount() {
  const countElement = document.getElementById("count-usuarios");
  if (!countElement) return;

  try {
    const response = await fetch("/api/clientes/count");
    if (!response.ok) throw new Error("Error al obtener clientes");

    const data = await response.json();
    countElement.textContent = formatNumber(data.total);
  } catch (error) {
    console.error("Error cargando count de usuarios:", error);
    countElement.textContent = "Error";
  }
}

async function loadSalesCount() {
  const countElement = document.getElementById("count-ventas");
  if (!countElement) return;

  try {
    const response = await fetch("/api/ventas/count");
    if (!response.ok) throw new Error("Error al obtener ventas");

    const data = await response.json();
    countElement.textContent = formatNumber(data.total);
  } catch (error) {
    console.error("Error cargando count de ventas:", error);
    countElement.textContent = "Error";
  }
}

async function loadSalesTotalMoney() {
  const totalElement = document.getElementById("total-ventas-dinero");
  if (!totalElement) return;

  try {
    const response = await fetch("/api/ventas-completas");
    if (!response.ok) throw new Error("Error al obtener ventas completas");

    const ventas = await response.json();
    const total = ventas.reduce((sum, venta) => sum + (venta.monto || 0), 0);
    totalElement.textContent = formatCurrency(total);
  } catch (error) {
    console.error("Error cargando total de ventas:", error);
    totalElement.textContent = "Error";
  }
}

document.addEventListener("DOMContentLoaded", () => {
  loadClientCount();
  loadSalesCount();
  loadSalesTotalMoney();
});

export default { loadClientCount, loadSalesCount, loadSalesTotalMoney };
