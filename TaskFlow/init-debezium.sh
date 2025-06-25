#!/bin/bash

echo "⏳ Ожидание запуска Debezium Connect..."
sleep 30

echo "📡 Создание Debezium коннектора для бизнес-таблиц (tasks, comments, projects, users)..."
curl -H "Content-Type: application/json" -X POST -d @debezium-postgres-connector.json http://localhost:8083/connectors

echo "✅ Debezium коннектор taskflow-postgres-connector успешно создан!"

echo "🔍 Проверяем статус коннектора..."
sleep 5
curl http://localhost:8083/connectors/taskflow-postgres-connector/status

echo "🎉 Инициализация завершена! CDC события будут отправляться в топики: tasks, comments, projects, users"