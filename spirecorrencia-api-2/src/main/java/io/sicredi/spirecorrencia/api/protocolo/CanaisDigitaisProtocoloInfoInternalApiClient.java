package io.sicredi.spirecorrencia.api.protocolo;

import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import io.sicredi.spirecorrencia.api.exceptions.decoder.DefaultErrorDecoder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "canaisdigitais-protocolo-info-internal-api",
        url = "${services.canaisdigitais-protocolo-info-internal-api.url}",
        configuration = DefaultErrorDecoder.class
)
public interface CanaisDigitaisProtocoloInfoInternalApiClient {

    @GetMapping("${services.canaisdigitais-protocolo-info-internal-api.consulta-protocolo-por-tipo-e-identificador}")
    ProtocoloDTO consultaProtocoloPorTipoEIdentificador(@PathVariable("codigoTipoTransacao") String codigoTipoTransacao,
                                                        @PathVariable("identificadorTransacao") String identificadorTransacao);

}
