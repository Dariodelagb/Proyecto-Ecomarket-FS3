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
  const productId = product.id || "";

  return `
    <article class="home-product-card products-page-card">
      <img src="${image}" alt="${escapeHtml(product.nombre)}" />
      <div>
        <span>${escapeHtml(category)}</span>
        <h3>${escapeHtml(product.nombre)}</h3>
        <p>${formatPrice(product.precio)}</p>
        <div class="product-quantity-control" data-quantity-product-id="${productId}">
          <button type="button" data-quantity-action="decrease" ${product.id ? "" : "disabled"}>-</button>
          <input
            type="number"
            value="1"
            min="1"
            max="99"
            inputmode="numeric"
            aria-label="Cantidad de ${escapeHtml(product.nombre)}"
            data-quantity-input
            ${product.id ? "" : "disabled"}
          />
          <button type="button" data-quantity-action="increase" ${product.id ? "" : "disabled"}>+</button>
        </div>
        <button
          type="button"
          class="product-add-button"
          data-product-id="${productId}"
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
    const quantityButton = event.target.closest("[data-quantity-action]");
    if (quantityButton) {
      const control = quantityButton.closest(".product-quantity-control");
      const input = control?.querySelector("[data-quantity-input]");
      if (!input) return;

      const currentValue = Number(input.value) || 1;
      const nextValue =
        quantityButton.dataset.quantityAction === "increase"
          ? currentValue + 1
          : currentValue - 1;

      input.value = String(Math.min(99, Math.max(1, nextValue)));
      return;
    }

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
    const card = button.closest(".products-page-card");
    const quantityInput = card?.querySelector("[data-quantity-input]");
    const quantity = Math.min(99, Math.max(1, Number(quantityInput?.value) || 1));

    try {
      const response = await fetch(
        `/api/carritos/cliente/${clientId}/productos/${button.dataset.productId}`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ cantidad: quantity }),
        },
      );

      if (!response.ok) throw new Error("No se pudo agregar el producto");

      button.textContent = quantity === 1 ? "Agregado" : `${quantity} agregados`;
      if (quantityInput) quantityInput.value = "1";
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

  grid.addEventListener("input", (event) => {
    const input = event.target.closest("[data-quantity-input]");
    if (!input) return;

    const value = Number(input.value) || 1;
    input.value = String(Math.min(99, Math.max(1, value)));
  });
};

document.addEventListener("DOMContentLoaded", () => {
  loadProductsPage();
  setupAddToCart();
});
