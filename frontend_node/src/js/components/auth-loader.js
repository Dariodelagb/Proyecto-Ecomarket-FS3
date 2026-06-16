const SESSION_KEY = "ecomarketSession";

const getSession = () => {
  try {
    return JSON.parse(localStorage.getItem(SESSION_KEY));
  } catch {
    return null;
  }
};

const setSession = (session) => {
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
};

const clearSession = () => {
  localStorage.removeItem(SESSION_KEY);
};

const getActiveClientId = () => getSession()?.cliente?.id || null;

const updateAuthControls = () => {
  const hasSession = Boolean(getSession()?.token);

  document.querySelectorAll(".auth-guest-only").forEach((element) => {
    element.hidden = hasSession;
  });

  document.querySelectorAll(".auth-session-only").forEach((element) => {
    element.hidden = !hasSession;
  });
};

const setupLogout = () => {
  document.querySelectorAll("[data-logout]").forEach((button) => {
    button.addEventListener("click", async () => {
      const token = getSession()?.token;

      if (token) {
        try {
          await fetch(`/api/auth/sesion/${encodeURIComponent(token)}`, {
            method: "DELETE",
          });
        } catch (error) {
          console.error("Error cerrando sesion:", error);
        }
      }

      clearSession();
      updateAuthControls();
      window.location.href = "index.html";
    });
  });
};

const getAddressPayload = (form) => {
  const values = {
    calle: form.calle?.value.trim(),
    numero: form.numero?.value.trim(),
    comuna: form.comuna?.value.trim(),
    ciudad: form.ciudad?.value.trim(),
    region: form.region?.value.trim(),
    referencia: form.referencia?.value.trim(),
    principal: true,
  };

  const hasAddress = ["calle", "numero", "comuna", "ciudad", "region"].some(
    (field) => values[field],
  );

  return hasAddress ? values : null;
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

const setupRegisterForm = () => {
  const form = document.getElementById("register-form");
  if (!form) return;

  const status = document.getElementById("register-status");
  const button = form.querySelector('button[type="submit"]');

  form.addEventListener("submit", async (event) => {
    event.preventDefault();

    const direccion = getAddressPayload(form);
    const payload = {
      nombres: form.nombres.value.trim(),
      apellidos: form.apellidos.value.trim(),
      rut: Number(form.rut.value),
      dvrut: form.dvrut.value.trim().toUpperCase(),
      email: form.email.value.trim(),
      contrasena: form.contrasena.value,
      direcciones: direccion ? [direccion] : [],
    };

    if (status) status.textContent = "Creando cuenta...";
    if (button) button.disabled = true;

    try {
      const response = await fetch("/api/auth/registro", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        const message = await getResponseMessage(response, "No se pudo registrar el usuario");
        throw new Error(message);
      }

      const session = await response.json();
      setSession(session);
      window.location.href = "index.html";
    } catch (error) {
      console.error("Error registrando usuario:", error);
      if (status) status.textContent = error.message || "No se pudo registrar. Revisa los datos.";
      if (button) button.disabled = false;
    }
  });
};

const setupLoginForm = () => {
  const form = document.getElementById("login-form");
  if (!form) return;

  const status = document.getElementById("login-status");
  const button = form.querySelector('button[type="submit"]');

  form.addEventListener("submit", async (event) => {
    event.preventDefault();

    const payload = {
      email: form.email.value.trim(),
      rut: form.rut.value,
      dvrut: form.dvrut.value.trim().toUpperCase(),
      contrasena: form.contrasena.value,
    };

    if (status) status.textContent = "Iniciando sesion...";
    if (button) button.disabled = true;

    try {
      const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        const message = await getResponseMessage(response, "Credenciales invalidas");
        throw new Error(message);
      }

      const session = await response.json();
      setSession(session);
      window.location.href = "index.html";
    } catch (error) {
      console.error("Error iniciando sesion:", error);
      if (status) status.textContent = error.message || "Datos incorrectos. Intenta nuevamente.";
      if (button) button.disabled = false;
    }
  });
};

document.addEventListener("DOMContentLoaded", () => {
  updateAuthControls();
  setupLogout();
  setupRegisterForm();
  setupLoginForm();
});

export { clearSession, getActiveClientId, getSession, updateAuthControls };
