package io.sicredi.spirecorrencia.api.automatico.autorizacao;

import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.framework.web.spring.exception.NotFoundException;
import br.com.sicredi.spi.dto.Pain012Dto;
import br.com.sicredi.spi.entities.type.MotivoRejeicaoPain012;
import br.com.sicredi.spi.entities.type.StatusRecorrenciaPain012;
import feign.RetryableException;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentTransaction;
import io.sicredi.spirecorrencia.api.RecorrenciaConstantes;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrenciaRepository;
import io.sicredi.spirecorrencia.api.automatico.enums.*;
import io.sicredi.spirecorrencia.api.consulta.PaginacaoDTO;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;
import io.sicredi.spirecorrencia.api.idempotente.CriaResponseStrategyFactory;
import io.sicredi.spirecorrencia.api.idempotente.EventoResponseFactory;
import io.sicredi.spirecorrencia.api.idempotente.OperacaoRequest;
import io.sicredi.spirecorrencia.api.idempotente.TipoResponseIdempotente;
import io.sicredi.spirecorrencia.api.metrica.MetricaCounter;
import io.sicredi.spirecorrencia.api.metrica.RegistraMetricaService;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoUtils;
import io.sicredi.spirecorrencia.api.protocolo.CanaisDigitaisProtocoloInfoInternalApiClient;
import io.sicredi.spirecorrencia.api.utils.RecorrenciaMdc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.*;
import static io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusAutorizacao.APROVADA;
import static io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusAutorizacao.CANCELADA;
import static io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusSolicitacaoAutorizacao.ACEITA;
import static io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusSolicitacaoAutorizacao.PENDENTE_CONFIRMACAO;
import static io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode.AUTORIZACAO_NAO_ENCONTRADA_RETORNO_BACEN;
import static io.sicredi.spirecorrencia.api.idempotente.OperacaoRequest.criarOperacaoRequest;
import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO.InformacaoAdicional.of;
import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO.TipoTemplate.*;
import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoInformacaoAdicional.DOCUMENTO_PAGADOR;
import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoInformacaoAdicional.NOME_RECEBEDOR;

@Slf4j
@Service
@RequiredArgsConstructor
class AutorizacaoServiceImpl implements AutorizacaoService {

    private final EventoResponseFactory eventoResponseFactory;
    private final RegistraMetricaService registraMetricaService;
    private final RecorrenciaAutorizacaoRepository autorizacaoRepository;
    private final CriaResponseStrategyFactory<OperacaoRequest> criaResponseStrategyFactory;
    private final SolicitacaoAutorizacaoRecorrenciaRepository solicitacaoRepository;
    private final RecorrenciaAutorizacaoCancelamentoRepository autorizacaoCancelamentoRepository;
    private final CanaisDigitaisProtocoloInfoInternalApiClient canaisDigitaisProtocoloInfoInternalApiClient;

    @Override
    public AutorizacaoResponseWrapper consultarTodas(ConsultaAutorizacaoRequest request) {
        final var pageable = PageRequest.of(
                request.getNumeroPagina(),
                request.getTamanhoPagina(),
                Sort.by(Sort.Direction.ASC, RecorrenciaConstantes.DATA_CRIACAO_RECORRENCIA)
        );

        final var pageAutorizacao = autorizacaoRepository.findAllByFiltros(request, pageable);

        final var listaAutorizacaoResponse = pageAutorizacao.getContent().stream()
                .map(AutorizacaoResponse::fromListagem)
                .toList();

        return new AutorizacaoResponseWrapper(
                listaAutorizacaoResponse,
                PaginacaoDTO.fromPage(pageAutorizacao));
    }

    @Override
    public AutorizacaoResponse consultarDetalhes(Long oidRecorrenciaAutorizacao) {
        var autorizacao = autorizacaoRepository.consultarPorIdentificadorRecorrencia(oidRecorrenciaAutorizacao)
                .orElseThrow(() -> new NotFoundException(AppExceptionCode.AUTORIZACAO_NAO_ENCONTRADA));

        RecorrenciaAutorizacaoCancelamento cancelamento = Optional.ofNullable(autorizacao.getTipoStatus())
                .filter(CANCELADA::equals)
                .flatMap(x -> autorizacaoCancelamentoRepository
                        .findFirstByIdRecorrenciaAndTipoStatusOrderByDataAlteracaoRegistroDesc(
                                autorizacao.getIdRecorrencia(),
                                TipoStatusCancelamentoAutorizacao.ACEITA))
                .orElse(null);

        return AutorizacaoResponse.fromDetalhes(autorizacao, cancelamento);
    }

    @IdempotentTransaction
    public IdempotentResponse<?> processarRecebimentoPain012Bacen(IdempotentRequest<Pain012Dto> request) {
        try {
            Pain012Dto dto = request.getValue();

            String idRecorrencia = dto.getIdRecorrencia();

            MDC.put(RecorrenciaMdc.ID_RECORRENCIA.getChave(), idRecorrencia);

            log.debug("(Pain012) Processamento de resposta do PSP do Recebedor.");

            registrarMetricaRecebimentoPain012(dto);

            List<RecorrenciaAutorizacao> autorizacoes = autorizacaoRepository.findAllByIdRecorrenciaOrderByDataAlteracaoRegistroDesc(idRecorrencia);

            if (Boolean.TRUE.equals(dto.getStatus())) {
                StatusRecorrenciaPain012 statusRecorrencia = StatusRecorrenciaPain012.of(dto.getStatusRecorrencia());
                log.debug("(Pain012) Processando resposta com ACEITA do PSP do Recebedor. Status: {}", statusRecorrencia);
                return switch (statusRecorrencia) {
                    case CANCELADA -> {
                        RecorrenciaAutorizacao autorizacao = buscarAutorizacaoPorStatus(autorizacoes, APROVADA);
                        yield processarRespostaBacenCancelamentoAutorizacao(request, autorizacao, false);
                    }
                    case CONFIRMADO_USUARIO_PAGADOR -> {
                        RecorrenciaAutorizacao autorizacao = buscarAutorizacaoPorStatus(autorizacoes, TipoStatusAutorizacao.CRIADA);
                        yield processarRespostaBacenConfirmacaoAutorizacao(request, autorizacao, false);
                    }
                    case PENDENTE_CONFIRMACAO -> {
                        log.warn("(Pain012) Processando solicitação PENDENTE_CONFIRMACAO ignorada");
                        yield IdempotentResponse.builder().build();
                    }
                };
            }

            log.info("(Pain012) Processando resposta NÃO ACEITA do PSP do Recebedor. Motivo de rejeição: {}", dto.getMotivoRejeicao());

            RecorrenciaAutorizacao autorizacao = autorizacoes.stream()
                    .filter(aut -> isCancelamentoAutorizacao(aut) || isConfirmacaoAutorizacao(aut))
                    .findFirst()
                    .orElseThrow(() -> new TechnicalException(AUTORIZACAO_NAO_ENCONTRADA_RETORNO_BACEN));

            if (isCancelamentoAutorizacao(autorizacao)) {
                return processarRespostaBacenCancelamentoAutorizacao(request, autorizacao, true);
            }

            return processarRespostaBacenConfirmacaoAutorizacao(request, autorizacao, true);
        } finally {
            MDC.remove(RecorrenciaMdc.ID_RECORRENCIA.getChave());
        }

    }

    @Override
    @IdempotentTransaction
    public IdempotentResponse<?> processarRetornoAutorizacaoAposEnvioBacen(IdempotentRequest<Pain012Dto> request) {
        Pain012Dto pain012 = request.getValue();
        MDC.put(RecorrenciaMdc.ID_RECORRENCIA.getChave(), pain012.getIdRecorrencia());

        try {
            log.debug("(Pain012) Iniciando processamento de callback de autorização após envio para o PSP do Recebedor.");

            var autorizacao = autorizacaoRepository.findByIdInformacaoStatusEnvio(pain012.getIdInformacaoStatus())
                    .orElseThrow(() -> new TechnicalException(AUTORIZACAO_NAO_ENCONTRADA_RETORNO_BACEN));

            if (Boolean.FALSE.equals(pain012.getStatus())) {
                log.debug("(Pain012) Processando callback de autorização NÃO ACEITA. Motivo: {}", pain012.getMotivoRejeicao());
                processarAutorizacaoNaoAceitaAposEnvioBacen(autorizacao);
            }
            if (Boolean.TRUE.equals(pain012.getStatus()) && !APROVADA.equals(autorizacao.getTipoStatus())) {
                log.debug("(Pain012) Processando callback de autorização ACEITA.");
                processarAutorizacaoAceitaAposEnvioBacen(autorizacao);
            }

            var protocoloDTO = obterDadosProtocolo(autorizacao.getTipoJornada(), pain012.getIdInformacaoStatus());
            return criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)
                    .criarResponseIdempotentSucesso(protocoloDTO, request.getTransactionId(), request.getHeaders(), Collections.emptyList());
        } finally {
            MDC.remove(RecorrenciaMdc.ID_RECORRENCIA.getChave());
        }
    }

    @Override
    @IdempotentTransaction
    public IdempotentResponse<?> processarRetornoPedidoCancelamento(IdempotentRequest<Pain012Dto> request) {
        throw new TechnicalException("Funcionalidade não implementada");
    }

    private void processarAutorizacaoNaoAceitaAposEnvioBacen(RecorrenciaAutorizacao autorizacao) {
        autorizacao.setTipoStatus(TipoStatusAutorizacao.REJEITADA);
        autorizacao.setTipoSubStatus(null);
        autorizacaoRepository.save(autorizacao);

        if (TipoJornada.JORNADA_1.name().equals(autorizacao.getTipoJornada())) {
            solicitacaoRepository.atualizaSubStatusSeConfirmadaEAguardandoEnvioPorIdRecorrencia(
                    autorizacao.getIdRecorrencia(),
                    TipoStatusSolicitacaoAutorizacao.REJEITADA,
                    null
            );
        }
    }

    private void processarAutorizacaoAceitaAposEnvioBacen(RecorrenciaAutorizacao autorizacao) {
        try {
            autorizacaoRepository.atualizaSubStatusSeCriadaEAguardandoEnvio(
                    autorizacao.getOidRecorrenciaAutorizacao(),
                    TipoSubStatus.AGUARDANDO_RETORNO.name(),
                    LocalDateTime.now()
            );

            if (TipoJornada.JORNADA_1.name().equals(autorizacao.getTipoJornada())) {
                solicitacaoRepository.atualizaSubStatusSeConfirmadaEAguardandoEnvioPorIdRecorrencia(
                        autorizacao.getIdRecorrencia(),
                        TipoStatusSolicitacaoAutorizacao.CONFIRMADA,
                        TipoSubStatus.AGUARDANDO_RETORNO.name()
                );
            }
        } catch (Exception ex) {
            log.error("Erro ao atualizar substatus de autorização e/ou solicitação: ID_RECORRENCIA: {}", autorizacao.getIdRecorrencia(), ex);
            throw new TechnicalException(ex);
        }
    }

    private IdempotentResponse<?> processarRespostaBacenConfirmacaoAutorizacao(IdempotentRequest<Pain012Dto> request, RecorrenciaAutorizacao autorizacaoSelecionada, boolean comRejeicao) {
        var pain012Dto = request.getValue();

        if (comRejeicao) {
            processarRejeicaoConfirmacao(autorizacaoSelecionada, pain012Dto);
            log.info("(Pain012) Processamento de confirmação de solicitação de autorização realizado com sucesso. Motivo de rejeição: {}", pain012Dto.getMotivoRejeicao());
            var evento = eventoResponseFactory.criarEventoNotificacao(criarNotificacaoConfirmacaoCancelamentoEAutorizacao(autorizacaoSelecionada, AUTOMATICO_AUTORIZACAO_CONFIRMADA_PAGADOR_FALHA_NAO_RESPONDIDA_OU_CANCELADA_RECEBEDOR));
            return criaResponseStrategyFactory.criar(TipoResponseIdempotente.OPERACAO).criarResponseIdempotentSucesso(
                    criarOperacaoRequest(TipoMensagem.PAIN012, pain012Dto.getMotivoRejeicao()),
                    request.getTransactionId(),
                    request.getHeaders(),
                    List.of(evento)
            );
        }

        processarAprovacaoConfirmacao(autorizacaoSelecionada, pain012Dto);
        log.debug("(Pain012) Processamento de confirmação de solicitação de autorização realizado com sucesso.");

        var evento = eventoResponseFactory.criarEventoNotificacao(criarNotificacaoConfirmacaoCancelamentoEAutorizacao(autorizacaoSelecionada, AUTOMATICO_AUTORIZACAO_CONFIRMADA_SUCESSO));

        return criaResponseStrategyFactory.criar(TipoResponseIdempotente.OPERACAO).criarResponseIdempotentSucesso(
                criarOperacaoRequest(TipoMensagem.PAIN012, null),
                request.getTransactionId(),
                request.getHeaders(),
                List.of(evento)
        );
    }

    private IdempotentResponse<?> processarRespostaBacenCancelamentoAutorizacao(IdempotentRequest<Pain012Dto> request, RecorrenciaAutorizacao autorizacaoSelecionada, boolean comRejeicao) {
        var pain012Dto = request.getValue();

        final String idRecorrencia = pain012Dto.getIdRecorrencia();
        final LocalDateTime agora = LocalDateTime.now();

        if (comRejeicao) {
            processarRejeicaoCancelamento(pain012Dto, autorizacaoSelecionada, idRecorrencia, agora);

            log.info("(Pain012) Processamento de rejeição de cancelamento realizado com sucesso. Motivo de rejeição: {}", pain012Dto.getMotivoRejeicao());
            var evento = eventoResponseFactory.criarEventoNotificacao(criarNotificacaoConfirmacaoCancelamentoEAutorizacao(autorizacaoSelecionada, AUTOMATICO_AUTORIZACAO_PEDIDO_CANCELAMENTO_NEGADO));
            return criaResponseStrategyFactory.criar(TipoResponseIdempotente.OPERACAO).criarResponseIdempotentSucesso(
                    criarOperacaoRequest(TipoMensagem.PAIN012, pain012Dto.getMotivoRejeicao()),
                    request.getTransactionId(),
                    request.getHeaders(),
                    List.of(evento)
            );
        }

        processarAprovacaoCancelamento(pain012Dto, autorizacaoSelecionada, idRecorrencia, agora);
        log.info("(Pain012) Processamento de cancelamento realizado com sucesso.");

        // TODO: Consultar todos os agendamentos e realizar o cancelamento dos débitos via CAMNT055.

        var evento = eventoResponseFactory.criarEventoNotificacao(criarNotificacaoConfirmacaoCancelamentoEAutorizacao(autorizacaoSelecionada, AUTOMATICO_AUTORIZACAO_PEDIDO_CANCELAMENTO_SUCESSO));

        return criaResponseStrategyFactory.criar(TipoResponseIdempotente.OPERACAO).criarResponseIdempotentSucesso(
                criarOperacaoRequest(TipoMensagem.PAIN012, null),
                request.getTransactionId(),
                request.getHeaders(),
                List.of(evento)
        );
    }

    private void registrarMetricaRecebimentoPain012(Pain012Dto pain012Dto) {
        var metrica = new MetricaCounter(
                "pix_automatico_recebimento_pain012",
                "Métrica responsável para registrar informações do recebimento de PAIN012 do BACEN.")
                .adicionarTag("motivo_rejeicao", pain012Dto.getMotivoRejeicao())
                .adicionarTag("status_recorrencia", pain012Dto.getStatusRecorrencia());

        registraMetricaService.registrar(metrica);
    }

    private void processarRejeicaoCancelamento(Pain012Dto dto, RecorrenciaAutorizacao autorizacao, String idRecorrencia, LocalDateTime agora) {
        try {
            autorizacao.setTipoSubStatus(null);
            autorizacaoRepository.save(autorizacao);

            autorizacaoCancelamentoRepository.atualizaStatusIdInformacaoStatusEMotivoRejeicao(idRecorrencia, TipoStatusCancelamentoAutorizacao.REJEITADA, dto.getIdInformacaoStatus(), dto.getMotivoRejeicao(), agora);
        } catch (Exception ex) {
            log.error("Erro ao processar rejeição do cancelamento. ID_RECORRENCIA: {}", autorizacao.getIdRecorrencia(), ex);
            throw new TechnicalException("Erro ao processar rejeição de cancelamento", ex);
        }
    }

    private void processarAprovacaoCancelamento(Pain012Dto dto, RecorrenciaAutorizacao autorizacao, String idRecorrencia, LocalDateTime agora) {
        try {
            autorizacao.setTipoStatus(CANCELADA);
            autorizacao.setTipoSubStatus(null);
            autorizacaoRepository.save(autorizacao);

            autorizacaoCancelamentoRepository.atualizaStatusEIdInformacaoStatus(idRecorrencia, TipoStatusCancelamentoAutorizacao.ACEITA, dto.getIdInformacaoStatus(), agora);
        } catch (Exception ex) {
            log.error("Erro ao processar aprovação do cancelamento. ID_RECORRENCIA: {}", autorizacao.getIdRecorrencia(), ex);
            throw new TechnicalException("Erro ao processar aprovação de cancelamento", ex);
        }
    }

    private void processarRejeicaoConfirmacao(RecorrenciaAutorizacao autorizacao, Pain012Dto dto) {
        try {
            autorizacao.setTipoStatus(TipoStatusAutorizacao.REJEITADA);
            autorizacao.setTipoSubStatus(null);
            autorizacao.setMotivoRejeicao(MotivoRejeicaoPain012.of(dto.getMotivoRejeicao()).name());
            autorizacao.setIdInformacaoStatusRecebimento(dto.getIdInformacaoStatus());
            autorizacaoRepository.save(autorizacao);

            if (TipoJornada.JORNADA_1.name().equals(autorizacao.getTipoJornada())) {
                solicitacaoRepository.atualizaStatusSeStatusAtualForConfirmadoPorIdRecorrencia(autorizacao.getIdRecorrencia(), PENDENTE_CONFIRMACAO, null, LocalDateTime.now());
            }
        } catch (Exception ex) {
            log.error("Ocorreu um erro ao atualizar dados da autorização de recorrencia: ID_RECORRENCIA: {}", autorizacao.getIdRecorrencia(), ex);
            throw new TechnicalException(ex);
        }
    }

    private void processarAprovacaoConfirmacao(RecorrenciaAutorizacao autorizacao, Pain012Dto dto) {
        try {
            autorizacao.setTipoStatus(APROVADA);
            autorizacao.setTipoSubStatus(null);
            autorizacao.setIdInformacaoStatusRecebimento(dto.getIdInformacaoStatus());
            autorizacaoRepository.save(autorizacao);

            if (TipoJornada.JORNADA_1.name().equals(autorizacao.getTipoJornada())) {
                solicitacaoRepository.atualizaStatusSeStatusAtualForConfirmadoPorIdRecorrencia(autorizacao.getIdRecorrencia(), ACEITA, null, LocalDateTime.now());
            }
        } catch (Exception ex) {
            log.error("Ocorreu um erro ao atualizar dados da autorização de recorrencia: ID_RECORRENCIA: {}", autorizacao.getIdRecorrencia(), ex);
            throw new TechnicalException(ex);
        }
    }

    private ProtocoloDTO obterDadosProtocolo(String tipoJornada, String idInformacaoStatus) {
        try {
            String codigoPlataformaCanais = obterCodigoProtocoloPorTipoJornada(tipoJornada);

            return Optional.ofNullable(canaisDigitaisProtocoloInfoInternalApiClient.consultaProtocoloPorTipoEIdentificador(codigoPlataformaCanais, idInformacaoStatus))
                    .orElseThrow(() -> new NotFoundException("Protocolo %s não encontrado para o identificador %s".formatted(codigoPlataformaCanais, idInformacaoStatus)));
        } catch (RetryableException retryableException) {
            log.error("Erro ao consultar dados do protocolo. Motivo: TimeOut | Detalhes: {}", retryableException.getMessage());
            throw new TechnicalException(retryableException);
        }
    }

    private String obterCodigoProtocoloPorTipoJornada(String tipoJornada) {
        TipoJornada jornada = TipoJornada.valueOf(tipoJornada);

        return switch (jornada) {
            case JORNADA_1 -> CODIGO_PROTOCOLO_AUTOMATICO_PAGADOR_CONFIRMACAO_AUTORIZACAO;
            case JORNADA_2, JORNADA_4 -> CODIGO_PROTOCOLO_AUTOMATICO_PAGADOR_CADASTRO_COM_AUTENTICACAO;
            case JORNADA_3 -> CODIGO_PROTOCOLO_AUTOMATICO_PAGADOR_CADASTRO_SEM_AUTENTICACAO;
        };
    }

    private RecorrenciaAutorizacao buscarAutorizacaoPorStatus(Collection<RecorrenciaAutorizacao> autorizacoes, TipoStatusAutorizacao status) {
        return autorizacoes.stream()
                .filter(aut -> aut.getTipoStatus() == status)
                .findFirst()
                .orElseThrow(() -> new TechnicalException(AUTORIZACAO_NAO_ENCONTRADA_RETORNO_BACEN));
    }

    private NotificacaoDTO criarNotificacaoConfirmacaoCancelamentoEAutorizacao(RecorrenciaAutorizacao autorizacao, NotificacaoDTO.TipoTemplate tipoTemplate) {
        var informacoesAdicionais = List.of(
                of(NOME_RECEBEDOR, autorizacao.getNomeRecebedor()),
                of(DOCUMENTO_PAGADOR, autorizacao.getCpfCnpjPagador())
        );

        var canal = NotificacaoUtils.converterCanalParaNotificacao(autorizacao.getTipoCanalPagador(), autorizacao.getTipoSistemaPagador());

        return NotificacaoDTO.builder()
                .agencia(autorizacao.getAgenciaPagador())
                .conta(autorizacao.getContaPagador())
                .canal(canal)
                .operacao(tipoTemplate)
                .informacoesAdicionais(informacoesAdicionais)
                .build();
    }

    private boolean isCancelamentoAutorizacao(RecorrenciaAutorizacao autorizacao) {
        return APROVADA.equals(autorizacao.getTipoStatus()) &&
               TipoSubStatus.AGUARDANDO_CANCELAMENTO.name().equals(autorizacao.getTipoSubStatus());
    }

    private boolean isConfirmacaoAutorizacao(RecorrenciaAutorizacao autorizacao) {
        return TipoStatusAutorizacao.CRIADA.equals(autorizacao.getTipoStatus());
    }

    @Override
    public RecorrenciaAutorizacao salvar(RecorrenciaAutorizacao recorrenciaAutorizacao) {
        return autorizacaoRepository.save(recorrenciaAutorizacao);
    }

    @Override
    public Optional<RecorrenciaAutorizacao> consultarAutorizacaoPorIdEStatus(String idRecorrencia, TipoStatusAutorizacao status) {
        return autorizacaoRepository.findFirstByIdRecorrenciaAndTipoStatus(idRecorrencia, status);
    }

    @Override
    public List<RecorrenciaAutorizacao> findAll() {
        return autorizacaoRepository.findAll();
    }

    @Override
    public List<RecorrenciaAutorizacao> buscarComCiclosPorIdRecorrencia(String idRecorrencia) {
        return autorizacaoRepository.findWithCiclosByIdRecorrencia(idRecorrencia);
    }

    @Override
    public Optional<RecorrenciaAutorizacao> buscarPorOid(Long oid) {
        return autorizacaoRepository.findById(oid);
    }
}
