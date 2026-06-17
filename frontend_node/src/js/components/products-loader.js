// Función para formatear números con separador de miles
import { getProductImage } from "./product-image-resolver";

function formatNumber(num) {
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

// Cargar productos en la tabla
async function loadProductsTable() {
  try {
    const tableBody = document.getElementById("products-table-body");
    if (!tableBody) return;

    const [productsResponse, stockResponse] = await Promise.all([
      fetch("/api/productos"),
      fetch("/api/stock-producto"),
    ]);

    if (!productsResponse.ok) throw new Error("Error al obtener productos");
    if (!stockResponse.ok) throw new Error("Error al obtener stock");

    const productos = await productsResponse.json();
    const stockProductos = await stockResponse.json();
    const stockByProductId = new Map(
      stockProductos
        .filter((item) => item.producto?.id)
        .map((item) => [item.producto.id, item.stock]),
    );

    // Limpiar filas existentes (mantener solo estructura)
    tableBody.innerHTML = "";

    // Insertar productos reales
    productos.forEach((prod) => {
      const row = document.createElement("tr");
      const categoriaNombre = prod.categoria
        ? prod.categoria.categoria
        : "Sin categoría";
      const stock = stockByProductId.get(prod.id) ?? 0;
      const estado = "Disponible"; // Puedes cambiar esto según lógica de negocio

      row.innerHTML = `
        <td>
          <div class="flex items-center">
            <div class="flex items-center gap-3">
              <div class="h-[50px] w-[50px] overflow-hidden rounded-md bg-gray-200">
                <img src="${getProductImage(prod)}" alt="${prod.nombre}" class="w-full h-full object-cover" />
              </div>
              <div>
                <p class="font-medium text-gray-800 text-theme-sm dark:text-white/90">
                  ${prod.nombre}
                </p>
                <span class="text-gray-500 text-theme-xs dark:text-gray-400">
                  1 Variants
                </span>
              </div>
            </div>
          </div>
        </td>
        <td>
          <div class="flex items-center">
            <p class="text-gray-500 text-theme-sm dark:text-gray-400">
              ${categoriaNombre}
            </p>
          </div>
        </td>
        <td>
          <div class="flex items-center">
            <p class="text-gray-500 text-theme-sm dark:text-gray-400">
              $${formatNumber(prod.precio || 0)}
            </p>
          </div>
        </td>
        <td>
          <div class="flex items-center">
            <p class="rounded-full bg-brand-50 px-2 py-0.5 text-theme-xs font-medium text-brand-700 dark:bg-brand-500/15 dark:text-brand-300">
              ${formatNumber(stock)}
            </p>
          </div>
        </td>
        <td>
          <div class="flex items-center">
            <p class="rounded-full bg-success-50 px-2 py-0.5 text-theme-xs font-medium text-success-600 dark:bg-success-500/15 dark:text-success-500">
              ${estado}
            </p>
          </div>
        </td>
      `;

      tableBody.appendChild(row);
    });
  } catch (error) {
    console.error("Error cargando productos:", error);
  }
}

// Ejecutar cuando el DOM esté listo
document.addEventListener("DOMContentLoaded", () => {
  loadProductsTable();
});

export default { loadProductsTable };
