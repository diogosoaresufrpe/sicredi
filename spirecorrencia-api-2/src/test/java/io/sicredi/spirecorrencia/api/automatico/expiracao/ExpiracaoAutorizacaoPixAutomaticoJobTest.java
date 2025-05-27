package io.sicredi.spirecorrencia.api.automatico.expiracao;

import io.micrometer.tracing.Tracer;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrencia;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoProducer;
import io.sicredi.spirecorrencia.api.utils.NotificadorProcessadorPaginacaoUtil;
import io.sicredi.spiutils.core.lib.commons.observabilidade.tracing.ObservabilidadeDecorator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static br.com.sicredi.canaisdigitais.enums.OrigemEnum.FISITAL;
import static br.com.sicredi.canaisdigitais.enums.OrigemEnum.LEGADO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpiracaoAutorizacaoPixAutomaticoJobTest {

    AppConfig.JobShedLock jobShedLock = Mockito.mock(AppConfig.JobShedLock.class);

    @Mock
    private Tracer tracer;

    @Mock
    private AppConfig appConfig;

    @Mock
    private NotificadorProcessadorPaginacaoUtil notificadorProcessadorPaginacaoUtil;

    @Mock
    private ConsultaSolicitacaoExpiracaoPixAutomaticoService consultaSolicitacaoExpiracaoPixAutomaticoService;

    @Mock
    private NotificacaoProducer producer;

    private ObservabilidadeDecorator observabilidadeDecorator;

    private ExpiracaoAutorizacaoPixAutomaticoJob job;

    @Nested
    @DisplayName("Dado job desabilitado")
    class DadoJobDesabilitado {
        @BeforeEach
        void setUp() {
            observabilidadeDecorator = Mockito.spy(new ObservabilidadeDecorator(tracer));
            job = new ExpiracaoAutorizacaoPixAutomaticoJob(
                    appConfig,
                    observabilidadeDecorator,
                    notificadorProcessadorPaginacaoUtil,
                    consultaSolicitacaoExpiracaoPixAutomaticoService,
                    producer
            );

            when(appConfig.getJobNotificaoExpiracaoPixAutomatico()).thenReturn(jobShedLock);
            when(jobShedLock.isJobHabilitado()).thenReturn(false);
        }

        @Test
        @DisplayName("Quando executar, deve ignorar processamento")
        void dadoJobDesabilitado_quandoExecutar_deveIgnorarProcessamento() {
            when(appConfig.getJobNotificaoExpiracaoPixAutomatico()).thenReturn(jobShedLock);
            when(jobShedLock.isJobHabilitado()).thenReturn(false);

            job.executar();

            verifyNoInteractions(observabilidadeDecorator, notificadorProcessadorPaginacaoUtil, consultaSolicitacaoExpiracaoPixAutomaticoService);
        }
    }

    @Nested
    @DisplayName("Dado job habilitado")
    class DadoJobHabilitado {

        public static final String AGENCIA = "1234";
        public static final String CPF_CNPJ = "12345678901";
        public static final String CONTA = "12345";
        private static final String VALOR = "200.50";
        private static final String DATA = "19/05/2025";


        @BeforeEach
        void setUp() {
            observabilidadeDecorator = Mockito.spy(new ObservabilidadeDecorator(tracer));
            job = new ExpiracaoAutorizacaoPixAutomaticoJob(appConfig,
                    observabilidadeDecorator,
                    notificadorProcessadorPaginacaoUtil,
                    consultaSolicitacaoExpiracaoPixAutomaticoService,
                    producer);

            when(jobShedLock.isJobHabilitado()).thenReturn(true);
            when(jobShedLock.getNomeJob()).thenReturn("AgendadoExpiracaoSolicitacaoPixAutomaticoJob");
        }

        @Test
        @DisplayName("Dado nenhuma solicitação para notificar, quando executar, deve ignorar processamento")
        void dadoNenhumaTransacaoParaNotificar_quandoExecutar_deveIgnorarProcessamento() {
            when(jobShedLock.getTamanhoDaConsulta()).thenReturn(10);
            when(appConfig.getJobNotificaoExpiracaoPixAutomatico()).thenReturn(jobShedLock);

            job.executar();

            verify(appConfig, times(3)).getJobNotificaoExpiracaoPixAutomatico();
            verify(jobShedLock).isJobHabilitado();
            verify(consultaSolicitacaoExpiracaoPixAutomaticoService,times(1)).buscarSolicitacoesExpiradas(any(Pageable.class));
            verify(notificadorProcessadorPaginacaoUtil,times(1)).processaPaginacoesEnviaNotificacao(any(),any(),any());
        }

        @Test
        @DisplayName("Dado busca paginada retornando solicitações, quando chamar método, então retornar página com resultados")
        void dadoBuscaPaginadaRetornandoResultados_quandoChamarMetodo_deveRetornarPagina() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<SolicitacaoAutorizacaoRecorrencia> paginaMock = mock(Page.class);

            when(consultaSolicitacaoExpiracaoPixAutomaticoService.buscarSolicitacoesExpiradas(pageable)).thenReturn(paginaMock);
            when(appConfig.getJobNotificaoExpiracaoPixAutomatico()).thenReturn(jobShedLock);
            when(jobShedLock.getTamanhoDaConsulta()).thenReturn(10);

            Page<SolicitacaoAutorizacaoRecorrencia> resultado = job.buscaPaginadaDeSolicitacoes();

            assertEquals(paginaMock, resultado);
            verify(consultaSolicitacaoExpiracaoPixAutomaticoService, times(1)).buscarSolicitacoesExpiradas(pageable);
        }

        @Test
        @DisplayName("Dado busca paginada sem resultados, quando chamar método, então retornar página vazia")
        void dadoBuscaPaginadaSemResultados_quandoChamarMetodo_deveRetornarPaginaVazia() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<SolicitacaoAutorizacaoRecorrencia> paginaVazia = Page.empty();

            when(consultaSolicitacaoExpiracaoPixAutomaticoService.buscarSolicitacoesExpiradas(pageable)).thenReturn(paginaVazia);
            when(appConfig.getJobNotificaoExpiracaoPixAutomatico()).thenReturn(jobShedLock);
            when(jobShedLock.getTamanhoDaConsulta()).thenReturn(10);

            Page<SolicitacaoAutorizacaoRecorrencia> resultado = job.buscaPaginadaDeSolicitacoes();

            assertEquals(paginaVazia, resultado);
            verify(consultaSolicitacaoExpiracaoPixAutomaticoService, times(1)).buscarSolicitacoesExpiradas(pageable);
        }

        @Test
        @DisplayName("Dado solicitação com sistema pagador FISITAL, quando converter, deve retornar notificação com canal Sicredi App")
        void dadoSolicitacaoComSistemaPagadorFisital_quandoConverter_deveRetornarNotificacaoComCanalSicrediApp() {

            SolicitacaoAutorizacaoRecorrencia solicitacao = SolicitacaoAutorizacaoRecorrencia.builder()
                    .tipoSistemaPagador(FISITAL)
                    .contaPagador(CONTA)
                    .agenciaPagador(AGENCIA)
                    .nomeRecebedor("Joao")
                    .cpfCnpjPagador(CPF_CNPJ)
                    .dataExpiracaoConfirmacaoSolicitacao(LocalDateTime.of(2025, 5, 19, 0, 0))
                    .valor(new BigDecimal(VALOR))
                    .build();

            NotificacaoDTO resultado = job.converteSolicitacaoEmNotificacao(solicitacao);

            assertEquals(4, resultado.getInformacoesAdicionais().size());
            assertEquals(CONTA, resultado.getConta());
            assertEquals(AGENCIA, resultado.getAgencia());
            assertEquals("SICREDI_APP", resultado.getCanal());
            assertEquals(VALOR, resultado.getInformacoesAdicionais().get("valor"));
            assertEquals("Joao", resultado.getInformacoesAdicionais().get("nomeRecebedor"));
            assertEquals(DATA, resultado.getInformacoesAdicionais().get("dataExpiracaoAutorizacao"));
        }

        @Test
        @DisplayName("Dado solicitação com sistema pagador LEGADO, quando converter, deve retornar notificação com canal Mobi")
        void dadoSolicitacaoComSistemaPagadorLegado_quandoConverter_deveRetornarNotificacaoComCanalMobi() {

            SolicitacaoAutorizacaoRecorrencia solicitacao = SolicitacaoAutorizacaoRecorrencia.builder()
                    .tipoSistemaPagador(LEGADO)
                    .contaPagador(CONTA)
                    .agenciaPagador(AGENCIA)
                    .nomeRecebedor("Maria")
                    .cpfCnpjPagador("98765432109")
                    .dataExpiracaoConfirmacaoSolicitacao(LocalDateTime.of(2026, 6, 20, 0, 0))
                    .valor(new BigDecimal("1000.75"))
                    .build();

            NotificacaoDTO resultado = job.converteSolicitacaoEmNotificacao(solicitacao);

            assertEquals(4, resultado.getInformacoesAdicionais().size());
            assertEquals(CONTA, resultado.getConta());
            assertEquals(AGENCIA, resultado.getAgencia());
            assertEquals("MOBI", resultado.getCanal());
            assertEquals("1000.75", resultado.getInformacoesAdicionais().get("valor"));
            assertEquals("Maria", resultado.getInformacoesAdicionais().get("nomeRecebedor"));
            assertEquals("20/06/2026", resultado.getInformacoesAdicionais().get("dataExpiracaoAutorizacao"));
        }
    }
}