package br.edu.atitus.currencyapi.servicesimpl;

import br.edu.atitus.currencyapi.clients.BcbClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Acesso resiliente à API PTAX do BCB.
 * <p>
 * Ordem de execução dos aspectos (definida em @EnableCaching e no Consul):
 * Cache (99) -> Retry (100) -> CircuitBreaker (101) -> chamada HTTP (com timeouts).
 * <p>
 * Esta classe precisa ser um bean separado do CurrencyServiceJpa: as anotações só funcionam
 * quando a chamada passa pelo proxy do Spring (auto-invocação dentro da mesma classe ignora-as).
 */
@Service
public class Bcbquoteservice {

    private static final Logger log = LoggerFactory.getLogger(Bcbquoteservice.class);

    private final BcbClient bcbClient;

    public Bcbquoteservice(BcbClient bcbClient) {
        this.bcbClient = bcbClient;
    }

    /**
     * Retorna quantos BRL valem 1 unidade de {@code moeda} na data informada.
     * <p>
     * - Optional vazio  = o BCB respondeu normalmente, mas sem cotação (não é falha de comunicação:
     *   não dispara Retry nem conta como falha no Circuit Breaker).
     * - Exceção         = falha de comunicação (rede, timeout, HTTP 5xx...) ou circuito aberto.
     * <p>
     * Cache: a chave combina moeda + data. {@code unless = "#result == null"} faz o Spring ignorar
     * o Optional e NÃO gravar no cache quando não há cotação. Exceções nunca são cacheadas.
     */
    @Cacheable(
            value = "bcb-quotes",
            key = "#moeda + '-' + #dataCotacao",
            unless = "#result == null"
    )
    @Retry(name = "bcb-api")
    @CircuitBreaker(name = "bcb-api")
    public Optional<BigDecimal> getBrlQuote(String moeda, String dataCotacao) {
        log.info("Consultando API do BCB: moeda={} data={}", moeda, dataCotacao);
        return bcbClient.getQuote(moeda, dataCotacao).closingRate();
    }
}