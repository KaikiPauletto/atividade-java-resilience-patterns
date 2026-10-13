@echo off
echo Iniciando Consul...
start "Consul" consul agent -dev
echo Aguardando Consul iniciar...
timeout /t 10 /nobreak > nul
echo Carregando configuracoes...

REM Configuração do Product API
consul kv put config/product-api/server.port "8000"
consul kv put config/product-api/spring.datasource.url "jdbc:postgresql://localhost/db_product"
consul kv put config/product-api/spring.datasource.username "postgres"
consul kv put config/product-api/spring.datasource.password "postgres"

consul kv put config/product-api/app.promotion.message "Promoção Semana do Cliente"
consul kv put config/product-api,en/app.promotion.message "Christmas Promotion!!!!"

REM Configuração do Currency API
consul kv put config/currency-api/server.port "8100"
consul kv put config/currency-api/spring.datasource.url "jdbc:postgresql://localhost/db_currency"
consul kv put config/currency-api/spring.datasource.username "postgres"
consul kv put config/currency-api/spring.datasource.password "postgres"

REM Configuração para todos os microserviços
consul kv put config/application/management.endpoints.web.exposure.include "*"
consul kv put config/application/management.endpoint.health.show-components "always"
consul kv put config/application/management.endpoint.health.show-details "always"
consul kv put config/application/management.health.livenessstate.enabled "true"
consul kv put config/application/management.health.readinessstate.enabled "true"
consul kv put config/application/management.health.db.enabled "true"

REM Configuração do Retry
consul kv put config/product-api/resilience4j.retry.instances.currency-api.max-attempts "3"
consul kv put config/product-api/resilience4j.retry.instances.currency-api.wait-duration "1s"

REM Configuração do Circuit Breaker
consul kv put config/product-api/resilience4j.circuitbreaker.instances.currency-api.sliding-window-type "COUNT_BASED"
consul kv put config/product-api/resilience4j.circuitbreaker.instances.currency-api.sliding-window-size "5"
consul kv put config/product-api/resilience4j.circuitbreaker.instances.currency-api.minimum-number-of-calls "5"
consul kv put config/product-api/resilience4j.circuitbreaker.instances.currency-api.failure-rate-threshold "50"
consul kv put config/product-api/resilience4j.circuitbreaker.instances.currency-api.wait-duration-in-open-state "100s"

REM Não repetir chamadas quando o Circuit Breaker estiver aberto
consul kv put config/product-api/resilience4j.retry.instances.currency-api.ignore-exceptions[0] "io.github.resilience4j.circuitbreaker.CallNotPermittedException"

REM Configuração do cache utilizando Caffeine
consul kv put config/product-api/spring.cache.type "caffeine"
consul kv put config/product-api/spring.cache.cache-names "currency-api"
consul kv put config/product-api/spring.cache.caffeine.spec "maximumSize=1000,expireAfterWrite=10m"

REM Ordem de execução dos aspectos de resiliência e cache
consul kv put config/product-api/resilience4j.retry.retry-aspect-order "100"
consul kv put config/product-api/resilience4j.circuitbreaker.circuit-breaker-aspect-order "101"

REM ============================================================
REM Currency API -> API do Banco Central (BCB): resiliencia e cache
REM Nome da instancia Resilience4j: bcb-api | Cache: bcb-quotes
REM ============================================================

REM Retry: no maximo 3 tentativas (incluindo a inicial), 1s entre elas
consul kv put config/currency-api/resilience4j.retry.instances.bcb-api.max-attempts "3"
consul kv put config/currency-api/resilience4j.retry.instances.bcb-api.wait-duration "1s"

REM Nao repetir chamadas rejeitadas pelo Circuit Breaker
consul kv put config/currency-api/resilience4j.retry.instances.bcb-api.ignore-exceptions[0] "io.github.resilience4j.circuitbreaker.CallNotPermittedException"

REM Circuit Breaker
consul kv put config/currency-api/resilience4j.circuitbreaker.instances.bcb-api.sliding-window-type "COUNT_BASED"
consul kv put config/currency-api/resilience4j.circuitbreaker.instances.bcb-api.sliding-window-size "5"
consul kv put config/currency-api/resilience4j.circuitbreaker.instances.bcb-api.minimum-number-of-calls "5"
consul kv put config/currency-api/resilience4j.circuitbreaker.instances.bcb-api.failure-rate-threshold "50"
consul kv put config/currency-api/resilience4j.circuitbreaker.instances.bcb-api.wait-duration-in-open-state "100s"

REM Cache Caffeine: maximo 1000 entradas, expira 10 minutos apos a gravacao
consul kv put config/currency-api/spring.cache.type "caffeine"
consul kv put config/currency-api/spring.cache.cache-names "bcb-quotes"
consul kv put config/currency-api/spring.cache.caffeine.spec "maximumSize=1000,expireAfterWrite=10m"

REM Ordem de execucao dos aspectos: Cache (99) -> Retry (100) -> CircuitBreaker (101)
consul kv put config/currency-api/resilience4j.retry.retry-aspect-order "100"
consul kv put config/currency-api/resilience4j.circuitbreaker.circuit-breaker-aspect-order "101"

echo.
echo Consul iniciado e configuracoes carregadas!
pause