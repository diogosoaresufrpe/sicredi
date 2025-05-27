package io.sicredi.spirecorrencia.api.idempotente;

import br.com.sicredi.canaisdigitais.dto.protocolo.ComandoProtocoloDTO;
import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import br.com.sicredi.canaisdigitais.dto.transacao.OutTransacaoDTO;
import br.com.sicredi.canaisdigitais.enums.AcaoProtocoloEnum;
import br.com.sicredi.canaisdigitais.enums.TipoRetornoTransacaoEnum;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentEvent;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentHeaders;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.CadastroAutorizacaoRequest;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.ConfirmacaoAutorizacaoRequestDTO;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.deadletter.DeadLetterRequest;
import io.sicredi.spirecorrencia.api.messasing.MessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
final class CriaResponseComProtocoloStrategy implements CriaResponseStrategy<IdempotenteRequest> {

    private final EventoResponseFactory eventoResponseFactory;
    private final MessageProducer messageProducer;
    private final AppConfig appConfig;
    private static final String ERRO_VALIDACAO_CONSTRAINT_DEFAULT = "Não foi possível processar sua solicitação. Por favor, tente novamente mais tarde.";

    @Override
    public TipoResponseIdempotente obterTipoResponse() {
        return TipoResponseIdempotente.ORDEM_COM_PROTOCOLO;
    }

    @Override
    public IdempotentResponse<?> criarResponseIdempotentSucesso(IdempotenteRequest ordemRequest,
                                                                String transactionId,
                                                                Map<String, String> headers,
                                                                List<EventoResponseDTO> listaEventos) {
        log.debug("Sucesso ao realizar a transação. Identificador da transação: {}", transactionId);
        return criarResponseSucesso(ordemRequest, headers, listaEventos);
    }

    @Override
    public IdempotentResponse<?> criarResponseIdempotentErro(IdempotenteRequest ordemRequest,
                                                             String transactionId,
                                                             ErroDTO erro) {
        logTransacao("Falha ao realizar validações. Identificador da transação: %s | Detalhes do erro: %s", transactionId, erro.mensagemErro());
        return criarResponseErro(ordemRequest, transactionId, erro);
    }

    @Override
    public IdempotentResponse<?> criarResponseIdempotentSucesso(ProtocoloDTO protocoloDTO, String transactionId, Map<String, String> headers, List<EventoResponseDTO> listaEventos) {
        log.debug("Sucesso ao realizar a transação. Identificador da transação: {}", transactionId);

        return criarResponseSucesso(protocoloDTO, headers, listaEventos);
    }

    private IdempotentResponse<?> criarResponseSucesso(ProtocoloDTO protocoloDTO, Map<String, String> headers, List<EventoResponseDTO> listaEventos) {
        var listaIdempotentEvent = new ArrayList<IdempotentEvent<?>>();

        adicionarEventoSinalizacaoSucessoProtocolo(protocoloDTO, listaIdempotentEvent);

        Stream.ofNullable(listaEventos)
                .flatMap(List::stream)
                .map(evento -> IdempotentEvent.builder()
                        .headers(evento.headers())
                        .topic(evento.topic())
                        .value(evento.mensagemJson())
                        .build())
                .forEach(listaIdempotentEvent::add);

        return IdempotentResponse.builder()
                .events(listaIdempotentEvent)
                .headers(headers)
                .errorResponse(false)
                .build();
    }

    @Override
    public void criarResponseReprocessamentoIdempotentTransactionDuplicated(DeadLetterRequest reprocessamentoRequest, ErroDTO erro) {
        log.error("(DLT - Idempotent Transaction Duplicated) Falha ao realizar o processamento. Detalhes do erro: {}", erro.mensagemErro());
        processarErroDLT(reprocessamentoRequest, erro);
    }

    @Override
    public void criarResponseReprocessamentoOutrasExceptions(DeadLetterRequest reprocessamentoRequest, ErroDTO erro) {
        log.error("(DLT - Outras Exceptions) Falha ao realizar o processamento. Detalhes do erro: {}", erro.mensagemErro());
        processarErroDLT(reprocessamentoRequest, erro);
    }

    private void logTransacao(String mensagemErro, String identificadorTransacao, String detalhesErro) {
        var mensagemLog = String.format(mensagemErro, identificadorTransacao, detalhesErro);
        log.error(mensagemLog);
    }

    private IdempotentResponse<?> criarResponseSucesso(IdempotenteRequest ordemRequest, Map<String, String> headers, List<EventoResponseDTO> listaEventos) {
        var listaIdempotentEvent = new ArrayList<IdempotentEvent<?>>();
        adicionarEventoSinalizacaoRecepcaoProtocolo(ordemRequest, listaIdempotentEvent);

        if (Boolean.TRUE.equals(ordemRequest.deveSinalizacaoSucessoProtocolo())) {
            adicionarEventoSinalizacaoSucessoProtocolo(ordemRequest.getProtocoloDTO(), listaIdempotentEvent);
        }

        Stream.ofNullable(listaEventos)
                .flatMap(List::stream)
                .map(evento -> IdempotentEvent.builder()
                        .headers(evento.headers())
                        .topic(evento.topic())
                        .value(evento.mensagemJson())
                        .build())
                .forEach(listaIdempotentEvent::add);

        return IdempotentResponse.builder()
                .events(listaIdempotentEvent)
                .headers(headers)
                .errorResponse(false)
                .build();
    }

    private IdempotentResponse<?> criarResponseErro(IdempotenteRequest ordemRequest,
                                                    String transactionId,
                                                    ErroDTO erro) {

        var headersResponse = new HashMap<String, String>();
        headersResponse.put(IdempotentHeaders.TRANSACTION_ID, transactionId);

        var listaIdempotentEvent = new ArrayList<IdempotentEvent<?>>();
        adicionarEventoSinalizacaoRecepcaoProtocolo(ordemRequest, listaIdempotentEvent);
        adicionarEventoSinalizacaoErroProcessamentoProtocolo(ordemRequest, erro, erro.tipoRetornoTransacaoEnum(), listaIdempotentEvent);

        Stream.ofNullable(erro.listaEventos())
                .flatMap(List::stream)
                .map(evento -> IdempotentEvent.builder()
                        .headers(evento.headers())
                        .topic(evento.topic())
                        .value(evento.mensagemJson())
                        .build())
                .forEach(listaIdempotentEvent::add);

        return IdempotentResponse.builder()
                .headers(headersResponse)
                .events(listaIdempotentEvent)
                .errorResponse(true)
                .build();
    }

    private void adicionarEventoSinalizacaoSucessoProtocolo(ProtocoloDTO protocoloDTO,
                                                            List<IdempotentEvent<?>> listaIdempotentEvent) {
        var outTransacao = criarOutTransacaoSucessoDTO();
        var comandoProtocolo = criarComandoProtocoloDTO(protocoloDTO, outTransacao, AcaoProtocoloEnum.CONFIRMAR_PROCESSAMENTO, ZonedDateTime.now());

        Optional.of(eventoResponseFactory.criarEventoAtualizacaoProtocolo(comandoProtocolo))
                .map(evento -> IdempotentEvent.builder()
                        .headers(evento.headers())
                        .topic(evento.topic())
                        .value(evento.mensagemJson())
                        .build())
                .ifPresent(listaIdempotentEvent::add);
    }

    private void adicionarEventoSinalizacaoRecepcaoProtocolo(IdempotenteRequest ordemRequest,
                                                             List<IdempotentEvent<?>> listaIdempotentEvent) {
        var comandoProtocolo = criarComandoProtocoloDTO(ordemRequest.getProtocoloDTO(), null, AcaoProtocoloEnum.SINALIZAR_RECEPCAO, ordemRequest.getDataHoraRecepcao());

        Optional.of(eventoResponseFactory.criarEventoAtualizacaoProtocolo(comandoProtocolo))
                .map(evento -> IdempotentEvent.builder()
                        .headers(evento.headers())
                        .topic(evento.topic())
                        .value(evento.mensagemJson())
                        .build())
                .ifPresent(listaIdempotentEvent::add);
    }

    private void adicionarEventoSinalizacaoErroProcessamentoProtocolo(IdempotenteRequest ordemRequest,
                                                                      ErroDTO erro,
                                                                      TipoRetornoTransacaoEnum tipoRetornoTransacao,
                                                                      List<IdempotentEvent<?>> listaIdempotentEvent) {
        var payload = criarOutTransacaoDTO(erro, tipoRetornoTransacao);
        var comandoProtocolo = criarComandoProtocoloDTO(ordemRequest.getProtocoloDTO(), payload, AcaoProtocoloEnum.SINALIZAR_ERRO_PROCESSAMENTO, null);

        Optional.of(eventoResponseFactory.criarEventoAtualizacaoProtocolo(comandoProtocolo))
                .map(evento -> IdempotentEvent.builder()
                        .headers(evento.headers())
                        .topic(evento.topic())
                        .value(evento.mensagemJson())
                        .build())
                .ifPresent(listaIdempotentEvent::add);
    }

    private void processarErroDLT(DeadLetterRequest deadLetterRequest, ErroDTO erro) {
        var protocoloDTO = deadLetterRequest.protocoloDTO();
        if (protocoloDTO != null) {

            var comandoRecepcaoProtocolo = criarComandoProtocoloDTO(
                    protocoloDTO,
                    null,
                    AcaoProtocoloEnum.SINALIZAR_RECEPCAO,
                    ZonedDateTime.now()
            );

            var payloadComandoErro = criarOutTransacaoDTO(
                    erro,
                    erro.tipoRetornoTransacaoEnum()
            );

            var comandoErroProtocolo = criarComandoProtocoloDTO(
                    protocoloDTO,
                    payloadComandoErro,
                    AcaoProtocoloEnum.SINALIZAR_ERRO_PROCESSAMENTO,
                    null
            );

            var topico = appConfig.getKafka().getProducer().getComandoProtocolo().getTopico();
            messageProducer.enviar(comandoRecepcaoProtocolo, topico, Map.of());
            messageProducer.enviar(comandoErroProtocolo, topico, Map.of());
        }
    }

    private OutTransacaoDTO criarOutTransacaoDTO(ErroDTO erro, TipoRetornoTransacaoEnum tipoRetornoTransacao) {
        String mensagemErro = ERRO_VALIDACAO_CONSTRAINT_DEFAULT;
        if (TipoRetornoTransacaoEnum.ERRO_NEGOCIO == tipoRetornoTransacao && StringUtils.isNotBlank(erro.mensagemErro())) {
            mensagemErro = erro.mensagemErro();
        }

        var outTransacaoDTO = new OutTransacaoDTO();
        outTransacaoDTO.setMensagem(mensagemErro);
        outTransacaoDTO.setTipoRetorno(tipoRetornoTransacao);
        outTransacaoDTO.setCodigo(erro.codigoErro().name());
        return outTransacaoDTO;
    }

    private OutTransacaoDTO criarOutTransacaoSucessoDTO() {
        var outTransacaoDTO = new OutTransacaoDTO();
        outTransacaoDTO.setTipoRetorno(TipoRetornoTransacaoEnum.SUCESSO);
        return outTransacaoDTO;
    }

    private ComandoProtocoloDTO criarComandoProtocoloDTO(ProtocoloDTO protocoloDTO, Object payload, AcaoProtocoloEnum acaoProtocolo, ZonedDateTime dataHoraRecepcao) {
        var comandoProtocoloDTO = new ComandoProtocoloDTO();
        comandoProtocoloDTO.setDataHora(dataHoraRecepcao != null ? dataHoraRecepcao : ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")));
        comandoProtocoloDTO.setOidProtocolo(protocoloDTO.getIdProtocolo());
        comandoProtocoloDTO.setIdentificadorTransacao(protocoloDTO.getIdentificadorTransacao());
        comandoProtocoloDTO.setCodigoTipoTransacao(protocoloDTO.getCodigoTipoTransacao());
        comandoProtocoloDTO.setCooperativa(protocoloDTO.getCooperativa());
        comandoProtocoloDTO.setConta(protocoloDTO.getConta());
        comandoProtocoloDTO.setCodigoCanal(protocoloDTO.getCodigoCanal());
        comandoProtocoloDTO.setAcao(acaoProtocolo);
        comandoProtocoloDTO.setPayload(payload);
        return comandoProtocoloDTO;
    }

}
