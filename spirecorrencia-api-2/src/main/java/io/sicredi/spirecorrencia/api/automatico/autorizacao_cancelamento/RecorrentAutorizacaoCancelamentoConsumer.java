package io.sicredi.spirecorrencia.api.automatico.autorizacao_cancelamento;

import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.spicanais.transacional.transport.lib.automatico.CancelamentoAutorizacaoRecorrenciaTransacaoDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import io.sicredi.spirecorrencia.api.commons.ProtocoloBaseConsumer;
import io.sicredi.spirecorrencia.api.deadletter.DeadLetterService;
import io.sicredi.spirecorrencia.api.utils.ObjectMapperUtil;
import io.sicredi.spiutils.core.lib.commons.observabilidade.tracing.ObservabilidadeDecorator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.StringJoiner;

@Slf4j
@Component
public class RecorrentAutorizacaoCancelamentoConsumer extends ProtocoloBaseConsumer<CancelamentoAutorizacaoRequest> {

    private final AutorizacaoCancelamentoProtocoloService autorizacaoCancelamentoService;

    protected RecorrentAutorizacaoCancelamentoConsumer(DeadLetterService reprocessamentoService,
                                                       ObservabilidadeDecorator observabilidadeDecorator,
                                                       AutorizacaoCancelamentoProtocoloService autorizacaoCancelamentoService) {
        super(reprocessamentoService, observabilidadeDecorator);
        this.autorizacaoCancelamentoService = autorizacaoCancelamentoService;
    }

    @RetryableTopic(
            attempts = "#{@appConfig.kafka.consumer.autorizacaoCancelamento.retry.tentativas}",
            autoCreateTopics = "false",
            backoff = @Backoff(delayExpression = "#{@appConfig.kafka.consumer.autorizacaoCancelamento.retry.delay}"),
            sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.SINGLE_TOPIC,
            include = {KafkaException.class, TechnicalException.class},
            timeout = "#{@appConfig.kafka.consumer.autorizacaoCancelamento.retry.timeout}",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(
            topics = "#{@appConfig.kafka.consumer.autorizacaoCancelamento.nome}",
            concurrency = "#{@appConfig.kafka.consumer.autorizacaoCancelamento.concurrency}",
            groupId = "#{@appConfig.kafka.consumer.autorizacaoCancelamento.groupId}",
            containerFactory = "protocoloKafkaListenerContainerFactory"
    )
    protected void consumidor(@Header(value = "X-Ultima-Interacao", required = false) String dataHoraInicioCanal,
                              @Payload final String payload,
                              Acknowledgment acknowledgment) {
        processarMensagem(payload,
                dataHoraInicioCanal,
                "Autorizacao de Recorrencia Cancelamento",
                autorizacaoCancelamentoService::processaCancelamentoRecorrenciaAutorizacao,
                acknowledgment);
    }

    @Override
    protected CancelamentoAutorizacaoRequest lerPayloadTransacao(ProtocoloDTO protocolo,
                                                                 String transactionId,
                                                                 String dataHoraInicioCanal,
                                                                 ZonedDateTime dataHoraRecebimentoMensagem) {
        var transacao = ObjectMapperUtil.converterStringParaObjeto(protocolo.getPayloadTransacao(),
                new TypeReference<CancelamentoAutorizacaoRecorrenciaTransacaoDTO>() {
        });

        var dataHoraCanal = Optional.ofNullable(dataHoraInicioCanal)
                .map(data -> ZonedDateTime.parse(data, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSZ"))
                        .withZoneSameInstant(ZoneId.of("America/Sao_Paulo"))
                        .toLocalDateTime())
                .orElseGet(LocalDateTime::now);

        return CancelamentoAutorizacaoRequest.builder()
                .idInformacaoCancelamento(transacao.getIdInformacaoCancelamento())
                .oidRecorrenciaAutorizacao(transacao.getOidRecorrenciaAutorizacao())
                .cpfCnpjSolicitanteCancelamento(transacao.getCpfCnpjSolicitanteCancelamento())
                .identificadorTransacao(transactionId)
                .dataHoraRecepcao(dataHoraRecebimentoMensagem)
                .motivoCancelamento(transacao.getMotivoCancelamento())
                .protocoloDTO(protocolo)
                .dataHoraInicioCanal(dataHoraCanal)
                .build();
    }

    @Override
    protected byte[] criarCheckSum(CancelamentoAutorizacaoRequest objeto) {
         return new StringJoiner(StringUtils.EMPTY)
                 .add(objeto.getIdInformacaoCancelamento())
                 .add(String.valueOf(objeto.getOidRecorrenciaAutorizacao()))
                 .toString()
                 .getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected String criarChaveIdempotencia(String identificadorTransacao) {
        return identificadorTransacao;
    }
}
