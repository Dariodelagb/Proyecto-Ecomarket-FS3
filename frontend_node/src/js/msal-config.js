import { PublicClientApplication } from "@azure/msal-browser";

const msalConfig = {
  auth: {
    clientId: "64533da2-6952-4e6c-8afa-c4eafddd0060",
    authority: "https://login.microsoftonline.com/d00e23b8-6f53-4f67-8a7b-b1f47ab3a272",
    redirectUri: window.location.origin,
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
    "api://64533da2-6952-4e6c-8afa-c4eafddd0060/access_as_user",
  ],
};

const msalInstance = new PublicClientApplication(msalConfig);

export async function signIn() {
  try {
    const loginResponse = await msalInstance.loginPopup(loginRequest);
    const tokenResponse = await msalInstance.acquireTokenSilent({
      account: loginResponse.account,
      scopes: loginRequest.scopes,
    });
    sessionStorage.setItem("access_token", tokenResponse.accessToken);
    return tokenResponse.accessToken;
  } catch (err) {
    // If popup is blocked or window.open failed, fallback to redirect
    const msg = (err && (err.errorCode || err.message || err.errorMessage) || "").toString().toLowerCase();
    if (
      msg.includes("popup_window_error") ||
      msg.includes("empty_window_error") ||
      msg.includes("window.open returned null") ||
      msg.includes("popup blocked")
    ) {
      try {
        // This will redirect the browser to Azure; the response must be handled after redirect
        await msalInstance.loginRedirect(loginRequest);
        return null;
      } catch (redirectErr) {
        console.error("[MSAL] loginRedirect failed", redirectErr);
        throw redirectErr;
      }
    }

    // Fallback to interactive popup acquire token (may also fail if popup blocked)
    try {
      const tokenResponse = await msalInstance.acquireTokenPopup(loginRequest);
      sessionStorage.setItem("access_token", tokenResponse.accessToken);
      return tokenResponse.accessToken;
    } catch (popupErr) {
      const popupMsg = (popupErr && (popupErr.errorCode || popupErr.message || popupErr.errorMessage) || "").toString().toLowerCase();
      if (popupMsg.includes("popup_window_error") || popupMsg.includes("empty_window_error") || popupMsg.includes("popup blocked")) {
        // Last resort: redirect to acquire token
        await msalInstance.acquireTokenRedirect(loginRequest);
        return null;
      }
      console.error("[MSAL] acquireTokenPopup failed", popupErr);
      throw popupErr;
    }
  }
}

export async function handleRedirect() {
  try {
    const result = await msalInstance.handleRedirectPromise();
    if (result && result.account) {
      try {
        const tokenResponse = await msalInstance.acquireTokenSilent({
          account: result.account,
          scopes: loginRequest.scopes,
        });
        sessionStorage.setItem("access_token", tokenResponse.accessToken);
        return tokenResponse.accessToken;
      } catch (silentErr) {
        // If silent fails after redirect, try interactive fallback (redirect)
        console.warn("[MSAL] acquireTokenSilent after redirect failed", silentErr);
        try {
          await msalInstance.acquireTokenRedirect(loginRequest);
        } catch (redirErr) {
          console.error("[MSAL] acquireTokenRedirect failed", redirErr);
        }
      }
    }
  } catch (e) {
    console.error("[MSAL] handleRedirect error", e);
  }
  return null;
}

export async function getAccessToken() {
  const accounts = msalInstance.getAllAccounts();
  if (!accounts || accounts.length === 0) return null;
  try {
    const silentResponse = await msalInstance.acquireTokenSilent({
      account: accounts[0],
      scopes: loginRequest.scopes,
    });
    sessionStorage.setItem("access_token", silentResponse.accessToken);
    return silentResponse.accessToken;
  } catch (e) {
    const msg = (e && (e.errorCode || e.message || e.errorMessage) || "").toString().toLowerCase();
    if (msg.includes("popup_window_error") || msg.includes("empty_window_error") || msg.includes("popup blocked")) {
      // fallback to redirect to acquire token
      await msalInstance.acquireTokenRedirect(loginRequest);
      return null;
    }
    try {
      const popupResp = await msalInstance.acquireTokenPopup(loginRequest);
      sessionStorage.setItem("access_token", popupResp.accessToken);
      return popupResp.accessToken;
    } catch (popupErr) {
      const popupMsg = (popupErr && (popupErr.errorCode || popupErr.message || popupErr.errorMessage) || "").toString().toLowerCase();
      if (popupMsg.includes("popup_window_error") || popupMsg.includes("empty_window_error") || popupMsg.includes("popup blocked")) {
        await msalInstance.acquireTokenRedirect(loginRequest);
        return null;
      }
      console.error("[MSAL] acquireTokenPopup failed", popupErr);
      throw popupErr;
    }
  }
}

export async function callApi(path, options = {}) {
  const token = await getAccessToken();
  if (!token) throw new Error("No token available; user not signed in.");
  const headers = Object.assign(
    {
      Authorization: `Bearer ${token}`,
      "Content-Type": "application/json",
    },
    options.headers || {}
  );
  const resp = await fetch(path, Object.assign({}, options, { headers }));
  if (!resp.ok) {
    const text = await resp.text();
    throw new Error(`API error ${resp.status}: ${text}`);
  }
  return resp.json();
}

export function signOut() {
  const accounts = msalInstance.getAllAccounts();
  if (accounts && accounts.length) {
    msalInstance.logoutPopup({ account: accounts[0] }).catch(() => msalInstance.logout());
  }
  sessionStorage.removeItem("access_token");
}

export { msalInstance };
