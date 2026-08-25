// frontend_node/src/js/msal-config.js
// Requiere: incluir msal-browser (CDN o npm). Aquí usamos el clientId y tenant que pasaste.
const msalConfig = {
  auth: {
    clientId: "64533da2-6952-4e6c-8afa-c4eafddd0060",
    authority: "https://login.microsoftonline.com/d00e23b8-6f53-4f67-8a7b-b1f47ab3a272",
    redirectUri: window.location.origin
  },
  cache: {
    cacheLocation: "sessionStorage",
    storeAuthStateInCookie: false
  }
};

const loginRequest = {
  scopes: ["openid", "profile", "api://64533da2-6952-4e6c-8afa-c4eafddd0060/access_as_user"]
};

const msalInstance = new msal.PublicClientApplication(msalConfig);

async function signIn() {
  try {
    const loginResponse = await msalInstance.loginPopup(loginRequest);
    // intentamos silent token
    const tokenResponse = await msalInstance.acquireTokenSilent({
      account: loginResponse.account,
      scopes: loginRequest.scopes
    });
    sessionStorage.setItem("access_token", tokenResponse.accessToken);
    return tokenResponse.accessToken;
  } catch (err) {
    // fallback al popup si silent falla
    const tokenResponse = await msalInstance.acquireTokenPopup(loginRequest);
    sessionStorage.setItem("access_token", tokenResponse.accessToken);
    return tokenResponse.accessToken;
  }
}

async function getAccessToken() {
  const accounts = msalInstance.getAllAccounts();
  if (accounts.length === 0) return null;
  try {
    const silentResponse = await msalInstance.acquireTokenSilent({
      account: accounts[0],
      scopes: loginRequest.scopes
    });
    sessionStorage.setItem("access_token", silentResponse.accessToken);
    return silentResponse.accessToken;
  } catch (e) {
    // fallback interactivo
    const popupResp = await msalInstance.acquireTokenPopup(loginRequest);
    sessionStorage.setItem("access_token", popupResp.accessToken);
    return popupResp.accessToken;
  }
}

async function callApi(path, options = {}) {
  const token = await getAccessToken();
  if (!token) throw new Error("No token available; user not signed in.");
  const headers = Object.assign({
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json"
  }, options.headers || {});
  const resp = await fetch(path, Object.assign({}, options, { headers }));
  if (!resp.ok) {
    const text = await resp.text();
    throw new Error(`API error ${resp.status}: ${text}`);
  }
  return resp.json();
}

function signOut() {
  const accounts = msalInstance.getAllAccounts();
  if (accounts.length) {
    msalInstance.logoutPopup({ account: accounts[0] }).catch(() => msalInstance.logout());
    sessionStorage.removeItem("access_token");
  }
}
