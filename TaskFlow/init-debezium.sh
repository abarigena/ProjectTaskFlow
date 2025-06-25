#!/bin/bash

echo "🔄 Ожидание запуска Debezium Connect..."
sleep 30

echo "📡 Создание Debezium коннектора..."
curl -H "Content-Type: application/json" -X POST -d @debezium-postgres-connector.json http://localhost:8083/connectors

echo "✅ Debezium коннектор инициализирован!" 