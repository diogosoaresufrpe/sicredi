package io.sicredi.spirecorrencia.api.exclusao;

import br.com.sicredi.canaisdigitais.enums.TipoRetornoTransacaoEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoChaveEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoRecorrencia;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentTransaction;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;
import io.sicredi.spirecorrencia.api.idempotente.*;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoUtils;
import io.sicredi.spirecorrencia.api.repositorio.Recorrencia;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaRepository;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacao;
import io.sicredi.spirecorrencia.api.utils.RecorrenciaMdc;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO.InformacaoAdicional.of;
import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoInformacaoAdicional.DOCUMENTO_PAGADOR;
import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoInformacaoAdicional.NOME_RECORRENCIA;

@Slf4j
@Service
@RequiredArgsConstructor
class ExclusaoService {

    private final AppConfig appConfig;
    private final CriaResponseStrategyFactory<ExclusaoRequisicaoDTO> criaResponseStrategyFactory;
    private final RecorrenciaRepository recorrenciaRepository;
    private final Validator validator;
    private final EventoResponseFactory eventoResponseFactory;

    @IdempotentTransaction
    public IdempotentResponse<?> processarProtocolo(IdempotentRequest<ExclusaoRequisicaoDTO> request) {
        var exclusaoRequest = request.getValue();
        var tipoResponse = exclusaoRequest.getTipoResponse();

        log.debug("Início das validações de constraints da exclusão.");
        var erroValidacaoConstraint = validarRequest(exclusaoRequest);
        log.debug("Fim das validações de constraints da exclusão. Erro de validação: {}", erroValidacaoConstraint.isPresent());
        if (erroValidacaoConstraint.isPresent()) {
            return criaResponseStrategyFactory.criar(tipoResponse).criarResponseIdempotentErro(exclusaoRequest,
                    request.getTransactionId(),
                    erroValidacaoConstraint.get()
            );
        }

        log.debug("Início da consulta dos dados da recorrencia para exclusão.");
        var buscaRecorrenciaWrapperDTO = consultaRecorrencia(exclusaoRequest);
        var erroBuscaRecorrencia = buscaRecorrenciaWrapperDTO.getErro();
        log.debug("Fim da consulta dos dados da recorrencia para exclusão. Erro ao consulta recorrencia: {}", erroBuscaRecorrencia.isPresent());
        if (erroBuscaRecorrencia.isPresent()) {
            return criaResponseStrategyFactory.criar(tipoResponse).criarResponseIdempotentErro(
                    exclusaoRequest,
                    request.getTransactionId(),
                    erroBuscaRecorrencia.get()
            );
        }
        var recorrencia = buscaRecorrenciaWrapperDTO.getObjeto();

        log.debug("Início das validações de negócio da exclusão de recorrência.");
        var erroValidacaoNegocio = validarRegraNegocio(exclusaoRequest, recorrencia);
        log.debug("Fim das validações de negócio da exclusão de recorrência. Erro validação de negócio: {}", erroValidacaoNegocio.isPresent());
        if (erroValidacaoNegocio.isPresent()) {
            return criaResponseStrategyFactory.criar(tipoResponse).criarResponseIdempotentErro(exclusaoRequest,
                    request.getTransactionId(),
                    erroValidacaoNegocio.get()
            );
        }

        log.debug("Início do processamento da recorrencia para exclusão.");
        var processamentoResponseWrapper = executarProcessamento(recorrencia, exclusaoRequest);
        log.debug("Fim do processamento da recorrencia para exclusão.");


        return criaResponseStrategyFactory.criar(tipoResponse).criarResponseIdempotentSucesso(request.getValue(),
                request.getTransactionId(),
                request.getHeaders(),
                processamentoResponseWrapper.listaEventos()
        );
    }


    private ProcessamentoResponseWrapper executarProcessamento(Recorrencia recorrencia, ExclusaoRequisicaoDTO exclusaoRequest) {
        var listaExcluidos = new ArrayList<RecorrenciaTransacao>();
        for (RecorrenciaTransacao recorrenciaTransacao : recorrencia.getRecorrencias()) {
            try {
                String idParcela = recorrenciaTransacao.getIdParcela();
                MDC.put(RecorrenciaMdc.ID_PARCELA.getChave(), idParcela);

                if (exclusaoRequest.getIdentificadoresParcelas().contains(idParcela)) {
                    recorrenciaTransacao.excluir(exclusaoRequest.getTipoMotivoExclusao());
                    listaExcluidos.add(recorrenciaTransacao);
                }
            } finally {
                MDC.remove(RecorrenciaMdc.ID_PARCELA.getChave());
            }
        }

        recorrencia.criarStatusFinalizacao(recorrencia.getRecorrencias())
                .ifPresent(status -> {
                    if (TipoStatusEnum.EXCLUIDO.equals(status)) {
                        recorrencia.setDataExclusao(LocalDateTime.now());
                    }
                    recorrencia.setTipoStatus(status);
                });

        var recorrenciaSaved = recorrenciaRepository.save(recorrencia);

        var listaEventos = new ArrayList<>(listaExcluidos.stream()
                .map(parcelaExcluida -> {
                    var eventoExclusao = EventoExclusaoPayloadDTO.builder()
                            .oidRecorrenciaTransacao(parcelaExcluida.getOidRecorrenciaTransacao())
                            .tipoMotivoExclusao(parcelaExcluida.getTipoMotivoExclusao())
                            .dataExclusao(parcelaExcluida.getDataExclusao())
                            .identificadorParcela(parcelaExcluida.getIdParcela())
                            .idFimAFim(parcelaExcluida.getIdFimAFim())
                            .build();
                    return eventoResponseFactory.criarEventoExclusaoOpenFinance(recorrenciaSaved, eventoExclusao);
                }).toList());


        if (exclusaoRequest.isFluxoLiquidacao() && TipoStatusEnum.CONCLUIDO == recorrenciaSaved.getTipoStatus() && TipoRecorrencia.AGENDADO_RECORRENTE == recorrenciaSaved.getTipoRecorrencia()) {
            var eventoConclucaoRecorrencia = eventoResponseFactory.criarEventoNotificacao(criarNotificacaoConclucaoRecorrencia(recorrenciaSaved));
            listaEventos.add(eventoConclucaoRecorrencia);
        }
        return new ProcessamentoResponseWrapper(listaExcluidos, listaEventos);
    }

    private Optional<ErroDTO> validarRequest(ExclusaoRequisicaoDTO exclusao) {
        return validator.validate(exclusao).stream()
                .map(ConstraintViolation::getMessage)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .map(mensagemErro -> new ErroDTO(
                        AppExceptionCode.SPIRECORRENCIA_REC0001,
                        AppExceptionCode.SPIRECORRENCIA_REC0001.getMensagemFormatada(mensagemErro),
                        TipoRetornoTransacaoEnum.ERRO_VALIDACAO));
    }

    private Optional<ErroDTO> validarRegraNegocio(ExclusaoRequisicaoDTO exclusaoRequest, Recorrencia recorrencia) {
        var parcelasRecorrencia = recorrencia.getRecorrencias();
        var identificadoresParcelas = exclusaoRequest.getIdentificadoresParcelas();

        if (exclusaoRequest.isFluxoLiquidacao()) {
            return validarExistenciasRecorrenciaTransacao(identificadoresParcelas, parcelasRecorrencia)
                    .or(() -> validarStatusRecorrenciaTransacao(identificadoresParcelas, parcelasRecorrencia));
        }

        return validarStatusRecorrencia(recorrencia.getTipoStatus())
                .or(() -> validarExistenciasRecorrenciaTransacao(identificadoresParcelas, parcelasRecorrencia))
                .or(() -> validarStatusRecorrenciaTransacao(identificadoresParcelas, parcelasRecorrencia))
                .or(() -> validarCancelamentoAntesDeLimiteDiaAnterior(identificadoresParcelas, parcelasRecorrencia));
    }

    private ErroWrapperDTO<Recorrencia> consultaRecorrencia(ExclusaoRequisicaoDTO exclusao) {
        var identificadorRecorrencia = exclusao.getIdentificadorRecorrencia();
        return recorrenciaRepository.findByIdRecorrencia(identificadorRecorrencia)
                .map(ErroWrapperDTO::new)
                .orElseGet(() -> new ErroWrapperDTO<>(
                        new ErroDTO(AppExceptionCode.SPIRECORRENCIA_BU0010,
                                AppExceptionCode.SPIRECORRENCIA_BU0010.getMessage(),
                                TipoRetornoTransacaoEnum.ERRO_NEGOCIO)));
    }

    private Optional<ErroDTO> validarCancelamentoAntesDeLimiteDiaAnterior(Collection<String> listIdentificadoresParcelasDeleted, Collection<RecorrenciaTransacao> listRecorrenciaTransacaoSaved) {
        return listRecorrenciaTransacaoSaved.stream()
                .filter(transacao -> listIdentificadoresParcelasDeleted.contains(transacao.getIdParcela()))
                .map(RecorrenciaTransacao::getDataTransacao)
                .filter(Objects::nonNull)
                .map(dataTransacao -> dataTransacao.minusDays(1).atTime(appConfig.getRegras().getExclusaoHorarioLimite()))
                .filter(LocalDateTime.now()::isAfter)
                .findFirst()
                .map(transacao -> new ErroDTO(AppExceptionCode.SPIRECORRENCIA_BU0026, AppExceptionCode.SPIRECORRENCIA_BU0026.getMessage(), TipoRetornoTransacaoEnum.ERRO_NEGOCIO));
    }

    private Optional<ErroDTO> validarExistenciasRecorrenciaTransacao(List<String> listIdentificadoresParcelasDeleted, List<RecorrenciaTransacao> listRecorrenciaTransacaoSaved) {
        var listIdentificadoresParcelasSaved = listRecorrenciaTransacaoSaved.stream()
                .map(RecorrenciaTransacao::getIdParcela)
                .toList();

        var listIdentificadoresParcelasDeletedInvalido = listIdentificadoresParcelasDeleted.stream()
                .filter(id -> !listIdentificadoresParcelasSaved.contains(id))
                .toList();

        if (!listIdentificadoresParcelasDeletedInvalido.isEmpty()) {
            var mensagem = MessageFormat.format(AppExceptionCode.SPIRECORRENCIA_BU0007.getMessage(), listIdentificadoresParcelasDeletedInvalido.size() == 1 ? 0 : 1, StringUtils.join(listIdentificadoresParcelasDeletedInvalido, ","));
            return Optional.of(new ErroDTO(AppExceptionCode.SPIRECORRENCIA_BU0007, mensagem, TipoRetornoTransacaoEnum.ERRO_NEGOCIO));
        }
        return Optional.empty();
    }

    private Optional<ErroDTO> validarStatusRecorrenciaTransacao(List<String> listIdentificadoresParcelasDeleted, List<RecorrenciaTransacao> listRecorrenciaTransacaoSaved) {
        var listRecorrenciaTransacaoInvalido = listRecorrenciaTransacaoSaved.stream()
                .filter(recorrenciaTransacao -> listIdentificadoresParcelasDeleted.contains(recorrenciaTransacao.getIdParcela()))
                .filter(recorrenciaTransacao -> TipoStatusEnum.CRIADO != recorrenciaTransacao.getTpoStatus() && TipoStatusEnum.PENDENTE != recorrenciaTransacao.getTpoStatus())
                .toList();

        if (listRecorrenciaTransacaoInvalido.isEmpty()) {
            return Optional.empty();
        }

        var listStatusInvalido = listRecorrenciaTransacaoInvalido.stream()
                .map(parcelaRecorrencia -> parcelaRecorrencia.getTpoStatus().name())
                .collect(Collectors.toSet());

        var listIdentificadoresParcelasInvalido = listRecorrenciaTransacaoInvalido.stream()
                .map(RecorrenciaTransacao::getIdParcela)
                .toList();

        var mensagem = MessageFormat.format(AppExceptionCode.SPIRECORRENCIA_BU0006.getMessage(), StringUtils.join(listStatusInvalido, "/"), listRecorrenciaTransacaoInvalido.size() == 1 ? 0 : 1, StringUtils.join(listIdentificadoresParcelasInvalido, ","));
        return Optional.of(new ErroDTO(AppExceptionCode.SPIRECORRENCIA_BU0006, mensagem, TipoRetornoTransacaoEnum.ERRO_NEGOCIO));
    }

    private Optional<ErroDTO> validarStatusRecorrencia(TipoStatusEnum tipoStatus) {
        if (TipoStatusEnum.CRIADO != tipoStatus) {
            var erroDTO = new ErroDTO(AppExceptionCode.SPIRECORRENCIA_BU0008, AppExceptionCode.SPIRECORRENCIA_BU0008.getMessage(), TipoRetornoTransacaoEnum.ERRO_NEGOCIO);
            return Optional.of(erroDTO);
        }
        return Optional.empty();
    }

    private NotificacaoDTO criarNotificacaoConclucaoRecorrencia(Recorrencia recorrencia) {
        var pagador = recorrencia.getPagador();
        var recebedor = recorrencia.getRecebedor();

        var informacoesAdicionais = List.of(
                of(NOME_RECORRENCIA, recorrencia.getNome()),
                of(DOCUMENTO_PAGADOR, pagador.getCpfCnpj())
        );

        var tipoChave = Optional.ofNullable(recebedor.getTipoChave()).map(TipoChaveEnum::name).orElse(null);
        var canal = NotificacaoUtils.converterCanalParaNotificacao(recorrencia.getTipoCanal(), recorrencia.getTipoOrigemSistema());
        return NotificacaoDTO.builder()
                .agencia(pagador.getAgencia())
                .conta(pagador.getConta())
                .chave(recebedor.getChave())
                .tipoChave(tipoChave)
                .operacao(NotificacaoDTO.TipoTemplate.RECORRENCIA_SUCESSO_FINALIZACAO)
                .canal(canal)
                .informacoesAdicionais(informacoesAdicionais)
                .build();
    }

    record ProcessamentoResponseWrapper(List<RecorrenciaTransacao> listaParcelasExcluidas,
                                        List<EventoResponseDTO> listaEventos) {
    }
}
