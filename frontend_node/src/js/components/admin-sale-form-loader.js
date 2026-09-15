import { getSession } from "./auth-loader";

const currency = new Intl.NumberFormat("es-CL", {
  style: "currency",
  currency: "CLP",
  maximumFractionDigits: 0,
});

const getErrorMessage = async (response) => {
  try {
    const data = await response.json();
    return data.detail || data.message || data.error || "No se pudo completar la operacion";
  } catch {
    return "No se pudo completar la operacion";
  }
};

const initializeAdminSaleForm = () => {
  const form = document.getElementById("admin-sale-form");
  const session = getSession();
  if (!form || session?.cliente?.rol !== "ADMIN") return;

  const clientSelect = document.getElementById("admin-sale-client");
  const addressSelect = document.getElementById("admin-sale-address");
  const productSelect = document.getElementById("admin-sale-product");
  const quantityInput = document.getElementById("admin-sale-quantity");
  const shippingSelect = document.getElementById("admin-sale-shipping");
  const unitPriceInput = document.getElementById("admin-sale-unit-price");
  const totalElement = document.getElementById("admin-sale-total");
  const statusElement = document.getElementById("admin-sale-status");
  const submitButton = form.querySelector('button[type="submit"]');
  const headers = { "X-Session-Token": session.token };

  const setStatus = (message, isError = false) => {
    statusElement.textContent = message;
    statusElement.classList.toggle("is-error", isError);
  };

  const selectedPrice = () => Number(productSelect.selectedOptions[0]?.dataset.price || 0);
  const updateTotals = () => {
    const quantity = Math.max(1, Number(quantityInput.value) || 1);
    const price = selectedPrice();
    unitPriceInput.value = currency.format(price);
    totalElement.textContent = currency.format(price * quantity);
  };

  const loadAddresses = async () => {
    addressSelect.innerHTML = '<option value="">Sin direccion</option>';
    addressSelect.disabled = true;
    if (!clientSelect.value) return;

    try {
      const response = await fetch(`/api/clientes/${clientSelect.value}/direcciones`, { headers });
      if (!response.ok) throw new Error(await getErrorMessage(response));
      const addresses = await response.json();
      addresses.forEach((address) => {
        const option = document.createElement("option");
        option.value = address.id;
        option.textContent = [address.calle, address.numero, address.comuna, address.ciudad]
          .filter(Boolean)
          .join(", ");
        addressSelect.appendChild(option);
      });
      addressSelect.disabled = !addresses.length;
    } catch (error) {
      setStatus(error.message, true);
    }
  };

  const loadOptions = async () => {
    setStatus("Cargando datos...");
    try {
      const [clientsResponse, productsResponse] = await Promise.all([
        fetch("/api/clientes", { headers }),
        fetch("/api/productos"),
      ]);
      if (!clientsResponse.ok) throw new Error(await getErrorMessage(clientsResponse));
      if (!productsResponse.ok) throw new Error(await getErrorMessage(productsResponse));

      const [clients, products] = await Promise.all([clientsResponse.json(), productsResponse.json()]);
      clients
        .filter((client) => client.rol === "CLIENTE")
        .forEach((client) => {
          const option = document.createElement("option");
          option.value = client.id;
          option.textContent = `${client.nombres || ""} ${client.apellidos || ""} (${client.email || `ID ${client.id}`})`.trim();
          clientSelect.appendChild(option);
        });
      products.forEach((product) => {
        const option = document.createElement("option");
        option.value = product.id;
        option.dataset.price = product.precio || 0;
        option.textContent = `${product.nombre} - ${currency.format(product.precio || 0)}`;
        productSelect.appendChild(option);
      });
      setStatus("");
    } catch (error) {
      setStatus(error.message, true);
      submitButton.disabled = true;
    }
  };

  clientSelect.addEventListener("change", loadAddresses);
  productSelect.addEventListener("change", updateTotals);
  quantityInput.addEventListener("input", updateTotals);

  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const quantity = Number(quantityInput.value);
    const price = selectedPrice();
    if (!clientSelect.value || !productSelect.value || !Number.isInteger(quantity) || quantity < 1 || price < 0) {
      setStatus("Selecciona cliente, producto y una cantidad valida.", true);
      return;
    }

    const payload = {
      tipoEnvio: shippingSelect.value,
      monto: price * quantity,
      cliente: { id: Number(clientSelect.value) },
      detalles: [{
        cantidad: quantity,
        precioUnitario: price,
        producto: { id: Number(productSelect.value) },
      }],
    };
    if (addressSelect.value) payload.direccion = { id: Number(addressSelect.value) };

    submitButton.disabled = true;
    setStatus("Creando venta...");
    try {
      const response = await fetch("/api/ventas", {
        method: "POST",
        headers: { ...headers, "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      if (!response.ok) throw new Error(await getErrorMessage(response));
      const sale = await response.json();
      setStatus(`Venta #${sale.id} creada correctamente.`);
      productSelect.value = "";
      quantityInput.value = "1";
      updateTotals();
      window.dispatchEvent(new CustomEvent("pedidos360:orders-updated"));
    } catch (error) {
      setStatus(error.message, true);
    } finally {
      submitButton.disabled = false;
    }
  });

  loadOptions();
  updateTotals();
};

document.addEventListener("DOMContentLoaded", initializeAdminSaleForm);

export default initializeAdminSaleForm;
