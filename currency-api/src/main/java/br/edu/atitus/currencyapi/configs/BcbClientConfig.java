package br.edu.atitus.currencyapi.configs;

import br.edu.atitus.currencyapi.clients.BcbClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.autoconfigure.RestClientBuilderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

@Configuration
public class BcbClientConfig {

    /**
     * Cliente da API do BCB. Atenção: este RestClient NÃO é @LoadBalanced, pois o BCB é um
     * serviço externo (não está registrado no Consul).
     * <p>
     * A propriedade app.bcb.base-url existe apenas para facilitar os testes de falha/timeout
     * (ex.: apontar para um servidor local que não responde). O padrão é o BCB real.
     */
    @Bean
    BcbClient bcbClient(
            RestClientBuilderConfigurer configurer,
            @Value("${app.bcb.base-url:https://olinda.bcb.gov.br}") String baseUrl) {

        // Timeout Pattern: limita a espera pela conexão (2s) e pela leitura da resposta (5s)
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        RestClient restClient = configurer.configure(RestClient.builder())
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();

        return factory.createClient(BcbClient.class);
    }
}