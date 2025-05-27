package io.sicredi.spirecorrencia.api.automatico.autorizacao;

import br.com.sicredi.spi.dto.Pain012Dto;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusAutorizacao;

import java.util.List;
import java.util.Optional;

public interface AutorizacaoService {

    AutorizacaoResponseWrapper consultarTodas(ConsultaAutorizacaoRequest request);

    AutorizacaoResponse consultarDetalhes(Long oidRecorrenciaAutorizacao);

    List<RecorrenciaAutorizacao> buscarComCiclosPorIdRecorrencia(String idRecorrencia);

    IdempotentResponse<?> processarRetornoAutorizacaoAposEnvioBacen(IdempotentRequest<Pain012Dto> request);

    IdempotentResponse<?> processarRetornoPedidoCancelamento(IdempotentRequest<Pain012Dto> request);

    IdempotentResponse<?> processarRecebimentoPain012Bacen(IdempotentRequest<Pain012Dto> request);

    RecorrenciaAutorizacao salvar(RecorrenciaAutorizacao recorrenciaAutorizacao);

    Optional<RecorrenciaAutorizacao> consultarAutorizacaoPorIdEStatus(String idRecorrencia, TipoStatusAutorizacao status);

    List<RecorrenciaAutorizacao> findAll();

    Optional<RecorrenciaAutorizacao> buscarPorOid(Long oidRecorrenciaAutorizacao);
}
