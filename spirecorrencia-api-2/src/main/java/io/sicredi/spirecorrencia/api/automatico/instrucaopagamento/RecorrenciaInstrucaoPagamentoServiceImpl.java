package io.sicredi.spirecorrencia.api.automatico.instrucaopagamento;

import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.spi.dto.Pain013Dto;
import br.com.sicredi.spi.dto.Pain014Dto;
import br.com.sicredi.spi.entities.type.MotivoRejeicaoPain014;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentAsyncRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentTransaction;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoMensagem;
import io.sicredi.spirecorrencia.api.automatico.pain.Pain014ResponseFactory;
import io.sicredi.spirecorrencia.api.idempotente.CriaResponseStrategyFactory;
import io.sicredi.spirecorrencia.api.idempotente.EventoResponseFactory;
import io.sicredi.spirecorrencia.api.idempotente.OperacaoRequest;
import io.sicredi.spirecorrencia.api.idempotente.TipoResponseIdempotente;
import io.sicredi.spirecorrencia.api.utils.RecorrenciaMdc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static io.sicredi.spirecorrencia.api.idempotente.OperacaoRequest.criarOperacaoRequest;

@Service
@Slf4j
@RequiredArgsConstructor
class RecorrenciaInstrucaoPagamentoServiceImpl implements RecorrenciaInstrucaoPagamentoService {
    private final ProcessaRecorrenciaInstrucaoPagamentoService processaRecorrenciaInstrucaoPagamentoService;
    private final EventoResponseFactory eventoResponseFactory;
    private final CriaResponseStrategyFactory<OperacaoRequest> criaResponseStrategyFactory;
    private final RecorrenciaInstrucaoPagamentoRepository repository;

    @Override
    @IdempotentTransaction
    public IdempotentResponse<?> processarPedidoAgendamentoDebito(IdempotentAsyncRequest<Pain013Dto> request) {
        try {
            var pain013 = request.getValue();
            MDC.put(RecorrenciaMdc.ID_RECORRENCIA.getChave(), pain013.getIdRecorrencia());
            MDC.put(RecorrenciaMdc.ID_FIM_A_FIM.getChave(), pain013.getIdFimAFim());

            log.debug("(Pain013) Processamento pedido de agendamento de débito.");
            var recorrenciaInstrucaoPagamento = processaRecorrenciaInstrucaoPagamentoService.processar(pain013);

            if (recorrenciaInstrucaoPagamento.getCodMotivoRejeicao() != null) {
                var motivoRejeicao = MotivoRejeicaoPain014.of(recorrenciaInstrucaoPagamento.getCodMotivoRejeicao());
                log.info("(Pain013) Processando pedido de agendamento NAO ACEITO pelo PSP do Pagador. TransactionId: {}, Motivo de rejeição: {}-{}, idRecorrencia: {}, idFimAFim: {}, dataVencimento: {}",
                        request.getTransactionId(), motivoRejeicao.getXmlValue(), motivoRejeicao, pain013.getIdRecorrencia(), pain013.getIdFimAFim(), pain013.getDataVencimento());

                var pain014 = Pain014ResponseFactory.fromPain013Erro(pain013, motivoRejeicao, recorrenciaInstrucaoPagamento.getDatConfirmacao());
                return criarResponseIdempotentePedidoAgendamentoDebito(request, pain014);
            }

            log.debug("(Pain013) Processando pedido de agendamento ACEITO pelo PSP do Pagador. TransactionId: {}, idRecorrencia: {}, idFimAFim: {}, dataVencimento: {}",
                    request.getTransactionId(), pain013.getIdRecorrencia(), pain013.getIdFimAFim(), pain013.getDataVencimento());

            var pain014 = Pain014ResponseFactory.fromPain013Sucesso(pain013, recorrenciaInstrucaoPagamento.getDatConfirmacao());
            return criarResponseIdempotentePedidoAgendamentoDebito(request, pain014);
        } finally {
            MDC.remove(RecorrenciaMdc.ID_RECORRENCIA.getChave());
            MDC.remove(RecorrenciaMdc.ID_FIM_A_FIM.getChave());
        }

    }

    @Override
    public IdempotentResponse<?> processarRetornoPedidoAgendamentoDebito(IdempotentAsyncRequest<Pain014Dto> request) {
        throw new TechnicalException("Funcionalidade não implementada");
    }


    private IdempotentResponse<?> criarResponseIdempotentePedidoAgendamentoDebito(IdempotentRequest<Pain013Dto> request, Pain014Dto pain014Dto) {
        var evento = eventoResponseFactory.criarEventoPain14IcomEnvio(pain014Dto);

        log.debug("Enviando resposta de instrução de agendamento de pagamento com Situacao:{} | TransactionId: {} | IdRecorrencia: {} | IdFimAFim: {} | DataVencimento: {}",
                pain014Dto.getSituacaoDoAgendamento(), request.getTransactionId(), request.getValue().getIdRecorrencia(), request.getValue().getIdFimAFim(), request.getValue().getDataVencimento());

        return criaResponseStrategyFactory.criar(TipoResponseIdempotente.OPERACAO).criarResponseIdempotentSucesso(
                criarOperacaoRequest(TipoMensagem.PAIN013, pain014Dto.getCodigoDeErro()),
                request.getTransactionId(),
                request.getHeaders(),
                List.of(evento)
        );
    }

    @Override
    public Optional<RecorrenciaInstrucaoPagamento> buscarPorCodFimAFimComAutorizacao(String idFimAFim) {
        return repository.buscarPorCodFimAFimComAutorizacao(idFimAFim);
    }

    @Override
    public void atualizaTpoStatusETpoSubStatus(String id, String status, String subStatus) {
        repository.atualizaTpoStatusETipoSubStatus(id, status, subStatus, LocalDateTime.now());
    }

    @Override
    public RecorrenciaInstrucaoPagamento salvarInstrucaoPagamento(RecorrenciaInstrucaoPagamento recorrenciaInstrucaoPagamento) {
        return repository.save(recorrenciaInstrucaoPagamento);
    }
}
