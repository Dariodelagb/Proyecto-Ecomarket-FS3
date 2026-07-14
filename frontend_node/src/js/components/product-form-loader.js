const PRODUCT_FORM_ID = "admin-product-form";
const CATEGORY_SELECT_ID = "admin-product-category";
const MESSAGE_ID = "admin-product-message";
const SUBMIT_ID = "admin-product-submit";

const setMessage = (message, type = "info") => {
  const element = document.getElementById(MESSAGE_ID);
  if (!element) return;

  const classes = {
    success:
      "border border-success-200 bg-success-50 text-success-700 dark:border-success-500/20 dark:bg-success-500/10 dark:text-success-400",
    error:
      "border border-error-200 bg-error-50 text-error-700 dark:border-error-500/20 dark:bg-error-500/10 dark:text-error-400",
    info:
      "border border-gray-200 bg-gray-50 text-gray-600 dark:border-gray-800 dark:bg-white/[0.03] dark:text-gray-300",
  };

  element.className = `rounded-lg px-3 py-2 text-theme-sm lg:col-span-12 ${classes[type] || classes.info}`;
  element.textContent = message;
};

const clearMessage = () => {
  const element = document.getElementById(MESSAGE_ID);
  if (!element) return;

  element.className = "hidden rounded-lg px-3 py-2 text-theme-sm lg:col-span-12";
  element.textContent = "";
};

const setSubmitting = (isSubmitting) => {
  const button = document.getElementById(SUBMIT_ID);
  if (!button) return;

  button.disabled = isSubmitting;
  button.textContent = isSubmitting ? "Guardando" : "Guardar";
};

const loadCategories = async () => {
  const select = document.getElementById(CATEGORY_SELECT_ID);
  if (!select) return;

  try {
    const response = await fetch("/api/categorias");
    if (!response.ok) throw new Error("No se pudieron cargar las categorias");

    const categories = await response.json();
    select.innerHTML = '<option value="">Selecciona una categoria</option>';

    categories.forEach((category) => {
      const option = document.createElement("option");
      option.value = category.id;
      option.textContent = category.categoria;
      select.appendChild(option);
    });
  } catch (error) {
    console.error("Error cargando categorias:", error);
    select.innerHTML = '<option value="">Error al cargar categorias</option>';
    setMessage("No se pudieron cargar las categorias.", "error");
  }
};

const toPositiveInteger = (value, fieldName, allowZero = false) => {
  const number = Number(value);
  const min = allowZero ? 0 : 1;

  if (!Number.isInteger(number) || number < min) {
    throw new Error(`${fieldName} debe ser un numero entero ${allowZero ? "mayor o igual a 0" : "mayor a 0"}.`);
  }

  return number;
};

const createProduct = async ({ nombre, precio, categoriaId }) => {
  const response = await fetch("/api/productos", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      nombre,
      precio,
      categoria: {
        id: categoriaId,
      },
    }),
  });

  if (!response.ok) {
    throw new Error("No se pudo crear el producto.");
  }

  return response.json();
};

const createStock = async ({ productoId, stock }) => {
  const response = await fetch("/api/stock-producto", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      stock,
      producto: {
        id: productoId,
      },
    }),
  });

  if (!response.ok) {
    throw new Error("El producto se creo, pero no se pudo registrar su stock inicial.");
  }

  return response.json();
};

const handleSubmit = async (event) => {
  event.preventDefault();
  clearMessage();

  const form = event.currentTarget;
  const formData = new FormData(form);

  try {
    setSubmitting(true);

    const nombre = String(formData.get("nombre") || "").trim();
    const precio = toPositiveInteger(formData.get("precio"), "El precio");
    const stock = toPositiveInteger(formData.get("stock"), "El stock", true);
    const categoriaId = toPositiveInteger(formData.get("categoria"), "La categoria");

    if (nombre.length < 3) {
      throw new Error("El nombre debe tener al menos 3 caracteres.");
    }

    const product = await createProduct({ nombre, precio, categoriaId });
    await createStock({ productoId: product.id, stock });

    form.reset();
    await loadCategories();
    setMessage(`Producto "${product.nombre}" agregado correctamente.`, "success");
    window.dispatchEvent(new CustomEvent("ecomarket:products-updated"));
  } catch (error) {
    console.error("Error creando producto:", error);
    setMessage(error.message || "No se pudo crear el producto.", "error");
  } finally {
    setSubmitting(false);
  }
};

document.addEventListener("DOMContentLoaded", () => {
  const form = document.getElementById(PRODUCT_FORM_ID);
  if (!form) return;

  loadCategories();
  form.addEventListener("submit", handleSubmit);
});
