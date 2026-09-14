import { PublicClientApplication } from "@azure/msal-browser";

const CLIENT_ID = "64533da2-6952-4e6c-8afa-c4eafddd0060";

const msalConfig = {
  auth: {
    clientId: CLIENT_ID,
    authority: "https://login.microsoftonline.com/d00e23b8-6f53-4f67-8a7b-b1f47ab3a272",
    redirectUri: window.location.origin,
    postLogoutRedirectUri: window.location.origin,
    navigateToLoginRequestUrl: false,
  },
  cache: {
    cacheLocation: "sessionStorage",
    storeAuthStateInCookie: false,
  },
};

const loginRequest = {
  scopes: [
    "openid",
    "profile",
    "email",
    `api://${CLIENT_ID}/Access-as-user`,
  ],
};

const msalInstance = new PublicClientApplication(msalConfig);

// Se conserva en window para compatibilidad con integraciones anteriores del proyecto.
window.msalInstance = msalInstance;

const isMicrosoftAuthenticationPopup = () =>
  Boolean(
    window.opener &&
      window.opener !== window &&
      typeof window.name === "string" &&
      window.name.startsWith("msal."),
  );

const getErrorText = (error) =>
  String(error?.errorCode || error?.message || error?.errorMessage || "").toLowerCase();

const popupWasBlocked = (error) => {
  const message = getErrorText(error);
  return [
    "popup_window_error",
    "empty_window_error",
    "window.open returned null",
    "popup blocked",
  ].some((fragment) => message.includes(fragment));
};

const splitDisplayName = (displayName = "") => {
  const parts = displayName.trim().split(/\s+/).filter(Boolean);
  return {
    givenName: parts[0] || "",
    familyName: parts.slice(1).join(" "),
  };
};

const createMicrosoftSession = (account, authenticationResult = null) => {
  if (!account) return null;

  const claims = account.idTokenClaims || {};
  const displayName = claims.name || account.name || account.username || "Usuario";
  const parsedName = splitDisplayName(displayName);
  const givenName = claims.given_name || parsedName.givenName;
  const familyName = claims.family_name || parsedName.familyName;
  const email =
    claims.preferred_username ||
    claims.email ||
    account.username ||
    "";

  return {
    provider: "microsoft",
    authenticated: true,
    accountId: account.localAccountId,
    homeAccountId: account.homeAccountId,
    displayName,
    givenName,
    familyName,
    email,
    expiresAt: authenticationResult?.expiresOn?.toISOString?.() || null,
    cliente: {
      nombres: givenName || displayName,
      apellidos: familyName,
      email,
      rol: "CLIENTE",
    },
  };
};

const getCurrentAccount = () =>
  msalInstance.getActiveAccount() || msalInstance.getAllAccounts()[0] || null;

const acquireTokenForAccount = async (account) => {
  if (!account) return null;

  try {
    return await msalInstance.acquireTokenSilent({
      ...loginRequest,
      account,
    });
  } catch (error) {
    console.warn("[MSAL] No se pudo renovar el token silenciosamente.", error);
    return null;
  }
};

export async function restoreMicrosoftSession() {
  let redirectResponse = null;

  try {
    redirectResponse = await msalInstance.handleRedirectPromise();
  } catch (error) {
    console.error("[MSAL] No se pudo procesar el retorno del inicio de sesion.", error);
  }

  const account = redirectResponse?.account || getCurrentAccount();
  if (!account) return null;

  msalInstance.setActiveAccount(account);
  const tokenResponse = redirectResponse?.accessToken
    ? redirectResponse
    : await acquireTokenForAccount(account);

  return createMicrosoftSession(account, tokenResponse);
}

export async function signInWithMicrosoft() {
  try {
    const response = await msalInstance.loginPopup(loginRequest);
    msalInstance.setActiveAccount(response.account);
    return createMicrosoftSession(response.account, response);
  } catch (error) {
    if (popupWasBlocked(error)) {
      await msalInstance.loginRedirect(loginRequest);
      return null;
    }

    throw error;
  }
}

export async function getAccessToken() {
  const account = getCurrentAccount();
  const response = await acquireTokenForAccount(account);
  return response?.accessToken || null;
}

export async function signOutFromMicrosoft() {
  const account = getCurrentAccount();
  if (!account) return;

  const logoutRequest = {
    account,
    postLogoutRedirectUri: window.location.origin,
    mainWindowRedirectUri: window.location.origin,
  };

  try {
    await msalInstance.logoutPopup(logoutRequest);
  } catch (error) {
    if (popupWasBlocked(error)) {
      await msalInstance.logoutRedirect(logoutRequest);
      return;
    }

    throw error;
  }
}

export async function callApi(path, options = {}) {
  const token = await getAccessToken();
  if (!token) throw new Error("No hay una sesion de Microsoft activa.");

  const headers = {
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json",
    ...(options.headers || {}),
  };
  const response = await fetch(path, { ...options, headers });

  if (!response.ok) {
    const responseText = await response.text();
    throw new Error(`API error ${response.status}: ${responseText}`);
  }

  return response.json();
}

export {
  createMicrosoftSession,
  isMicrosoftAuthenticationPopup,
  msalInstance,
};
