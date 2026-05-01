package com.ecomarket.frontend.controller;

import com.ecomarket.frontend.dto.ClienteDTO;
import com.ecomarket.frontend.dto.ProductoDTO;
import com.ecomarket.frontend.dto.VentaDTO;
import com.ecomarket.frontend.dto.BodegaDTO;
import com.ecomarket.frontend.dto.CategoriaDTO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;

@Controller
public class WebController {

    @Autowired
    private RestTemplate restTemplate;

    // Vista de Productos
    @GetMapping("/productos")
    public String verProductos(Model model) {
        ProductoDTO[] productos = restTemplate.getForObject("http://localhost:8080/api/productos", ProductoDTO[].class);
        model.addAttribute("lista", Arrays.asList(productos));
        model.addAttribute("view", "productos"); // Indicador de vista activa
        return "dashboard"; 
    }

    // Vista de Ventas
    @GetMapping("/ventas-completas")
    public String verVentas(Model model) {
        VentaDTO[] ventas = restTemplate.getForObject("http://localhost:8080/api/ventas-completas", VentaDTO[].class);
        model.addAttribute("lista", Arrays.asList(ventas));
        model.addAttribute("view", "ventas"); // Indicador de vista activa
        return "dashboard";
    }

    @GetMapping("/clientes")
    public String verClientes(Model model) {
        try {
            ClienteDTO[] clientes = restTemplate.getForObject("http://localhost:8080/api/clientes", ClienteDTO[].class);
            model.addAttribute("lista", clientes != null ? Arrays.asList(clientes) : new ArrayList<>());
        } catch (Exception e) {
            model.addAttribute("lista", new ArrayList<>()); // Evita el error si el backend falla
            model.addAttribute("error", "No se pudieron cargar los clientes");
        }
        model.addAttribute("view", "clientes");
        return "dashboard";
    }

    @GetMapping("/bodega")
    public String verBodega(Model model) {
        BodegaDTO[] bodega = restTemplate.getForObject("http://localhost:8080/api/bodega", BodegaDTO[].class);
        model.addAttribute("lista", Arrays.asList(bodega));
        model.addAttribute("view", "bodega");
        return "dashboard";
    }

    @GetMapping("/categorias")
    public String verCategorias(Model model) {
        CategoriaDTO[] categorias = restTemplate.getForObject("http://localhost:8080/api/categorias", CategoriaDTO[].class);
        model.addAttribute("lista", Arrays.asList(categorias));
        model.addAttribute("view", "categorias");
        return "dashboard";
    }
}