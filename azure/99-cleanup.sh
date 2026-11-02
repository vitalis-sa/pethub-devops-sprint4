#!/usr/bin/env bash
# =====================================================================
# LIMPEZA - apaga TODO o Resource Group da Sprint 4.
#
# Rode só DEPOIS do feedback dos professores (11/11/2026): a aplicação
# precisa estar no ar durante a correção. A operação é IRREVERSÍVEL e
# leva junto o App Service Plan e o Web App.
# =====================================================================
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"
source ./00-vars.sh

echo
echo "Isto vai APAGAR o Resource Group ${RESOURCE_GROUP} e tudo dentro dele:"
az resource list \
  --resource-group "${RESOURCE_GROUP}" \
  --query "[].{Nome:name, Tipo:type}" \
  --output table

echo
read -r -p "Digite o nome do Resource Group para confirmar: " CONFIRMACAO

if [[ "${CONFIRMACAO}" != "${RESOURCE_GROUP}" ]]; then
  echo "Nome não confere. Nada foi apagado."
  exit 1
fi

echo
echo "Apagando ${RESOURCE_GROUP} (roda em segundo plano na Azure)..."
az group delete --name "${RESOURCE_GROUP}" --yes --no-wait

echo
echo "Solicitação enviada. Acompanhe com:"
echo "  az group show --name ${RESOURCE_GROUP} --query properties.provisioningState -o tsv"
