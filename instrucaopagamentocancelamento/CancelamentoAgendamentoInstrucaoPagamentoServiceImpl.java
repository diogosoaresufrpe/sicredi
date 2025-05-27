package io.sicredi.spirecorrencia.api.automatico.instrucaopagamentocancelamento;

import br.com.sicredi.spi.entities.type.MotivoCancelamentoCamt55;
import br.com.sicredi.spi.entities.type.TipoSolicitacaoCamt55;
import br.com.sicredi.spi.util.SpiUtil;
import br.com.sicredi.spi.util.type.TipoId;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentTransaction;
import io.sicredi.spirecorrencia.api.RecorrenciaConstantes;
import io.sicredi.spirecorrencia.api.automatico.camt.Camt055ResponseFactory;
import io.sicredi.spirecorrencia.api.automatico.instrucaopagamento.RecorrenciaInstrucaoPagamento;
import io.sicredi.spirecorrencia.api.idempotente.*;
import io.sicredi.spirecorrencia.api.messasing.MessageProducer;
import io.sicredi.spirecorrencia.api.utils.RecorrenciaMdc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.fasterxml.jackson.databind.type.LogicalType.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CancelamentoAgendamentoInstrucaoPagamentoServiceImpl implements CancelamentoAgendamentoInstrucaoPagamentoService {

    private final EventoResponseFactory eventoResponseFactory;
    private final MessageProducer messageProducer;
    private final ProcessaCancelamentoAgendamentoService processarCancelamentoAgendamento;
    private final CriaResponseStrategyFactory<CancelamentoAgendamentoDebitoRequest> criaResponseStrategyFactory;

    @IdempotentTransaction
    public IdempotentResponse<?> processarCancelamentoDebito(IdempotentRequest<CancelamentoAgendamentoDebitoRequest> request) {
        var cancelamentoRequest = request.getValue();
        var tipoResponse = cancelamentoRequest.getTipoResponse();

        try {
            MDC.put(RecorrenciaMdc.ID_CANCELAMENTO_AGENDAMENTO.getChave(), cancelamentoRequest.getIdCancelamentoAgendamento());
            MDC.put(RecorrenciaMdc.ID_FIM_A_FIM.getChave(), cancelamentoRequest.getIdCancelamentoAgendamento());

            log.debug("Início do processamento de cancelamento de débito.");

            var resultadoProcessamento = processarCancelamentoAgendamento.processarCancelamento(cancelamentoRequest);

            if (resultadoProcessamento.getErro().isPresent()) {
                return criaResponseStrategyFactory.criar(tipoResponse).criarResponseIdempotentErro(cancelamentoRequest,
                        request.getTransactionId(),
                        resultadoProcessamento.getErro().get()
                );
            }

            var camt055Dto = resultadoProcessamento.getObjeto();
            var eventoCamt055 = eventoResponseFactory.criarEventoCamt055(camt055Dto);

           var response = criaResponseStrategyFactory.criar(tipoResponse).criarResponseIdempotentSucesso(cancelamentoRequest,
                    request.getTransactionId(),
                    request.getHeaders(),
                    List.of(eventoCamt055)
            );

            log.debug("Fim do processamento de cancelamento de débito com sucesso.");

            return response;
        } finally {
            MDC.remove(RecorrenciaMdc.ID_CANCELAMENTO_AGENDAMENTO.getChave());
            MDC.remove(RecorrenciaMdc.ID_FIM_A_FIM.getChave());
        }
    }

    public ErroWrapperDTO<EventoResponseDTO> processarCancelamentoDebito(CancelamentoAgendamentoWrapperDTO wrapperDTO) {

        try {
            MDC.put(RecorrenciaMdc.ID_CANCELAMENTO_AGENDAMENTO.getChave(), wrapperDTO.getIdCancelamentoAgendamento());
            MDC.put(RecorrenciaMdc.ID_FIM_A_FIM.getChave(), wrapperDTO.getInstrucaoPagamento().getCodFimAFim());

            var resultadoProcessamento = processarCancelamentoAgendamento.processarCancelamentoComInstrucao(wrapperDTO);

            if (resultadoProcessamento.getErro().isPresent()) {
                log.error("Erro ao processar cancelamento com instrução. Erro: {}", resultadoProcessamento.getErro().get().mensagemErro());
                return new ErroWrapperDTO<>(resultadoProcessamento.getErro().get());
            }

            var camt055Dto = resultadoProcessamento.getObjeto();
            var eventoCamt055 = eventoResponseFactory.criarEventoCamt055(camt055Dto);
            return new ErroWrapperDTO<>(eventoCamt055);
        } finally {
            MDC.remove(RecorrenciaMdc.ID_CANCELAMENTO_AGENDAMENTO.getChave());
            MDC.remove(RecorrenciaMdc.ID_FIM_A_FIM.getChave());
        }
    }

    @Transactional
    public Optional<ErroDTO> processarCancelamentoDebitoSemIdempotencia(CancelamentoAgendamentoWrapperDTO wrapperDTO) {

        try {
            MDC.put(RecorrenciaMdc.ID_CANCELAMENTO_AGENDAMENTO.getChave(), wrapperDTO.getIdCancelamentoAgendamento());
            MDC.put(RecorrenciaMdc.ID_FIM_A_FIM.getChave(), wrapperDTO.getInstrucaoPagamento().getCodFimAFim());

            var resultadoProcessamento = processarCancelamentoAgendamento.processarCancelamentoComInstrucao(wrapperDTO);

            if (resultadoProcessamento.getErro().isPresent()) {
                log.error("Ocorreu um erro ao realizar o cancelamento de um agendamento de débito. Erro: {}", resultadoProcessamento.getErro().get().mensagemErro());
                return resultadoProcessamento.getErro();
            }

            var camt055Dto = resultadoProcessamento.getObjeto();
            var eventoCamt055 = eventoResponseFactory.criarEventoCamt055(camt055Dto);

            messageProducer.enviar(eventoCamt055.mensagemJson(), eventoCamt055.topic(), eventoCamt055.headers());

            return Optional.empty();
        } finally {
            MDC.remove(RecorrenciaMdc.ID_CANCELAMENTO_AGENDAMENTO.getChave());
            MDC.remove(RecorrenciaMdc.ID_FIM_A_FIM.getChave());
        }

    }
}