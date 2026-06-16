import product01 from "../../images/product/product-01.jpg";
import product02 from "../../images/product/product-02.jpg";
import product03 from "../../images/product/product-03.jpg";
import product04 from "../../images/product/product-04.jpg";
import product05 from "../../images/product/product-05.jpg";
import { getActiveClientId, getSession } from "./auth-loader";
import { updateCartBadge } from "./cart-badge-loader";

const productImages = [product01, product02, product03, product04, product05];

let currentCart = null;
let currentDirections = [];

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

const getResponseMessage = async (response, fallback) => {
  try {
    const text = await response.text();
    if (!text) return fallback;

    try {
      const data = JSON.parse(text);
      return data.message || data.detail || data.error || fallback;
    } catch {
      return text;
    }
  } catch {
    return fallback;
  }
};

const showCheckoutMessage = (title, text, actionHref = "login.html", actionText = "Iniciar sesion") => {
  const content = document.getElementById("checkout-content");
  if (!content) return;

  content.innerHTML = `
    <div class="cart-empty checkout-message">
      <h3>${escapeHtml(title)}</h3>
      <p>${escapeHtml(text)}</p>
      <a href="${actionHref}" class="home-button home-button--catalog">${escapeHtml(actionText)}</a>
    </div>
  `;
};

const fillCustomerFields = () => {
  const form = document.getElementById("checkout-form");
  const cliente = getSession()?.cliente;
  if (!form || !cliente) return;

  form.nombreCompleto.value = [cliente.nombres, cliente.apellidos].filter(Boolean).join(" ");
  form.rut.value = cliente.rut || "";
  form.dvrut.value = cliente.dvrut || "";
  form.email.value = cliente.email || "";
};

const renderSummary = () => {
  const list = document.getElementById("checkout-items");
  const total = document.getElementById("checkout-total");
  const count = document.getElementById("checkout-count");
  const products = currentCart?.productos || [];

  if (total) total.textContent = formatPrice(getCartTotal(products));
  if (count) {
    count.textContent = `${products.length} ${products.length === 1 ? "producto" : "productos"}`;
  }

  if (!list) return;

  list.innerHTML = products
    .map((product, index) => {
      const image = productImages[index % productImages.length];

      return `
        <article class="checkout-item">
          <img src="${image}" alt="${escapeHtml(product.nombre)}" />
          <div>
            <h3>${escapeHtml(product.nombre)}</h3>
            <p>${formatPrice(product.precio)}</p>
          </div>
        </article>
      `;
    })
    .join("");
};

const renderDirections = () => {
  const selectWrap = document.getElementById("checkout-address-select-wrap");
  const select = document.getElementById("checkout-address-select");
  const addressForm = document.getElementById("checkout-address-form");
  if (!selectWrap || !select || !addressForm) return;

  if (!currentDirections.length) {
    selectWrap.hidden = true;
    addressForm.hidden = false;
    addressForm.querySelectorAll("input").forEach((input) => {
      if (input.name !== "referencia") input.required = true;
    });
    return;
  }

  selectWrap.hidden = false;
  addressForm.hidden = true;
  addressForm.querySelectorAll("input").forEach((input) => {
    input.required = false;
  });

  select.innerHTML = currentDirections
    .map(
      (direccion) => `
        <option value="${direccion.id}">
          ${escapeHtml(direccion.calle)} ${escapeHtml(direccion.numero)}, ${escapeHtml(direccion.comuna)}
        </option>
      `,
    )
    .join("");
};

const loadCheckout = async () => {
  const form = document.getElementById("checkout-form");
  if (!form) return;

  const clientId = getActiveClientId();
  if (!clientId) {
    showCheckoutMessage(
      "Inicia sesion para comprar",
      "Tu compra se registra con el cliente asociado a la sesion.",
    );
    return;
  }

  try {
    const [cartResponse, directionsResponse] = await Promise.all([
      fetch(`/api/carritos/cliente/${clientId}`),
      fetch(`/api/clientes/${clientId}/direcciones`),
    ]);

    if (!cartResponse.ok) throw new Error("No se pudo cargar el carrito");
    if (!directionsResponse.ok) throw new Error("No se pudieron cargar las direcciones");

    currentCart = await cartResponse.json();
    currentDirections = await directionsResponse.json();

    if (!currentCart.productos?.length) {
      showCheckoutMessage(
        "Tu carrito esta vacio",
        "Agrega productos antes de finalizar la compra.",
        "productos.html",
        "Ver productos",
      );
      return;
    }

    fillCustomerFields();
    renderDirections();
    renderSummary();
  } catch (error) {
    console.error("Error cargando compra:", error);
    showCheckoutMessage(
      "No se pudo cargar la compra",
      "Revisa que el backend este iniciado e intenta nuevamente.",
      "carrito.html",
      "Volver al carrito",
    );
  }
};

const getNewAddressPayload = (form) => ({
  calle: form.calle.value.trim(),
  numero: form.numero.value.trim(),
  comuna: form.comuna.value.trim(),
  ciudad: form.ciudad.value.trim(),
  region: form.region.value.trim(),
  referencia: form.referencia.value.trim(),
  principal: true,
});

const resolveAddress = async (form, clientId) => {
  if (currentDirections.length) {
    return { id: Number(form.direccionId.value) };
  }

  const response = await fetch(`/api/clientes/${clientId}/direcciones`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(getNewAddressPayload(form)),
  });

  if (!response.ok) {
    const message = await getResponseMessage(response, "No se pudo guardar la direccion");
    throw new Error(message);
  }

  return response.json();
};

const createSale = async (form) => {
  const clientId = getActiveClientId();
  const products = currentCart?.productos || [];
  const direccion = await resolveAddress(form, clientId);

  const payload = {
    tipoEnvio: "Envio a domicilio",
    monto: getCartTotal(products),
    cliente: { id: clientId },
    direccion: { id: direccion.id },
    detalles: products.map((product) => ({
      cantidad: 1,
      precioUnitario: product.precio || 0,
      producto: { id: product.id },
    })),
  };

  const response = await fetch("/api/ventas", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const message = await getResponseMessage(response, "No se pudo registrar la venta");
    throw new Error(message);
  }

  return response.json();
};

const clearCart = async () => {
  const clientId = getActiveClientId();
  if (!clientId) return;

  await fetch(`/api/carritos/cliente/${clientId}`, { method: "DELETE" });
  await updateCartBadge();
};

const setupCheckoutSubmit = () => {
  const form = document.getElementById("checkout-form");
  if (!form) return;

  const status = document.getElementById("checkout-status");
  const button = form.querySelector('button[type="submit"]');

  form.addEventListener("submit", async (event) => {
    event.preventDefault();

    if (status) status.textContent = "Registrando compra...";
    if (button) button.disabled = true;

    try {
      await createSale(form);
      await clearCart();

      if (status) status.textContent = "Compra registrada correctamente.";
      showCheckoutMessage(
        "Compra registrada",
        "La venta fue guardada y el carrito quedo vacio.",
        "productos.html",
        "Seguir comprando",
      );
    } catch (error) {
      console.error("Error registrando compra:", error);
      if (status) status.textContent = error.message || "No se pudo completar la compra.";
      if (button) button.disabled = false;
    }
  });
};

document.addEventListener("DOMContentLoaded", () => {
  loadCheckout();
  setupCheckoutSubmit();
});
