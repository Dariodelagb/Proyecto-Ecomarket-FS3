// Función para formatear números con separador de miles
function formatNumber(num) {
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

// Cargar conteo de usuarios (clientes)
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

// Cargar conteo de ventas
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

// Ejecutar cuando el DOM esté listo
document.addEventListener("DOMContentLoaded", () => {
  loadClientCount();
  loadSalesCount();
});

export default { loadClientCount, loadSalesCount };
