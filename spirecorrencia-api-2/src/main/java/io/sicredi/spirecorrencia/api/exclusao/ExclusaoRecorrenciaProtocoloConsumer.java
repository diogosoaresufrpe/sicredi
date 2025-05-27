package io.sicredi.spirecorrencia.api.exclusao;

import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.ExclusaoRecorrenciaParcelaTransacaoDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import io.sicredi.spirecorrencia.api.commons.ProtocoloBaseConsumer;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.deadletter.DeadLetterService;
import io.sicredi.spirecorrencia.api.utils.ObjectMapperUtil;
import io.sicredi.spiutils.core.lib.commons.observabilidade.tracing.ObservabilidadeDecorator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.StringJoiner;

@Slf4j
@Component
class ExclusaoRecorrenciaProtocoloConsumer extends ProtocoloBaseConsumer<ExclusaoRequisicaoDTO> {

    public static final String MENSAGEM_DE_TRANSACAO = "mensagem de transação";
    private final ExclusaoService exclusaoService;
    private final AppConfig appConfig;

    public ExclusaoRecorrenciaProtocoloConsumer(DeadLetterService reprocessamentoService,
                                                ObservabilidadeDecorator observabilidadeDecorator,
                                                ExclusaoService exclusaoService,
                                                AppConfig appConfig) {
        super(reprocessamentoService, observabilidadeDecorator);
        this.exclusaoService = exclusaoService;
        this.appConfig = appConfig;
    }

    @RetryableTopic(
            attempts = "#{@appConfig.kafka.consumer.exclusaoRecorrencia.retry.tentativas}",
            autoCreateTopics = "false",
            backoff = @Backoff(delayExpression = "#{@appConfig.kafka.consumer.exclusaoRecorrencia.retry.delay}"),
            sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.SINGLE_TOPIC,
            include = {KafkaException.class, TechnicalException.class},
            timeout = "#{@appConfig.kafka.consumer.exclusaoRecorrencia.retry.timeout}",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(
            topics = "#{@appConfig.kafka.consumer.exclusaoRecorrencia.nome}",
            concurrency = "#{@appConfig.kafka.consumer.exclusaoRecorrencia.concurrency}",
            groupId = "#{@appConfig.kafka.consumer.exclusaoRecorrencia.groupId}",
            containerFactory = "protocoloKafkaListenerContainerFactory"
    )
    protected void consumir(@Header(value = "X-Ultima-Interacao", required = false) String dataHoraInicioCanal,
                            @Payload final String payload,
                            Acknowledgment acknowledgment) {
        processarMensagem(
                payload,
                dataHoraInicioCanal,
                MENSAGEM_DE_TRANSACAO,
                exclusaoService::processarProtocolo,
                acknowledgment
        );
    }

    @DltHandler
    public void handlerDlt(@Payload final String payload,
                           @Header(value = KafkaHeaders.EXCEPTION_CAUSE_FQCN, required = false) String causaException,
                           @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String mensagemException,
                           @Header(value = "X-Ultima-Interacao", required = false) String dataHoraInicioCanal,
                           Acknowledgment acknowledgment) {
        processarDlt(payload, causaException, mensagemException, acknowledgment);
    }

    @Override
    protected ExclusaoRequisicaoDTO lerPayloadTransacao(ProtocoloDTO protocolo, String identificadorTransacao, String dataHoraInicioCanal, ZonedDateTime dataHoraRecebimentoMensagem) {
        var listaExclucaoParcelas = ObjectMapperUtil.converterStringParaObjeto(protocolo.getPayloadTransacao(), new TypeReference<List<ExclusaoRecorrenciaParcelaTransacaoDTO>>() {
        });
        return ExclusaoRequisicaoDTO.fromEmissaoProtocolo(
                protocolo,
                listaExclucaoParcelas,
                identificadorTransacao,
                dataHoraInicioCanal,
                dataHoraRecebimentoMensagem
        );
    }

    @Override
    protected byte[] criarCheckSum(ExclusaoRequisicaoDTO exclusaoRequisicaoDTO) {
        var joiner = new StringJoiner(StringUtils.EMPTY);
        joiner.add(exclusaoRequisicaoDTO.getIdentificadorRecorrencia());
        joiner.add(exclusaoRequisicaoDTO.getIdentificadoresParcelas().toString());
        return joiner.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected String criarChaveIdempotencia(String identificadorTransacao) {
        return identificadorTransacao;
    }
}
