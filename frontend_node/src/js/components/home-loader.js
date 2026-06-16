import product01 from "../../images/product/product-01.jpg";
import product02 from "../../images/product/product-02.jpg";
import product03 from "../../images/product/product-03.jpg";
import product04 from "../../images/product/product-04.jpg";
import product05 from "../../images/product/product-05.jpg";

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

const renderProducts = (products) => {
  const grid = document.getElementById("home-products-grid");
  if (!grid) return;

  const visibleProducts = products.slice(0, 6);

  grid.innerHTML = visibleProducts
    .map((product, index) => {
      const category = product.categoria?.categoria || "Producto";
      const image = productImages[index % productImages.length];

      return `
        <article class="home-product-card">
          <img src="${image}" alt="${escapeHtml(product.nombre)}" />
          <div>
            <span>${escapeHtml(category)}</span>
            <h3>${escapeHtml(product.nombre)}</h3>
            <p>${formatPrice(product.precio)}</p>
          </div>
        </article>
      `;
    })
    .join("");
};

const loadHomeProducts = async () => {
  const grid = document.getElementById("home-products-grid");
  if (!grid) return;

  try {
    const response = await fetch("/api/productos");
    if (!response.ok) throw new Error("No se pudieron cargar productos");

    const products = await response.json();
    renderProducts(products.length ? products : fallbackProducts);
  } catch (error) {
    console.error("Error cargando productos de la home:", error);
    renderProducts(fallbackProducts);
  }
};

const setupHomeForm = () => {
  const form = document.getElementById("contact-form");
  if (!form) return;

  const status = document.getElementById("contact-form-status");
  const submitButton = form.querySelector('button[type="submit"]');

  form.addEventListener("submit", async (event) => {
    event.preventDefault();

    const payload = {
      nombre: form.nombre.value.trim(),
      correo: form.correo.value.trim(),
      mensaje: form.mensaje.value.trim(),
    };

    if (!payload.nombre || !payload.correo || !payload.mensaje) {
      if (status) status.textContent = "Completa todos los campos.";
      return;
    }

    if (status) status.textContent = "Enviando mensaje...";
    if (submitButton) submitButton.disabled = true;

    try {
      const response = await fetch("/api/contactos", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      if (!response.ok) throw new Error("No se pudo guardar el mensaje");

      form.reset();
      if (status) status.textContent = "Mensaje enviado correctamente.";
    } catch (error) {
      console.error("Error enviando formulario:", error);
      if (status) status.textContent = "No se pudo enviar el mensaje. Intenta nuevamente.";
    } finally {
      if (submitButton) submitButton.disabled = false;
    }
  });
};

document.addEventListener("DOMContentLoaded", () => {
  loadHomeProducts();
  setupHomeForm();
});
