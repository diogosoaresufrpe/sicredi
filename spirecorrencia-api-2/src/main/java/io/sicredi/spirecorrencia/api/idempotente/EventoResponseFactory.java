package io.sicredi.spirecorrencia.api.idempotente;

import br.com.sicredi.canaisdigitais.dto.protocolo.ComandoProtocoloDTO;
import br.com.sicredi.spi.dto.Camt055Dto;
import br.com.sicredi.spi.dto.Camt029Dto;
import br.com.sicredi.spi.dto.Pain011Dto;
import br.com.sicredi.spi.dto.Pain012Dto;
import br.com.sicredi.spi.dto.Pain014Dto;
import io.sicredi.spirecorrencia.api.RecorrenciaConstantes;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.exclusao.EventoExclusaoPayloadDTO;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO;
import io.sicredi.spirecorrencia.api.repositorio.Recorrencia;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

import java.util.HashMap;

import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.ExcluirRecorrenciaTransacao.HeadersKafka.GESTAO;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.ExcluirRecorrenciaTransacao.HeadersKafka.PROCESSO;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.ExcluirRecorrenciaTransacao.HeadersKafka.TIPO_CANAL;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.ExcluirRecorrenciaTransacao.HeadersKafka.TIPO_RECORRENCIA;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Headers.PAGADOR;

@Component
@RequiredArgsConstructor
public class EventoResponseFactory {

    private final AppConfig appConfig;


    public EventoResponseDTO criarEventoAtualizacaoProtocolo(ComandoProtocoloDTO comandoProtocolo) {
        var headerKafka = new HashMap<String, String>();
        var topico = appConfig.getKafka().getProducer().getComandoProtocolo().getTopico();
        return new EventoResponseDTO(comandoProtocolo, headerKafka, topico);
    }

    public EventoResponseDTO criarEventoNotificacao(NotificacaoDTO notificacaoDTO) {
        var headerKafka = new HashMap<String, String>();
        var topico = appConfig.getKafka().getProducer().getNotificacaoRecorrencia().getTopico();
        return new EventoResponseDTO(notificacaoDTO, headerKafka, topico);
    }

    public EventoResponseDTO criarEventoExclusaoOpenFinance(Recorrencia recorrencia, EventoExclusaoPayloadDTO eventoExclusaoPayloadDTO) {
        var headerKafka = new HashMap<String, String>();
        headerKafka.put(TIPO_CANAL, recorrencia.getTipoCanal().name());
        headerKafka.put(TIPO_RECORRENCIA, recorrencia.getTipoRecorrencia().name());
        headerKafka.put(PROCESSO, GESTAO);
        var topico = appConfig.getKafka().getProducer().getExclusaoRecorrencia().getTopico();
        return new EventoResponseDTO(eventoExclusaoPayloadDTO, headerKafka, topico);
    }

    public EventoResponseDTO criarEventoPain012(Pain012Dto pain012Dto, String operacao) {
        return criaEventoResponse(operacao, "PAIN012", pain012Dto.getIdInformacaoStatus(), pain012Dto.getIdRecorrencia(), pain012Dto);
    }

    public EventoResponseDTO criaEventoPain011(Pain011Dto pain011, String operacao) {
        return criaEventoResponse(operacao, "PAIN011", pain011.getIdInformacaoCancelamento(), pain011.getIdRecorrencia(), pain011);
    }

    private EventoResponseDTO criaEventoResponse(String operacao,
                                                          String tipoMensagem,
                                                          String idIdempotencia,
                                                          String mensagemKey,
                                                          Object payload) {
        var headerKafka = new HashMap<String, String>();
        headerKafka.put(RecorrenciaConstantes.Headers.TIPO_MENSAGEM, tipoMensagem);
        headerKafka.put(RecorrenciaConstantes.Headers.OPERACAO, operacao);
        headerKafka.put(RecorrenciaConstantes.Headers.PSP_EMISSOR, PAGADOR);
        headerKafka.put(RecorrenciaConstantes.Headers.ID_IDEMPOTENCIA, idIdempotencia);

        headerKafka.put(KafkaHeaders.KEY, mensagemKey);
        var topico = appConfig.getKafka().getProducer().getIcomPainEnvio().getTopico();
        return new EventoResponseDTO(payload, headerKafka, topico);
    }

    public EventoResponseDTO criarEventoPain14IcomEnvio(Pain014Dto pain014Dto) {
        var headerKafka = new HashMap<String, String>();
        headerKafka.put(RecorrenciaConstantes.Headers.TIPO_MENSAGEM, "PAIN014");
        headerKafka.put(RecorrenciaConstantes.Headers.PSP_EMISSOR, PAGADOR);
        headerKafka.put(RecorrenciaConstantes.Headers.ID_IDEMPOTENCIA, pain014Dto.getIdFimAFimOriginal());
        headerKafka.put(KafkaHeaders.KEY, pain014Dto.getIdFimAFimOriginal());

        var topico = appConfig.getKafka().getProducer().getIcomPainEnvio().getTopico();
        return new EventoResponseDTO(pain014Dto, headerKafka, topico);
    }

    public EventoResponseDTO criarEventoCamt029IcomEnvio(Camt029Dto camt029Dto) {
        var headerKafka = new HashMap<String, String>();
        headerKafka.put(RecorrenciaConstantes.Headers.TIPO_MENSAGEM, "CAMT029");
        headerKafka.put(RecorrenciaConstantes.Headers.PSP_EMISSOR, PAGADOR);

        headerKafka.put(KafkaHeaders.KEY, camt029Dto.getIdCancelamentoAgendamentoOriginal());
        var topico = appConfig.getKafka().getProducer().getIcomCamtEnvio().getTopico();
        return new EventoResponseDTO(camt029Dto, headerKafka, topico);
    }

    public EventoResponseDTO criarEventoCamt055(Camt055Dto camt055Dto) {
        var headerKafka = new HashMap<String, String>();

        headerKafka.put(RecorrenciaConstantes.Headers.TIPO_MENSAGEM, "CAMT55");
        headerKafka.put(RecorrenciaConstantes.Headers.PSP_EMISSOR, PAGADOR);
        headerKafka.put(RecorrenciaConstantes.Headers.ID_IDEMPOTENCIA, camt055Dto.getIdCancelamentoAgendamento());

        headerKafka.put(KafkaHeaders.KEY, camt055Dto.getIdFimAFimOriginal());
        var topico = appConfig.getKafka().getProducer().getIcomCamtEnvio().getTopico();
        return new EventoResponseDTO(camt055Dto, headerKafka, topico);
    }

}
