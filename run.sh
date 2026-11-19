#!/usr/bin/env bash
set -e

echo "=============================================================="
echo "  Plantao Clima - Compilando e Iniciando (Java + JDBC + H2)"
echo "=============================================================="

mkdir -p build/classes
find src/main/java -name "*.java" > build/sources.txt
javac -encoding UTF-8 -cp "lib/h2-2.3.232.jar" -d build/classes @build/sources.txt

echo "Compilacao concluida! Iniciando servidor em http://localhost:8080 ..."
java -cp "build/classes:lib/h2-2.3.232.jar" br.com.plantaoclima.Main "$@"
