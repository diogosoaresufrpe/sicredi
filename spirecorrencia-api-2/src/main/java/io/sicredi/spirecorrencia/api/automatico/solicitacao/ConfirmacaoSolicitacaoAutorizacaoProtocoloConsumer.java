package io.sicredi.spirecorrencia.api.automatico.solicitacao;

import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.spi.entities.type.MotivoRejeicaoPain012;
import br.com.sicredi.spicanais.transacional.transport.lib.automatico.ConfirmacaoSolicitacaoAutorizacaoRecorrenciaTransacaoDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoCanalEnum;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.ConfirmacaoAutorizacaoRequestDTO;
import io.sicredi.spirecorrencia.api.commons.ProtocoloBaseConsumer;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.deadletter.DeadLetterService;
import io.sicredi.spirecorrencia.api.idempotente.TipoResponseIdempotente;
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
import java.util.StringJoiner;

import static io.sicredi.spirecorrencia.api.automatico.autorizacao.ConfirmacaoAutorizacaoRequestDTO.criarDataHoraInicioCanal;

@Slf4j
@Component
class ConfirmacaoSolicitacaoAutorizacaoProtocoloConsumer extends ProtocoloBaseConsumer<ConfirmacaoAutorizacaoRequestDTO> {

    private static final String CONFIRMACAO_DE_AUTORIZACAO = "confirmação de autorização";

    private final ConfirmacaoSolicitacaoAutorizacaoService service;
    private final AppConfig appConfig;

    public ConfirmacaoSolicitacaoAutorizacaoProtocoloConsumer(DeadLetterService reprocessamentoService,
                                                              ObservabilidadeDecorator observabilidadeDecorator,
                                                              ConfirmacaoSolicitacaoAutorizacaoService service,
                                                              AppConfig appConfig) {
        super(reprocessamentoService, observabilidadeDecorator);
        this.service = service;
        this.appConfig = appConfig;
    }

    @RetryableTopic(
            attempts = "#{@appConfig.kafka.consumer.confirmacaoSolicitacaoAutorizacao.retry.tentativas}",
            autoCreateTopics = "false",
            backoff = @Backoff(delayExpression = "#{@appConfig.kafka.consumer.confirmacaoSolicitacaoAutorizacao.retry.delay}"),
            sameIntervalTopicReuseStrategy = SameIntervalTopicReuseStrategy.SINGLE_TOPIC,
            include = {KafkaException.class, TechnicalException.class},
            timeout = "#{@appConfig.kafka.consumer.confirmacaoSolicitacaoAutorizacao.retry.timeout}",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )
    @KafkaListener(
            topics = "#{@appConfig.kafka.consumer.confirmacaoSolicitacaoAutorizacao.nome}",
            concurrency = "#{@appConfig.kafka.consumer.confirmacaoSolicitacaoAutorizacao.concurrency}",
            groupId = "#{@appConfig.kafka.consumer.confirmacaoSolicitacaoAutorizacao.groupId}",
            containerFactory = "protocoloKafkaListenerContainerFactory"
    )
    protected void consumir(@Header(value = "X-Ultima-Interacao", required = false) String dataHoraInicioCanal,
                            @Payload final String payload,
                            Acknowledgment acknowledgment) {
        processarMensagem(
                payload,
                dataHoraInicioCanal,
                CONFIRMACAO_DE_AUTORIZACAO,
                service::processarConfirmacao,
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
    protected ConfirmacaoAutorizacaoRequestDTO lerPayloadTransacao(ProtocoloDTO protocolo, String transactionId, String dataHoraInicioCanal, ZonedDateTime dataHoraRecebimentoMensagem) {
        var dto = ObjectMapperUtil.converterStringParaObjeto(protocolo.getPayloadTransacao(), ConfirmacaoSolicitacaoAutorizacaoRecorrenciaTransacaoDTO.class);
        return ConfirmacaoAutorizacaoRequestDTO.builder()
                .idSolicitacaoRecorrencia(dto.getIdSolicitacaoRecorrencia())
                .idRecorrencia(dto.getIdRecorrencia())
                .idInformacaoStatus(dto.getIdInformacaoStatus())
                .valorMaximo(dto.getValorMaximo())
                .aprovada(dto.getAprovada())
                .motivoRejeicao(dto.getMotivoRejeicao() != null ? MotivoRejeicaoPain012.valueOf(dto.getMotivoRejeicao().name()) : null)
                .identificadorTransacao(transactionId)
                .dataHoraInicioCanal(criarDataHoraInicioCanal(dataHoraInicioCanal))
                .dataHoraRecepcao(dataHoraRecebimentoMensagem)
                .tipoResponse(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)
                .protocoloDTO(protocolo)
                .cpfCnpjPagador(dto.getCpfCnpjConta())
                .tipoCanalPagador(TipoCanalEnum.valueOf(dto.getCanal()))
                .build();
    }

    @Override
    protected byte[] criarCheckSum(ConfirmacaoAutorizacaoRequestDTO request) {
        var joiner = new StringJoiner(StringUtils.EMPTY);
        joiner.add(request.getIdRecorrencia());
        joiner.add(request.getIdSolicitacaoRecorrencia());
        return joiner.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected String criarChaveIdempotencia(String identificadorTransacao) {
        return identificadorTransacao.concat("_JORNADA1B");
    }
}