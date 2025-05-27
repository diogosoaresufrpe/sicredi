package io.sicredi.spirecorrencia.api.protocolo;

import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.automatico.CadastroAutorizacaoRecorrenciaProtocoloRequestDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.CadastroOrdemRequest;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoCanalEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoLiquidacao;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.CadastroRecorrenciaProtocoloRequest;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.ExclusaoRecorrenciaProtocoloRequest;
import io.sicredi.spirecorrencia.api.liquidacao.TipoExclusaoRecorrencia;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "spicanais-protocolo-api",
        url = "${services.spicanais-protocolo-api.url}",
        configuration = SpiCanaisProtocoloApiConfig.class
)
public interface SpiCanaisProtocoloApiClient {

    @PostMapping(value = "${services.spicanais-protocolo-api.cadastro-recorrencia-integrada}")
    ProtocoloDTO emitirProtocoloCadastroRecorrenciaIntegrada(
            @RequestHeader(name = "X-Canal") final TipoCanalEnum canal,
            @RequestHeader(name = "X-Tipo-Cadastro") final String tipoCadastro,
            @RequestBody final CadastroRecorrenciaProtocoloRequest body);

    @PostMapping("${services.spicanais-protocolo-api.liquidacao-recorrencia}")
    ProtocoloDTO emitirProtocoloLiquidacaoRecorrencia(
            @RequestHeader("X-Canal") final TipoCanalEnum canal,
            @RequestHeader("X-Tipo-Liquidacao") final TipoLiquidacao tipoLiquidacao,
            @RequestBody final CadastroOrdemRequest request);

    @PostMapping("${services.spicanais-protocolo-api.cancelamento-recorrencia}")
    ProtocoloDTO emitirProtocoloCancelamentoRecorrencia(
            @RequestHeader("X-Canal") final TipoCanalEnum canal,
            @RequestHeader("X-Tipo-Exclusao") final TipoExclusaoRecorrencia tipoExclusaoRecorrencia,
            @RequestBody final ExclusaoRecorrenciaProtocoloRequest request);

    @PostMapping(value = "${services.spicanais-protocolo-api.cadastro-autorizacao-integrada}")
    ProtocoloDTO emitirProtocoloCadastroAutorizacaoIntegrada(
            @RequestHeader(name = "X-Canal") final TipoCanalEnum canal,
            @RequestHeader(name = "X-Tipo-Cadastro") final String tipoCadastro,
            @RequestBody final CadastroAutorizacaoRecorrenciaProtocoloRequestDTO body);
}

