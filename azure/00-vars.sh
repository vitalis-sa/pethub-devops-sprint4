#!/usr/bin/env bash
# =====================================================================
# Variáveis compartilhadas pelos scripts de provisionamento da Sprint 4.
# NÃO executa nada sozinho: é carregado com "source" pelos demais.
#
#   RM do representante do grupo: 566234 (João Guilherme Carvalho Novaes)
#
# Nenhum segredo fica aqui. Usuário e senha do banco e o segredo do JWT
# são variáveis protegidas (cadeado) no Azure DevOps, e a Release do CD
# as grava nas configurações do Web App a cada deploy.
# =====================================================================

export RM="rm566234"

# Localização e Resource Group. A política "Allowed resource deployment regions" da
# assinatura Azure for Students só libera southafricanorth, brazilsouth, spaincentral,
# chilecentral e eastus. brazilsouth fica perto do Oracle da FIAP.
export LOCATION="${LOCATION:-brazilsouth}"
export RESOURCE_GROUP="rg-${RM}-sprint4"

# App Service: plano Linux B1 (Always On, a JVM não dorme durante a correção)
export APP_SERVICE_PLAN="plan-${RM}-pethub"
export APP_SERVICE_SKU="B1"

# Web App Java SE: o CD publica nele o .jar gerado pelo CI
export WEBAPP="${RM}-pethub"
export WEBAPP_RUNTIME="JAVA:21-java21"
