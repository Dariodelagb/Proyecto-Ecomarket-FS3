// Función para formatear números con separador de miles
function formatNumber(num) {
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

// Cargar conteo de usuarios (clientes)
async function loadClientCount() {
  try {
    const response = await fetch("/api/clientes/count");
    if (!response.ok) throw new Error("Error al obtener clientes");
    
    const data = await response.json();
    const countElement = document.getElementById("count-usuarios");
    
    if (countElement) {
      countElement.textContent = formatNumber(data.total);
    }
  } catch (error) {
    console.error("Error cargando count de usuarios:", error);
    const countElement = document.getElementById("count-usuarios");
    if (countElement) {
      countElement.textContent = "Error";
    }
  }
}

// Cargar conteo de ventas
async function loadSalesCount() {
  try {
    const response = await fetch("/api/ventas/count");
    if (!response.ok) throw new Error("Error al obtener ventas");
    
    const data = await response.json();
    const countElement = document.getElementById("count-ventas");
    
    if (countElement) {
      countElement.textContent = formatNumber(data.total);
    }
  } catch (error) {
    console.error("Error cargando count de ventas:", error);
    const countElement = document.getElementById("count-ventas");
    if (countElement) {
      countElement.textContent = "Error";
    }
  }
}

// Ejecutar cuando el DOM esté listo
document.addEventListener("DOMContentLoaded", () => {
  loadClientCount();
  loadSalesCount();
});

export default { loadClientCount, loadSalesCount };
