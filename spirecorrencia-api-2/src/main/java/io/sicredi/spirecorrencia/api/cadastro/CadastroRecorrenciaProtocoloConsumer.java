package io.sicredi.spirecorrencia.api.cadastro;

import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.CadastroRecorrenciaTransacaoDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import io.sicredi.spirecorrencia.api.commons.ProtocoloBaseConsumer;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.deadletter.DeadLetterService;
import io.sicredi.spirecorrencia.api.utils.ObjectMapperUtil;
import io.sicredi.spiutils.core.lib.commons.observabilidade.tracing.ObservabilidadeDecorator;
import lombok.extern.slf4j.Slf4j;
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

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;

@Slf4j
@Component
class CadastroRecorrenciaProtocoloConsumer extends ProtocoloBaseConsumer<CadastroRequestWrapper> {

    private static final String CADASTRO_DE_RECORRENCIA = "cadastro de recorrência";

    private final CadastroService service;
    private final AppConfig appConfig;

    public CadastroRecorrenciaProtocoloConsumer(DeadLetterService reprocessamentoService,
                                                ObservabilidadeDecorator observabilidadeDecorator,
                                                CadastroService service,
                                                AppConfig appConfig) {
        super(reprocessamentoService, observabilidadeDecorator);
        this.service = service;
        this.appConfig = appConfig;
    }

    @RetryableTopic(
            attempts = "#{@appConfig.kafka.consumer.cadastroRecorrencia.retry.tentativas}",
            autoCreateTopics = "false",
            backoff = @Backoff(delayExpression = "#{@appConfig.kafka.consumer.cadastroRecorrencia.retry.delay}"),
            sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.SINGLE_TOPIC,
            include = {KafkaException.class, TechnicalException.class},
            timeout = "#{@appConfig.kafka.consumer.cadastroRecorrencia.retry.timeout}",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(
            topics = "#{@appConfig.kafka.consumer.cadastroRecorrencia.nome}",
            concurrency = "#{@appConfig.kafka.consumer.cadastroRecorrencia.concurrency}",
            groupId = "#{@appConfig.kafka.consumer.cadastroRecorrencia.groupId}",
            containerFactory = "protocoloKafkaListenerContainerFactory"
    )
    protected void consumir(@Header(value = "X-Ultima-Interacao", required = false) String dataHoraInicioCanal,
                            @Payload final String payload,
                            Acknowledgment acknowledgment) {
        processarMensagem(
                payload,
                dataHoraInicioCanal,
                CADASTRO_DE_RECORRENCIA,
                service::processarRecorrencia,
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
    protected CadastroRequestWrapper lerPayloadTransacao(ProtocoloDTO protocolo, String identificadorTransacao, String dataHoraInicioCanal, ZonedDateTime dataHoraRecebimentoMensagem) {
        var listaParcelas = ObjectMapperUtil.converterStringParaObjeto(protocolo.getPayloadTransacao(), new TypeReference<List<CadastroRecorrenciaTransacaoDTO>>() {
        });
        var recorrencia = CadastroRequest.criarRecorrencia(
                protocolo,
                listaParcelas,
                identificadorTransacao,
                dataHoraInicioCanal,
                dataHoraRecebimentoMensagem
        );

        return CadastroRequestWrapper.criarRecorrencia(
                recorrencia
        );
    }

    @Override
    protected byte[] criarCheckSum(CadastroRequestWrapper cadastroWrapperRequest) {
        return cadastroWrapperRequest.listaIdentificadoresParcelas().stream()
                .map(identificadorParcela -> new BigInteger(identificadorParcela.getBytes(StandardCharsets.UTF_8)))
                .reduce(BigInteger.ZERO, BigInteger::xor).toByteArray();
    }

    @Override
    protected String criarChaveIdempotencia(String identificadorTransacao) {
        return identificadorTransacao;
    }
}
