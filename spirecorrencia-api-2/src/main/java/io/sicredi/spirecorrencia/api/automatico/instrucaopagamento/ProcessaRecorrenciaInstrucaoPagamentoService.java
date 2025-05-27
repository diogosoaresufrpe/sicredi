package io.sicredi.spirecorrencia.api.automatico.instrucaopagamento;

import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.spi.dto.Pain013Dto;
import br.com.sicredi.spi.entities.type.FinalidadeAgendamento;
import br.com.sicredi.spi.entities.type.MotivoRejeicaoPain014;
import io.sicredi.spirecorrencia.api.accountdata.AccountDataService;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.AutorizacaoService;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusAutorizacao;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.chrono.ChronoLocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static br.com.sicredi.spi.entities.type.MotivoRejeicaoPain014.*;
import static io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusInstrucaoPagamento.*;
import static io.sicredi.spirecorrencia.api.utils.IdentificadorTransacaoUtils.extrairData;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Comparator.comparing;

@Service
@AllArgsConstructor
@Slf4j
class ProcessaRecorrenciaInstrucaoPagamentoService {
    private final AccountDataService accountDataService;
    private final AppConfig appConfig;
    private final AutorizacaoService autorizacaoService;
    private final RecorrenciaInstrucaoPagamentoRepository recorrenciaInstrucaoPagamentoRepository;

    public RecorrenciaInstrucaoPagamento processar(Pain013Dto pain013) {
        var instrucao = RecorrenciaInstrucaoPagamentoFactory.criar(pain013);

        Optional<MotivoRejeicaoPain014> erro = processarAutorizacoes(instrucao)
                .or(() -> processarCiclo(instrucao))
                .or(() -> validarStatusAutorizacao(instrucao))
                .or(() -> validarHorarioProcessamentoLimite(instrucao))
                .or(() -> validarCpfCnpjRecebedor(instrucao))
                .or(() -> validarCpfCnpjPagador(instrucao))
                .or(() -> validarCpfCnpjDevedor(instrucao))
                .or(() -> validarValorMaximo(instrucao))
                .or(() -> validarValorFixo(instrucao))
                .or(() -> validarPrazoDeRecebimentoDeInstrucao(instrucao))
                .or(() -> processarInstrucoesDePagamentoDoCicloAberto(instrucao))
                .or(() -> validarContaPagador(instrucao));

        erro.ifPresent(motivoRejeicaoPain014 ->
                instrucao.setCodMotivoRejeicao(motivoRejeicaoPain014.name()));

        recorrenciaInstrucaoPagamentoRepository.save(instrucao);

        return instrucao;
    }

    private Optional<MotivoRejeicaoPain014> processarAutorizacoes(RecorrenciaInstrucaoPagamento instrucao) {
        var autorizacoes = autorizacaoService.buscarComCiclosPorIdRecorrencia(instrucao.getIdRecorrencia());

        if (autorizacoes.isEmpty()) {
            log.info("processarAutorizacoes -> MIDI - Nenhuma autorização encontrada");
            return Optional.of(ID_RECORRENCIA_INEXISTENTE_OU_INCORRETO);
        }

        var autorizacaoComCiclos = autorizacoes.stream()
                .filter(autorizacao -> autorizacao.getCiclos() != null && !autorizacao.getCiclos().isEmpty())
                .findFirst();

        if (autorizacaoComCiclos.isEmpty()) {
            log.info("processarAutorizacoes -> MSUC - Autorização não está aprovada");
            return Optional.of(STATUS_RECORRENCIA_DIFERENTE_CFDB_CONFIRMADO_USUARIO_PAGADOR);
        }

        instrucao.setAutorizacao(autorizacaoComCiclos.get());

        return Optional.empty();
    }

    private Optional<MotivoRejeicaoPain014> processarCiclo(RecorrenciaInstrucaoPagamento instrucao) {
        var ciclo = instrucao.getAutorizacao().getCiclos().stream()
                .filter(c -> isDataDentroDoCiclo(instrucao.getDatVencimento(), c))
                .findFirst();

        if (ciclo.isEmpty()) {
            log.info("processarCiclo -> DTED - Data de vencimento não está dentro do ciclo aberto da recorrência");
            return Optional.of(DIVERGENCIA_ENTRE_DATA_VENCIMENTO_E_PERIODICIDADE_RECORRENCIA);
        }

        instrucao.setCiclo(ciclo.get());
        return Optional.empty();
    }


    private Optional<MotivoRejeicaoPain014> validarHorarioProcessamentoLimite(RecorrenciaInstrucaoPagamento instrucao) {
        var horarioLimite = appConfig.getAutomatico().getInstrucaoPagamento().getHorarioLimiteProcessamento();
        if (isReenvioDevidoErro(instrucao)
                && LocalTime.now().isAfter(horarioLimite)) {
            log.info("validarHorarioProcessamentoLimite -> FBRD - PAIN 013 recebida fora do prazo");
            return Optional.of(PAIN_013_RECEBIDA_FORA_PRAZO);
        }
        return Optional.empty();
    }

    private Optional<MotivoRejeicaoPain014> validarStatusAutorizacao(RecorrenciaInstrucaoPagamento instrucao) {
        if (!isReenvioDevidoErro(instrucao)
                && instrucao.getAutorizacao().getTipoStatus() != TipoStatusAutorizacao.APROVADA) {
            log.info("validarStatusAutorizacao -> MSUC - Autorização não está aprovada");
            return Optional.of(STATUS_RECORRENCIA_DIFERENTE_CFDB_CONFIRMADO_USUARIO_PAGADOR);
        }
        return Optional.empty();
    }

    private Optional<MotivoRejeicaoPain014> validarCpfCnpjRecebedor(RecorrenciaInstrucaoPagamento instrucao) {
        if (!Objects.equals(instrucao.getNumCpfCnpjRecebedor(), instrucao.getAutorizacao().getCpfCnpjRecebedor())) {
            log.info("validarCpfCnpjRecebedor -> AB10 - CPF/CNPJ do recebedor não confere com o da autorização");
            return Optional.of(TRANSACAO_INTERROMPIDA_ERRO_PSP_USUARIO_PAGADOR);
        }
        return Optional.empty();
    }

    private Optional<MotivoRejeicaoPain014> validarCpfCnpjPagador(RecorrenciaInstrucaoPagamento instrucao) {
        if (!Objects.equals(instrucao.getNumCpfCnpjPagador(), instrucao.getAutorizacao().getCpfCnpjPagador())) {
            log.info("validarCpfCnpjPagador -> DENC - CPF/CNPJ do pagador não confere com o da autorização");
            return Optional.of(CPF_CNPJ_USUARIO_PAGADOR_DIVERGENTE_RECORRENCIA_OU_AUTORIZACAO);
        }
        return Optional.empty();
    }

    private Optional<MotivoRejeicaoPain014> validarCpfCnpjDevedor(RecorrenciaInstrucaoPagamento instrucao) {
        if (!Objects.equals(instrucao.getNumCpfCnpjDevedor(), instrucao.getAutorizacao().getCpfCnpjDevedor())) {
            log.info("validarCpfCnpjDevedor -> UDEI - CPF/CNPJ do devedor não confere com o da autorização");
            return Optional.of(CPF_CNPJ_DEVEDOR_INVALIDO);
        }
        return Optional.empty();
    }

    private Optional<MotivoRejeicaoPain014> validarValorMaximo(RecorrenciaInstrucaoPagamento instrucao) {
        if (!isReenvioDevidoErro(instrucao)
                && instrucao.getAutorizacao().getValorMaximo() != null
                && instrucao.getNumValor().compareTo(instrucao.getAutorizacao().getValorMaximo()) > 0) {
            log.info("validarValorMaximo -> AM02 - Valor de cobrança superior ao valor máximo definido pelo usuário pagador");
            return Optional.of(VALOR_COBRANCA_SUPERIOR_AO_VALOR_MAXIMO_DEFINIDO_USUARIO_PAGADOR);
        }
        return Optional.empty();
    }

    private Optional<MotivoRejeicaoPain014> validarValorFixo(RecorrenciaInstrucaoPagamento instrucao) {
        if (instrucao.getAutorizacao().getValor() != null
                && instrucao.getNumValor().compareTo(instrucao.getAutorizacao().getValor()) != 0) {
            log.info("validarValorFixo -> AM09 - Valor de cobrança divergente ao valor estabelecido na recorrência");
            return Optional.of(VALOR_COBRANCA_DIVERGENTE_AO_VALOR_ESTABELECIDO_RECORRENCIA);
        }
        return Optional.empty();
    }

    private Optional<MotivoRejeicaoPain014> processarInstrucoesDePagamentoDoCicloAberto(RecorrenciaInstrucaoPagamento instrucao) {
        var instrucoesDoCicloAberto = instrucao.getCiclo().getInstrucoesPagamento().stream()
                .collect(Collectors.groupingBy(RecorrenciaInstrucaoPagamento::getTpoStatus));

        if (instrucoesDoCicloAberto.get(CONCLUIDA.name()) != null) {
            log.info("validarComInstrucoesDePagamentoDoCicloAberto -> NIPA - Já existe pagamento efetivado para a cobrança");
            return Optional.of(PAGAMENTO_JA_EFETIVADO);
        }

        if (instrucoesDoCicloAberto.get(ATIVA.name()) != null) {
            log.info("validarComInstrucoesDePagamentoDoCicloAberto -> NIEC - Já existe pagamento agendado para a cobrança");
            return Optional.of(COBRANCA_JA_POSSUI_PAGAMENTO_AGENDADO_PENDENTE_ENVIO_SPI);
        }

        var instrucaoOriginal = consultarInstrucaoOriginal(instrucao, instrucoesDoCicloAberto);

        if (!isAgendamento(instrucao) && instrucaoOriginal.isEmpty()) {
            log.info("validarComInstrucoesDePagamentoDoCicloAberto -> AB10 - Instrução tem finalidade de reenvio ou retentativa e não existe instrução de agendamento cancelada");
            return Optional.of(TRANSACAO_INTERROMPIDA_ERRO_PSP_USUARIO_PAGADOR);
        }

        if (!isAgendamento(instrucao) && instrucaoOriginal.isPresent()) {
            return validarValorInstrucaoOriginal(instrucao, instrucaoOriginal.get())
                    .or(() -> validarTxidInstrucaoOriginal(instrucao, instrucaoOriginal.get()))
                    .or(() -> validarPermiteRetentativaAposVencimento(instrucao))
                    .or(() -> validarDataEmissaoDeTentativaAposVencimento(instrucao))
                    .or(() -> validarLiteDeDiasAposVencimento(instrucao, instrucaoOriginal.get()))
                    .or(() -> validarQuantidadeDeTentativasAposVencimento(instrucao));
        }

        return Optional.empty();
    }

    private Optional<RecorrenciaInstrucaoPagamento> consultarInstrucaoOriginal(RecorrenciaInstrucaoPagamento instrucao, Map<String, List<RecorrenciaInstrucaoPagamento>> instrucoesDoCicloAberto) {
        if (isReenvioDevidoErro(instrucao)) {
            var mapaFinalidades = Optional.ofNullable(instrucoesDoCicloAberto.get(CANCELADA.name()))
                    .orElse(List.of())
                    .stream()
                    .filter(instrucaoCancelada -> isAgendamento(instrucaoCancelada) || isRetentativaAposVencimento(instrucaoCancelada))
                    .collect(Collectors.groupingBy(RecorrenciaInstrucaoPagamento::getTpoFinalidadeAgendamento));

            if (mapaFinalidades.containsKey(FinalidadeAgendamento.AGENDADO_NOVA_TENTATIVA.name())) {
                return mapaFinalidades.get(FinalidadeAgendamento.AGENDADO_NOVA_TENTATIVA.name()).stream()
                        .max(comparing(RecorrenciaInstrucaoPagamento::getDatCriacaoRegistro));
            }
            if (mapaFinalidades.containsKey(FinalidadeAgendamento.AGENDADO.name())) {
                return mapaFinalidades.get(FinalidadeAgendamento.AGENDADO.name()).stream()
                        .max(comparing(RecorrenciaInstrucaoPagamento::getDatCriacaoRegistro));
            }
        }
        if (isRetentativaAposVencimento(instrucao)) {
            return Optional.ofNullable(instrucoesDoCicloAberto.get(CANCELADA.name()))
                    .orElse(List.of())
                    .stream()
                    .filter(this::isAgendamento)
                    .max(comparing(RecorrenciaInstrucaoPagamento::getDatCriacaoRegistro));
        }
        return Optional.empty();
    }

    private Optional<MotivoRejeicaoPain014> validarValorInstrucaoOriginal(RecorrenciaInstrucaoPagamento instrucao, RecorrenciaInstrucaoPagamento instrucaoOriginalDeAgendamentoCancelada) {
        if (instrucao.getNumValor().compareTo(instrucaoOriginalDeAgendamentoCancelada.getNumValor()) != 0) {
            log.info("validarValorInstrucaoOriginal -> AB10 - Valor da nova tentativa de agendamento não confere com o valor da instrução original");
            return Optional.of(TRANSACAO_INTERROMPIDA_ERRO_PSP_USUARIO_PAGADOR);
        }
        return Optional.empty();
    }

    private Optional<MotivoRejeicaoPain014> validarTxidInstrucaoOriginal(RecorrenciaInstrucaoPagamento instrucao, RecorrenciaInstrucaoPagamento instrucaoOriginalDeAgendamentoCancelada) {
        if (!Objects.equals(instrucao.getIdConciliacaoRecebedor(), instrucaoOriginalDeAgendamentoCancelada.getIdConciliacaoRecebedor())) {
            log.info("validarTxidInstrucaoOriginal -> NITX - Txid da nova tentativa de agendamento não confere com o txid da instrução original");
            return Optional.of(NAO_CORRESPONDE_A_COBRANCA_GERADA_ANTERIORMENTE);
        }
        return Optional.empty();
    }

    private Optional<MotivoRejeicaoPain014> validarPrazoDeRecebimentoDeInstrucao(RecorrenciaInstrucaoPagamento instrucao) {
        if (isAgendamento(instrucao)) {
            var dataPrevistaLiquidacao = extrairData(instrucao.getCodFimAFim());
            var dataDaEmissao = Optional.ofNullable(instrucao.getDatEmissao()).orElseGet(LocalDateTime::now);
            long diferencaEmDias = DAYS.between(dataDaEmissao, dataPrevistaLiquidacao);
            if (diferencaEmDias < 2 || diferencaEmDias > 10) {
                log.info("validarPrazoDeRecebimentoDeInstrucao -> FBRD - PAIN 013 recebida fora do prazo limite entre 2 a 10 dias de antecedência");
                return Optional.of(PAIN_013_RECEBIDA_FORA_PRAZO);
            }
        }
        return Optional.empty();
    }

    private Optional<MotivoRejeicaoPain014> validarPermiteRetentativaAposVencimento(RecorrenciaInstrucaoPagamento instrucao) {
        if (isRetentativaAposVencimento(instrucao) &&
                !"S".equals(instrucao.getAutorizacao().getPermiteRetentativa())) {
            log.info("validarPermiteRetentativaAposVencimento -> IRNT - Cobrança recorrente não permite novas tentativas de agendamento");
            return Optional.of(COBRANCA_RECORRENTE_NAO_PERMITE_NOVAS_TENTATIVAS_AGENDAMENTO_POS_VENCIMENTO);
        }
        return Optional.empty();
    }

    private Optional<MotivoRejeicaoPain014> validarLiteDeDiasAposVencimento(RecorrenciaInstrucaoPagamento instrucao, RecorrenciaInstrucaoPagamento instrucaoOriginalDeAgendamentoCancelada) {
        if (isRetentativaAposVencimento(instrucao)) {

            var dataPrevistaLiquidacao = extrairData(instrucao.getCodFimAFim());
            var dataPrevistaLiquidacaoOriginal = extrairData(instrucaoOriginalDeAgendamentoCancelada.getCodFimAFim());

            if (DAYS.between(dataPrevistaLiquidacaoOriginal, dataPrevistaLiquidacao) > 7) {
                log.info("validarLiteDeDiasAposVencimento -> DTNT - Data de vencimento da nova tentativa de agendamento está fora do limite definido de 7 dias");
                return Optional.of(TENTATIVAS_AGENDAMENTO_POS_VENCIMENTO_EM_DESACORDO_LIMITE_DIAS_DEFINIDO);
            }
        }

        return Optional.empty();
    }

    private Optional<MotivoRejeicaoPain014> validarQuantidadeDeTentativasAposVencimento(RecorrenciaInstrucaoPagamento instrucao) {
        if (isRetentativaAposVencimento(instrucao)) {

            var qtdTentativasAposVencimento = instrucao.getCiclo()
                    .getInstrucoesPagamento()
                    .stream()
                    .filter(this::isTentativaAposVencimentoCancelada)
                    .count();

            if (qtdTentativasAposVencimento >= 3) {
                log.info("validarQuantidadeDeTentativasAposVencimento -> QUNT - Quantidade de tentativas de agendamento após vencimento está acima do limite definido");
                return Optional.of(QUANTIDADE_TENTATIVAS_AGENDAMENTO_POS_VENCIMENTO_ACIMA_DO_LIMITE_DEFINIDO);
            }
        }

        return Optional.empty();
    }

    private Optional<MotivoRejeicaoPain014> validarDataEmissaoDeTentativaAposVencimento(RecorrenciaInstrucaoPagamento instrucao) {
        var dataPrevistaLiquidacao = extrairData(instrucao.getCodFimAFim());
        var dataDaEmissao = Optional.ofNullable(instrucao.getDatEmissao()).orElseGet(LocalDateTime::now);
        var dataLimite = dataPrevistaLiquidacao.minusDays(1).toLocalDate().atTime(LocalTime.MAX);
        if (dataDaEmissao.isAfter(dataLimite)) {
            log.info("validarHorarioRecebimentoDeTentativaAposVencimento -> FBRD - PAIN 013 recebida fora do prazo limite do dia anterior a data prevista para liquidação com finalidade NTAG");
            return Optional.of(PAIN_013_RECEBIDA_FORA_PRAZO);
        }
        return Optional.empty();
    }

    private Optional<MotivoRejeicaoPain014> validarContaPagador(RecorrenciaInstrucaoPagamento instrucao) {
        try {
            var contaPagador = accountDataService.consultarConta(
                    instrucao.getAutorizacao().getCpfCnpjPagador(),
                    instrucao.getAutorizacao().getAgenciaPagador(),
                    instrucao.getAutorizacao().getContaPagador()
            );

            if ("CANCELED".equals(contaPagador.status()) || "CLOSED".equals(contaPagador.status())) {
                log.info("validarContaPagador -> AC05 - Conta do usuário pagador está encerrada");
                return Optional.of(CONTA_ENCERRADA_USUARIO_PAGADOR);
            }

            if (("ACTIVE".equals(contaPagador.status()) || "CAPITALIZING".equals(contaPagador.status()))
                    && (contaPagador.temCreditoBloqueado() || contaPagador.temDebitoBloqueado())) {
                log.info("validarContaPagador -> AC06 - Conta do usuário pagador está bloqueada");
                return Optional.of(CONTA_BLOQUEADA_USUARIO_PAGADOR);
            }
        } catch (TechnicalException e) {
            log.info("validarContaPagador -> AB10 - Falha ao consultar conta do usuário pagador");
            return Optional.of(TRANSACAO_INTERROMPIDA_ERRO_PSP_USUARIO_PAGADOR);
        }
        return Optional.empty();
    }

    private boolean isAgendamento(RecorrenciaInstrucaoPagamento instrucao) {
        return Objects.equals(instrucao.getTpoFinalidadeAgendamento(), FinalidadeAgendamento.AGENDADO.name());
    }

    private boolean isDataDentroDoCiclo(ChronoLocalDate data, RecorrenciaAutorizacaoCiclo ciclo) {
        return (ciclo.getDataInicial().isBefore(data) || ciclo.getDataInicial().isEqual(data)) &&
                (ciclo.getDataFinal().isAfter(data) || ciclo.getDataFinal().isEqual(data));
    }

    private boolean isTentativaAposVencimentoCancelada(RecorrenciaInstrucaoPagamento instrucao) {
        return isRetentativaAposVencimento(instrucao)
                && Objects.equals(instrucao.getTpoStatus(), CANCELADA.name());
    }

    private boolean isReenvioDevidoErro(RecorrenciaInstrucaoPagamento instrucao) {
        return Objects.equals(instrucao.getTpoFinalidadeAgendamento(), FinalidadeAgendamento.REENVIO_INST_PAG_DEVIDO_ERRO.name());
    }

    private boolean isRetentativaAposVencimento(RecorrenciaInstrucaoPagamento instrucao) {
        return Objects.equals(instrucao.getTpoFinalidadeAgendamento(), FinalidadeAgendamento.AGENDADO_NOVA_TENTATIVA.name());
    }
}