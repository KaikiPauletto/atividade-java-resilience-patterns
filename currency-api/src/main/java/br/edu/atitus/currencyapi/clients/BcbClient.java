package br.edu.atitus.currencyapi.clients;

import br.edu.atitus.currencyapi.dtos.BcbQuoteResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Cliente HTTP declarativo (HttpExchange) para a API PTAX do Banco Central do Brasil.
 * A URL base (https://olinda.bcb.gov.br) é definida em {@code BcbClientConfig}.
 */
@HttpExchange
public interface BcbClient {

    /**
     * @param moeda        código da moeda, ex.: USD
     * @param dataCotacao  data no formato exigido pelo BCB: MM-dd-yyyy
     */
    @GetExchange("/olinda/servico/PTAX/versao/v1/odata/"
            + "CotacaoMoedaDia(moeda=@moeda,dataCotacao=@dataCotacao)"
            + "?@moeda='{moeda}'&@dataCotacao='{dataCotacao}'&$format=json")
    BcbQuoteResponse getQuote(
            @PathVariable("moeda") String moeda,
            @PathVariable("dataCotacao") String dataCotacao);
}