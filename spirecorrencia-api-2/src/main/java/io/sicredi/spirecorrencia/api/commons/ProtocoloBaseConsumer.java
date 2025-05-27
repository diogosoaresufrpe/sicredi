package io.sicredi.spirecorrencia.api.commons;

import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentAsyncRequest;
import io.sicredi.spirecorrencia.api.deadletter.DeadLetterRequest;
import io.sicredi.spirecorrencia.api.deadletter.DeadLetterService;
import io.sicredi.spirecorrencia.api.idempotente.TipoResponseIdempotente;
import io.sicredi.spirecorrencia.api.utils.ObjectMapperUtil;
import io.sicredi.spirecorrencia.api.utils.RecorrenciaMdc;
import io.sicredi.spiutils.core.lib.commons.observabilidade.tracing.ObservabilidadeDecorator;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.Acknowledgment;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ProtocoloBaseConsumer<T> {

    private final DeadLetterService reprocessamentoService;
    private final ObservabilidadeDecorator observabilidadeDecorator;

    public void processarMensagem(
            String payload,
            String dataHoraInicioCanal,
            String tipoProcessamento,
            Consumer<IdempotentAsyncRequest<T>> serviceProcessor,
            Acknowledgment acknowledgment
    ) {
        var dataHoraRecebimentoMensagem = ZonedDateTime.now();
        var protocoloDTO = lerMensagemProtocoloDto(payload);
        var transactionId = criarChaveIdempotencia(protocoloDTO.getIdentificadorTransacao());

        var atributos = Map.of(
                RecorrenciaMdc.ID_PROTOCOLO, String.valueOf(protocoloDTO.getIdProtocolo()),
                RecorrenciaMdc.IDENTIFICADOR_TRANSACAO, transactionId
        );

        observabilidadeDecorator.executar(atributos, () -> {
            log.debug("Início do processamento de {}.", tipoProcessamento);
            var objetoRequest = lerPayloadTransacao(protocoloDTO, transactionId, dataHoraInicioCanal, dataHoraRecebimentoMensagem);
            var request = IdempotentAsyncRequest.<T>builder()
                    .value(objetoRequest)
                    .transactionId(transactionId)
                    .checkSumContentRule(() -> criarCheckSum(objetoRequest))
                    .build();
            serviceProcessor.accept(request);
            log.debug("Fim do processamento de {}.", tipoProcessamento);
        });
        acknowledgment.acknowledge();
    }

    protected void processarDlt(String payload, String causaException, String mensagemException, Acknowledgment acknowledgment) {
        log.debug("(DLT) Início do processamento da mensagem de transação. Payload: {}", payload);
        lerMensagemProtocoloDtoDlt(payload)
                .ifPresentOrElse(protocoloDTO -> {
                    var transactionId = protocoloDTO.getIdentificadorTransacao();
                    var atributos = Map.of(
                            RecorrenciaMdc.ID_PROTOCOLO, String.valueOf(protocoloDTO.getIdProtocolo()),
                            RecorrenciaMdc.IDENTIFICADOR_TRANSACAO, transactionId
                    );
                    observabilidadeDecorator.executar(atributos, () -> {
                        var deadLetterRequest = DeadLetterRequest.builder()
                                .protocoloDTO(protocoloDTO)
                                .causaException(causaException)
                                .mensagemErro(mensagemException)
                                .tipoResponse(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)
                                .payload(payload)
                                .build();

                        reprocessamentoService.processar(deadLetterRequest);
                        log.debug("(DLT) Fim do processamento da mensagem de transação. Payload: {}", payload);
                    });
                }, () -> log.warn("(DLT) Mensagem ignorada. Payload do Protocolo da Transação inválido. Payload: {}", payload));
        acknowledgment.acknowledge();
    }

    protected abstract T lerPayloadTransacao(ProtocoloDTO protocolo, String transactionId, String dataHoraInicioCanal, ZonedDateTime dataHoraRecebimentoMensagem);

    protected abstract byte[] criarCheckSum(T objeto);

    protected abstract String criarChaveIdempotencia(String identificadorTransacao);

    protected static Optional<ProtocoloDTO> lerMensagemProtocoloDtoDlt(String payload) {
        try {
            return Optional.ofNullable(lerMensagemProtocoloDto(payload));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    protected static ProtocoloDTO lerMensagemProtocoloDto(String payload) {
        return ObjectMapperUtil.converterStringParaObjeto(payload, ProtocoloDTO.class);
    }
}
