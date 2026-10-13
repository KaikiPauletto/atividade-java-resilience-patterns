# Microserviços — Resilience Patterns

> **Observação:** precisei criar um novo repositório em vez de continuar no que já existia.

Dois microserviços Spring Boot com descoberta de serviços pelo Consul:

- **product-api** (porta 8000): lista produtos e converte o preço consultando o `currency-api`.
- **currency-api** (porta 8100): informa a taxa de conversão entre moedas, consultando a API PTAX do Banco Central e usando o banco local como fallback.

## Evolução do currency-api

A consulta ao Banco Central usa `HttpExchange` + `RestClient` e é protegida por:

| Padrão | Configuração |
|---|---|
| Timeout | conexão 2 s, leitura 5 s |
| Retry | até 3 tentativas, 1 s de intervalo (não repete `CallNotPermittedException`) |
| Circuit Breaker | janela de 5 chamadas, mínimo 5, abre com 50% de falhas, 100 s aberto |
| Cache (Caffeine) | 1.000 entradas, expira em 10 min; chave = moeda + data |
| Fallback | banco local; sem cotação em nenhum dos dois → HTTP 404 |

O fallback é acionado quando há falha de comunicação ou quando o BCB responde sem cotação. Só cotações válidas são cacheadas.

## Como executar

1. Crie os bancos `db_product` e `db_currency` no PostgreSQL (usuário e senha `postgres`).
2. Execute `start_consul.bat` (inicia o Consul e carrega as configurações; repita a cada reinício do Consul).
3. Suba o `currency-api` e o `product-api`.
4. Teste:
   ```
   curl "http://localhost:8100/currencies?source=USD&target=BRL"
   curl "http://localhost:8000/products/1?targetCurrency=BRL"
   ```
