#!/usr/bin/env sh
set -eu

export SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:3308/sistema_ventas?createDatabaseIfNotExist=true"
export SPRING_DATASOURCE_USERNAME="root"
export SPRING_DATASOURCE_PASSWORD="root"

export SERVER_ADDRESS="${SERVER_ADDRESS:-0.0.0.0}"
export PEDIDOS360_REPORTES_INTERNAL_KEY="pedidos360-internal"

exec sh ./mvnw spring-boot:run
