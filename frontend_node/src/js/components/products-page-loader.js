import { validateSession } from "./auth-loader";
import { updateCartBadge } from "./cart-badge-loader";
import { getProductImage } from "./product-image-resolver";

const fallbackProducts = [
  { nombre: "Perfume natural", precio: 8500, categoria: { categoria: "Perfumes" } },
  { nombre: "Bloqueador solar", precio: 11900, categoria: { categoria: "Bloqueadores" } },
  { nombre: "Jabon artesanal", precio: 6500, categoria: { categoria: "Jabones" } },
  { nombre: "Botella reutilizable", precio: 9800, categoria: { categoria: "Botellas" } },
];

const formatPrice = (value) =>
  new Intl.NumberFormat("es-CL", {
    style: "currency",
    currency: "CLP",
    maximumFractionDigits: 0,
  }).format(value || 0);

const escapeHtml = (value) =>
  String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");

const getCategoryName = (product) => product.categoria?.categoria || "Sin categoria";

const groupProductsByCategory = (products) =>
  products.reduce((groups, product) => {
    const category = getCategoryName(product);

    if (!groups.has(category)) {
      groups.set(category, []);
    }

    groups.get(category).push(product);
    return groups;
  }, new Map());

const renderProductCard = (product) => {
  const category = getCategoryName(product);
  const image = getProductImage(product);

  return `
    <article class="home-product-card products-page-card">
      <img src="${image}" alt="${escapeHtml(product.nombre)}" />
      <div>
        <span>${escapeHtml(category)}</span>
        <h3>${escapeHtml(product.nombre)}</h3>
        <p>${formatPrice(product.precio)}</p>
        <button
          type="button"
          class="product-add-button"
          data-product-id="${product.id || ""}"
          ${product.id ? "" : "disabled"}
        >
          Agregar al carrito
        </button>
      </div>
    </article>
  `;
};

const renderProductsPage = (products) => {
  const grid = document.getElementById("products-page-grid");
  const count = document.getElementById("products-page-count");

  if (!grid) return;

  if (count) {
    count.textContent = `${products.length} productos`;
  }

  const groupedProducts = groupProductsByCategory(products);

  grid.innerHTML = [...groupedProducts.entries()]
    .map(([category, categoryProducts]) => {
      return `
        <section class="products-category-section">
          <div class="products-category-heading">
            <div>
              <p>Categoria</p>
              <h3>${escapeHtml(category)}</h3>
            </div>
            <span>${categoryProducts.length} productos</span>
          </div>
          <div class="products-category-grid">
            ${categoryProducts.map(renderProductCard).join("")}
          </div>
        </section>
      `;
    })
    .join("");
};

const loadProductsPage = async () => {
  const grid = document.getElementById("products-page-grid");
  if (!grid) return;

  try {
    const response = await fetch("/api/productos");
    if (!response.ok) throw new Error("No se pudieron cargar productos");

    const products = await response.json();
    renderProductsPage(products.length ? products : fallbackProducts);
  } catch (error) {
    console.error("Error cargando pagina de productos:", error);
    renderProductsPage(fallbackProducts);
  }
};

const setupAddToCart = () => {
  const grid = document.getElementById("products-page-grid");
  if (!grid) return;

  grid.addEventListener("click", async (event) => {
    const button = event.target.closest("[data-product-id]");
    if (!button || !button.dataset.productId) return;

    const session = await validateSession();
    const clientId = session?.cliente?.id;

    if (!clientId) {
      window.location.href = "login.html";
      return;
    }

    const previousText = button.textContent;
    button.disabled = true;
    button.textContent = "Agregando...";

    try {
      const response = await fetch(
        `/api/carritos/cliente/${clientId}/productos/${button.dataset.productId}`,
        { method: "POST" },
      );

      if (!response.ok) throw new Error("No se pudo agregar el producto");

      button.textContent = "Agregado";
      await updateCartBadge();
    } catch (error) {
      console.error("Error agregando producto al carrito:", error);
      button.textContent = "Error";
      setTimeout(() => {
        button.textContent = previousText;
        button.disabled = false;
      }, 1600);
      return;
    }

    setTimeout(() => {
      button.textContent = previousText;
      button.disabled = false;
    }, 1200);
  });
};

document.addEventListener("DOMContentLoaded", () => {
  loadProductsPage();
  setupAddToCart();
});
