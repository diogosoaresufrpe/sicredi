package io.sicredi.spirecorrencia.api.liquidacao;

import br.com.sicredi.canaisdigitais.dto.IdentificacaoAssociadoDTO;
import br.com.sicredi.framework.web.spring.exception.NotFoundException;
import br.com.sicredi.spi.dto.TransacaoDto;
import br.com.sicredi.spi.entities.type.CodigoErro;
import br.com.sicredi.spi.entities.type.OrdemStatus;
import br.com.sicredi.spi.util.SpiUtil;
import br.com.sicredi.spi.util.type.TipoId;
import br.com.sicredi.spicanais.transacional.transport.lib.automatico.CadastroAutorizacaoRecorrenciaProtocoloRequestDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.*;
import br.com.sicredi.spicanais.transacional.transport.lib.pagamento.CadastroOrdemPagamentoTransacaoDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.CadastroRecorrenciaProtocoloRequest;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.PagadorRequestDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.RecebedorRequestDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import io.sicredi.spirecorrencia.api.cadastro.RecorrenteMapper;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.dict.ChannelDataDTO;
import io.sicredi.spirecorrencia.api.dict.ConsultaChaveDictClient;
import io.sicredi.spirecorrencia.api.dict.DictConsultaDTO;
import io.sicredi.spirecorrencia.api.dict.DictException;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoProducer;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoUtils;
import io.sicredi.spirecorrencia.api.protocolo.CanaisDigitaisProtocoloInfoInternalApiClient;
import io.sicredi.spirecorrencia.api.protocolo.ExclusaoRecorrenciaProtocoloRequestBuilder;
import io.sicredi.spirecorrencia.api.protocolo.SpiCanaisProtocoloApiClient;
import io.sicredi.spirecorrencia.api.recorrencia_tentativa.TentativaRecorrenciaTransacaoService;
import io.sicredi.spirecorrencia.api.repositorio.*;
import io.sicredi.spirecorrencia.api.utils.ObjectMapperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO.InformacaoAdicional.of;
import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO.TipoTemplate.*;
import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoInformacaoAdicional.*;

@Slf4j
@Component
@RequiredArgsConstructor
class LiquidacaoService {

    private static final String CODIGO_PROTOCOLO_DE_PAGAMENTO = "358";
    private static final Set<String> ERROS_PASSIVEIS_RETRY;

    static {
        ERROS_PASSIVEIS_RETRY = Stream.of(
                        CodigoErro.AB03,
                        CodigoErro.AB09,
                        CodigoErro.AB11,
                        CodigoErro.AM04,
                        CodigoErro.AM18,
                        CodigoErro.DT02,
                        CodigoErro.ED05,
                        CodigoErro.S910,
                        CodigoErro.S920,
                        CodigoErro.S932,
                        CodigoErro.S940,
                        CodigoErro.S999
                ).map(Enum::name)
                .collect(Collectors.toUnmodifiableSet());
    }

    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;
    private final NotificacaoProducer notificacaoProducer;
    private final RecorrenciaRepository recorrenciaRepository;
    private final ConsultaChaveDictClient consultaChaveDictClient;
    private final SpiCanaisProtocoloApiClient spiCanaisProtocoloApiClient;
    private final TentativaRecorrenciaTransacaoService tentativaService;
    private final RecorrenciaTransacaoRepository recorrenciaTransacaoRepository;
    private final CanaisDigitaisProtocoloInfoInternalApiClient canaisDigitaisProtocoloInfoInternalApiClient;

    /**
     * Processa o retorno da liquidação de um Pix do tipo Pagamento cujo tipoProduto é PAGAMENTO_COM_RECORRENCIA.
     *
     * @param identificadorTransacao
     * @param transacaoDto           Retorno da liquidação de um Pix do tipo Pagamento.
     */
    public void processarRetornoPagamentoComRecorrencia(String identificadorTransacao, TransacaoDto transacaoDto) {
        if (OrdemStatus.CONCLUIDO != transacaoDto.getStatus()) {
            log.info("Descartando processamento de cadastro de recorrência integrada pois a ordem de pagamento não foi concluída com sucesso. Status recebido = {}", transacaoDto.getStatus());
            return;
        }

        final var protocoloPagamento = Optional.ofNullable(canaisDigitaisProtocoloInfoInternalApiClient.consultaProtocoloPorTipoEIdentificador(CODIGO_PROTOCOLO_DE_PAGAMENTO, transacaoDto.getIdFimAFim()))
                .orElseThrow(() -> new NotFoundException("Protocolo %s não encontrado para o idFimAFim %s".formatted(CODIGO_PROTOCOLO_DE_PAGAMENTO, transacaoDto.getIdFimAFim())));

        final var cadastroOrdemPagamentoTransacaoDTO = ObjectMapperUtil.converterStringParaObjeto(protocoloPagamento.getPayloadTransacao(), new TypeReference<CadastroOrdemPagamentoTransacaoDTO>() {
        });
        final var requestProtocolo = criarRequestCadastroRecorrenciaIntegrada(identificadorTransacao, cadastroOrdemPagamentoTransacaoDTO);

        try {
            spiCanaisProtocoloApiClient.emitirProtocoloCadastroRecorrenciaIntegrada(requestProtocolo.getTipoCanal(), "CADASTRO_INTEGRADO", requestProtocolo);
        } catch (Exception ex) {
            log.error("Erro ao emitir protocolo de cadastro de recorrência integrada com pagamento", ex);
            notificarErroCriacaoRecorrenciaIntegrada(requestProtocolo);
        }
    }

    /**
     * Processa o retorno da liquidação de um Pix do tipo Pagamento cujo tipoProduto é PAGAMENTO_COM_AUTOMATICO.
     *
     * @param transacaoDto           Retorno da liquidação de um Pix do tipo Pagamento.
     */
    public void processarRetornoPagamentoComAutomatico(TransacaoDto transacaoDto) {
        if (OrdemStatus.CONCLUIDO != transacaoDto.getStatus()) {
            log.info("Descartando processamento de cadastro de autorização integrada pois a ordem de pagamento não foi concluída com sucesso. Status recebido = {}", transacaoDto.getStatus());
            return;
        }

        final var protocoloPagamento = Optional.ofNullable(canaisDigitaisProtocoloInfoInternalApiClient.consultaProtocoloPorTipoEIdentificador(CODIGO_PROTOCOLO_DE_PAGAMENTO, transacaoDto.getIdFimAFim()))
                .orElseThrow(() -> new NotFoundException("Protocolo %s não encontrado para o idFimAFim %s".formatted(CODIGO_PROTOCOLO_DE_PAGAMENTO, transacaoDto.getIdFimAFim())));

        final var cadastroOrdemPagamentoTransacaoDTO = ObjectMapperUtil.converterStringParaObjeto(protocoloPagamento.getPayloadTransacao(), new TypeReference<CadastroOrdemPagamentoTransacaoDTO>() {
        });
        final var requestProtocolo = criarRequestCadastroAutorizacaoIntegrada(cadastroOrdemPagamentoTransacaoDTO, transacaoDto);
        final var tipoCanal = TipoCanalEnum.valueOf(cadastroOrdemPagamentoTransacaoDTO.getCanal());

        try {
            spiCanaisProtocoloApiClient.emitirProtocoloCadastroAutorizacaoIntegrada(tipoCanal, "CADASTRO_INTEGRADO", requestProtocolo);
        } catch (Exception ex) {
            log.error("Erro ao emitir protocolo de cadastro de cadastro de autorização integrada com pagamento", ex);
            notificarErroCriacaoAutorizacaoIntegrada(requestProtocolo, tipoCanal);
        }
    }

    private CadastroRecorrenciaProtocoloRequest criarRequestCadastroRecorrenciaIntegrada(String identificadorTransacao,
                                                                                         CadastroOrdemPagamentoTransacaoDTO ordemPagamento) {
        var tipoCanal = TipoCanalEnum.valueOf(ordemPagamento.getCanal());
        var tipoMarca = RecorrenteMapper.getTipoMarca(tipoCanal);

        var pagador = PagadorRequestDTO.builder()
                .cpfCnpj(ordemPagamento.getCpfCnpjUsuarioPagador())
                .nome(ordemPagamento.getNomeUsuarioPagador())
                .instituicao(ordemPagamento.getParticipantePagador().getIspb())
                .agencia(ordemPagamento.getCooperativa())
                .conta(ordemPagamento.getConta())
                .posto(ordemPagamento.getAgencia())
                .tipoConta(ordemPagamento.getTipoContaUsuarioPagador())
                .tipoPessoa(TipoPessoaEnum.valueOf(ordemPagamento.getTipoPessoaConta().name()))
                .build();

        var recorrencia = ordemPagamento.getRecorrencia();

        var recebedor = RecebedorRequestDTO.builder()
                .cpfCnpj(ordemPagamento.getCpfCnpjUsuarioRecebedor())
                .nome(ordemPagamento.getNomeUsuarioRecebedor())
                .agencia(ordemPagamento.getAgenciaUsuarioRecebedor())
                .conta(ordemPagamento.getContaUsuarioRecebedor())
                .instituicao(ordemPagamento.getParticipanteRecebedor().getIspb())
                .tipoConta(ordemPagamento.getTipoContaUsuarioRecebedor())
                .chave(ordemPagamento.getChaveDict())
                .tipoPessoa(recorrencia.getTipoPessoaRecebedor())
                .tipoChave(recorrencia.getTipoChaveRecebedor())
                .build();

        var associado = new IdentificacaoAssociadoDTO();
        associado.setTipoConta(pagador.getTipoConta().getTipoContaCanaisDigitais());
        associado.setCpfUsuario(pagador.getCpfCnpj());
        associado.setCpfCnpjConta(pagador.getCpfCnpj());
        associado.setConta(pagador.getConta());
        associado.setAgencia(pagador.getPosto());
        associado.setNomeAssociadoConta(pagador.getNome());
        associado.setNomeUsuario(pagador.getNome());
        associado.setCooperativa(pagador.getAgencia());
        associado.setOrigemConta(ordemPagamento.getOrigemConta());
        associado.setIdentificadorUsuario(ordemPagamento.getOidUsuario());

        var nomeRecorrencia = Optional.ofNullable(recorrencia.getNomeRecorrencia())
                .filter(StringUtils::isNotBlank)
                .orElseGet(ordemPagamento::getNomeUsuarioRecebedor);

        var dataPrimeiraParcela = recorrencia.getParcelas().stream()
                .filter(parcela -> parcela.getDataTransacao() != null)
                .map(parcela -> parcela.getDataTransacao().toLocalDate())
                .min(LocalDate::compareTo)
                .stream()
                .findFirst()
                .orElse(null);

        var cadastroRecorrenciaRequest = CadastroRecorrenciaProtocoloRequest.builder()
                .tipoIniciacao(ordemPagamento.getTipoPagamento().getTipoPagamentoPix())
                .tipoMarca(tipoMarca)
                .tipoCanal(tipoCanal)
                .pagador(pagador)
                .recebedor(recebedor)
                .nome(nomeRecorrencia)
                .identificadorRecorrencia(recorrencia.getIdentificadorRecorrencia())
                .dataPrimeiraParcela(dataPrimeiraParcela)
                .tipoIniciacaoCanal(getTipoIniciacaoCanal(ordemPagamento))
                .tipoFrequencia(recorrencia.getTipoFrequencia())
                .tipoRecorrencia(TipoRecorrencia.AGENDADO_RECORRENTE)
                .parcelas(recorrencia.getParcelas())
                .build();

        cadastroRecorrenciaRequest.setDispositivoAutenticacao(ordemPagamento.getDispositivoAutenticacao());
        cadastroRecorrenciaRequest.setIdentificadorTransacao(identificadorTransacao);
        cadastroRecorrenciaRequest.setIdentificacaoAssociado(associado);
        cadastroRecorrenciaRequest.setIdentificadorSimulacaoLimite(recorrencia.getIdentificadorSimulacaoLimite());
        return cadastroRecorrenciaRequest;

    }

    private CadastroAutorizacaoRecorrenciaProtocoloRequestDTO criarRequestCadastroAutorizacaoIntegrada(CadastroOrdemPagamentoTransacaoDTO ordemPagamento,
                                                                                                       TransacaoDto transacaoDto) {

        var associado = new IdentificacaoAssociadoDTO();
        associado.setTipoConta(ordemPagamento.getTipoContaUsuarioPagador().getTipoContaCanaisDigitais());
        associado.setCpfUsuario(ordemPagamento.getCpfCnpjUsuarioPagador());
        associado.setCpfCnpjConta(ordemPagamento.getCpfCnpjUsuarioPagador());
        associado.setConta(ordemPagamento.getConta());
        associado.setAgencia(ordemPagamento.getAgencia());
        associado.setNomeAssociadoConta(ordemPagamento.getNomeUsuarioPagador());
        associado.setCooperativa(ordemPagamento.getCooperativa());
        associado.setOrigemConta(ordemPagamento.getOrigemConta());
        associado.setIdentificadorUsuario(ordemPagamento.getOidUsuario());

        var autorizacaoIntegrada = ordemPagamento.getAutorizacaoAutomatico();

        var request = CadastroAutorizacaoRecorrenciaProtocoloRequestDTO.builder()
                .idInformacaoStatus(autorizacaoIntegrada.getIdInformacaoStatus())
                .idRecorrencia(autorizacaoIntegrada.getIdRecorrencia())
                .tipoJornada(autorizacaoIntegrada.getTipoJornada())
                .contrato(autorizacaoIntegrada.getContrato())
                .objeto(autorizacaoIntegrada.getObjeto())
                .codigoMunicipioIbge(autorizacaoIntegrada.getCodigoMunicipioIbge())
                .nomeDevedor(autorizacaoIntegrada.getNomeDevedor())
                .cpfCnpjDevedor(autorizacaoIntegrada.getCpfCnpjDevedor())
                .nomeRecebedor(autorizacaoIntegrada.getNomeRecebedor())
                .cpfCnpjRecebedor(autorizacaoIntegrada.getCpfCnpjRecebedor())
                .instituicaoRecebedor(autorizacaoIntegrada.getInstituicaoRecebedor())
                .tipoFrequencia(autorizacaoIntegrada.getTipoFrequencia())
                .valor(autorizacaoIntegrada.getValor())
                .pisoValorMaximo(autorizacaoIntegrada.getPisoValorMaximo())
                .valorMaximo(autorizacaoIntegrada.getValorMaximo())
                .politicaRetentativa(autorizacaoIntegrada.getPoliticaRetentativa())
                .idFimAFimPagamentoImediato(ordemPagamento.getIdFimAFim())
                .dataRecebimentoConfirmacaoPacs002PagamentoImediato(transacaoDto.getDataRecebimentoPacs002())
                .dataInicialRecorrencia(autorizacaoIntegrada.getDataInicialRecorrencia())
                .dataFinalRecorrencia(autorizacaoIntegrada.getDataFinalRecorrencia())
                .dataCriacaoRecorrencia(autorizacaoIntegrada.getDataCriacaoRecorrencia())
                .build();

        request.setDispositivoAutenticacao(ordemPagamento.getDispositivoAutenticacao());
        request.setIdentificadorTransacao(autorizacaoIntegrada.getIdInformacaoStatus());
        request.setIdentificacaoAssociado(associado);

        return request;

    }

    private TipoIniciacaoCanal getTipoIniciacaoCanal(CadastroOrdemPagamentoTransacaoDTO dadosTransacao) {
        if (TipoCanalEnum.WEB_OPENBK.name().equals(dadosTransacao.getCanal())) {
            return TipoIniciacaoCanal.OPEN_FINANCE;
        }
        return StringUtils.isEmpty(dadosTransacao.getChaveDict()) ? TipoIniciacaoCanal.DADOS_BANCARIOS : TipoIniciacaoCanal.CHAVE;
    }

    @Transactional
    public void atualizaRecorrenciaLiquidacaoDaTransacaoComErro(String identificadorTransacao, OrdemErroProcessamentoResponse ordemErroProcessamentoResponse) {
        final var recorrenciaTransacao = consultarRecorrenciaTransacao(ordemErroProcessamentoResponse.getIdFimAFim());
        final var codigoErro = ordemErroProcessamentoResponse.getCodigoErro();
        final var motivo = ordemErroProcessamentoResponse.getMensagemErro();

        tentativaService.registrarRecorrenciaTransacaoTentativa(
                motivo,
                codigoErro,
                recorrenciaTransacao
        );

        trataRetornoTransacao(identificadorTransacao, codigoErro, recorrenciaTransacao);
    }

    private void emitirProtocoloExclusao(String identificadorTransacao, Recorrencia recorrencia, RecorrenciaTransacao parcela) {
        var request = ExclusaoRecorrenciaProtocoloRequestBuilder.criarExclusaoRecorrenciaProtocoloRequest(
                identificadorTransacao, recorrencia,
                List.of(parcela)
        );

        spiCanaisProtocoloApiClient.emitirProtocoloCancelamentoRecorrencia(
                recorrencia.getTipoCanal(),
                TipoExclusaoRecorrencia.EXCLUSAO_INTEGRADA,
                request
        );
    }

    private void notificarErroCriacaoAutorizacaoIntegrada(CadastroAutorizacaoRecorrenciaProtocoloRequestDTO cadastroRequest,
                                                          TipoCanalEnum tipoCanal) {
        var tipoMarca = RecorrenteMapper.getTipoMarca(tipoCanal);

        var associado = cadastroRequest.getIdentificacaoAssociado();
        var informacoesAdicionais = List.of(
                of(NOME_RECEBEDOR, cadastroRequest.getNomeRecebedor()),
                of(DOCUMENTO_PAGADOR, associado.getCpfCnpjConta())
        );

        var canal = NotificacaoUtils.converterCanalParaNotificacao(tipoCanal, tipoMarca);

        var notificacao =  NotificacaoDTO.builder()
                .agencia(associado.getCooperativa())
                .conta(associado.getConta())
                .canal(canal)
                .operacao(AUTOMATICO_AUTORIZACAO_CONFIRMADA_PAGADOR_FALHA_NAO_RESPONDIDA_OU_CANCELADA_RECEBEDOR)
                .informacoesAdicionais(informacoesAdicionais)
                .build();

        notificacaoProducer.enviarNotificacao(notificacao);
    }

    private void notificarErroCriacaoRecorrenciaIntegrada(CadastroRecorrenciaProtocoloRequest cadastroRequest) {
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
        var canal = NotificacaoUtils.converterCanalParaNotificacao(cadastroRequest.getTipoCanal(), cadastroRequest.getTipoMarca());
        var notificacao = NotificacaoDTO.builder()
                .agencia(pagadorDTO.getAgencia())
                .conta(pagadorDTO.getConta())
                .chave(recebedorDTO.getChave())
                .tipoChave(tipoChave)
                .operacao(RECORRENCIA_CADASTRO_FALHA)
                .canal(canal)
                .informacoesAdicionais(listInformacaoAdicional)
                .build();

        notificacaoProducer.enviarNotificacao(notificacao);
    }

    private void notificarErroTentativaLiquidacao(Recorrencia recorrencia, BigDecimal valor, NotificacaoDTO.TipoTemplate tipoTemplate) {
        var recebedor = recorrencia.getRecebedor();

        final var variaveisNotificacao = List.of(
                of(VALOR, String.valueOf(valor)),
                of(NOME_RECEBEDOR, recebedor.getNome()),
                of(DOCUMENTO_RECEBEDOR, recebedor.getCpfCnpj()),
                of(DOCUMENTO_PAGADOR, recorrencia.getDocumentoPagador())
        );

        final var notificacaoDTO = NotificacaoDTO.builder()
                .agencia(recorrencia.getAgenciaPagador())
                .conta(recorrencia.getContaPagador())
                .canal(NotificacaoUtils.converterCanalParaNotificacao(recorrencia.getTipoCanal(), recorrencia.getTipoOrigemSistema()))
                .operacao(tipoTemplate)
                .informacoesAdicionais(variaveisNotificacao)
                .build();

        notificacaoProducer.enviarNotificacao(notificacaoDTO);
    }

    private void trataRetornoTransacao(String identificadorTransacao, String codigoErro, RecorrenciaTransacao recorrenciaTransacao) {
        var recorrencia = recorrenciaTransacao.getRecorrencia();
        var dataHora = LocalTime.now();
        var dataHoraLimiteTentativa = appConfig.getRegras().getProcessamento().getHorarioLimiteLiquidacao();

        if (TipoCanalEnum.WEB_OPENBK == recorrencia.getTipoCanal() || dataHora.isAfter(dataHoraLimiteTentativa)) {
            var templateNotificacao = CodigoErro.S920.name().equalsIgnoreCase(codigoErro) ? RECORRENCIA_FALHA_SALDO_INSUFICIENTE_NAO_EFETIVADA : RECORRENCIA_FALHA_OPERACIONAL;
            notificarErroTentativaLiquidacao(recorrencia, recorrenciaTransacao.getValor(), templateNotificacao);
            emitirProtocoloExclusao(identificadorTransacao, recorrencia, recorrenciaTransacao);
        } else {
            if (ERROS_PASSIVEIS_RETRY.contains(codigoErro)) {
                recorrenciaTransacao.setTpoStatus(TipoStatusEnum.CRIADO);
                recorrenciaTransacaoRepository.save(recorrenciaTransacao);

                if (CodigoErro.S920.name().equalsIgnoreCase(codigoErro)) {
                    notificarErroTentativaLiquidacao(recorrencia, recorrenciaTransacao.getValor(), RECORRENCIA_FALHA_SALDO_INSUFICIENTE);
                }
                return;
            }

            notificarErroTentativaLiquidacao(recorrencia, recorrenciaTransacao.getValor(), RECORRENCIA_FALHA_OPERACIONAL);
            emitirProtocoloExclusao(identificadorTransacao, recorrencia, recorrenciaTransacao);
        }
    }


    /**
     * Processa o retorno da liquidação de um Pix do tipo Pagamento cujo tipoProduto é AGENDADO_RECORRENTE.
     *
     * @param identificadorTransacao
     * @param transacaoDto           Retorno da liquidação de um Pix do tipo Pagamento.
     */
    @Transactional
    public void processarRetornoPagamentoAgendadoRecorrente(String identificadorTransacao, TransacaoDto transacaoDto) {
        String idFimAFim = transacaoDto.getIdFimAFim();
        final var recorrenciaTransacao = consultarRecorrenciaTransacao(idFimAFim);

        if (TipoStatusEnum.PENDENTE != recorrenciaTransacao.getTpoStatus()) {
            log.warn("Ignorando processamento do retorno de pagamento agendado recorrente pois a parcela da recorrência encontra-se com status {} mas deveria ser {}", recorrenciaTransacao.getTpoStatus(), TipoStatusEnum.PENDENTE);
            return;
        }

        switch (transacaoDto.getStatus()) {
            case CONCLUIDO -> {
                recorrenciaTransacao.setTpoStatus(TipoStatusEnum.CONCLUIDO);
                recorrenciaTransacaoRepository.save(recorrenciaTransacao);

                final var recorrencia = recorrenciaTransacao.getRecorrencia();

                final var isRecorrenciaTotalmenteFinalizada = recorrencia.getRecorrencias().stream()
                        .noneMatch(transacao ->
                                TipoStatusEnum.CRIADO == transacao.getTpoStatus()
                                        || TipoStatusEnum.PENDENTE == transacao.getTpoStatus());

                if (isRecorrenciaTotalmenteFinalizada) {
                    recorrencia.setTipoStatus(TipoStatusEnum.CONCLUIDO);
                    recorrenciaRepository.save(recorrencia);

                    if (TipoRecorrencia.AGENDADO_RECORRENTE == recorrencia.getTipoRecorrencia()) {
                        notificarConclusaoDaRecorrencia(recorrencia);
                    }
                    
                    log.info("Todas as parcelas da recorrência {} foram liquidadas com sucesso", recorrencia.getIdRecorrencia());
                }

                notificarLiquidacaoDaTransacao(recorrenciaTransacao);
            }
            case CANCELADO -> {
                var motivoErro = Optional.ofNullable(transacaoDto.getTextoMotivoErro())
                        .filter(StringUtils::isNotBlank)
                        .orElseGet(() -> Optional.ofNullable(transacaoDto.getCodigoErro())
                                .filter(StringUtils::isNotBlank)
                                .orElse("Erro desconhecido"));
                tentativaService.registrarRecorrenciaTransacaoTentativa(motivoErro, transacaoDto.getCodigoErro(), recorrenciaTransacao);
                trataRetornoTransacao(identificadorTransacao, transacaoDto.getCodigoErro(), recorrenciaTransacao);
            }
        }
    }

    private RecorrenciaTransacao consultarRecorrenciaTransacao(String idFimAFim) {
        return recorrenciaTransacaoRepository.findByIdFimAFim(idFimAFim)
                .orElseThrow(() -> new NotFoundException("Parcela não encontrada para o idFimAFim %s".formatted(idFimAFim)));
    }

    private void notificarConclusaoDaRecorrencia(Recorrencia recorrencia) {
        final var variaveisNotificacao = List.of(
                of(NOME_RECORRENCIA, String.valueOf(recorrencia.getNome())),
                of(DOCUMENTO_PAGADOR, recorrencia.getDocumentoPagador())
        );

        final var notificacaoDTO = NotificacaoDTO.builder()
                .agencia(recorrencia.getAgenciaPagador())
                .conta(recorrencia.getContaPagador())
                .canal(NotificacaoUtils.converterCanalParaNotificacao(recorrencia.getTipoCanal(), recorrencia.getTipoOrigemSistema()))
                .operacao(NotificacaoDTO.TipoTemplate.RECORRENCIA_SUCESSO_FINALIZACAO)
                .informacoesAdicionais(variaveisNotificacao)
                .build();

        notificacaoProducer.enviarNotificacao(notificacaoDTO);
    }

    private void notificarLiquidacaoDaTransacao(RecorrenciaTransacao recorrenciaTransacao) {
        final var variaveisNotificacao = List.of(
                of(VALOR, String.valueOf(recorrenciaTransacao.getValor())),
                of(NOME_RECEBEDOR, recorrenciaTransacao.getNomeRecebedor()),
                of(DOCUMENTO_PAGADOR, recorrenciaTransacao.getDocumentoPagador())
        );

        final var notificacaoDTO = NotificacaoDTO.builder()
                .agencia(recorrenciaTransacao.getAgenciaPagador())
                .conta(recorrenciaTransacao.getContaPagador())
                .canal(NotificacaoUtils.converterCanalParaNotificacao(recorrenciaTransacao.getTipoCanal(), recorrenciaTransacao.getRecorrencia().getTipoOrigemSistema()))
                .operacao(NotificacaoDTO.TipoTemplate.RECORRENCIA_SUCESSO_PAGAMENTO_PARCELA)
                .informacoesAdicionais(variaveisNotificacao)
                .build();

        notificacaoProducer.enviarNotificacao(notificacaoDTO);
    }

    public TipoProcessamentoWrapperDTO consultarTipoProcessamento(String identificadorTransacao, RecorrenciaTransacao recorrenciaTransacao) {
        var recorrencia = recorrenciaTransacao.getRecorrencia();
        var recebedor = recorrencia.getRecebedor();

        if (TipoPagamentoPixEnum.PIX_PAYMENT_MANUAL == recorrencia.getTipoIniciacao() || recebedor.getChave() == null) {
            if (TipoCanalEnum.WEB_OPENBK != recorrencia.getTipoCanal()) {
                recorrenciaTransacao.setIdFimAFim(SpiUtil.gerarIdFimAFim(TipoId.PAGAMENTO, recorrencia.getPagador().getInstituicao()));
            }
            return TipoProcessamentoWrapperDTO.criarTipoLiquidacao(identificadorTransacao, recorrenciaTransacao);
        }

        return validarChaveDict(identificadorTransacao, recorrenciaTransacao, recebedor, recorrencia.getTipoCanal());
    }

    private TipoProcessamentoWrapperDTO validarChaveDict(String identificadorTransacao, RecorrenciaTransacao recorrenciaTransacao, Recebedor recebedor, TipoCanalEnum tipoCanal) {
        try {
            return consultarByRecorrenciaTransacao(recorrenciaTransacao)
                    .filter(consultaChaveDict -> recebedor.getCpfCnpj().equalsIgnoreCase(consultaChaveDict.getCpfCnpj()))
                    .map(consultaChaveDict -> {
                        if (TipoCanalEnum.WEB_OPENBK != tipoCanal) {
                            recorrenciaTransacao.setIdFimAFim(consultaChaveDict.getEndToEndBacen());
                        }
                        return TipoProcessamentoWrapperDTO.criarTipoLiquidacao(identificadorTransacao, recorrenciaTransacao);
                    })
                    .orElseGet(() -> TipoProcessamentoWrapperDTO.criarTipoExclusaoTotal(
                            identificadorTransacao,
                            recorrenciaTransacao,
                            AppExceptionCode.REC_PROC_BU0001.getCode(),
                            AppExceptionCode.REC_PROC_BU0001.getMessage(),
                            TipoMotivoExclusao.SOLICITADO_SISTEMA,
                            RECORRENCIA_FALHA_MUDANCA_TITULARIDADE_CHAVE
                            ));
        } catch (DictException dictException) {
            if (dictException.getHttpStatus() == HttpStatus.NOT_FOUND) {
                return TipoProcessamentoWrapperDTO.criarTipoExclusaoTotal(
                        identificadorTransacao,
                        recorrenciaTransacao,
                        AppExceptionCode.REC_PROC_BU0002.getCode(),
                        AppExceptionCode.REC_PROC_BU0002.getMessage(),
                        TipoMotivoExclusao.SOLICITADO_SISTEMA,
                        RECORRENCIA_FALHA_MUDANCA_TITULARIDADE_CHAVE
                );
            }

            if (deveProcessarExclusaoParcial(recorrenciaTransacao.getTipoCanal())) {
                return TipoProcessamentoWrapperDTO.criarTipoExclusaoParcial(
                        identificadorTransacao,
                        recorrenciaTransacao,
                        dictException.getCode(),
                        dictException.getMensagem(),
                        TipoMotivoExclusao.SOLICITADO_SISTEMA,
                        RECORRENCIA_FALHA_OPERACIONAL
                );
            }

            return TipoProcessamentoWrapperDTO.criarTipoIgnoradaComErro(identificadorTransacao, recorrenciaTransacao, dictException.getCode(), dictException.getMensagem());
        }
    }

    private Optional<DictConsultaDTO> consultarByRecorrenciaTransacao(RecorrenciaTransacao recorrenciaTransacao) {
        var recorrencia = recorrenciaTransacao.getRecorrencia();
        var pagador = recorrencia.getPagador();
        var recebedor = recorrencia.getRecebedor();
        var channelData = criarDictChannelData(recorrenciaTransacao.criarChannelData()).orElse("");
        try {
            return consultaChaveDictClient.consultaChaveDict(recebedor.getChave(), pagador.getCpfCnpj(), pagador.getAgencia(), recorrencia.getTipoCanal().name(), channelData, false);
        } catch (FeignException feignException) {
            var httpStatus = feignException.status() > 0 ? HttpStatus.valueOf(feignException.status()) : HttpStatus.INTERNAL_SERVER_ERROR;

            throw new DictException(httpStatus, AppExceptionCode.REC_PROC_BU0003.getCode(), feignException.getMessage());
        }
    }

    private Optional<String> criarDictChannelData(ChannelDataDTO channelDataDTO) {
        try {
            return Optional.ofNullable(objectMapper.writeValueAsString(channelDataDTO));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private boolean deveProcessarExclusaoParcial(TipoCanalEnum tipoCanal) {
        var dataHoraLimiteTentativa = appConfig.getRegras().getProcessamento().getHorarioLimiteLiquidacao();
        return LocalTime.now().isAfter(dataHoraLimiteTentativa) || tipoCanal == TipoCanalEnum.WEB_OPENBK;
    }

}
