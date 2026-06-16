import product01 from "../../images/product/product-01.jpg";
import product02 from "../../images/product/product-02.jpg";
import product03 from "../../images/product/product-03.jpg";
import product04 from "../../images/product/product-04.jpg";
import product05 from "../../images/product/product-05.jpg";
import { getActiveClientId } from "./auth-loader";
import { updateCartBadge } from "./cart-badge-loader";

const productImages = [product01, product02, product03, product04, product05];

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

const getCartTotal = (products) =>
  products.reduce((total, product) => total + (product.precio || 0), 0);

const renderCart = (cart) => {
  const list = document.getElementById("cart-items");
  const total = document.getElementById("cart-total");
  const summaryCount = document.getElementById("cart-summary-count");
  const clientName = document.getElementById("cart-client-name");

  if (!list) return;

  const products = cart.productos || [];
  const cliente = cart.cliente;

  if (clientName) {
    clientName.textContent = cliente
      ? `${cliente.nombres} ${cliente.apellidos}`
      : "Cliente invitado";
  }

  if (total) total.textContent = formatPrice(getCartTotal(products));
  if (summaryCount) {
    summaryCount.textContent = `${products.length} ${products.length === 1 ? "producto" : "productos"}`;
  }

  if (!products.length) {
    list.innerHTML = `
      <div class="cart-empty">
        <h3>Tu carrito esta vacio</h3>
        <p>Agrega productos desde el catalogo para verlos aqui.</p>
        <a href="productos.html" class="home-button home-button--catalog">Ver productos</a>
      </div>
    `;
    return;
  }

  list.innerHTML = products
    .map((product, index) => {
      const image = productImages[index % productImages.length];
      const category = product.categoria?.categoria || "Producto";

      return `
        <article class="cart-item">
          <img src="${image}" alt="${escapeHtml(product.nombre)}" />
          <div>
            <span>${escapeHtml(category)}</span>
            <h3>${escapeHtml(product.nombre)}</h3>
            <p>${formatPrice(product.precio)}</p>
          </div>
          <button type="button" data-remove-product-id="${product.id}">Quitar</button>
        </article>
      `;
    })
    .join("");
};

const loadCartPage = async () => {
  const list = document.getElementById("cart-items");
  if (!list) return;

  const clientId = getActiveClientId();
  if (!clientId) {
    list.innerHTML = `
      <div class="cart-empty">
        <h3>Inicia sesion para usar el carrito</h3>
        <p>Tu carrito se guarda junto a tu cuenta de cliente.</p>
        <a href="login.html" class="home-button home-button--catalog">Iniciar sesion</a>
      </div>
    `;
    return;
  }

  try {
    const response = await fetch(`/api/carritos/cliente/${clientId}`);
    if (!response.ok) throw new Error("No se pudo cargar el carrito");

    const cart = await response.json();
    renderCart(cart);
  } catch (error) {
    console.error("Error cargando carrito:", error);
    list.innerHTML = `
      <div class="cart-empty">
        <h3>No se pudo cargar el carrito</h3>
        <p>Revisa que el backend este iniciado e intenta nuevamente.</p>
      </div>
    `;
  }
};

const setupCartActions = () => {
  const list = document.getElementById("cart-items");
  if (!list) return;

  list.addEventListener("click", async (event) => {
    const button = event.target.closest("[data-remove-product-id]");
    if (!button) return;

    button.disabled = true;

    try {
      const clientId = getActiveClientId();
      if (!clientId) {
        window.location.href = "login.html";
        return;
      }

      const response = await fetch(
        `/api/carritos/cliente/${clientId}/productos/${button.dataset.removeProductId}`,
        { method: "DELETE" },
      );

      if (!response.ok) throw new Error("No se pudo quitar el producto");

      await loadCartPage();
      await updateCartBadge();
    } catch (error) {
      console.error("Error quitando producto del carrito:", error);
      button.disabled = false;
    }
  });
};

document.addEventListener("DOMContentLoaded", () => {
  loadCartPage();
  setupCartActions();
});
