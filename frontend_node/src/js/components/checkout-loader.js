import { getSession, validateSession } from "./auth-loader";
import { updateCartBadge } from "./cart-badge-loader";
import { getProductImage } from "./product-image-resolver";

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

const getCartItems = (cart) => {
  if (cart?.items?.length) {
    return cart.items.map((item) => ({
      producto: item.producto,
      cantidad: item.cantidad || 1,
    }));
  }

  return (cart?.productos || []).map((product) => ({
    producto: product,
    cantidad: 1,
  }));
};

const getCartTotal = (items) =>
  items.reduce((total, item) => total + ((item.producto?.precio || 0) * item.cantidad), 0);

const getCartCount = (items) =>
  items.reduce((total, item) => total + item.cantidad, 0);

const enforceDigits = (input, maxLength) => {
  if (!input) return;

  input.addEventListener("input", () => {
    input.value = input.value.replace(/\D/g, "").slice(0, maxLength);
  });
};

const setupInputConstraints = () => {
  const form = document.getElementById("checkout-form");
  if (!form) return;

  enforceDigits(form.rut, 8);
  enforceDigits(form.expiracion, 4);
  enforceDigits(form.tarjeta, 19);
  enforceDigits(form.cvv, 4);
};

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
  const items = getCartItems(currentCart);
  const itemCount = getCartCount(items);

  if (total) total.textContent = formatPrice(getCartTotal(items));
  if (count) {
    count.textContent = `${itemCount} ${itemCount === 1 ? "producto" : "productos"}`;
  }

  if (!list) return;

  list.innerHTML = items
    .map((item) => {
      const product = item.producto;
      const image = getProductImage(product);
      const subtotal = (product.precio || 0) * item.cantidad;

      return `
        <article class="checkout-item">
          <img src="${image}" alt="${escapeHtml(product.nombre)}" />
          <div>
            <h3>${escapeHtml(product.nombre)}</h3>
            <p>${formatPrice(product.precio)} x ${item.cantidad}</p>
            <strong>${formatPrice(subtotal)}</strong>
          </div>
        </article>
      `;
    })
    .join("");
};

const setNewAddressRequired = (required) => {
  const addressForm = document.getElementById("checkout-address-form");
  if (!addressForm) return;

  addressForm.querySelectorAll("input").forEach((input) => {
    input.required = required && input.name !== "referencia";
  });
};

const setNewAddressMode = (enabled) => {
  const selectWrap = document.getElementById("checkout-address-select-wrap");
  const addressForm = document.getElementById("checkout-address-form");
  const toggle = document.getElementById("checkout-add-address-toggle");

  if (!addressForm) return;

  addressForm.hidden = !enabled;
  setNewAddressRequired(enabled);

  if (selectWrap) {
    selectWrap.hidden = enabled || !currentDirections.length;
  }

  if (toggle) {
    toggle.hidden = !currentDirections.length;
    toggle.textContent = enabled ? "Usar direccion guardada" : "Agregar otra direccion";
  }
};

const shouldUseNewAddress = () => {
  const addressForm = document.getElementById("checkout-address-form");
  return Boolean(addressForm && !addressForm.hidden);
};

const renderDirections = () => {
  const selectWrap = document.getElementById("checkout-address-select-wrap");
  const select = document.getElementById("checkout-address-select");
  if (!selectWrap || !select) return;

  if (!currentDirections.length) {
    setNewAddressMode(true);
    return;
  }

  select.innerHTML = currentDirections
    .map(
      (direccion) => `
        <option value="${direccion.id}">
          ${escapeHtml(direccion.calle)} ${escapeHtml(direccion.numero)}, ${escapeHtml(direccion.comuna)}
        </option>
      `,
    )
    .join("");

  setNewAddressMode(false);
};

const loadCheckout = async () => {
  const form = document.getElementById("checkout-form");
  if (!form) return;

  const session = await validateSession();
  const clientId = session?.cliente?.id;

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

    if (!getCartItems(currentCart).length) {
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
  if (currentDirections.length && !shouldUseNewAddress()) {
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

const setupAddressToggle = () => {
  const toggle = document.getElementById("checkout-add-address-toggle");
  if (!toggle) return;

  toggle.addEventListener("click", () => {
    const addressForm = document.getElementById("checkout-address-form");
    setNewAddressMode(addressForm?.hidden ?? true);
  });
};

const createSale = async (form) => {
  const session = await validateSession();
  const clientId = session?.cliente?.id;
  const items = getCartItems(currentCart);

  if (!clientId) {
    throw new Error("Debes iniciar sesion para completar la compra");
  }

  const direccion = await resolveAddress(form, clientId);

  const payload = {
    tipoEnvio: "Envio a domicilio",
    monto: getCartTotal(items),
    cliente: { id: clientId },
    direccion: { id: direccion.id },
    detalles: items.map((item) => ({
      cantidad: item.cantidad,
      precioUnitario: item.producto?.precio || 0,
      producto: { id: item.producto?.id },
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
  const session = await validateSession();
  const clientId = session?.cliente?.id;

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
  setupInputConstraints();
  setupAddressToggle();
  loadCheckout();
  setupCheckoutSubmit();
});
