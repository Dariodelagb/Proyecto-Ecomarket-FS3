const SESSION_KEY = "ecomarketSession";
const MSAL_SESSION_KEY = "msalSession";

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

const getMsalSession = () => {
  try {
    return JSON.parse(localStorage.getItem(MSAL_SESSION_KEY));
  } catch {
    return null;
  }
};

const setMsalSession = (session) => {
  localStorage.setItem(MSAL_SESSION_KEY, JSON.stringify(session));
};

const getActiveClientId = () => getSession()?.cliente?.id || null;

const isAdminSession = () => getSession()?.cliente?.rol === "ADMIN";

const enforceDigits = (input, maxLength) => {
  if (!input) return;

  input.addEventListener("input", () => {
    input.value = input.value.replace(/\D/g, "").slice(0, maxLength);
  });
};

const setupInputConstraints = () => {
  document.querySelectorAll('input[name="rut"]').forEach((input) => {
    enforceDigits(input, 8);
  });
};

const updateAuthControls = () => {
  const session = getSession();
  const msalSession = getMsalSession();
  const hasSession = Boolean(session?.token || msalSession?.token);
  const hasAdminSession = hasSession && isAdminSession();
  const activeSession = session || msalSession;

  // Mostrar nombre del usuario si está logueado
  const userNameElement = document.getElementById("user-name");
  if (userNameElement && activeSession?.cliente?.nombres) {
    userNameElement.textContent = activeSession.cliente.nombres;
  } else if (userNameElement && msalSession?.displayName) {
    userNameElement.textContent = msalSession.displayName;
  }

  document.querySelectorAll(".auth-guest-only").forEach((element) => {
    element.hidden = hasSession;
  });

  document.querySelectorAll(".auth-session-only").forEach((element) => {
    element.hidden = !hasSession;
  });

  document.querySelectorAll(".auth-admin-only").forEach((element) => {
    element.hidden = !hasAdminSession;
  });
};

const validateSession = async () => {
  const session = getSession();
  if (!session?.token) return null;

  try {
    const response = await fetch(`/api/auth/sesion/${encodeURIComponent(session.token)}`);

    if (!response.ok) {
      clearSession();
      updateAuthControls();
      return null;
    }

    const freshSession = await response.json();

    if (!freshSession?.cliente?.id) {
      clearSession();
      updateAuthControls();
      return null;
    }

    setSession(freshSession);
    updateAuthControls();
    return freshSession;
  } catch (error) {
    console.error("Error validando sesion:", error);
    return session;
  }
};

const setupLogout = () => {
  document.querySelectorAll("[data-logout]").forEach((button) => {
    button.addEventListener("click", async () => {
      const session = getSession();
      const token = session?.token;

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
      localStorage.removeItem(MSAL_SESSION_KEY);
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
      const redirectUrl = session?.cliente?.rol === "ADMIN" ? "dashboard.html" : "index.html";
      window.location.href = redirectUrl;
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
      const redirectUrl = session?.cliente?.rol === "ADMIN" ? "dashboard.html" : "index.html";
      window.location.href = redirectUrl;
    } catch (error) {
      console.error("Error iniciando sesion:", error);
      if (status) status.textContent = error.message || "Datos incorrectos. Intenta nuevamente.";
      if (button) button.disabled = false;
    }
  });
};

// Función para procesar respuesta de MSAL
const handleMsalLoginResponse = (response) => {
  if (response && response.account) {
    const msalSessionData = {
      token: response.accessToken || response.idToken || "msal_token",
      displayName: response.account.name || response.account.username,
      email: response.account.username,
      cliente: {
        nombres: response.account.name || response.account.username,
        email: response.account.username,
        id: response.account.homeAccountId,
        rol: "USER"
      }
    };
    setMsalSession(msalSessionData);
    updateAuthControls();
  }
};

document.addEventListener("DOMContentLoaded", () => {
  setupInputConstraints();
  updateAuthControls();
  setupLogout();
  setupRegisterForm();
  setupLoginForm();
  validateSession();
});

export { 
  clearSession, 
  getActiveClientId, 
  getSession, 
  getMsalSession,
  handleMsalLoginResponse,
  isAdminSession, 
  setMsalSession,
  updateAuthControls, 
  validateSession 
};
