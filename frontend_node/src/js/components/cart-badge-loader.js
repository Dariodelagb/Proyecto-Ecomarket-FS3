import { getActiveClientId } from "./auth-loader";

const updateCartBadge = async () => {
  const badge = document.getElementById("cart-count");
  if (!badge) return;

  const clientId = getActiveClientId();
  if (!clientId) {
    badge.textContent = "0";
    return;
  }

  try {
    const response = await fetch(`/api/carritos/cliente/${clientId}`);
    if (!response.ok) throw new Error("No se pudo cargar el carrito");

    const cart = await response.json();
    badge.textContent = cart.productos?.length || 0;
  } catch (error) {
    console.error("Error cargando contador del carrito:", error);
    badge.textContent = "0";
  }
};

document.addEventListener("DOMContentLoaded", () => {
  updateCartBadge();
});

export { updateCartBadge };
