import {
  isMicrosoftAuthenticationPopup,
  restoreMicrosoftSession,
  signInWithMicrosoft,
  signOutFromMicrosoft,
} from "../msal-config";

const SESSION_KEY = "pedidos360Session";
const MSAL_SESSION_KEY = "msalSession";

const readJson = (storage, key) => {
  try {
    return JSON.parse(storage.getItem(key));
  } catch {
    return null;
  }
};

const getSession = () => readJson(localStorage, SESSION_KEY);

const setSession = (session) => {
  localStorage.setItem(SESSION_KEY, JSON.stringify(session));
};

const clearSession = () => {
  localStorage.removeItem(SESSION_KEY);
};

const getMsalSession = () => readJson(localStorage, MSAL_SESSION_KEY);

const setMsalSession = (session) => {
  if (session) {
    localStorage.setItem(MSAL_SESSION_KEY, JSON.stringify(session));
  } else {
    localStorage.removeItem(MSAL_SESSION_KEY);
  }

  // Limpia los datos temporales usados por las versiones anteriores.
  sessionStorage.removeItem(MSAL_SESSION_KEY);
  sessionStorage.removeItem("access_token");
};

const getActiveSession = () => getSession() || getMsalSession();

const getActiveProfile = () => {
  const session = getActiveSession();
  const cliente = session?.cliente || {};
  const givenName = session?.givenName || cliente.nombres || "";
  const familyName = session?.familyName || cliente.apellidos || "";
  const displayName =
    session?.displayName ||
    [givenName, familyName].filter(Boolean).join(" ") ||
    session?.email ||
    cliente.email ||
    "";

  return {
    displayName,
    givenName,
    familyName,
    email: session?.email || cliente.email || "",
    rut: cliente.rut || "",
    dvrut: cliente.dvrut || "",
  };
};

const getActiveClientId = () => getSession()?.cliente?.id || null;

const isAdminSession = () => getSession()?.cliente?.rol === "ADMIN";
const isOperatorSession = () => getSession()?.cliente?.rol === "OPERADOR";
const isClientSession = () => getSession()?.cliente?.rol === "CLIENTE";

const getRoleRedirect = (role) => {
  if (role === "ADMIN") return "dashboard.html";
  if (role === "OPERADOR") return "operador.html";
  return "mis-pedidos.html";
};

const enforceRequiredRole = () => {
  const requiredRole = document.body?.dataset.requiredRole;
  if (!requiredRole) return true;

  const role = getSession()?.cliente?.rol;
  const allowedRoles = requiredRole.split(",").map((item) => item.trim());
  if (!role || !allowedRoles.includes(role)) {
    window.location.replace(role ? getRoleRedirect(role) : "login.html");
    return false;
  }
  return true;
};

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

const setText = (selector, value, fallback = "") => {
  document.querySelectorAll(selector).forEach((element) => {
    element.textContent = value || fallback;
  });
};

const updateAuthControls = () => {
  const hasSession = Boolean(getSession()?.token || getMsalSession()?.authenticated);
  const hasAdminSession = hasSession && isAdminSession();
  const hasOperatorSession = hasSession && isOperatorSession();
  const hasClientSession = hasSession && isClientSession();
  const profile = getActiveProfile();

  setText("[data-auth-user-name], #user-name", profile.displayName, "Usuario");
  setText("[data-auth-given-name]", profile.givenName, "Usuario");
  setText("[data-auth-family-name]", profile.familyName);
  setText("[data-auth-user-email]", profile.email);

  document.querySelectorAll(".auth-guest-only").forEach((element) => {
    element.hidden = hasSession;
  });

  document.querySelectorAll(".auth-session-only").forEach((element) => {
    element.hidden = !hasSession;
  });

  document.querySelectorAll(".auth-admin-only").forEach((element) => {
    element.hidden = !hasAdminSession;
  });

  document.querySelectorAll(".auth-operator-only").forEach((element) => {
    element.hidden = !hasOperatorSession;
  });

  document.querySelectorAll(".auth-client-only").forEach((element) => {
    element.hidden = !hasClientSession;
  });
};

const fillEmptyField = (selector, value) => {
  if (!value) return;

  document.querySelectorAll(selector).forEach((input) => {
    if (!input.value.trim()) {
      input.value = value;
      input.dispatchEvent(new Event("input", { bubbles: true }));
    }
  });
};

const autofillUserFields = () => {
  const profile = getActiveProfile();
  if (!profile.displayName && !profile.email) return;

  fillEmptyField('[data-auth-field="given-name"]', profile.givenName);
  fillEmptyField('[data-auth-field="family-name"]', profile.familyName);
  fillEmptyField('[data-auth-field="full-name"]', profile.displayName);
  fillEmptyField('[data-auth-field="email"]', profile.email);
  fillEmptyField('[data-auth-field="rut"]', profile.rut);
  fillEmptyField('[data-auth-field="rut-dv"]', profile.dvrut);
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
    autofillUserFields();
    return freshSession;
  } catch (error) {
    console.error("Error validando sesion:", error);
    return session;
  }
};

const setupLogout = () => {
  document.querySelectorAll("[data-logout]").forEach((button) => {
    button.addEventListener("click", async (event) => {
      event.preventDefault();

      const session = getSession();
      const originalText = button.textContent;
      button.disabled = true;
      button.textContent = "Cerrando sesion...";

      if (session?.token) {
        try {
          await fetch(`/api/auth/sesion/${encodeURIComponent(session.token)}`, {
            method: "DELETE",
          });
        } catch (error) {
          console.error("Error cerrando la sesion local:", error);
        }
      }

      clearSession();
      setMsalSession(null);
      updateAuthControls();

      try {
        // Tambien consulta la cache real de MSAL para cerrar cuentas que aun
        // no alcanzaron a sincronizarse con el estado visual.
        await signOutFromMicrosoft();
      } catch (error) {
        console.error("Error cerrando la sesion de Microsoft:", error);
      }

      button.textContent = originalText;
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
    const responseText = await response.text();
    if (!responseText) return fallback;

    try {
      const data = JSON.parse(responseText);
      return data.message || data.detail || data.error || fallback;
    } catch {
      return responseText;
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
      const redirectUrl = getRoleRedirect(session?.cliente?.rol);
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
      const redirectUrl = getRoleRedirect(session?.cliente?.rol);
      window.location.href = redirectUrl;
    } catch (error) {
      console.error("Error iniciando sesion:", error);
      if (status) status.textContent = error.message || "Datos incorrectos. Intenta nuevamente.";
      if (button) button.disabled = false;
    }
  });
};

const setupMicrosoftLogin = () => {
  const button = document.querySelector("[data-msal-signin]");
  if (!button) return;

  const status = document.getElementById("login-status");

  button.addEventListener("click", async () => {
    const form = document.getElementById("login-form");
    const submitButton = form?.querySelector('button[type="submit"]');
    button.disabled = true;
    if (submitButton) submitButton.disabled = true;
    if (status) status.textContent = "Conectando con Microsoft...";

    try {
      const microsoftSession = await signInWithMicrosoft();
      if (!microsoftSession) return;

      clearSession();
      setMsalSession(microsoftSession);
      updateAuthControls();
      autofillUserFields();
      window.location.href = "index.html";
    } catch (error) {
      console.error("Error iniciando sesion con Microsoft:", error);
      if (status) {
        status.textContent = "No se pudo iniciar sesion con Microsoft. Intenta nuevamente.";
      }
      button.disabled = false;
      if (submitButton) submitButton.disabled = false;
    }
  });
};

const initializeAuth = async () => {
  setupInputConstraints();
  setupLogout();
  setupRegisterForm();
  setupLoginForm();
  setupMicrosoftLogin();

  // Muestra de inmediato cualquier sesion ya sincronizada y luego confirma
  // la cuenta real guardada por MSAL.
  updateAuthControls();
  autofillUserFields();

  const localSessionPromise = validateSession();
  const microsoftSessionPromise = restoreMicrosoftSession().catch((error) => {
    console.error("Error restaurando la sesion de Microsoft:", error);
    return null;
  });
  const [, microsoftSession] = await Promise.all([
    localSessionPromise,
    microsoftSessionPromise,
  ]);
  setMsalSession(microsoftSession);

  updateAuthControls();
  autofillUserFields();

  enforceRequiredRole();
};

// La pagina configurada como redirectUri tambien se carga dentro del popup.
// No debe procesar la respuesta: la instancia MSAL de la ventana principal
// observa esa URL, obtiene el resultado y cierra el popup automaticamente.
if (!isMicrosoftAuthenticationPopup()) {
  enforceRequiredRole();
  document.addEventListener("DOMContentLoaded", initializeAuth);
}

export {
  autofillUserFields,
  clearSession,
  getActiveClientId,
  getActiveProfile,
  getMsalSession,
  getSession,
  isAdminSession,
  isOperatorSession,
  isClientSession,
  setMsalSession,
  updateAuthControls,
  validateSession,
};
