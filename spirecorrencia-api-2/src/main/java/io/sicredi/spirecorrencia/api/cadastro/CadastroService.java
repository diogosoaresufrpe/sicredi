package io.sicredi.spirecorrencia.api.cadastro;

import br.com.sicredi.canaisdigitais.enums.TipoRetornoTransacaoEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoCanalEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoChaveEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentTransaction;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;
import io.sicredi.spirecorrencia.api.idempotente.CriaResponseStrategyFactory;
import io.sicredi.spirecorrencia.api.idempotente.ErroDTO;
import io.sicredi.spirecorrencia.api.idempotente.EventoResponseDTO;
import io.sicredi.spirecorrencia.api.idempotente.EventoResponseFactory;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO.TipoTemplate;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoUtils;
import io.sicredi.spirecorrencia.api.protocolo.SpiCanaisProtocoloApiClient;
import io.sicredi.spirecorrencia.api.repositorio.*;
import io.sicredi.spirecorrencia.api.utils.RecorrenciaMdc;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO.InformacaoAdicional.of;
import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoInformacaoAdicional.*;

@Slf4j
@Service
@RequiredArgsConstructor
class CadastroService {

    private final AppConfig appConfig;
    private final Validator validator;
    private final RecorrenciaRepository recorrenciaRepository;
    private final PagadorRepository pagadorRepository;
    private final RecebedorRepository recebedorRepository;
    private final SpiCanaisProtocoloApiClient spiCanaisProtocoloApiClient;
    private final CriaResponseStrategyFactory<CadastroRequest> criaResponseStrategyFactory;
    private final EventoResponseFactory eventoResponseFactory;

    @IdempotentTransaction
    public IdempotentResponse<?> processarAgendamento(IdempotentRequest<CadastroRequestWrapper> request) {
        var agendamento = request.getValue().getAgendamento();
        var response = processarCadastro(
                agendamento,
                request.getTransactionId(),
                request.getHeaders()
        );
        if (request.getValue().possuiRecorrencia() && !response.isErrorResponse()) {
            var recorrencia = request.getValue().getRecorrencia();
            try {
                MDC.put(RecorrenciaMdc.IDENTIFICADOR_TRANSACAO_INTEGRADO.getChave(), recorrencia.getIdentificadorTransacao());
                log.debug("Realizando emissão de cadastro de protocolo de recorrência integrada. Identificador de transação agendamento: {}. Identificador da transação da recorrência: {}", agendamento.getIdentificadorTransacao(), recorrencia.getIdentificadorTransacao());
                var cadastroRecorrenciaProtocoloRequest = RecorrenteMapper.toCadastroRecorrenciaProtocoloRequest(recorrencia);
                cadastroRecorrenciaProtocoloRequest.setDispositivoAutenticacao(recorrencia.getDispositivoAutenticacao());
                spiCanaisProtocoloApiClient.emitirProtocoloCadastroRecorrenciaIntegrada(recorrencia.getTipoCanal(), "CADASTRO_INTEGRADO", cadastroRecorrenciaProtocoloRequest);
            } catch (Exception ex) {
                log.error("Erro ao emitir protocolo de cadastro de recorrência integrada com agendamento", ex);
                var eventoResponse = criarEventoNotificacao(TipoTemplate.RECORRENCIA_CADASTRO_FALHA, recorrencia);
                response.getEvents().add(eventoResponse.toIdempotentEvent());
            } finally {
                MDC.remove(RecorrenciaMdc.IDENTIFICADOR_TRANSACAO_INTEGRADO.getChave());
            }
        }
        return response;
    }

    @IdempotentTransaction
    public IdempotentResponse<?> processarRecorrencia(IdempotentRequest<CadastroRequestWrapper> request) {
        var recorrencia = request.getValue().getRecorrencia();
        var response =  processarCadastro(
                recorrencia,
                request.getTransactionId(),
                request.getHeaders()
        );

        var tipoTemplateNotificacao = response.isErrorResponse() ? TipoTemplate.RECORRENCIA_CADASTRO_FALHA : TipoTemplate.RECORRENCIA_CADASTRO_SUCESSO;
        var eventoResponse = criarEventoNotificacao(tipoTemplateNotificacao, recorrencia);
        response.getEvents().add(eventoResponse.toIdempotentEvent());
        return response;
    }

    private IdempotentResponse<?> processarCadastro(CadastroRequest cadastroRequest,
                                                    String transactionId,
                                                    Map<String, String> headers) {
        var tipoResponse = cadastroRequest.getTipoResponse();

        log.debug("Início das validações de constraints da cadastro de recorrência. Tipo de recorrência: {}", cadastroRequest.getTipoRecorrencia());
        var erroValidacaoConstraint = validarRequest(cadastroRequest);
        log.debug("Fim das validações de constraints da cadastro de recorrência. Tipo de recorrência: {}. Erro de validação: {}", cadastroRequest.getTipoRecorrencia(), erroValidacaoConstraint.isPresent());
        if (erroValidacaoConstraint.isPresent()) {
            return criaResponseStrategyFactory.criar(tipoResponse).criarResponseIdempotentErro(cadastroRequest,
                    transactionId,
                    erroValidacaoConstraint.get()
            );
        }

        log.debug("Início das validações de negócio do cadastro de recorrência.");
        var erroValidacaoNegocio = validarRegraNegocio(cadastroRequest);
        log.debug("Fim das validações de negócio da cadastro de recorrência. Erro validação de negócio: {}", erroValidacaoNegocio.isPresent());
        if (erroValidacaoNegocio.isPresent()) {
            return criaResponseStrategyFactory.criar(tipoResponse).criarResponseIdempotentErro(cadastroRequest,
                    transactionId,
                    erroValidacaoNegocio.get()
            );
        }

        log.debug("Início da persistência do cadastro de recorrência.");
        var erroPersistirRecorrenciaNoBanco = salvar(cadastroRequest);
        log.debug("Fim da persistência do cadastro de recorrência. Erro ao salvar: {}", erroPersistirRecorrenciaNoBanco.isPresent());
        if (erroPersistirRecorrenciaNoBanco.isPresent()) {
            return criaResponseStrategyFactory.criar(tipoResponse).criarResponseIdempotentErro(cadastroRequest,
                    transactionId,
                    erroPersistirRecorrenciaNoBanco.get()
            );
        }

        return criaResponseStrategyFactory.criar(tipoResponse).criarResponseIdempotentSucesso(cadastroRequest,
                transactionId,
                headers,
                Collections.emptyList()
        );
    }

    private EventoResponseDTO criarEventoNotificacao(TipoTemplate tipoTemplate, CadastroRequest cadastroRequest) {
        var recebedorDTO = cadastroRequest.getRecebedor();

        var listInformacaoAdicional = List.of(
                of(NOME_RECEBEDOR, recebedorDTO.getNome()),
                of(DOCUMENTO_RECEBEDOR, recebedorDTO.getCpfCnpj()),
                of(QUANTIDADE_PARCELAS, String.valueOf(cadastroRequest.getParcelas().size())),
                of(FREQUENCIA, cadastroRequest.getTipoFrequencia().getTituloPlural().toLowerCase(Locale.ROOT)),
                of(VALOR, cadastroRequest.getParcelas().getFirst().getValor().toString()),
                of(DOCUMENTO_PAGADOR, cadastroRequest.getPagador().getCpfCnpj())
        );

        var pagadorDTO = cadastroRequest.getPagador();
        var tipoChave = Optional.ofNullable(recebedorDTO.getTipoChave()).map(TipoChaveEnum::name).orElse(null);
        var canal = NotificacaoUtils.converterCanalParaNotificacao(cadastroRequest.getTipoCanal(), cadastroRequest.getTipoOrigemSistema());
        var notificacao = NotificacaoDTO.builder()
                .agencia(pagadorDTO.getAgencia())
                .conta(pagadorDTO.getConta())
                .chave(recebedorDTO.getChave())
                .tipoChave(tipoChave)
                .operacao(tipoTemplate)
                .canal(canal)
                .informacoesAdicionais(listInformacaoAdicional)
                .build();
        return eventoResponseFactory.criarEventoNotificacao(notificacao);
    }

    private Optional<ErroDTO> validarRequest(CadastroRequest agendadoRecorrente) {
        return validator.validate(agendadoRecorrente).stream()
                .map(ConstraintViolation::getMessage)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .map(mensagemErro -> new ErroDTO(
                        AppExceptionCode.SPIRECORRENCIA_REC0001,
                        AppExceptionCode.SPIRECORRENCIA_REC0001.getMensagemFormatada(mensagemErro),
                        TipoRetornoTransacaoEnum.ERRO_VALIDACAO));
    }

    private Optional<ErroDTO> validarRegraNegocio(CadastroRequest cadastroAgendadoCadastroRequest) {
        return validarNumeroTotalParcelas(cadastroAgendadoCadastroRequest)
                .or(() -> validarDatasParcelas(cadastroAgendadoCadastroRequest))
                .or(() -> validarMesmaOrigemDestino(cadastroAgendadoCadastroRequest));
    }

    private Optional<ErroDTO> validarNumeroTotalParcelas(CadastroRequest cadastroAgendadoCadastroRequest) {
        var tipoCanal = cadastroAgendadoCadastroRequest.getTipoCanal();
        int numeroTotalParcelas = cadastroAgendadoCadastroRequest.getParcelas().size();
        var regrasParcela = TipoCanalEnum.WEB_OPENBK == tipoCanal ? appConfig.getRegras().getParcelaOpenFinance() : appConfig.getRegras().getParcela();
        if (regrasParcela.getNumeroMaximoParcelas() < numeroTotalParcelas) {
            var mensagem = AppExceptionCode.SPIRECORRENCIA_BU0004.getMensagemFormatada(String.valueOf(regrasParcela.getNumeroMaximoParcelas()));
            return Optional.of(new ErroDTO(AppExceptionCode.SPIRECORRENCIA_BU0004, mensagem, TipoRetornoTransacaoEnum.ERRO_NEGOCIO));
        }
        return Optional.empty();
    }

    private Optional<ErroDTO> validarDatasParcelas(CadastroRequest cadastroAgendadoCadastroRequest) {
        var tipoCanal = cadastroAgendadoCadastroRequest.getTipoCanal();
        var dataMinimaPermitida = consultarDataMinimaPermitida(tipoCanal);
        for (RecorrenteParcelaRequisicaoDTO parcela : cadastroAgendadoCadastroRequest.getParcelas()) {
            if (parcela.getDataTransacao().toLocalDate().isBefore(dataMinimaPermitida)) {
                var formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                var mensagem = AppExceptionCode.SPIRECORRENCIA_BU0003.getMensagemFormatada(dataMinimaPermitida.format(formatter));
                return Optional.of(new ErroDTO(AppExceptionCode.SPIRECORRENCIA_BU0003, mensagem, TipoRetornoTransacaoEnum.ERRO_NEGOCIO));
            }
        }
        return Optional.empty();
    }

    private Optional<ErroDTO> validarMesmaOrigemDestino(CadastroRequest cadastroAgendadoCadastroRequest) {
        var pagador = cadastroAgendadoCadastroRequest.getPagador();
        var recebedor = cadastroAgendadoCadastroRequest.getRecebedor();
        if (pagador.getCpfCnpj().equals(recebedor.getCpfCnpj())
                && pagador.getAgencia().equals(recebedor.getAgencia())
                && pagador.getConta().equals(recebedor.getConta())
                && pagador.getTipoConta() == recebedor.getTipoConta()) {
            return Optional.of(new ErroDTO(AppExceptionCode.SPIRECORRENCIA_BU0001, AppExceptionCode.SPIRECORRENCIA_BU0001.getMessage(), TipoRetornoTransacaoEnum.ERRO_NEGOCIO));
        }
        return Optional.empty();
    }

    private LocalDate consultarDataMinimaPermitida(TipoCanalEnum tipoCanal) {
        var regraHorario = TipoCanalEnum.WEB_OPENBK.equals(tipoCanal) ? appConfig.getRegras().getHorarioOpenFinance() : appConfig.getRegras().getHorario();
        var horaAtual = LocalTime.now();
        if (horaAtual.isAfter(regraHorario.getInicio()) && horaAtual.isBefore(regraHorario.getFim())) {
            return LocalDate.now().plusDays(regraHorario.getDiaMinimoCadastroEntreInicioFim());
        }
        return LocalDate.now().plusDays(regraHorario.getDiaMinimoCadastroForaInicioFim());
    }

    private Optional<ErroDTO> salvar(CadastroRequest cadastroAgendadoCadastroRequest) {
        try {
            var parcelas = cadastroAgendadoCadastroRequest.getParcelas();
            var pagador = consultarCadastroPagador(cadastroAgendadoCadastroRequest.getPagador());
            var recebedor = consultarCadastroRecebedor(cadastroAgendadoCadastroRequest.getRecebedor());

            var nomeRecorrencia = Optional.ofNullable(cadastroAgendadoCadastroRequest.getNome())
                    .filter(StringUtils::isNotBlank)
                    .orElseGet(recebedor::getNome);

            var recorrencia = Recorrencia.builder()
                    .idRecorrencia(cadastroAgendadoCadastroRequest.getIdentificadorRecorrencia())
                    .pagador(pagador)
                    .recebedor(recebedor)
                    .nome(StringUtils.substring(nomeRecorrencia, 0, 30))
                    .tipoCanal(cadastroAgendadoCadastroRequest.getTipoCanal())
                    .tipoOrigemSistema(cadastroAgendadoCadastroRequest.getTipoOrigemSistema())
                    .tipoIniciacao(cadastroAgendadoCadastroRequest.getTipoIniciacao())
                    .tipoStatus(TipoStatusEnum.CRIADO)
                    .dataCriacao(LocalDateTime.now())
                    .tipoIniciacaoCanal(cadastroAgendadoCadastroRequest.getTipoIniciacaoCanal())
                    .tipoFrequencia(cadastroAgendadoCadastroRequest.getTipoFrequencia())
                    .numInicCnpj(cadastroAgendadoCadastroRequest.getNumInicCnpj())
                    .tipoRecorrencia(cadastroAgendadoCadastroRequest.getTipoRecorrencia())
                    .build();

            var listRecorrenciaTransacao = parcelas.stream()
                    .map(parcela -> RecorrenciaTransacao.builder()
                            .recorrencia(recorrencia)
                            .tpoStatus(TipoStatusEnum.CRIADO)
                            .idParcela(parcela.getIdentificadorParcela())
                            .idFimAFim(parcela.getIdFimAFim())
                            .valor(parcela.getValor())
                            .notificadoDiaAnterior(Boolean.FALSE)
                            .dataTransacao(parcela.getDataTransacao().toLocalDate())
                            .informacoesEntreUsuarios(parcela.getInformacoesEntreUsuarios())
                            .idConciliacaoRecebedor(parcela.getIdConciliacaoRecebedor())
                            .dataCriacaoRegistro(LocalDateTime.now())
                            .build()
                    ).toList();

            recorrencia.setRecorrencias(listRecorrenciaTransacao);

            recorrenciaRepository.save(recorrencia);
        } catch (DataIntegrityViolationException | ConstraintViolationException e) {
            return Optional.of(
                    new ErroDTO(
                            AppExceptionCode.SPIRECORRENCIA_REC0004,
                            AppExceptionCode.SPIRECORRENCIA_REC0004.getMensagemFormatada(e.getMessage()),
                            TipoRetornoTransacaoEnum.ERRO_INFRA));
        }
        return Optional.empty();
    }

    private Recebedor consultarCadastroRecebedor(RecorrenteRecebedorDTO recebedorRequestDto) {
        return recebedorRepository.buscarRecebedor(recebedorRequestDto)
                .orElseGet(() -> Recebedor.builder()
                        .cpfCnpj(recebedorRequestDto.getCpfCnpj())
                        .nome(recebedorRequestDto.getNome())
                        .agencia(recebedorRequestDto.getAgencia())
                        .conta(recebedorRequestDto.getConta())
                        .instituicao(recebedorRequestDto.getInstituicao())
                        .tipoConta(recebedorRequestDto.getTipoConta())
                        .tipoPessoa(recebedorRequestDto.getTipoPessoa())
                        .tipoChave(recebedorRequestDto.getTipoChave())
                        .chave(recebedorRequestDto.getChave())
                        .dataCriacaoRegistro(LocalDateTime.now())
                        .build());
    }

    private Pagador consultarCadastroPagador(RecorrentePagadorDTO recorrenteRequisicaoPagadorDTO) {
        return pagadorRepository.findByCpfCnpjAndAgenciaAndContaAndTipoConta(recorrenteRequisicaoPagadorDTO.getCpfCnpj(), recorrenteRequisicaoPagadorDTO.getAgencia(), recorrenteRequisicaoPagadorDTO.getConta(), recorrenteRequisicaoPagadorDTO.getTipoConta())
                .stream().findFirst()
                .map(pagador -> atualizarDadosPagador(recorrenteRequisicaoPagadorDTO, pagador))
                .orElseGet(() -> Pagador.builder()
                        .cpfCnpj(recorrenteRequisicaoPagadorDTO.getCpfCnpj())
                        .nome(recorrenteRequisicaoPagadorDTO.getNome())
                        .instituicao(recorrenteRequisicaoPagadorDTO.getInstituicao())
                        .agencia(recorrenteRequisicaoPagadorDTO.getAgencia())
                        .conta(recorrenteRequisicaoPagadorDTO.getConta())
                        .codPosto(recorrenteRequisicaoPagadorDTO.getPosto())
                        .tipoConta(recorrenteRequisicaoPagadorDTO.getTipoConta())
                        .tipoPessoa(recorrenteRequisicaoPagadorDTO.getTipoPessoa())
                        .dataCriacaoRegistro(LocalDateTime.now())
                        .build());
    }

    private Pagador atualizarDadosPagador(RecorrentePagadorDTO recorrenteRequisicaoPagadorDTO, Pagador pagador) {
        boolean atualizar = false;
        atualizar |= atualizarSeDiferente(pagador::getNome, pagador::setNome, recorrenteRequisicaoPagadorDTO.getNome());
        atualizar |= atualizarSeDiferente(pagador::getCodPosto, pagador::setCodPosto, recorrenteRequisicaoPagadorDTO.getPosto());
        if (atualizar) {
            pagadorRepository.save(pagador);
        }
        return pagador;
    }

    private <T> boolean atualizarSeDiferente(Supplier<T> getter, Consumer<T> setter, T valorAtual) {
        if (ObjectUtils.notEqual(getter.get(), valorAtual)) {
            setter.accept(valorAtual);
            return true;
        }
        return false;
    }
}
