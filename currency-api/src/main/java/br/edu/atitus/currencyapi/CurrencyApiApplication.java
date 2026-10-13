package br.edu.atitus.currencyapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
// order = 99 faz o Cache ser o aspecto mais externo (antes do Retry=100 e do CircuitBreaker=101)
@EnableCaching(order = 99)
public class CurrencyApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CurrencyApiApplication.class, args);
    }

}