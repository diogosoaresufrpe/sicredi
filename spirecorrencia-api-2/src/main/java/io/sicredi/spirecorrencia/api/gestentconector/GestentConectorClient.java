package io.sicredi.spirecorrencia.api.gestentconector;

import io.sicredi.spirecorrencia.api.exceptions.decoder.DefaultErrorDecoder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "GestentConectorClient",
        url = "${services.gestent-conector.url}",
        configuration = {DefaultErrorDecoder.class})
interface GestentConectorClient {

    @GetMapping("${services.gestent-conector.consulta-dados-agencia}")
    Page<InformacoesCooperativaDTO> consultarDadosCooperativa(
            @RequestParam("codigoTipoEntidade") final String tipoEntidade,
            @RequestParam("codigoAgencia") final String agencia,
            @RequestParam("codigoCooperativa") final String cooperativa,
            Pageable pageable
    );
}