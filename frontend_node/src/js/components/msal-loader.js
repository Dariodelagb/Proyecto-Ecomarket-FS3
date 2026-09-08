import { setMsalSession, handleMsalLoginResponse, updateAuthControls } from './auth-loader.js';

const MSAL_SESSION_KEY = 'msalSession';

/**
 * Verifica si hay un usuario logueado en MSAL
 * y sincroniza la sesión con localStorage
 */
const checkMsalSession = async () => {
  try {
    // Verificar si msalInstance está disponible en window
    if (!window.msalInstance) {
      console.log('MSAL no inicializado aún');
      return null;
    }

    // Obtener todas las cuentas activas
    const accounts = window.msalInstance.getAllAccounts();
    
    if (accounts && accounts.length > 0) {
      // Usuario logueado en MSAL
      const account = accounts[0];
      console.log('Usuario MSAL detectado:', account);
      
      // Intentar obtener token silenciosamente
      try {
        const tokenRequest = {
          scopes: ['user.read'],
          account: account
        };
        
        const response = await window.msalInstance.acquireTokenSilent(tokenRequest);
        
        // Guardar sesión MSAL en localStorage
        const msalSessionData = {
          token: response.accessToken,
          displayName: account.name || account.username,
          email: account.username,
          id: account.localAccountId,
          homeAccountId: account.homeAccountId,
          cliente: {
            nombres: account.name || account.username,
            email: account.username,
            id: account.localAccountId,
            rol: 'USER'
          }
        };
        
        setMsalSession(msalSessionData);
        handleMsalLoginResponse({ account, accessToken: response.accessToken });
        return msalSessionData;
      } catch (error) {
        console.log('Error obteniendo token silenciosamente:', error);
        // Si falla el token silencioso, al menos guardar la sesión básica
        const msalSessionData = {
          displayName: account.name || account.username,
          email: account.username,
          id: account.localAccountId,
          homeAccountId: account.homeAccountId,
          cliente: {
            nombres: account.name || account.username,
            email: account.username,
            id: account.localAccountId,
            rol: 'USER'
          }
        };
        
        localStorage.setItem(MSAL_SESSION_KEY, JSON.stringify(msalSessionData));
        updateAuthControls();
        return msalSessionData;
      }
    } else {
      console.log('No hay usuario logueado en MSAL');
      return null;
    }
  } catch (error) {
    console.error('Error verificando sesión MSAL:', error);
    return null;
  }
};

/**
 * Maneja el evento de logout de MSAL
 */
const handleMsalLogout = () => {
  localStorage.removeItem(MSAL_SESSION_KEY);
  updateAuthControls();
};

/**
 * Inicializa el listener para cambios en la autenticación
 */
const initMsalListeners = () => {
  if (!window.msalInstance) {
    console.log('MSAL no disponible para listeners');
    return;
  }

  // Listener para cuando se completa el login
  window.msalInstance.addEventCallback((message) => {
    console.log('MSAL Event:', message.eventType);
    
    if (message.eventType === 'msal:loginSuccess' || message.eventType === 'msal:acquireTokenSuccess') {
      console.log('Login exitoso detectado');
      checkMsalSession();
      window.location.href = 'index.html';
    } else if (message.eventType === 'msal:logoutSuccess') {
      console.log('Logout detectado');
      handleMsalLogout();
    }
  });
};

/**
 * Verifica periódicamente el estado de la sesión MSAL
 */
const startMsalPolling = () => {
  // Verificar cada 10 segundos
  setInterval(() => {
    if (window.msalInstance) {
      checkMsalSession();
    }
  }, 10000);
};

/**
 * Inicializa el cargador de MSAL
 */
const initMsalLoader = async () => {
  // Esperar a que MSAL esté listo
  let attempts = 0;
  const maxAttempts = 50; // 5 segundos máximo
  
  const waitForMsal = async () => {
    if (window.msalInstance) {
      console.log('MSAL está listo');
      await checkMsalSession();
      initMsalListeners();
      startMsalPolling();
      return true;
    }
    
    if (attempts < maxAttempts) {
      attempts++;
      await new Promise(resolve => setTimeout(resolve, 100));
      return waitForMsal();
    }
    
    console.log('MSAL no se inicializó a tiempo');
    return false;
  };
  
  await waitForMsal();
};

// Inicializar cuando el DOM esté listo
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', initMsalLoader);
} else {
  initMsalLoader();
}

export { checkMsalSession, handleMsalLogout, initMsalLoader };
