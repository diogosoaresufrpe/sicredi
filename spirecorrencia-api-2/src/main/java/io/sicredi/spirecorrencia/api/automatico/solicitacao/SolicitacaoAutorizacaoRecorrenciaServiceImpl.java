package io.sicredi.spirecorrencia.api.automatico.solicitacao;

import br.com.sicredi.canaisdigitais.enums.OrigemEnum;
import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.framework.web.spring.exception.NotFoundException;
import br.com.sicredi.spi.dto.DetalheRecorrenciaPain009Dto;
import br.com.sicredi.spi.dto.Pain009Dto;
import br.com.sicredi.spi.dto.Pain012Dto;
import br.com.sicredi.spi.entities.type.MotivoRejeicaoPain012;
import br.com.sicredi.spi.util.SpiUtil;
import br.com.sicredi.spi.util.type.TipoId;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoPessoaEnum;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentTransaction;
import io.sicredi.spirecorrencia.api.RecorrenciaConstantes;
import io.sicredi.spirecorrencia.api.accountdata.AccountDataService;
import io.sicredi.spirecorrencia.api.accountdata.DadosContaResponseDTO;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrencia;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrenciaRepository;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoMensagem;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusSolicitacaoAutorizacao;
import io.sicredi.spirecorrencia.api.automatico.pain.Pain012ResponseFactory;
import io.sicredi.spirecorrencia.api.consulta.PaginacaoDTO;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;
import io.sicredi.spirecorrencia.api.gestentconector.GestentConectorService;
import io.sicredi.spirecorrencia.api.idempotente.*;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoUtils;
import io.sicredi.spirecorrencia.api.utils.RecorrenciaMdc;
import io.sicredi.spirecorrencia.api.utils.SystemDateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static br.com.sicredi.canaisdigitais.enums.OrigemEnum.FISITAL;
import static br.com.sicredi.spi.entities.type.MotivoRejeicaoPain012.CPF_CNPJ_USUARIO_PAGADOR_NAO_LOCALIZADO;
import static br.com.sicredi.spi.entities.type.TipoSituacaoPain009.*;
import static io.sicredi.spirecorrencia.api.accountdata.AccountDataService.DIGITAL;
import static io.sicredi.spirecorrencia.api.idempotente.OperacaoRequest.criarOperacaoRequest;
import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO.InformacaoAdicional.of;
import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoInformacaoAdicional.*;

@Slf4j
@Service
@RequiredArgsConstructor
class SolicitacaoAutorizacaoRecorrenciaServiceImpl implements SolicitacaoAutorizacaoRecorrenciaService {

    private static final String HEADER_OPERACAO = "RECORRENCIA_SOLICITACAO";

    private final AccountDataService accountDataService;
    private final EventoResponseFactory eventoResponseFactory;
    private final GestentConectorService gestentConectorService;
    private final SolicitacaoAutorizacaoRecorrenciaRepository solicitacaoRepository;
    private final CriaResponseStrategyFactory<OperacaoRequest> criaResponseStrategyFactory;

    @Override
    public SolicitacaoAutorizacaoRecorrenciaResponseWrapper consultarTodas(final ConsultaSolicitacaoAutorizacaoRecorrenciaRequest request) {
        final var pageable = PageRequest.of(
                request.getNumeroPagina(),
                request.getTamanhoPagina(),
                Sort.by(Sort.Direction.ASC, RecorrenciaConstantes.DATA_CRIACAO_RECORRENCIA)
        );

        final var pageAutorizacaoSolicitacao = solicitacaoRepository.findAllByFiltros(request, pageable);

        final var listaAutorizacaoSolicitacaoResponse = pageAutorizacaoSolicitacao.getContent().stream()
                .map(SolicitacaoAutorizacaoRecorrenciaResponse::fromListagem)
                .toList();

        return new SolicitacaoAutorizacaoRecorrenciaResponseWrapper(
                listaAutorizacaoSolicitacaoResponse,
                PaginacaoDTO.fromPage(pageAutorizacaoSolicitacao));
    }

    @Override
    public SolicitacaoAutorizacaoRecorrenciaResponse consultarDetalhes(String idSolicitacaoRecorrencia) {
        var solicitacaoAutorizacaoRecorrencia = solicitacaoRepository.findById(idSolicitacaoRecorrencia)
                .orElseThrow(() -> new NotFoundException("O detalhamento de solicitação de autorização do Pix Automático não foi encontrado."));

        return SolicitacaoAutorizacaoRecorrenciaResponse.fromDetalheSolicitacao(
                solicitacaoAutorizacaoRecorrencia
        );
    }

    @Override
    @IdempotentTransaction
    public IdempotentResponse<?> processarSolicitacaoAutorizacao(IdempotentRequest<Pain009Dto> request) {

        try {
            var pain009 = request.getValue();
            MDC.put(RecorrenciaMdc.ID_RECORRENCIA.getChave(), pain009.getIdRecorrencia());
            log.debug("(Pain009) Processamento de pedido de solicitação autorização.");

            var dadosContaPagador = accountDataService.consultarConta(pain009.getCpfCnpjUsuarioPagador(), pain009.getAgenciaUsuarioPagador(), pain009.getContaUsuarioPagador());

            var codigoIbge = obterCodigoIbge(dadosContaPagador);
            var dataHoraTipoSituacaoDaRecorrencia = mapearDetalhesRecorrencia(pain009);
            var idInformacaoStatus = SpiUtil.gerarIdFimAFim(TipoId.INFORMACAO_STATUS, pain009.getParticipanteDoUsuarioPagador(), LocalDateTime.now());

            var erroValidacao = validarRegraNegocio(pain009, dadosContaPagador);
            log.debug("(Pain009) Realizado validações de regras de negócio. Erro validação de negócio: {}", erroValidacao.isPresent());

            if (erroValidacao.isPresent()) {
                log.info("(Pain009) Realizando processamento de erro de negócio. Motivo de rejeição: {}", erroValidacao.get());
                criarSolicitacaoAutorizacaoRecorrencia(pain009, dadosContaPagador, erroValidacao.get().name(), codigoIbge, dataHoraTipoSituacaoDaRecorrencia, idInformacaoStatus);
                var pain012 = Pain012ResponseFactory.fromPain009Erro(pain009, erroValidacao.get(), codigoIbge, idInformacaoStatus);
                var evento = criarEventoPain12IcomEnvio(pain012);
                return criaResponseStrategyFactory.criar(TipoResponseIdempotente.OPERACAO).criarResponseIdempotentSucesso(
                        criarOperacaoRequest(TipoMensagem.PAIN009, pain012.getMotivoRejeicao()),
                        request.getTransactionId(),
                        request.getHeaders(),
                        List.of(evento)
                );
            }

            criarSolicitacaoAutorizacaoRecorrencia(pain009, dadosContaPagador, null, codigoIbge, dataHoraTipoSituacaoDaRecorrencia, idInformacaoStatus);
            log.debug("(Pain009) Processamento realizado com sucesso, criando eventos ....");

            var pain012 = Pain012ResponseFactory.fromPain009Sucesso(pain009, dadosContaPagador, dataHoraTipoSituacaoDaRecorrencia, codigoIbge, idInformacaoStatus);
            var evento = criarEventoPain12IcomEnvio(pain012);

            return criaResponseStrategyFactory.criar(TipoResponseIdempotente.OPERACAO).criarResponseIdempotentSucesso(
                    criarOperacaoRequest(TipoMensagem.PAIN009, null),
                    request.getTransactionId(),
                    request.getHeaders(),
                    List.of(evento)
            );
        } finally {
            MDC.remove(RecorrenciaMdc.ID_RECORRENCIA.getChave());
        }
    }

    @Override
    @IdempotentTransaction
    public IdempotentResponse<?> processarRetornoBacenSolicitacaoAutorizacao(IdempotentRequest<Pain012Dto> request) {
        try {
            var pain012 = request.getValue();

            MDC.put(RecorrenciaMdc.ID_RECORRENCIA.getChave(), pain012.getIdRecorrencia());
            log.debug("(Pain012) Processamento de callback de solicitação de autorização após envio para o PSP do Recebedor.");

            var idInformacaoStatus = pain012.getIdInformacaoStatus();
            var idRecorrencia = pain012.getIdRecorrencia();


            var solicitacao = solicitacaoRepository.findByIdInformacaoStatusAndIdRecorrencia(idInformacaoStatus, idRecorrencia)
                    .orElseThrow(() -> new NotFoundException("Solicitação não encontrada para dados informados: ID_INFORMACAO_STATUS: ", idInformacaoStatus));

            if (Boolean.FALSE.equals(pain012.getStatus())) {
                log.info("(Pain012) Processando callback de solicitação de autorização NAO ACEITA após envio para o PSP do Recebedor. Motivo de rejeição: {}", pain012.getMotivoRejeicao());
                atualizarStatusSolicitacao(solicitacao, TipoStatusSolicitacaoAutorizacao.REJEITADA);
                return criaResponseStrategyFactory.criar(TipoResponseIdempotente.OPERACAO).criarResponseIdempotentSucesso(
                        criarOperacaoRequest(TipoMensagem.PAIN012, pain012.getMotivoRejeicao()),
                        request.getTransactionId(),
                        request.getHeaders(),
                        Collections.emptyList());
            }

            log.debug("(Pain012) Processando callback de solicitação de autorização ACEITA após envio para o PSP do Recebedor.");
            atualizarStatusSolicitacao(solicitacao, TipoStatusSolicitacaoAutorizacao.PENDENTE_CONFIRMACAO);

            var listaEventos = criarEventoNotificacao(solicitacao);

            return criaResponseStrategyFactory.criar(TipoResponseIdempotente.OPERACAO).criarResponseIdempotentSucesso(
                    criarOperacaoRequest(TipoMensagem.PAIN012, null),
                    request.getTransactionId(),
                    request.getHeaders(),
                    List.of(listaEventos)
            );
        } finally {
            MDC.remove(RecorrenciaMdc.ID_RECORRENCIA.getChave());
        }
    }

    private void atualizarStatusSolicitacao(SolicitacaoAutorizacaoRecorrencia solicitacao, TipoStatusSolicitacaoAutorizacao status) {
        try {
            solicitacao.setTipoStatus(status);
            solicitacaoRepository.save(solicitacao);
            log.info("Solicitação {} salva com status: {}", solicitacao.getIdInformacaoStatus(), status);
        } catch (Exception ex) {
            log.error("Ocorreu um erro ao atualizar de solicitação de recorrencia: ID_SOLICITACAO_RECORRENICA: {}", solicitacao.getIdSolicitacaoRecorrencia(), ex);
            throw new TechnicalException(ex);
        }
    }

    private NotificacaoDTO criarNotificacaoConfirmacaoAutorizacao(SolicitacaoAutorizacaoRecorrencia solicitacaoAutorizacao) {
        var nomePagador = Optional.ofNullable(solicitacaoAutorizacao.getNomeDevedor())
                .filter(StringUtils::isNotBlank)
                .orElseGet(solicitacaoAutorizacao::getNomePagador);

        var informacoesAdicionais = new ArrayList<NotificacaoDTO.InformacaoAdicional>();

        informacoesAdicionais.add(of(NOME_RECEBEDOR, solicitacaoAutorizacao.getNomeRecebedor()));
        informacoesAdicionais.add(of(DOCUMENTO_PAGADOR, solicitacaoAutorizacao.getCpfCnpjPagador()));
        informacoesAdicionais.add(of(NOME_DEVEDOR, nomePagador));
        informacoesAdicionais.add(of(DATA_EXPIRACAO_AUTORIZACAO, SystemDateUtil.formatarData(solicitacaoAutorizacao.getDataExpiracaoConfirmacaoSolicitacao())));

        Optional.ofNullable(solicitacaoAutorizacao.getDescricao())
                .filter(StringUtils::isNotBlank)
                .map(descricao -> of(OBJETO_PAGAMENTO, descricao))
                .ifPresent(informacoesAdicionais::add);

        var canal = NotificacaoUtils.converterCanalParaNotificacao(solicitacaoAutorizacao.getTipoSistemaPagador());

        return NotificacaoDTO.builder()
                .agencia(solicitacaoAutorizacao.getAgenciaPagador())
                .conta(solicitacaoAutorizacao.getContaPagador())
                .canal(canal)
                .operacao(NotificacaoDTO.TipoTemplate.AUTOMATICO_AUTORIZACAO_PENDENTE_DE_APROVACAO)
                .informacoesAdicionais(informacoesAdicionais)
                .build();
    }

    private void criarSolicitacaoAutorizacaoRecorrencia(Pain009Dto pain009, DadosContaResponseDTO conta, String motivoRejeicao, String codigoIbge, Map<String, LocalDateTime> dataHoraTipoSituacaoDaRecorrencia, String idInformacaoStatus) {
        var agencia = Optional.ofNullable(pain009.getAgenciaUsuarioPagador())
                .filter(StringUtils::isNotBlank)
                .orElseGet(() -> conta != null ? conta.coop() : null);

        SolicitacaoAutorizacaoRecorrencia solicitacaoAutorizacao = new SolicitacaoAutorizacaoRecorrencia();

        solicitacaoAutorizacao.setIdSolicitacaoRecorrencia(pain009.getIdSolicitacaoRecorrencia());
        solicitacaoAutorizacao.setCodigoMunicipioIBGE(codigoIbge);
        solicitacaoAutorizacao.setMotivoRejeicao(motivoRejeicao);
        solicitacaoAutorizacao.setIdRecorrencia(pain009.getIdRecorrencia());
        solicitacaoAutorizacao.setTipoStatus(TipoStatusSolicitacaoAutorizacao.CRIADA);
        solicitacaoAutorizacao.setTipoPessoaPagador(pain009.getCpfCnpjUsuarioPagador().length() == 11 ? TipoPessoaEnum.PF : TipoPessoaEnum.PJ);
        solicitacaoAutorizacao.setTipoFrequencia(pain009.getTipoFrequencia());
        solicitacaoAutorizacao.setValor(pain009.getValor());
        solicitacaoAutorizacao.setPisoValorMaximo(pain009.getPisoValorMaximo());
        solicitacaoAutorizacao.setCpfCnpjPagador(pain009.getCpfCnpjUsuarioPagador());
        solicitacaoAutorizacao.setAgenciaPagador(agencia);
        solicitacaoAutorizacao.setContaPagador(pain009.getContaUsuarioPagador());
        solicitacaoAutorizacao.setInstituicaoPagador(pain009.getParticipanteDoUsuarioPagador());
        solicitacaoAutorizacao.setNomeRecebedor(pain009.getNomeUsuarioRecebedor());
        solicitacaoAutorizacao.setCpfCnpjRecebedor(pain009.getCpfCnpjUsuarioRecebedor());
        solicitacaoAutorizacao.setInstituicaoRecebedor(pain009.getParticipanteDoUsuarioRecebedor());
        solicitacaoAutorizacao.setNomeDevedor(pain009.getNomeDevedor());
        solicitacaoAutorizacao.setCpfCnpjDevedor(pain009.getCpfCnpjDevedor());
        solicitacaoAutorizacao.setNumeroContrato(pain009.getNumeroContrato());
        solicitacaoAutorizacao.setDescricao(pain009.getDescricao());
        solicitacaoAutorizacao.setDataInicioConfirmacao(dataHoraTipoSituacaoDaRecorrencia.get(DATA_CRIACAO_SOLICITACAO_CONFIRMACAO.name()));
        solicitacaoAutorizacao.setDataCriacaoRecorrencia(dataHoraTipoSituacaoDaRecorrencia.get(DATA_CRIACAO_RECORRENCIA.name()));
        solicitacaoAutorizacao.setDataInicialRecorrencia(pain009.getDataInicialRecorrencia());
        solicitacaoAutorizacao.setDataExpiracaoConfirmacaoSolicitacao(dataHoraTipoSituacaoDaRecorrencia.get(DATA_EXPIRACAO_SOLICITACAO_CONFIRMACAO.name()));
        solicitacaoAutorizacao.setDataFinalRecorrencia(pain009.getDataFinalRecorrencia());
        solicitacaoAutorizacao.setIdInformacaoStatus(idInformacaoStatus);

        if (conta != null && !CPF_CNPJ_USUARIO_PAGADOR_NAO_LOCALIZADO.name().equals(motivoRejeicao)) {
            solicitacaoAutorizacao.setNomePagador(conta.titular().nome());
            solicitacaoAutorizacao.setPostoPagador(conta.posto());
            solicitacaoAutorizacao.setTipoContaPagador(conta.tipoConta().getAsCanaisPixNomeSimples());
            solicitacaoAutorizacao.setTipoSistemaPagador(DIGITAL.equals(conta.sistema()) ? FISITAL : OrigemEnum.LEGADO);
        }

        solicitacaoRepository.save(solicitacaoAutorizacao);
    }

    private EventoResponseDTO criarEventoPain12IcomEnvio(Pain012Dto pain012) {
        return eventoResponseFactory.criarEventoPain012(pain012, HEADER_OPERACAO);
    }

    private EventoResponseDTO criarEventoNotificacao(SolicitacaoAutorizacaoRecorrencia solicitacao) {
        log.info("Evento de notificação criado para solicitação. ID_SOLICITACAO_RECORRENCIA: {}", solicitacao.getIdSolicitacaoRecorrencia());
        return eventoResponseFactory.criarEventoNotificacao(criarNotificacaoConfirmacaoAutorizacao(solicitacao));
    }

    private Optional<MotivoRejeicaoPain012> validarRegraNegocio(Pain009Dto pain009, DadosContaResponseDTO conta) {
        return validarExistenciaAutorizacaoSolicitacao(pain009)
                .or(() -> validarExistenciaConta(conta))
                .or(() -> validarTitularidadeConta(pain009, conta)
                        .or(() -> validarContaAtiva(conta))
                        .or(() -> validarBloqueioConta(conta)));
    }

    private Optional<MotivoRejeicaoPain012> validarExistenciaConta(DadosContaResponseDTO conta) {
        if (conta == null) {
            return Optional.of(MotivoRejeicaoPain012.CONTA_USUARIO_PAGADOR_NAO_LOCALIZADA);
        }
        return Optional.empty();
    }

    private Optional<MotivoRejeicaoPain012> validarTitularidadeConta(Pain009Dto pain009, DadosContaResponseDTO conta) {
        if (!conta.titular().documento().equals(pain009.getCpfCnpjUsuarioPagador())) {
            return Optional.of(CPF_CNPJ_USUARIO_PAGADOR_NAO_LOCALIZADO);
        }
        return Optional.empty();
    }

    private Optional<MotivoRejeicaoPain012> validarContaAtiva(DadosContaResponseDTO conta) {
        if ("CANCELED".equals(conta.status()) || "CLOSED".equals(conta.status())) {
            return Optional.of(MotivoRejeicaoPain012.CONTA_TRANSACIONAL_USUARIO_PAGADOR_ENCERRADA);
        }
        return Optional.empty();
    }

    private Optional<MotivoRejeicaoPain012> validarBloqueioConta(DadosContaResponseDTO conta) {
        if (("ACTIVE".equals(conta.status()) || "CAPITALIZING".equals(conta.status())) && (conta.temCreditoBloqueado() || conta.temDebitoBloqueado())) {
            return Optional.of(MotivoRejeicaoPain012.CONTA_TRANSACIONAL_USUARIO_PAGADOR_BLOQUEADA);
        }
        return Optional.empty();
    }

    private Optional<MotivoRejeicaoPain012> validarExistenciaAutorizacaoSolicitacao(Pain009Dto pain009) {
        var statusSolicitacao = List.of(TipoStatusSolicitacaoAutorizacao.PENDENTE_CONFIRMACAO, TipoStatusSolicitacaoAutorizacao.ACEITA);

        return solicitacaoRepository.findFirstByIdRecorrenciaAndTipoStatusIn(pain009.getIdRecorrencia(), statusSolicitacao)
                .map(autorizacao -> MotivoRejeicaoPain012.JA_CONFIRMADA_PREVIAMENTE_OU_STATUS_PENDENTE);
    }

    private String obterCodigoIbge(DadosContaResponseDTO conta) {
        if (conta == null) return null;
        return gestentConectorService.consultarCodigoMunicipio(conta.posto(), conta.coop());
    }

    private Map<String, LocalDateTime> mapearDetalhesRecorrencia(Pain009Dto pain009) {
        return pain009.getDetalhesRecorrenciaPain009Dto().stream()
                .collect(Collectors.toMap(
                        DetalheRecorrenciaPain009Dto::getTipoSituacaoDaRecorrencia,
                        DetalheRecorrenciaPain009Dto::getDataHoraTipoSituacaoDaRecorrencia
                ));
    }

    @Override
    public void atualizaRecorrenciaAutorizacaoSolicitacao(
            String id, LocalDateTime dataHoraInicioCanal, String codErro, TipoStatusSolicitacaoAutorizacao status, String subStatus) {
        solicitacaoRepository.atualizaRecorrenciaAutorizacaoSolicitacao(id, dataHoraInicioCanal, codErro, status, subStatus, LocalDateTime.now());
    }

    @Override
    public SolicitacaoAutorizacaoRecorrencia buscarSolicitacaoAutorizacaoPorIdSolicitacaoEStatus(
            String idSolicitacao, TipoStatusSolicitacaoAutorizacao tipoStatus) {

        return solicitacaoRepository.findFirstByIdSolicitacaoRecorrenciaAndTipoStatusIn(idSolicitacao, List.of(tipoStatus))
                .orElseThrow(() -> new NotFoundException(AppExceptionCode.SOLICITACAO_NAO_ENCONTRADA));
    }
}