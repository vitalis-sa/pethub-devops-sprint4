#!/usr/bin/env bash
# =====================================================================
# Situação do Web App: estado, URL, runtime e últimas linhas de log.
# Só leitura: não altera nenhum recurso.
# =====================================================================
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")"
source ./00-vars.sh

az webapp show \
  --name "${WEBAPP}" \
  --resource-group "${RESOURCE_GROUP}" \
  --query "{Estado:state, URL:defaultHostName, Runtime:siteConfig.linuxFxVersion, AlwaysOn:siteConfig.alwaysOn}" \
  -o table

HOST=$(az webapp show --name "${WEBAPP}" --resource-group "${RESOURCE_GROUP}" --query defaultHostName -o tsv)
echo
# Sem deploy, o Web App devolve 200 com a página padrão em qualquer caminho,
# então o teste é pelo conteúdo: só a API PetHub responde um JSON "openapi".
if curl -s --max-time 60 "https://${HOST}/api-docs" | grep -q '"openapi"'; then
  echo "API PetHub no ar: https://${HOST}/swagger-ui.html"
else
  echo "API PetHub NÃO respondeu em https://${HOST}/api-docs"
  echo "(sem deploy ainda, ou a aplicação está subindo/falhou: veja os logs abaixo)"
fi
echo
echo "Logs ao vivo:  az webapp log tail --name ${WEBAPP} --resource-group ${RESOURCE_GROUP}"
