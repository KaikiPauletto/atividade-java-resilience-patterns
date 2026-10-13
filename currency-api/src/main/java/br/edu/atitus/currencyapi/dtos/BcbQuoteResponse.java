package br.edu.atitus.currencyapi.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Resposta do endpoint CotacaoMoedaDia da API PTAX do Banco Central (formato OData/JSON).
 * <p>
 * Exemplo:
 * <pre>
 * { "@odata.context": "...",
 *   "value": [ { "paridadeCompra": 1.0, "paridadeVenda": 1.0,
 *                "cotacaoCompra": 5.4, "cotacaoVenda": 5.41,
 *                "dataHoraCotacao": "2025-10-10 13:07:12.123",
 *                "tipoBoletim": "Fechamento" } ] }
 * </pre>
 * Um mesmo dia pode ter vários boletins (Abertura, Intermediário e Fechamento PTAX).
 * Quando não há cotação (fim de semana, feriado, moeda não suportada), "value" vem vazio.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BcbQuoteResponse(
        @JsonProperty("value") List<Quote> value
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Quote(
            @JsonProperty("cotacaoCompra") BigDecimal cotacaoCompra,
            @JsonProperty("cotacaoVenda") BigDecimal cotacaoVenda,
            @JsonProperty("dataHoraCotacao") String dataHoraCotacao,
            @JsonProperty("tipoBoletim") String tipoBoletim
    ) {
    }

    /**
     * Valor em BRL de 1 unidade da moeda consultada (cotação de venda do boletim de Fechamento).
     * Se não houver boletim de Fechamento, usa o último boletim disponível.
     * Retorna vazio quando a resposta não traz nenhuma cotação válida.
     */
    public Optional<BigDecimal> closingRate() {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        Quote chosen = value.stream()
                .filter(q -> q != null && "Fechamento".equalsIgnoreCase(q.tipoBoletim()))
                .findFirst()
                .orElse(value.get(value.size() - 1));
        if (chosen == null
                || chosen.cotacaoVenda() == null
                || chosen.cotacaoVenda().signum() <= 0) {
            return Optional.empty();
        }
        return Optional.of(chosen.cotacaoVenda());
    }
}