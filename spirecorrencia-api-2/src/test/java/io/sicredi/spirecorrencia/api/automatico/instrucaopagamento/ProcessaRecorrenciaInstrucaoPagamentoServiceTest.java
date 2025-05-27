package io.sicredi.spirecorrencia.api.automatico.instrucaopagamento;

import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.spi.dto.Pain013Dto;
import br.com.sicredi.spi.entities.type.FinalidadeAgendamento;
import br.com.sicredi.spi.entities.type.MotivoRejeicaoPain014;
import io.sicredi.spirecorrencia.api.accountdata.AccountDataService;
import io.sicredi.spirecorrencia.api.accountdata.DadosContaResponseDTO;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.AutorizacaoService;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacao;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusAutorizacao;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import org.apache.commons.lang3.RandomStringUtils;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

import static br.com.sicredi.spi.entities.type.FinalidadeAgendamento.*;
import static br.com.sicredi.spi.entities.type.MotivoRejeicaoPain014.*;
import static io.sicredi.spirecorrencia.api.testconfig.TestFactory.IdentificadorTransacaoTestFactory.gerarIdFimAFim;
import static io.sicredi.spirecorrencia.api.testconfig.TestFactory.RecorrenciaInstrucaoPagamentoTestFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessaRecorrenciaInstrucaoPagamentoServiceTest {

    @Mock
    private RecorrenciaInstrucaoPagamentoRepository recorrenciaInstrucaoPagamentoRepository;
    @Mock
    private AutorizacaoService autorizacaoService;
    @Mock
    private AccountDataService accountDataService;
    @Mock
    private AppConfig appConfig;
    @InjectMocks
    private ProcessaRecorrenciaInstrucaoPagamentoService processaRecorrenciaInstrucaoPagamentoService;

    @ParameterizedTest
    @MethodSource("proverCenariosDeAgendamentoValido")
    void dadoPain013Valida_quandoProcessar_deveRetornarInstrucaoPagamentoComSucesso(FinalidadeAgendamento finalidadeAgendamento,
                                                                                    List<RecorrenciaInstrucaoPagamento> instrucoes) {
        var pain013Dto = pain013().finalidadeDoAgendamento(finalidadeAgendamento.name()).build();
        var ciclos = List.of(ciclo().instrucoesPagamento(instrucoes).build());
        var autorizacao = autorizacao().ciclos(ciclos).build();

        when(appConfig.getAutomatico()).thenReturn(automaticoConfig());
        when(accountDataService.consultarConta(any(), any(), any())).thenReturn(Instancio.create(DadosContaResponseDTO.class));
        when(autorizacaoService.buscarComCiclosPorIdRecorrencia(pain013Dto.getIdRecorrencia())).thenReturn(List.of(autorizacao));

        var instrucao = processaRecorrenciaInstrucaoPagamentoService.processar(pain013Dto);

        assertThat(instrucao.getCodMotivoRejeicao()).isNull();
        verify(accountDataService).consultarConta(any(), any(), any());
        verify(appConfig).getAutomatico();
        verify(autorizacaoService).buscarComCiclosPorIdRecorrencia(pain013Dto.getIdRecorrencia());
        verify(recorrenciaInstrucaoPagamentoRepository).save(any());
    }

    @ParameterizedTest
    @MethodSource("proverCenariosAutorizacaoNaoEncontrada")
    void dadoAutorizacaoNaoEncontrada_quandoProcessar_deveRetornarInstrucaoPagamentoRejeitada(List<RecorrenciaAutorizacao> autorizacoes, MotivoRejeicaoPain014 motivoRejeicao) {
        when(autorizacaoService.buscarComCiclosPorIdRecorrencia(PAIN013_AGENDAMENTO.getIdRecorrencia())).thenReturn(autorizacoes);

        var instrucao = processaRecorrenciaInstrucaoPagamentoService.processar(PAIN013_AGENDAMENTO);
        assertThat(instrucao.getCodMotivoRejeicao()).isEqualTo(motivoRejeicao.name());
    }

    @ParameterizedTest
    @MethodSource("proverExtremosDeData")
    void dadoDataVencimentoForaDoCiclo_quandoProcessar_deveRetornarInstrucaoPagamentoRejeitada(LocalDate dataDeVencimento) {
        var pain013Dto = pain013().dataVencimento(dataDeVencimento).build();
        var autorizacao = autorizacao().build();

        when(autorizacaoService.buscarComCiclosPorIdRecorrencia(pain013Dto.getIdRecorrencia())).thenReturn(List.of(autorizacao));

        var instrucao = processaRecorrenciaInstrucaoPagamentoService.processar(pain013Dto);
        assertThat(instrucao.getCodMotivoRejeicao()).isEqualTo(DIVERGENCIA_ENTRE_DATA_VENCIMENTO_E_PERIODICIDADE_RECORRENCIA.name());
    }

    @ParameterizedTest
    @MethodSource("proverCenariosAutorizacaoNaoAprovada")
    void dadoFinalidadeNaoReenvioEAutorizacaoNaoAprovada_quandoProcessar_deveRetornarInstrucaoPagamentoRejeitada(FinalidadeAgendamento finalidadeAgendamento, TipoStatusAutorizacao tipoStatusAutorizacao) {
        var pain013Dto = pain013().finalidadeDoAgendamento(finalidadeAgendamento.name()).build();
        var autorizacao = autorizacao().tipoStatus(tipoStatusAutorizacao).build();

        when(autorizacaoService.buscarComCiclosPorIdRecorrencia(pain013Dto.getIdRecorrencia())).thenReturn(List.of(autorizacao));

        var instrucao = processaRecorrenciaInstrucaoPagamentoService.processar(pain013Dto);
        assertThat(instrucao.getCodMotivoRejeicao()).isEqualTo(STATUS_RECORRENCIA_DIFERENTE_CFDB_CONFIRMADO_USUARIO_PAGADOR.name());
    }

    @Test
    void dadoReenvioEHorarioLimiteUltrapassado_quandoProcessar_deveRetornarInstrucaoPagamentoRejeitada() {
        var pain013Dto = PAIN013_REENVIO_DEVIDO_ERRO;
        var autorizacao = autorizacao().build();
        var automaticoConfig = automaticoConfig();
        automaticoConfig.getInstrucaoPagamento().setHorarioLimiteProcessamento(LocalTime.now().minusNanos(10));

        when(appConfig.getAutomatico()).thenReturn(automaticoConfig);
        when(autorizacaoService.buscarComCiclosPorIdRecorrencia(pain013Dto.getIdRecorrencia())).thenReturn(List.of(autorizacao));

        var instrucao = processaRecorrenciaInstrucaoPagamentoService.processar(pain013Dto);
        assertThat(instrucao.getCodMotivoRejeicao()).isEqualTo(PAIN_013_RECEBIDA_FORA_PRAZO.name());
    }

    @ParameterizedTest
    @MethodSource("proverCenariosCpfCnpjDiferenteDoAutorizado")
    void dadoCpfCnpjDiferenteDoAutorizado_quandoProcessar_deveRetornarInstrucaoPagamentoRejeitada(Pain013Dto pain013Dto, RecorrenciaAutorizacao autorizacao, MotivoRejeicaoPain014 motivoRejeicao) {
        when(appConfig.getAutomatico()).thenReturn(automaticoConfig());
        when(autorizacaoService.buscarComCiclosPorIdRecorrencia(pain013Dto.getIdRecorrencia())).thenReturn(List.of(autorizacao));

        var instrucao = processaRecorrenciaInstrucaoPagamentoService.processar(pain013Dto);
        assertThat(instrucao.getCodMotivoRejeicao()).isEqualTo(motivoRejeicao.name());
    }

    @ParameterizedTest
    @MethodSource("proverCenariosDeValoresDiferentesDoAutorizado")
    void dadoValorDiferenteDoAutorizado_quandoProcessar_deveRetornarInstrucaoPagamentoRejeitada(RecorrenciaAutorizacao autorizacao, MotivoRejeicaoPain014 motivoRejeicao) {
        var pain013Dto = pain013().valor(BigDecimal.TEN).build();

        when(appConfig.getAutomatico()).thenReturn(automaticoConfig());
        when(autorizacaoService.buscarComCiclosPorIdRecorrencia(pain013Dto.getIdRecorrencia())).thenReturn(List.of(autorizacao));

        var instrucao = processaRecorrenciaInstrucaoPagamentoService.processar(pain013Dto);
        assertThat(instrucao.getCodMotivoRejeicao()).isEqualTo(motivoRejeicao.name());
    }

    @ParameterizedTest
    @MethodSource("proverExtremosDeData")
    void dadoDataDeEmissaoForaDoPrazo_quandoProcessar_deveRetornarInstrucaoPagamentoRejeitada(LocalDate dataDeEmissao) {
        var pain013Dto = pain013().dataHoraCriacaoParaEmissao(dataDeEmissao.atStartOfDay()).build();
        var autorizacao = autorizacao().build();

        when(appConfig.getAutomatico()).thenReturn(automaticoConfig());
        when(autorizacaoService.buscarComCiclosPorIdRecorrencia(pain013Dto.getIdRecorrencia())).thenReturn(List.of(autorizacao));

        var instrucao = processaRecorrenciaInstrucaoPagamentoService.processar(pain013Dto);
        assertThat(instrucao.getCodMotivoRejeicao()).isEqualTo(PAIN_013_RECEBIDA_FORA_PRAZO.name());
    }

    @ParameterizedTest
    @MethodSource("proverCenariosDeInstrucaoDoCicloIncompativelComPedido")
    void dadoInstrucaoDoCicloIncompativelComPedido_quandoProcessar_deveRetornarInstrucaoPagamentoRejeitada(Pain013Dto pain013Dto, List<RecorrenciaInstrucaoPagamento> instrucoes, MotivoRejeicaoPain014 motivoRejeicao) {
        var ciclos = List.of(ciclo().instrucoesPagamento(instrucoes).build());
        var autorizacao = autorizacao().ciclos(ciclos).build();

        when(appConfig.getAutomatico()).thenReturn(automaticoConfig());
        when(autorizacaoService.buscarComCiclosPorIdRecorrencia(pain013Dto.getIdRecorrencia())).thenReturn(List.of(autorizacao));

        var instrucao = processaRecorrenciaInstrucaoPagamentoService.processar(pain013Dto);
        assertThat(instrucao.getCodMotivoRejeicao()).isEqualTo(motivoRejeicao.name());
    }

    @ParameterizedTest
    @MethodSource("proverCenariosValoresDiferentesDaInstrucaoOriginalCancelada")
    void dadoValoresDiferenteDaInstrucaoOriginalCancelada_quandoProcessar_deveRetornarInstrucaoPagamentoRejeitada(Pain013Dto pain013Dto, MotivoRejeicaoPain014 motivoRejeicao) {
        var ciclos = List.of(ciclo().instrucoesPagamento(List.of(INSTRUCAO_AGENDAMENTO_CANCELADA)).build());
        var autorizacao = autorizacao().ciclos(ciclos).build();

        when(appConfig.getAutomatico()).thenReturn(automaticoConfig());
        when(autorizacaoService.buscarComCiclosPorIdRecorrencia(pain013Dto.getIdRecorrencia())).thenReturn(List.of(autorizacao));


        var instrucao = processaRecorrenciaInstrucaoPagamentoService.processar(pain013Dto);
        assertThat(instrucao.getCodMotivoRejeicao()).isEqualTo(motivoRejeicao.name());
    }

    @Test
    void dadoRetentativaAposVencimentoEAutorizacaoNaoPermiteRetentativa_quandoProcessar_deveRetornarInstrucaoPagamentoRejeitada() {
        var pain013Dto = PAIN013_NOVA_TENTATIVA_APOS_VENC;
        var ciclos = List.of(ciclo().instrucoesPagamento(List.of(INSTRUCAO_AGENDAMENTO_CANCELADA)).build());
        var autorizacao = autorizacao().ciclos(ciclos).permiteRetentativa("N").build();

        when(appConfig.getAutomatico()).thenReturn(automaticoConfig());
        when(autorizacaoService.buscarComCiclosPorIdRecorrencia(pain013Dto.getIdRecorrencia())).thenReturn(List.of(autorizacao));

        var instrucao = processaRecorrenciaInstrucaoPagamentoService.processar(pain013Dto);
        assertThat(instrucao.getCodMotivoRejeicao()).isEqualTo(COBRANCA_RECORRENTE_NAO_PERMITE_NOVAS_TENTATIVAS_AGENDAMENTO_POS_VENCIMENTO.name());
    }

    @Test
    void dadoRetentativaAposVencimentoEUltrapassouLimiteDeDias_quandoProcessar_deveRetornarInstrucaoPagamentoRejeitada() {
        long antecedencia = 3L;
        long limiteDeDias = 7L;
        var idFimAFimOriginalCancelado = gerarIdFimAFim(LocalDateTime.now().plusDays(antecedencia));
        var idFimAFimSuperiorAoLimite = gerarIdFimAFim(LocalDateTime.now().plusDays(antecedencia + limiteDeDias + 1L));
        var pain013Dto = pain013().idFimAFim(idFimAFimSuperiorAoLimite).finalidadeDoAgendamento(AGENDADO_NOVA_TENTATIVA.name()).build();
        var instrucaoOriginalCancelada = instrucao().codFimAFim(idFimAFimOriginalCancelado).build();
        var ciclos = List.of(ciclo().instrucoesPagamento(List.of(instrucaoOriginalCancelada)).build());
        var autorizacao = autorizacao().ciclos(ciclos).build();

        when(appConfig.getAutomatico()).thenReturn(automaticoConfig());
        when(autorizacaoService.buscarComCiclosPorIdRecorrencia(pain013Dto.getIdRecorrencia())).thenReturn(List.of(autorizacao));

        var instrucao = processaRecorrenciaInstrucaoPagamentoService.processar(pain013Dto);
        assertThat(instrucao.getCodMotivoRejeicao()).isEqualTo(TENTATIVAS_AGENDAMENTO_POS_VENCIMENTO_EM_DESACORDO_LIMITE_DIAS_DEFINIDO.name());
    }

    @Test
    void dadoRetentativaAposVencimentoEMaximoTentativasAtigindo_quandoProcessar_deveRetornarInstrucaoPagamentoRejeitada() {
        var pain013Dto = PAIN013_NOVA_TENTATIVA_APOS_VENC;
        var autorizacao = autorizacao().ciclos(CICLO_COM_MAXIMO_DE_RETENTATIVAS).build();

        when(appConfig.getAutomatico()).thenReturn(automaticoConfig());
        when(autorizacaoService.buscarComCiclosPorIdRecorrencia(pain013Dto.getIdRecorrencia())).thenReturn(List.of(autorizacao));

        var instrucao = processaRecorrenciaInstrucaoPagamentoService.processar(pain013Dto);
        assertThat(instrucao.getCodMotivoRejeicao()).isEqualTo(QUANTIDADE_TENTATIVAS_AGENDAMENTO_POS_VENCIMENTO_ACIMA_DO_LIMITE_DEFINIDO.name());
    }

    @Test
    void dadoRetentativaAposVencimentoDataEmissaoMesmoDiaDataLiquidacao_quandoProcessar_deveRetornarInstrucaoPagamentoRejeitada() {
        var dataMesmoDia = LocalDateTime.now();
        var pain013Dto = pain013()
                .dataHoraCriacaoParaEmissao(dataMesmoDia)
                .idFimAFim(gerarIdFimAFim(dataMesmoDia))
                .finalidadeDoAgendamento(AGENDADO_NOVA_TENTATIVA.name())
                .build();
        var ciclos = List.of(ciclo().instrucoesPagamento(List.of(INSTRUCAO_AGENDAMENTO_CANCELADA)).build());
        var autorizacao = autorizacao().ciclos(ciclos).build();

        when(appConfig.getAutomatico()).thenReturn(automaticoConfig());
        when(autorizacaoService.buscarComCiclosPorIdRecorrencia(pain013Dto.getIdRecorrencia())).thenReturn(List.of(autorizacao));

        var instrucao = processaRecorrenciaInstrucaoPagamentoService.processar(pain013Dto);
        assertThat(instrucao.getCodMotivoRejeicao()).isEqualTo(PAIN_013_RECEBIDA_FORA_PRAZO.name());
    }

    @ParameterizedTest
    @MethodSource("proverCenariosContaPagadorInvalida")
    void dadoContaPagadorInvalida_quandoProcessar_deveRetornarInstrucaoPagamentoRejeitada(DadosContaResponseDTO contaPagador, MotivoRejeicaoPain014 motivoRejeicao) {
        var pain013Dto = PAIN013_AGENDAMENTO;
        var autorizacao = autorizacao().build();

        when(appConfig.getAutomatico()).thenReturn(automaticoConfig());
        when(accountDataService.consultarConta(any(), any(), any())).thenReturn(contaPagador);
        when(autorizacaoService.buscarComCiclosPorIdRecorrencia(pain013Dto.getIdRecorrencia())).thenReturn(List.of(autorizacao));

        var instrucao = processaRecorrenciaInstrucaoPagamentoService.processar(pain013Dto);
        assertThat(instrucao.getCodMotivoRejeicao()).isEqualTo(motivoRejeicao.name());
    }

    @Test
    void dadoFalhaAoObterContaPagador_quandoProcessar_deveRetornarInstrucaoPagamentoRejeitada() {
        var pain013Dto = PAIN013_AGENDAMENTO;
        var autorizacao = autorizacao().build();

        when(appConfig.getAutomatico()).thenReturn(automaticoConfig());
        when(accountDataService.consultarConta(any(), any(), any())).thenThrow(TechnicalException.class);
        when(autorizacaoService.buscarComCiclosPorIdRecorrencia(pain013Dto.getIdRecorrencia())).thenReturn(List.of(autorizacao));

        var instrucao = processaRecorrenciaInstrucaoPagamentoService.processar(pain013Dto);
        assertThat(instrucao.getCodMotivoRejeicao()).isEqualTo(TRANSACAO_INTERROMPIDA_ERRO_PSP_USUARIO_PAGADOR.name());
    }

    private static Stream<Arguments> proverCenariosAutorizacaoNaoEncontrada() {
        return Stream.of(
                Arguments.of(List.of(), ID_RECORRENCIA_INEXISTENTE_OU_INCORRETO),
                Arguments.of(List.of(AUTORIZACAO_SEM_CICLOS), STATUS_RECORRENCIA_DIFERENTE_CFDB_CONFIRMADO_USUARIO_PAGADOR),
                Arguments.of(List.of(AUTORIZACAO_CICLOS_VAZIOS), STATUS_RECORRENCIA_DIFERENTE_CFDB_CONFIRMADO_USUARIO_PAGADOR)
        );
    }

    private static Stream<Arguments> proverExtremosDeData() {
        return Stream.of(LocalDate.MAX, LocalDate.MIN).map(Arguments::of);
    }

    private static Stream<Arguments> proverCenariosAutorizacaoNaoAprovada() {
        return Stream.of(
                Arguments.of(AGENDADO.name(), TipoStatusAutorizacao.CANCELADA),
                Arguments.of(AGENDADO.name(), TipoStatusAutorizacao.CRIADA),
                Arguments.of(AGENDADO_NOVA_TENTATIVA, TipoStatusAutorizacao.REJEITADA),
                Arguments.of(AGENDADO_NOVA_TENTATIVA, TipoStatusAutorizacao.EXPIRADA)
        );
    }

    private static Stream<Arguments> proverCenariosCpfCnpjDiferenteDoAutorizado() {
        var cpfCnpj1 = "16126399099";
        var cpfCnpj2 = "51405286067";
        return Stream.of(
                Arguments.of(pain013().cpfCnpjUsuarioRecebedor(cpfCnpj2).build(), autorizacao().cpfCnpjRecebedor(cpfCnpj1).build(), TRANSACAO_INTERROMPIDA_ERRO_PSP_USUARIO_PAGADOR),
                Arguments.of(pain013().cpfCnpjUsuarioPagador(cpfCnpj2).build(), autorizacao().cpfCnpjPagador(cpfCnpj1).build(), CPF_CNPJ_USUARIO_PAGADOR_DIVERGENTE_RECORRENCIA_OU_AUTORIZACAO),
                Arguments.of(pain013().cpfCnpjDevedor(cpfCnpj2).build(), autorizacao().cpfCnpjDevedor(cpfCnpj1).build(), CPF_CNPJ_DEVEDOR_INVALIDO)
        );
    }

    private static Stream<Arguments> proverCenariosDeValoresDiferentesDoAutorizado() {
        return Stream.of(
                Arguments.of(autorizacao().valor(BigDecimal.ONE).build(), VALOR_COBRANCA_DIVERGENTE_AO_VALOR_ESTABELECIDO_RECORRENCIA),
                Arguments.of(autorizacao().valorMaximo(BigDecimal.ONE).build(), VALOR_COBRANCA_SUPERIOR_AO_VALOR_MAXIMO_DEFINIDO_USUARIO_PAGADOR)
        );
    }

    private static Stream<Arguments> proverCenariosDeInstrucaoDoCicloIncompativelComPedido() {
        return Stream.of(
                Arguments.of(PAIN013_AGENDAMENTO, List.of(INSTRUCAO_AGENDAMENTO_CANCELADA, INSTRUCAO_CONCLUIDA), PAGAMENTO_JA_EFETIVADO),
                Arguments.of(PAIN013_AGENDAMENTO, List.of(INSTRUCAO_AGENDAMENTO_CANCELADA, INSTRUCAO_ATIVA), COBRANCA_JA_POSSUI_PAGAMENTO_AGENDADO_PENDENTE_ENVIO_SPI),
                Arguments.of(PAIN013_REENVIO_DEVIDO_ERRO, List.of(), TRANSACAO_INTERROMPIDA_ERRO_PSP_USUARIO_PAGADOR),
                Arguments.of(PAIN013_NOVA_TENTATIVA_APOS_VENC, List.of(), TRANSACAO_INTERROMPIDA_ERRO_PSP_USUARIO_PAGADOR)
        );
    }

    private static Stream<Arguments> proverCenariosValoresDiferentesDaInstrucaoOriginalCancelada() {
        return Stream.of(
                Arguments.of(pain013().finalidadeDoAgendamento(AGENDADO_NOVA_TENTATIVA.name()).valor(BigDecimal.TEN).build(), TRANSACAO_INTERROMPIDA_ERRO_PSP_USUARIO_PAGADOR),
                Arguments.of(pain013().finalidadeDoAgendamento(REENVIO_INST_PAG_DEVIDO_ERRO.name()).valor(BigDecimal.TEN).build(), TRANSACAO_INTERROMPIDA_ERRO_PSP_USUARIO_PAGADOR),
                Arguments.of(pain013().finalidadeDoAgendamento(AGENDADO_NOVA_TENTATIVA.name()).idConciliacaoRecebedor(RandomStringUtils.random(26)).build(), NAO_CORRESPONDE_A_COBRANCA_GERADA_ANTERIORMENTE),
                Arguments.of(pain013().finalidadeDoAgendamento(REENVIO_INST_PAG_DEVIDO_ERRO.name()).idConciliacaoRecebedor(RandomStringUtils.random(26)).build(), NAO_CORRESPONDE_A_COBRANCA_GERADA_ANTERIORMENTE)
        );
    }

    private static Stream<Arguments> proverCenariosContaPagadorInvalida() {
        return Stream.of(
                Arguments.of(contaPagador().status("CANCELED").build(), CONTA_ENCERRADA_USUARIO_PAGADOR),
                Arguments.of(contaPagador().status("CLOSED").build(), CONTA_ENCERRADA_USUARIO_PAGADOR),
                Arguments.of(contaPagador().status("ACTIVE").temCreditoBloqueado(true).build(), CONTA_BLOQUEADA_USUARIO_PAGADOR),
                Arguments.of(contaPagador().status("CAPITALIZING").temCreditoBloqueado(true).build(), CONTA_BLOQUEADA_USUARIO_PAGADOR)
        );
    }

    public static Stream<Arguments> proverCenariosDeAgendamentoValido() {
        return Stream.of(
                Arguments.of(AGENDADO, List.of()),
                Arguments.of(AGENDADO, List.of(INSTRUCAO_AGENDAMENTO_CANCELADA)),
                Arguments.of(REENVIO_INST_PAG_DEVIDO_ERRO, List.of(INSTRUCAO_AGENDAMENTO_CANCELADA)),
                Arguments.of(AGENDADO_NOVA_TENTATIVA, List.of(INSTRUCAO_AGENDAMENTO_CANCELADA)),
                Arguments.of(REENVIO_INST_PAG_DEVIDO_ERRO, List.of(INSTRUCAO_NOVA_TENTATIVA_CANCELADA, INSTRUCAO_AGENDAMENTO_CANCELADA))
        );
    }
}