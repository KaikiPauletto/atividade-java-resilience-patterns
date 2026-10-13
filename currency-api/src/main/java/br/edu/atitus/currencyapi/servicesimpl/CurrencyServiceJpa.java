package br.edu.atitus.currencyapi.servicesimpl;

import br.edu.atitus.currencyapi.dtos.CurrencyResponse;
import br.edu.atitus.currencyapi.repositories.CurrencyRepository;
import br.edu.atitus.currencyapi.services.CurrencyService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class CurrencyServiceJpa implements CurrencyService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyServiceJpa.class);

    private static final String BRL = "BRL";
    private static final DateTimeFormatter BCB_DATE_FORMAT = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private final CurrencyRepository repository;
    private final BcbQuoteService bcbQuoteService;
    private final String bcbQuoteDate;

    @Value("${server.port:8080}")
    private String serverPort;

    public CurrencyServiceJpa(
            CurrencyRepository repository,
            BcbQuoteService bcbQuoteService,
            // Data fixa (dia útil com cotação) conforme permitido no enunciado. Formato ISO: yyyy-MM-dd
            @Value("${app.bcb.quote-date:2025-10-10}") String quoteDate) {
        this.repository = repository;
        this.bcbQuoteService = bcbQuoteService;
        this.bcbQuoteDate = LocalDate.parse(quoteDate).format(BCB_DATE_FORMAT);
    }

    @Override
    public CurrencyResponse findBySourceCurrencyAndTargetCurrency(String sourceCurrency, String targetCurrency) throws Exception {
        String source = sourceCurrency.toUpperCase();
        String target = targetCurrency.toUpperCase();

        // 1) Tenta a fonte externa (BCB), já protegida por Cache + Retry + CircuitBreaker + Timeout
        Optional<BigDecimal> externalRate = findExternalRate(source, target);
        if (externalRate.isPresent()) {
            return buildResponse(source, target, externalRate.get().doubleValue(),
                    "BCB PTAX " + bcbQuoteDate);
        }

        // 2) Fallback Pattern: falha externa OU ausência de cotação -> banco de dados local
        var currency = repository.findBySourceCurrencyAndTargetCurrency(source, target)
                .orElseThrow(() -> new EntityNotFoundException("Currency não encontrado"));
        log.info("Fallback: usando cotação do banco local para {}->{}", source, target);
        return buildResponse(source, target, currency.getConversionRate(),
                "banco de dados local (fallback)");
    }

    /**
     * Calcula a taxa source->target a partir das cotações do BCB, que são sempre "BRL por 1 unidade
     * da moeda". Assim: taxa = BRLpor(source) / BRLpor(target), com BRLpor(BRL) = 1.
     * Retorna vazio se qualquer etapa falhar ou não tiver cotação (o chamador aciona o fallback).
     */
    private Optional<BigDecimal> findExternalRate(String source, String target) {
        // Códigos inválidos nem chegam ao BCB (evita erros HTTP que contariam como falha no Circuit Breaker)
        if (!source.matches("[A-Z]{3}") || !target.matches("[A-Z]{3}")) {
            return Optional.empty();
        }
        try {
            Optional<BigDecimal> sourceInBrl = quoteInBrl(source);
            if (sourceInBrl.isEmpty()) {
                return Optional.empty();
            }
            Optional<BigDecimal> targetInBrl = quoteInBrl(target);
            if (targetInBrl.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(sourceInBrl.get().divide(targetInBrl.get(), 10, RoundingMode.HALF_UP));
        } catch (CallNotPermittedException e) {
            log.warn("Circuit Breaker ABERTO para o BCB ({}->{}): chamada rejeitada, acionando fallback", source, target);
        } catch (Exception e) {
            log.warn("Falha ao consultar o BCB ({}->{}): {} - acionando fallback",
                    source, target, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return Optional.empty();
    }

    private Optional<BigDecimal> quoteInBrl(String currency) {
        if (BRL.equals(currency)) {
            return Optional.of(BigDecimal.ONE);
        }
        Optional<BigDecimal> quote = bcbQuoteService.getBrlQuote(currency, bcbQuoteDate);
        if (quote.isEmpty()) {
            log.warn("BCB não retornou cotação para {} em {} - acionando fallback", currency, bcbQuoteDate);
        }
        return quote;
    }

    private CurrencyResponse buildResponse(String source, String target, Double rate, String origin) {
        String environment = "Currency API running in port " + serverPort + " - Fonte: " + origin;
        return new CurrencyResponse(source, target, rate, environment);
    }
}