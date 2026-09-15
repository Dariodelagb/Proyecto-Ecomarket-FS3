// Función para formatear números con separador de miles
import { getProductImage } from "./product-image-resolver";
import { getSession } from "./auth-loader";

function formatNumber(num) {
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

// Cargar productos en la tabla
async function loadProductsTable() {
  try {
    const tableBody = document.getElementById("products-table-body");
    if (!tableBody) return;

    const productsResponse = await fetch("/api/productos");

    if (!productsResponse.ok) throw new Error("Error al obtener productos");

    const productos = await productsResponse.json();

    // Limpiar filas existentes (mantener solo estructura)
    tableBody.innerHTML = "";

    // Insertar productos reales
    productos.forEach((prod) => {
      const row = document.createElement("tr");
      const categoriaNombre = prod.categoria
        ? prod.categoria.categoria
        : "Sin categoría";
      // El API Gateway actual no publica el recurso de inventario; el stock
      // se muestra si viene incluido en la respuesta del producto.
      const stock = prod.stock ?? prod.stockProducto?.stock ?? "N/D";
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
              ${typeof stock === "number" ? formatNumber(stock) : stock}
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
        <td>
          <button
            type="button"
            class="delete-product rounded-lg border border-error-200 px-3 py-1.5 text-theme-xs font-medium text-error-600 hover:bg-error-50 dark:border-error-500/30 dark:text-error-400 dark:hover:bg-error-500/10"
            data-product-id="${prod.id}"
            data-product-name="${prod.nombre}"
          >
            Eliminar
          </button>
        </td>
      `;

      tableBody.appendChild(row);
    });
  } catch (error) {
    console.error("Error cargando productos:", error);
  }
}

async function deleteProduct(productId, productName) {
  if (!window.confirm(`¿Eliminar el producto "${productName}"?`)) return;

  const response = await fetch(`/api/productos/${productId}`, {
    method: "DELETE",
    headers: { "X-Session-Token": getSession()?.token || "" },
  });

  if (!response.ok) throw new Error("No se pudo eliminar el producto.");
  await loadProductsTable();
}

// Ejecutar cuando el DOM esté listo
document.addEventListener("DOMContentLoaded", () => {
  loadProductsTable();

  document.addEventListener("click", async (event) => {
    const button = event.target.closest(".delete-product");
    if (!button) return;

    button.disabled = true;
    try {
      await deleteProduct(button.dataset.productId, button.dataset.productName);
    } catch (error) {
      console.error("Error eliminando producto:", error);
      window.alert(error.message || "No se pudo eliminar el producto.");
      button.disabled = false;
    }
  });
});

window.addEventListener("pedidos360:products-updated", () => {
  loadProductsTable();
});

export { loadProductsTable };
export default { loadProductsTable };
