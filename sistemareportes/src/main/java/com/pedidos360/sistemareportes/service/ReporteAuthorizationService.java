package com.pedidos360.sistemareportes.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.pedidos360.sistemareportes.client.AuthClient;
import com.pedidos360.sistemareportes.dto.SesionClienteDTO;

@Service
public class ReporteAuthorizationService {
    private final AuthClient authClient;

    public ReporteAuthorizationService(AuthClient authClient) {
        this.authClient = authClient;
    }

    public void exigirAdministrador(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Se requiere una sesion de administrador");
        }

        try {
            SesionClienteDTO sesion = authClient.obtenerSesion(token);
            if (sesion.getCliente() == null || !"ADMIN".equalsIgnoreCase(sesion.getCliente().getRol())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo un administrador puede generar reportes");
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesion invalida", ex);
        }
    }
}
