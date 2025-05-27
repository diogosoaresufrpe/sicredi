package io.sicredi.spirecorrencia.api;

import br.com.sicredi.framework.web.spring.exception.configuration.EnableDefaultExceptionHandler;
import io.sicredi.engineering.libraries.idempotent.transaction.EnableIdempotentTransaction;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableFeignClients
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = {"io.sicredi.spirecorrencia"})
@ComponentScan(basePackages = {"io.sicredi.spirecorrencia", "io.sicredi.spiutils", "br.com.sicredi"})
@EnableDefaultExceptionHandler
@EnableIdempotentTransaction
@EnableCaching
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
