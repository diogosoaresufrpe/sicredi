package io.sicredi.spirecorrencia.api.accountdata;

import io.sicredi.spirecorrencia.api.exceptions.decoder.DefaultErrorDecoder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "AccountDataClient",
        url = "${services.account-data.url}",
        configuration = {DefaultErrorDecoder.class})
interface AccountDataClient {

    @GetMapping("/accounts")
    List<DadosContaResponseDTO> consultarConta(
            @RequestParam("document") final String documento,
            @RequestParam("company") final String coop,
            @RequestParam("number") final String conta,
            @RequestParam("source") final String sistema
    );
}