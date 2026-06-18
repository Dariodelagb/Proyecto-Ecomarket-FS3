package com.Ecomarket.sistemareportes.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CoberturaCodigoServiceTest {

    @TempDir
    private Path tempDir;

    @Test
    void generarResumenCoberturaCalculaPorcentajesDesdeJacocoCsv() throws IOException {
        Path jacocoCsv = tempDir.resolve("jacoco.csv");
        Files.writeString(
            jacocoCsv,
            """
            GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,BRANCH_MISSED,BRANCH_COVERED,LINE_MISSED,LINE_COVERED,COMPLEXITY_MISSED,COMPLEXITY_COVERED,METHOD_MISSED,METHOD_COVERED
            sistemareportes,service,ReporteService,25,75,2,8,5,15,3,7,1,4
            """
        );

        CoberturaCodigoService service = new CoberturaCodigoService(jacocoCsv);

        String resumen = service.generarResumenCobertura();

        assertTrue(resumen.contains("Instrucciones=75.00%"));
        assertTrue(resumen.contains("Lineas=75.00%"));
        assertTrue(resumen.contains("Ramas=80.00%"));
        assertTrue(resumen.contains("Metodos=80.00%"));
        assertTrue(resumen.contains("Clases=100.00%"));
    }
}
