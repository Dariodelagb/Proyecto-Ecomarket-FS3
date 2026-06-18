package com.Ecomarket.sistemareportes.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class CoberturaCodigoService {

    private static final Path DEFAULT_JACOCO_CSV = Path.of("target", "site", "jacoco", "jacoco.csv");
    private static final DecimalFormat PORCENTAJE = new DecimalFormat(
        "0.00",
        DecimalFormatSymbols.getInstance(Locale.US)
    );

    private final Path jacocoCsv;

    public CoberturaCodigoService() {
        this(DEFAULT_JACOCO_CSV);
    }

    CoberturaCodigoService(Path jacocoCsv) {
        this.jacocoCsv = jacocoCsv;
    }

    public String generarResumenCobertura() {
        if (!Files.exists(jacocoCsv)) {
            return "Cobertura no disponible. Ejecuta './mvnw test' o reconstruye el contenedor para generar target/site/jacoco/jacoco.csv.";
        }

        try {
            TotalesCobertura totales = leerTotales();

            if (totales.instruccionesTotal() == 0 && totales.lineasTotal() == 0 && totales.ramasTotal() == 0) {
                return "Cobertura no disponible. El reporte JaCoCo no contiene metricas.";
            }

            return "Instrucciones=" + porcentaje(totales.instruccionesCubiertas(), totales.instruccionesTotal())
                + ", Lineas=" + porcentaje(totales.lineasCubiertas(), totales.lineasTotal())
                + ", Ramas=" + porcentaje(totales.ramasCubiertas(), totales.ramasTotal())
                + ", Metodos=" + porcentaje(totales.metodosCubiertos(), totales.metodosTotal())
                + ", Clases=" + porcentaje(totales.clasesCubiertas(), totales.clasesTotal()) + ".";
        } catch (IOException | NumberFormatException ex) {
            throw new IllegalStateException("No se pudo leer el reporte de cobertura JaCoCo.", ex);
        }
    }

    private TotalesCobertura leerTotales() throws IOException {
        TotalesCobertura totales = new TotalesCobertura();

        for (String line : Files.readAllLines(jacocoCsv)) {
            if (line.startsWith("GROUP,")) {
                continue;
            }

            String[] columns = line.split(",", -1);
            if (columns.length < 13) {
                continue;
            }

            totales.instructionMissed += entero(columns[3]);
            totales.instructionCovered += entero(columns[4]);
            totales.branchMissed += entero(columns[5]);
            totales.branchCovered += entero(columns[6]);
            totales.lineMissed += entero(columns[7]);
            totales.lineCovered += entero(columns[8]);
            totales.methodMissed += entero(columns[11]);
            totales.methodCovered += entero(columns[12]);

            int instruccionesClase = entero(columns[3]) + entero(columns[4]);
            if (instruccionesClase > 0) {
                if (entero(columns[4]) > 0) {
                    totales.classCovered++;
                } else {
                    totales.classMissed++;
                }
            }
        }

        return totales;
    }

    private int entero(String value) {
        return Integer.parseInt(value.trim());
    }

    private String porcentaje(int cubiertas, int total) {
        if (total == 0) {
            return "N/A";
        }

        return PORCENTAJE.format((cubiertas * 100.0) / total) + "%";
    }

    private static class TotalesCobertura {
        private int instructionMissed;
        private int instructionCovered;
        private int branchMissed;
        private int branchCovered;
        private int lineMissed;
        private int lineCovered;
        private int methodMissed;
        private int methodCovered;
        private int classMissed;
        private int classCovered;

        private int instruccionesCubiertas() {
            return instructionCovered;
        }

        private int instruccionesTotal() {
            return instructionMissed + instructionCovered;
        }

        private int ramasCubiertas() {
            return branchCovered;
        }

        private int ramasTotal() {
            return branchMissed + branchCovered;
        }

        private int lineasCubiertas() {
            return lineCovered;
        }

        private int lineasTotal() {
            return lineMissed + lineCovered;
        }

        private int metodosCubiertos() {
            return methodCovered;
        }

        private int metodosTotal() {
            return methodMissed + methodCovered;
        }

        private int clasesCubiertas() {
            return classCovered;
        }

        private int clasesTotal() {
            return classMissed + classCovered;
        }
    }
}
