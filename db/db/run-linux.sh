#!/usr/bin/env sh
set -eu

: "${SPRING_DATASOURCE_URL:?Define SPRING_DATASOURCE_URL antes de iniciar el servicio}"
: "${SPRING_DATASOURCE_USERNAME:?Define SPRING_DATASOURCE_USERNAME antes de iniciar el servicio}"
: "${SPRING_DATASOURCE_PASSWORD:?Define SPRING_DATASOURCE_PASSWORD antes de iniciar el servicio}"

export SERVER_ADDRESS="${SERVER_ADDRESS:-0.0.0.0}"
export PEDIDOS360_REPORTES_INTERNAL_KEY="${PEDIDOS360_REPORTES_INTERNAL_KEY:-pedidos360-internal}"

exec sh ./mvnw spring-boot:run
