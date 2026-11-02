#!/usr/bin/env bash
# =====================================================================
# ETAPA 1 - Cria os recursos do CD na Azure (rodar uma vez só)
#
#   Resource Group  ->  App Service Plan (Linux B1)  ->  Web App Java 21 SE
#
# Depois disto, quem publica a aplicação é a Release do Azure DevOps.
# Requer Azure CLI com "az login" na assinatura do grupo.
# Os recursos são PAGOS (B1): apague com 99-cleanup.sh após a correção.
# =====================================================================
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"
source ./00-vars.sh

echo
echo "[1/5] Resource Group ${RESOURCE_GROUP} (${LOCATION})..."
az group create --name "${RESOURCE_GROUP}" --location "${LOCATION}" -o none

echo "[2/5] App Service Plan ${APP_SERVICE_PLAN} (Linux ${APP_SERVICE_SKU})..."
az appservice plan create \
  --name "${APP_SERVICE_PLAN}" \
  --resource-group "${RESOURCE_GROUP}" \
  --location "${LOCATION}" \
  --is-linux \
  --sku "${APP_SERVICE_SKU}" \
  -o none

echo "[3/5] Web App ${WEBAPP} (${WEBAPP_RUNTIME})..."
az webapp create \
  --name "${WEBAPP}" \
  --resource-group "${RESOURCE_GROUP}" \
  --plan "${APP_SERVICE_PLAN}" \
  --runtime "${WEBAPP_RUNTIME}" \
  -o none

echo "[4/5] Configuração geral (Always On, só HTTPS, FTP desligado, logs)..."
az webapp config set \
  --name "${WEBAPP}" \
  --resource-group "${RESOURCE_GROUP}" \
  --always-on true \
  --ftps-state Disabled \
  -o none
az webapp update \
  --name "${WEBAPP}" \
  --resource-group "${RESOURCE_GROUP}" \
  --https-only true \
  -o none
az webapp log config \
  --name "${WEBAPP}" \
  --resource-group "${RESOURCE_GROUP}" \
  --application-logging filesystem \
  --docker-container-logging filesystem \
  --level information \
  -o none

# Só valores não sensíveis. Os segredos (DB_USER, DB_PASSWORD, JWT_SECRET)
# chegam pela Release, a partir do variable group protegido.
echo "[5/5] Configurações não sensíveis da aplicação..."
az webapp config appsettings set \
  --name "${WEBAPP}" \
  --resource-group "${RESOURCE_GROUP}" \
  --settings SERVER_PORT=80 ARMAZENAMENTO_MODO=local \
  -o none

HOST=$(az webapp show --name "${WEBAPP}" --resource-group "${RESOURCE_GROUP}" --query defaultHostName -o tsv)
echo
echo "Pronto. Web App: https://${HOST}"
echo "Swagger (depois do primeiro deploy): https://${HOST}/swagger-ui.html"
