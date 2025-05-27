package io.sicredi.spirecorrencia.api.automatico.autorizacao;

import br.com.sicredi.canaisdigitais.enums.OrigemEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoCanalEnum;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrenciaRepository;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusAutorizacao;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusCancelamentoAutorizacao;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusSolicitacaoAutorizacao;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.idempotente.IdempotenteService;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoInformacaoAdicional;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoProducer;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessarConfirmacaoAutorizacaoExpiradasServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    AppConfig appConfig;

    @Mock
    IdempotenteService idempotenteService;
    @Mock
    RecorrenciaAutorizacaoRepository recorrenciaAutorizacaoRepository;
    @Mock
    RecorrenciaAutorizacaoCancelamentoRepository recorrenciaAutorizacaoCancelamentoRepository;
    @Mock
    SolicitacaoAutorizacaoRecorrenciaRepository solicitacaoAutorizacaoRecorrenciaRepository;
    @Mock
    NotificacaoProducer notificacaoProducer;
    @Mock
    TransactionTemplate transactionTemplate;

    @InjectMocks
    ProcessarConfirmacaoAutorizacaoExpiradasService service;

    @Captor
    ArgumentCaptor<NotificacaoDTO> notificacaoCaptor;
    @Captor
    ArgumentCaptor<RecorrenciaAutorizacaoCancelamento> cancelamentoCaptor;
    @Captor
    ArgumentCaptor<String> idRecorrenciaCaptor;
    @Captor
    ArgumentCaptor<TipoStatusSolicitacaoAutorizacao> statusAtualCaptor;
    @Captor
    ArgumentCaptor<TipoStatusSolicitacaoAutorizacao> statusNovoCaptor;
    @Captor
    ArgumentCaptor<Long> oidCaptor;
    @Captor
    ArgumentCaptor<TipoStatusAutorizacao> statusCaptor;
    @Captor
    ArgumentCaptor<String> subStatusCaptor;

    @Test
    @DisplayName("""
            Ao processar uma autorização expirada, o mesmo deve atualizar recorrencia autorizacao, criar uma solicitacao de
            autorizacao de cancelamento, atualizar a solicitação de autorização (Jornada_1) e enviar uma notificação push.
            """)
    void dadoAutorizacaoExpirada_quandoProcessar_deveAtualizarRecorrenciaCriarUmaSolicitacaoEEnviarNotificacaoComSucesso() {

        var autorizacaoExpirada = ProviderTest.criarAutorizacao(111111L, "REC-EXP", "JORNADA_1", LocalDateTime.now().minusMinutes(65L));

        when(appConfig.getRegras().getProcessamento().getLimiteExpiracaoHoras()).thenReturn(ProviderTest.LIMITE_HORAS_EXPIRACAO);
        when(recorrenciaAutorizacaoRepository.buscarRecorrenciaAutorizacaoPorStatusEDataCriacaoAntesDe(
                eq(TipoStatusAutorizacao.CRIADA), any(), any()))
                .thenAnswer(invocation -> {
                    int page = ((PageRequest) invocation.getArgument(2)).getPageNumber();
                    if (page == 0) {
                        return new PageImpl<>(List.of(autorizacaoExpirada));
                    } else {
                        return Page.empty(); // ← IMPORTANTE!
                    }
                });

        // Simula execução da transação
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Object> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        service.processarAutorizacoesExpiradas(1000, 10);

        // Atualiza a recorrencia autorizacao
        verify(recorrenciaAutorizacaoRepository).atualizarRecorrenciaAutorizacaoPorTipoStatusESubStatus(
                oidCaptor.capture(), statusCaptor.capture(), subStatusCaptor.capture());

        assertThat(oidCaptor.getValue())
                .as("OID da recorrência deve ser igual ao da autorização")
                .isEqualTo(autorizacaoExpirada.getOidRecorrenciaAutorizacao());
        assertThat(statusCaptor.getValue())
                .as("Status deve ser CANCELADA")
                .isEqualTo(TipoStatusAutorizacao.CANCELADA);
        assertThat(subStatusCaptor.getValue())
                .as("Substatus deve ser nulo")
                .isNull();

        // Captura e valida Recorrencia Autorizacao Cancelamento
        verify(recorrenciaAutorizacaoCancelamentoRepository).save(cancelamentoCaptor.capture());

        RecorrenciaAutorizacaoCancelamento cancelamento = cancelamentoCaptor.getValue();
        assertThat(cancelamento.getIdRecorrencia())
                .as("IdRecorrencia do cancelamento deve ser o mesmo idRecorrencia da autorizacao.")
                .isEqualTo(autorizacaoExpirada.getIdRecorrencia());
        assertThat(cancelamento.getTipoStatus())
                .as("Status Cancelamento deve ACEITA")
                .isEqualTo(TipoStatusCancelamentoAutorizacao.ACEITA);

        // Captura atualização da solicitação caso seja JORNADA_1
        verify(solicitacaoAutorizacaoRecorrenciaRepository).atualizarTipoStatusESubStatusPorIdRecorrenciaETipoStatusAtual(
                idRecorrenciaCaptor.capture(), statusAtualCaptor.capture(), any(), any(), statusNovoCaptor.capture()
        );
        assertThat(idRecorrenciaCaptor.getValue())
                .as("IdRecorrência deve ser igual ao da autorização")
                .isEqualTo(autorizacaoExpirada.getIdRecorrencia());
        assertThat(statusAtualCaptor.getValue())
                .as("Status atual deve ser PENDENTE_CONFIRMACAO")
                .isEqualTo(TipoStatusSolicitacaoAutorizacao.PENDENTE_CONFIRMACAO);
        assertThat(statusNovoCaptor.getValue())
                .as("Status atualizado deve ser CONFIRMADA")
                .isEqualTo(TipoStatusSolicitacaoAutorizacao.CONFIRMADA);

        // Captura e valida notificação
        verify(notificacaoProducer).enviarNotificacao(notificacaoCaptor.capture());

        NotificacaoDTO notificacao = notificacaoCaptor.getValue();
        assertNotNull(notificacao);
        assertThat(notificacao.getAgencia())
                .as("Agência Pagador deve ser igual o da autorização.")
                .isEqualTo(autorizacaoExpirada.getAgenciaPagador());
        assertThat(notificacao.getConta())
                .as("Conta Pagador deve ser igual o da autorização.")
                .isEqualTo(autorizacaoExpirada.getContaPagador());
        assertEquals(2, notificacao.getInformacoesAdicionais().size());
        assertThat(notificacao.getInformacoesAdicionais().get(NotificacaoInformacaoAdicional.NOME_RECEBEDOR.getNomeVariavel()))
                .as("Nome recebedor a ser enviado na notificação deve ser igual o da autorização.")
                .isEqualTo(autorizacaoExpirada.getNomeRecebedor());

        verify(idempotenteService, never()).reenviarOperacao(any(), any());
    }

    @Test
    @DisplayName("""
            Ao processar uma autorização não expirada, o mesmo deve reenviar a operação ao topico kafka de envio.
            """)
    void dadoAutorizacaoNaoExpirada_quandoProcessar_deveReenviarAOperacaoComSucesso() {

        var autorizacaoNaoExpirada = ProviderTest.criarAutorizacao(222222L, "REC-VAL", "JORNADA_1", LocalDateTime.now().minusMinutes(2L));

        when(appConfig.getRegras().getProcessamento().getLimiteExpiracaoHoras()).thenReturn(ProviderTest.LIMITE_HORAS_EXPIRACAO);
        when(appConfig.getKafka().getProducer().getIcomPainEnvio().getTopico()).thenReturn(ProviderTest.TOPICO_ENVIO);
        when(appConfig.getRegras().getProcessamento().isReenvioOperacaoHabilitado()).thenReturn(Boolean.TRUE);
        when(recorrenciaAutorizacaoRepository
                .buscarRecorrenciaAutorizacaoPorStatusEDataCriacaoAntesDe(eq(TipoStatusAutorizacao.CRIADA), any(), any()))
                .thenAnswer(invocation -> {
                    PageRequest pageRequest = invocation.getArgument(2);
                    int page = pageRequest.getPageNumber();

                    if (page == 0) {
                        return new PageImpl<>(List.of(autorizacaoNaoExpirada), pageRequest, 1);
                    } else {
                        return Page.empty();
                    }
                });

        service.processarAutorizacoesExpiradas(10, 240);

        verify(idempotenteService).reenviarOperacao(ProviderTest.TOPICO_ENVIO, "info-envio-222222_JORNADA1B");
        verifyNoInteractions(recorrenciaAutorizacaoCancelamentoRepository);
        verify(recorrenciaAutorizacaoRepository, never()).atualizarRecorrenciaAutorizacaoPorTipoStatusESubStatus(any(), any(), any());
    }

    @Test
    @DisplayName("""
            Ao processar uma autorização não expirada jornada_2, o mesmo deve reenviar a operação ao topico kafka de envio,
            com uma chave de idempotencia = 'info-envio-333333_CADASTRO_AUTN'
            """)
    void dadoAutorizacaoNaoExpiradaJornada2_quandoProcessar_deveReenviarAOperacaoComSucesso() {
        var autorizacaoNaoExpiradaJornadaDois = ProviderTest.criarAutorizacao(333333L, "REC-UAT", "JORNADA_2", LocalDateTime.now().minusMinutes(2L));

        when(appConfig.getRegras().getProcessamento().getLimiteExpiracaoHoras()).thenReturn(ProviderTest.LIMITE_HORAS_EXPIRACAO);
        when(appConfig.getKafka().getProducer().getIcomPainEnvio().getTopico()).thenReturn(ProviderTest.TOPICO_ENVIO);
        when(appConfig.getRegras().getProcessamento().isReenvioOperacaoHabilitado()).thenReturn(Boolean.TRUE);
        when(recorrenciaAutorizacaoRepository
                .buscarRecorrenciaAutorizacaoPorStatusEDataCriacaoAntesDe(eq(TipoStatusAutorizacao.CRIADA), any(), any()))
                .thenAnswer(invocation -> {
                    PageRequest pageRequest = invocation.getArgument(2);
                    int page = pageRequest.getPageNumber();

                    if (page == 0) {
                        return new PageImpl<>(List.of(autorizacaoNaoExpiradaJornadaDois), pageRequest, 1);
                    } else {
                        return Page.empty();
                    }
                });

        service.processarAutorizacoesExpiradas(10, 240);

        verify(idempotenteService).reenviarOperacao(eq(ProviderTest.TOPICO_ENVIO), eq("info-envio-333333_CADASTRO_AUTN"));
        verifyNoInteractions(recorrenciaAutorizacaoCancelamentoRepository);
        verify(recorrenciaAutorizacaoRepository, never()).atualizarRecorrenciaAutorizacaoPorTipoStatusESubStatus(any(), any(), any());
    }

    @Test
    @DisplayName("""
            Ao processar uma autorização expirada de uma jornada diferente da Jornada_1, o mesmo deve atualizar recorrencia
            autorizacao, criar uma solicitacao de autorizacao de cancelamento, e nao deve atualizar a solicitação de
            autorização (Jornada_1) e enviar uma notificação push.
            """)
    void dadoAutorizacaoExpiradaDeJornada_2_quandoProcessar_deveCancelarAOperacaoPoremNaoDeveAtualizarASolicitacaoAutorizacao() {

        var autorizacaoOutraJornada = ProviderTest.criarAutorizacao(444444L, "REC-J2", "JORNADA_2", LocalDateTime.now().minusMinutes(5L));

        // Simula execução da transação
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Object> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        when(recorrenciaAutorizacaoRepository
                .buscarRecorrenciaAutorizacaoPorStatusEDataCriacaoAntesDe(eq(TipoStatusAutorizacao.CRIADA), any(), any()))
                .thenAnswer(invocation -> {
                    PageRequest pageRequest = invocation.getArgument(2);
                    int page = pageRequest.getPageNumber();

                    if (page == 0) {
                        return new PageImpl<>(List.of(autorizacaoOutraJornada), pageRequest, 1);
                    } else {
                        return Page.empty();
                    }
                });

        service.processarAutorizacoesExpiradas(10, 240);

        verify(recorrenciaAutorizacaoRepository).atualizarRecorrenciaAutorizacaoPorTipoStatusESubStatus(eq(444444L), eq(TipoStatusAutorizacao.CANCELADA), isNull());
        verify(recorrenciaAutorizacaoCancelamentoRepository).save(any());
        verify(notificacaoProducer).enviarNotificacao(any());
        verify(solicitacaoAutorizacaoRecorrenciaRepository, never()).atualizarTipoStatusESubStatusPorIdRecorrenciaETipoStatusAtual(any(), any(), any(), any(), any());
    }

    @Test
    void deveTratarErroDuranteReenvioDeOperacao() {
        var autorizacaoExpirada = ProviderTest.criarAutorizacao(111111L, "REC-EXP", "JORNADA_1", LocalDateTime.now().minusMinutes(65L));

        when(appConfig.getRegras().getProcessamento().getLimiteExpiracaoHoras()).thenReturn(ProviderTest.LIMITE_HORAS_EXPIRACAO);
        when(recorrenciaAutorizacaoRepository
                .buscarRecorrenciaAutorizacaoPorStatusEDataCriacaoAntesDe(eq(TipoStatusAutorizacao.CRIADA), any(), any()))
                .thenAnswer(invocation -> {
                    PageRequest pageRequest = invocation.getArgument(2);
                    int page = pageRequest.getPageNumber();

                    if (page == 0) {
                        return new PageImpl<>(List.of(autorizacaoExpirada), pageRequest, 1);
                    } else {
                        return Page.empty();
                    }
                });
        // Simula execução da transação
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<Object> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        doThrow(new RuntimeException("Erro interno")).when(recorrenciaAutorizacaoRepository)
                .atualizarRecorrenciaAutorizacaoPorTipoStatusESubStatus(
                        oidCaptor.capture(), statusCaptor.capture(), subStatusCaptor.capture());

        assertDoesNotThrow(() -> service.processarAutorizacoesExpiradas(10, 240));
        verifyNoInteractions(recorrenciaAutorizacaoCancelamentoRepository);
        verifyNoInteractions(solicitacaoAutorizacaoRecorrenciaRepository);
        verifyNoInteractions(notificacaoProducer);
    }

    @Test
    @DisplayName("""
            Ao processar uma autorização não expirada jornada_2, porem que esteja com a flag isReenvioOperacaoHabilitado = false
            o mesmo não deve reenviar a operação ao topico kafka de envio.
            """)
    void dadoAutorizacaoNaoExpiradaComReenvioOperacaoDesabilitado_quandoProcessar_deveNaoReenviarAOperacao() {
        var autorizacaoNaoExpiradaJornadaDois = ProviderTest.criarAutorizacao(333333L, "REC-UAT", "JORNADA_2", LocalDateTime.now().minusMinutes(2L));

        when(appConfig.getRegras().getProcessamento().getLimiteExpiracaoHoras()).thenReturn(ProviderTest.LIMITE_HORAS_EXPIRACAO);
        when(appConfig.getRegras().getProcessamento().isReenvioOperacaoHabilitado()).thenReturn(Boolean.FALSE);
        when(recorrenciaAutorizacaoRepository
                .buscarRecorrenciaAutorizacaoPorStatusEDataCriacaoAntesDe(eq(TipoStatusAutorizacao.CRIADA), any(), any()))
                .thenAnswer(invocation -> {
                    PageRequest pageRequest = invocation.getArgument(2);
                    int page = pageRequest.getPageNumber();

                    if (page == 0) {
                        return new PageImpl<>(List.of(autorizacaoNaoExpiradaJornadaDois), pageRequest, 1);
                    } else {
                        return Page.empty();
                    }
                });

        service.processarAutorizacoesExpiradas(10, 240);

        verifyNoInteractions(idempotenteService);
    }

    private static final class ProviderTest {

        public static final String CPF_CNPJ_PAGADOR = "12345678901";
        public static final String NOME_RECEBEDOR = "Recebedor Teste";
        public static final String AGENCIA_PAGADOR = "0001";
        public static final String CONTA_PAGADOR = "123456";
        public static final String TOPICO_ENVIO = "topico-envio";
        public static final Long LIMITE_HORAS_EXPIRACAO = 1L;

        private static RecorrenciaAutorizacao criarAutorizacao(Long oidRecorrenciaAutorizacao,
                                                               String idRecorrencia,
                                                               String jornada, LocalDateTime inicioConfirmacao) {
            return RecorrenciaAutorizacao.builder()
                    .oidRecorrenciaAutorizacao(oidRecorrenciaAutorizacao)
                    .idRecorrencia(idRecorrencia)
                    .tipoJornada(jornada)
                    .dataInicioConfirmacao(inicioConfirmacao)
                    .cpfCnpjPagador(CPF_CNPJ_PAGADOR)
                    .nomeRecebedor(NOME_RECEBEDOR)
                    .agenciaPagador(AGENCIA_PAGADOR)
                    .contaPagador(CONTA_PAGADOR)
                    .tipoCanalPagador(TipoCanalEnum.MOBI)
                    .tipoSistemaPagador(OrigemEnum.FISITAL)
                    .idInformacaoStatusEnvio(StringUtils.join("info-envio-", oidRecorrenciaAutorizacao))
                    .build();
        }
    }
}
