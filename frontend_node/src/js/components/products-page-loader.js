import product01 from "../../images/product/product-01.jpg";
import product02 from "../../images/product/product-02.jpg";
import product03 from "../../images/product/product-03.jpg";
import product04 from "../../images/product/product-04.jpg";
import product05 from "../../images/product/product-05.jpg";
import { getActiveClientId } from "./auth-loader";
import { updateCartBadge } from "./cart-badge-loader";

const productImages = [
  product01,
  product02,
  product03,
  product04,
  product05,
];

const fallbackProducts = [
  { nombre: "Perfume natural", precio: 8500, categoria: { categoria: "Perfumes" } },
  { nombre: "Base maquillante", precio: 12000, categoria: { categoria: "Maquillaje" } },
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

const renderProductsPage = (products) => {
  const grid = document.getElementById("products-page-grid");
  const count = document.getElementById("products-page-count");

  if (!grid) return;

  if (count) {
    count.textContent = `${products.length} productos`;
  }

  grid.innerHTML = products
    .map((product, index) => {
      const category = product.categoria?.categoria || "Sin categoria";
      const image = productImages[index % productImages.length];

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

    const clientId = getActiveClientId();
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
