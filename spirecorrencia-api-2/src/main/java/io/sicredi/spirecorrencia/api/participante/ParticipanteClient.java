package io.sicredi.spirecorrencia.api.participante;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "spi-participantes", url = "${services.spi-participantes.url}",
        configuration = ParticipanteClientConfig.class)
interface ParticipanteClient {

    @GetMapping("${services.spi-participantes.consulta}")
    ParticipanteWrapperDTO consultarInstituicao(@RequestParam(defaultValue = "0") Integer page,
                                                @RequestParam(defaultValue = "1") Integer size,
                                                @RequestParam(defaultValue = "ENABLE") String situacao,
                                                @RequestParam(required = false) String ispb);
}