package io.sicredi.spirecorrencia.api.dict;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@FeignClient(value = "dict-api", url = "${services.spi-dict-consulta.url}", configuration = DictFeignClientConfig.class)
public interface ConsultaChaveDictClient {

    @GetMapping("${services.spi-dict-consulta.consulta}")
    Optional<DictConsultaDTO> consultaChaveDict(
            @PathVariable(name = "key") String key,
            @RequestHeader("cpfCnpjPagador") String cpfCnpjPagador,
            @RequestHeader("cooperativaPagador") String cooperativaPagador,
            @RequestHeader("origem") String canalOrigemTransacao,
            @RequestHeader("channelData") String channelData,
            @RequestParam("incluirEstatisticas") boolean incluirEstatisticas
    );
}
