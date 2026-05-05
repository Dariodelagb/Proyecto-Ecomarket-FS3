package com.Ecomarket.sistemareportes.service;

import com.Ecomarket.sistemareportes.client.VentasClient;
import com.Ecomarket.sistemareportes.client.InventarioClient;
import com.Ecomarket.sistemareportes.dto.VentaDTO;
import com.Ecomarket.sistemareportes.dto.ProductoDTO;
import com.Ecomarket.sistemareportes.dto.BodegaDTO;
import com.Ecomarket.sistemareportes.dto.ResumenDTO;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@Service
public class ReporteService {

    @Autowired
    private VentasClient ventasClient;

    @Autowired
    private InventarioClient inventarioClient;

    public void procesarReporte() {
        // Reporte de ventas
        List<VentaDTO> listaVentas = ventasClient.obtenerTodasLasVentas();
        System.out.println("=== REPORTE DE VENTAS ===");
        System.out.println("Total de ventas registradas: " + listaVentas.size());
        for (VentaDTO venta : listaVentas) {
            System.out.println("Venta ID: " + venta.getId() + ", Monto: " + venta.getMonto() + ", Fecha: " + venta.getFecha());
        }

        // Reporte de productos y stock
        List<ProductoDTO> listaProductos = inventarioClient.obtenerTodosLosProductos();
        List<BodegaDTO> listaBodega = inventarioClient.obtenerTodoElStock();
        System.out.println("\n=== REPORTE DE PRODUCTOS Y STOCK ===");
        for (ProductoDTO producto : listaProductos) {
            // Encontrar el stock correspondiente
            Integer stock = listaBodega.stream()
                .filter(b -> b.getProducto() != null && b.getProducto().getId().equals(producto.getId()))
                .map(BodegaDTO::getStock)
                .findFirst()
                .orElse(0);
            System.out.println("Producto: " + producto.getNombre() + ", Precio: " + producto.getPrecio() + ", Stock: " + stock);
        }

        // Reporte de dinero total ingresado
        double totalDinero = listaVentas.stream()
            .mapToDouble(VentaDTO::getMonto)
            .sum();
        System.out.println("\n=== REPORTE DE INGRESOS TOTALES ===");
        System.out.println("Total de dinero ingresado: $" + totalDinero);
    }

    public ResumenDTO obtenerResumen() {
        List<VentaDTO> listaVentas = ventasClient.obtenerTodasLasVentas();
        List<ProductoDTO> listaProductos = inventarioClient.obtenerTodosLosProductos();

        double totalDinero = listaVentas.stream()
            .mapToDouble(VentaDTO::getMonto)
            .sum();

        int totalVentas = listaVentas.size();
        int totalProductos = listaProductos.size();

        ResumenDTO resumen = new ResumenDTO();
        resumen.setTotalDinero(totalDinero);
        resumen.setTotalVentas(totalVentas);
        resumen.setTotalProductos(totalProductos);

        return resumen;
    }
}