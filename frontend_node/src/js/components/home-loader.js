import { getProductImage } from "./product-image-resolver";

const fallbackProducts = [
  { nombre: "Perfume natural", precio: 8500, categoria: { categoria: "Perfumes" } },
  { nombre: "Bloqueador solar", precio: 11900, categoria: { categoria: "Bloqueadores" } },
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

let carouselTimer = null;
let carouselIndex = 0;

const renderProducts = (products) => {
  const grid = document.getElementById("home-products-grid");
  if (!grid) return;

  const visibleProducts = products.slice(0, 6);

  grid.innerHTML = visibleProducts
    .map((product) => {
      const category = product.categoria?.categoria || "Producto";
      const image = getProductImage(product);

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

const renderFeaturedCarousel = (products) => {
  const carousel = document.getElementById("home-featured-carousel");
  if (!carousel) return;

  const featuredProducts = products.slice(0, 4);
  if (!featuredProducts.length) {
    carousel.innerHTML = "";
    return;
  }

  carouselIndex = 0;
  if (carouselTimer) {
    clearInterval(carouselTimer);
  }

  carousel.innerHTML = `
    <div class="home-featured-carousel__viewport">
      <div class="home-featured-carousel__track">
        ${featuredProducts
          .map((product) => {
            const category = product.categoria?.categoria || "Producto";
            const image = getProductImage(product);

            return `
              <article class="home-featured-slide">
                <img src="${image}" alt="${escapeHtml(product.nombre)}" />
                <div>
                  <span>${escapeHtml(category)}</span>
                  <h2>${escapeHtml(product.nombre)}</h2>
                  <p>${formatPrice(product.precio)}</p>
                </div>
              </article>
            `;
          })
          .join("")}
      </div>
    </div>
    <div class="home-featured-carousel__dots" aria-hidden="true">
      ${featuredProducts.map((_, index) => `<span class="${index === 0 ? "is-active" : ""}"></span>`).join("")}
    </div>
  `;

  const track = carousel.querySelector(".home-featured-carousel__track");
  const dots = [...carousel.querySelectorAll(".home-featured-carousel__dots span")];

  const updateCarousel = () => {
    if (!track) return;

    track.style.transform = `translateX(-${carouselIndex * 100}%)`;
    dots.forEach((dot, index) => {
      dot.classList.toggle("is-active", index === carouselIndex);
    });
  };

  carouselTimer = setInterval(() => {
    carouselIndex = (carouselIndex + 1) % featuredProducts.length;
    updateCarousel();
  }, 3200);
};

const loadHomeProducts = async () => {
  const grid = document.getElementById("home-products-grid");
  const carousel = document.getElementById("home-featured-carousel");
  if (!grid && !carousel) return;

  try {
    const response = await fetch("/api/productos");
    if (!response.ok) throw new Error("No se pudieron cargar productos");

    const products = await response.json();
    const visibleProducts = products.length ? products : fallbackProducts;
    renderProducts(visibleProducts);
    renderFeaturedCarousel(visibleProducts);
  } catch (error) {
    console.error("Error cargando productos de la home:", error);
    renderProducts(fallbackProducts);
    renderFeaturedCarousel(fallbackProducts);
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
