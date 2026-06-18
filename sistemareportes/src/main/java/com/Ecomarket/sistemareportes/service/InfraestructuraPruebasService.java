package com.Ecomarket.sistemareportes.service;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class InfraestructuraPruebasService {

    private static final int TIMEOUT_SECONDS = 5;

    @Value("${ecomarket.pruebas.mysql.host:mysql}")
    private String mysqlHost;

    @Value("${ecomarket.pruebas.mysql.port:3306}")
    private int mysqlPort;

    @Value("${ecomarket.pruebas.api-url:http://db:8080/api}")
    private String apiBaseUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
        .build();

    public String probarConexionMysql() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(mysqlHost, mysqlPort), TIMEOUT_SECONDS * 1000);
            return "Conexion TCP exitosa con MySQL en " + mysqlHost + ":" + mysqlPort + ".";
        } catch (IOException ex) {
            throw new IllegalStateException(
                "No se pudo conectar a MySQL en " + mysqlHost + ":" + mysqlPort,
                ex
            );
        }
    }

    public String probarApiRest() {
        HttpResponse<String> response = enviarGet(apiBaseUrl + "/productos");

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("La API REST respondio con estado HTTP " + response.statusCode());
        }

        String body = response.body() == null ? "" : response.body().trim();
        if (!body.startsWith("[")) {
            throw new IllegalStateException("La API REST no devolvio una lista JSON de productos.");
        }

        return "API REST disponible en " + apiBaseUrl + " con estado HTTP " + response.statusCode() + ".";
    }

    private HttpResponse<String> enviarGet(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .GET()
                .build();

            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo conectar con " + url, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("La prueba fue interrumpida al conectar con " + url, ex);
        }
    }
}
