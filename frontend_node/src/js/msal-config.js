(function () {
  // Configuración MSAL (usa tus IDs)
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

  function initMsal() {
    if (!window.msal || !window.msal.PublicClientApplication) {
      console.error('[MSAL] msal-browser no está disponible. Revisa que la CDN se cargue correctamente.');
      return;
    }

    // Crear instancia y exponer en window
    try {
      window.msalInstance = new window.msal.PublicClientApplication(msalConfig);
    } catch (e) {
      console.error('[MSAL] error al crear PublicClientApplication', e);
      return;
    }

    window.signIn = async function signIn() {
      try {
        const loginResponse = await window.msalInstance.loginPopup(loginRequest);
        const tokenResponse = await window.msalInstance.acquireTokenSilent({
          account: loginResponse.account,
          scopes: loginRequest.scopes
        });
        sessionStorage.setItem('access_token', tokenResponse.accessToken);
        return tokenResponse.accessToken;
      } catch (err) {
        // fallback al popup si silent falla o si loginPopup devolvió account diferente
        try {
          const tokenResponse = await window.msalInstance.acquireTokenPopup(loginRequest);
          sessionStorage.setItem('access_token', tokenResponse.accessToken);
          return tokenResponse.accessToken;
        } catch (err2) {
          console.error('[MSAL] error acquiring token', err2);
          throw err2;
        }
      }
    };

    window.getAccessToken = async function getAccessToken() {
      const accounts = window.msalInstance.getAllAccounts();
      if (!accounts || accounts.length === 0) return null;
      try {
        const silentResponse = await window.msalInstance.acquireTokenSilent({
          account: accounts[0],
          scopes: loginRequest.scopes
        });
        sessionStorage.setItem('access_token', silentResponse.accessToken);
        return silentResponse.accessToken;
      } catch (e) {
        const popupResp = await window.msalInstance.acquireTokenPopup(loginRequest);
        sessionStorage.setItem('access_token', popupResp.accessToken);
        return popupResp.accessToken;
      }
    };

    window.callApi = async function callApi(path, options = {}) {
      const token = await window.getAccessToken();
      if (!token) throw new Error('No token available; user not signed in.');
      const headers = Object.assign({
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json'
      }, options.headers || {});
      const resp = await fetch(path, Object.assign({}, options, { headers }));
      if (!resp.ok) {
        const text = await resp.text();
        throw new Error(`API error ${resp.status}: ${text}`);
      }
      return resp.json();
    };

    window.signOut = function signOut() {
      const accounts = window.msalInstance.getAllAccounts();
      if (accounts && accounts.length) {
        window.msalInstance.logoutPopup({ account: accounts[0] }).catch(() => window.msalInstance.logout());
      }
      sessionStorage.removeItem('access_token');
    };

    console.info('[MSAL] inicializado correctamente.');
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initMsal);
  } else {
    // ya listo
    setTimeout(initMsal, 0);
  }
})();
