package io.sicredi.spirecorrencia.api.liquidacao;

import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoCanalEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;
import io.micrometer.core.instrument.Tags;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.exceptions.ErroLiquidacaoException;
import io.sicredi.spirecorrencia.api.exclusao.ProcessamentoExclusaoService;
import io.sicredi.spirecorrencia.api.metrica.MetricaCounter;
import io.sicredi.spirecorrencia.api.metrica.RegistraMetricaService;
import io.sicredi.spirecorrencia.api.recorrencia_tentativa.TentativaRecorrenciaTransacaoService;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacaoRepository;
import io.sicredi.spirecorrencia.api.testconfig.TestFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode.SPIRECORRENCIA_BU0027;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessamentoLiquidacaoJobTest {

    @Mock
    private RecorrenciaTransacaoRepository repository;
    @Mock
    private LiquidacaoService liquidacaoService;
    @Mock
    private ProcessamentoLiquidacaoService processamentoLiquidacaoService;
    @Mock
    private ProcessamentoExclusaoService processamentoExclusaoService;
    @Mock
    private RegistraMetricaService registraMetricaService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AppConfig appConfig;
    @Mock
    private TentativaRecorrenciaTransacaoService tentativaRecorrenciaTransacaoService;
    @InjectMocks
    private ProcessamentoLiquidacaoJob processamentoLiquidacaoJob;
    @Captor
    private ArgumentCaptor<TipoProcessamentoWrapperDTO> tipoProcessamentoWrapperDTOArgumentCaptor;
    @Captor
    private ArgumentCaptor<MetricaCounter> metricaCaptor;

    private static final String NOME_METRICA = "pix_emissao_liquidacao_parcela_recorrencia";
    private static final String DESCRICAO_METRICA = "Resultado do processamento da emissão de uma liquidação de parcela recorrente do Pix.";
    private static final String TIPO_RETORNO = "tipo_retorno";
    private static final String TIPO_CANAL = "tipo_canal";

    @Test
    void dadoJobNaoHabilitado_quandoExecutarJob_deveNaoProcessarLiquidacao() {
        when(appConfig.getJobProcessamentoLiquidacao().isJobHabilitado()).thenReturn(false);

        processamentoLiquidacaoJob.executar();

        verify(repository, never()).buscarParcelasParaProcessamento(any(), any());
        verify(liquidacaoService, never()).consultarTipoProcessamento(anyString(), any());
        verify(processamentoLiquidacaoService, never()).processarLiquidacao(any());
        verify(processamentoLiquidacaoService, never()).processarLiquidacao(any());
        verify(registraMetricaService, never()).registrar(any());
    }

    @Test
    void dadoTipoProcessamentoLiquidacao_quandoExecutarJob_deveProcessarComoLiquidacao() {
        var pagebleMock = criarPageable();
        var recorrenciaMock = TestFactory.RecorrenciaTestFactory.criarRecorrencia(LocalDateTime.now());
        var recorrenciaTransacaoMock = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrenciaMock, LocalDate.now(), new BigDecimal(100));
        var tipoProcessamentoDTO = TestFactory.TipoProcessamentoWrapperDTOTestFactory.liquidacao(recorrenciaTransacaoMock);

        var sliceMock = new SliceImpl<>(List.of(recorrenciaTransacaoMock), pagebleMock, false);

        when(appConfig.getJobProcessamentoLiquidacao().getTamanhoDaConsulta()).thenReturn(10);
        when(appConfig.getJobProcessamentoLiquidacao().isJobHabilitado()).thenReturn(true);
        when(repository.buscarParcelasParaProcessamento(LocalDate.now(), pagebleMock)).thenReturn(sliceMock);
        when(liquidacaoService.consultarTipoProcessamento(anyString(), eq(recorrenciaTransacaoMock))).thenReturn(tipoProcessamentoDTO);

        processamentoLiquidacaoJob.executar();

        verify(repository).buscarParcelasParaProcessamento(LocalDate.now(), pagebleMock);
        verify(liquidacaoService, times(1)).consultarTipoProcessamento(anyString(), eq(recorrenciaTransacaoMock));
        verify(processamentoLiquidacaoService, times(1)).processarLiquidacao(tipoProcessamentoDTO);

        verify(processamentoLiquidacaoService).processarLiquidacao(tipoProcessamentoWrapperDTOArgumentCaptor.capture());
        verify(registraMetricaService).registrar(metricaCaptor.capture());

        TipoProcessamentoWrapperDTO captor = tipoProcessamentoWrapperDTOArgumentCaptor.getValue();
        MetricaCounter metricaCapturada = metricaCaptor.getValue();

        Tags tags = metricaCapturada.criarTags();

        assertAll(
                () -> assertEquals(NOME_METRICA, metricaCapturada.getNome()),
                () -> assertEquals(DESCRICAO_METRICA, metricaCapturada.getDescricao()),
                () -> assertTrue(tags.stream().anyMatch(tag -> TIPO_RETORNO.equals(tag.getKey()) && "SUCESSO_LIQUIDACAO".equals(tag.getValue()))),
                () -> assertTrue(tags.stream().anyMatch(tag -> TIPO_CANAL.equals(tag.getKey()) && "MOBI".equals(tag.getValue()))),

                () -> assertEquals(tipoProcessamentoDTO.getTipoProcessamentoEnum(), captor.getTipoProcessamentoEnum()),
                () -> assertEquals(recorrenciaTransacaoMock.getIdParcela(), captor.getRecorrenciaTransacao().getIdParcela()),
                () -> assertEquals(recorrenciaTransacaoMock.getValor(), captor.getRecorrenciaTransacao().getValor()),
                () -> assertEquals(recorrenciaTransacaoMock.getDataTransacao(), captor.getRecorrenciaTransacao().getDataTransacao()),
                () -> assertEquals(recorrenciaTransacaoMock.getIdFimAFim(), captor.getRecorrenciaTransacao().getIdFimAFim()),
                () -> assertEquals(recorrenciaTransacaoMock.getInformacoesEntreUsuarios(), captor.getRecorrenciaTransacao().getInformacoesEntreUsuarios()),
                () -> assertEquals(recorrenciaTransacaoMock.getIdConciliacaoRecebedor(), captor.getRecorrenciaTransacao().getIdConciliacaoRecebedor()));
    }

    @Test
    void dadoTipoProcessamentoLiquidacao_quandoExecutarJob_deveProcessarComoLiquidacaoEIgnoradoUmaParcela() {
        var pagebleMock = criarPageable();
        var recorrenciaMock = TestFactory.RecorrenciaTestFactory.criarRecorrencia(LocalDateTime.now());
        var recorrenciaTransacaoCriada = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrenciaMock, LocalDate.now(), new BigDecimal(100));

        var recorrenciaTransacaoExcluida = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrenciaMock, LocalDate.now(), new BigDecimal(100));
        recorrenciaTransacaoExcluida.setTpoStatus(TipoStatusEnum.EXCLUIDO);

        var tipoProcessamentoDTO = TestFactory.TipoProcessamentoWrapperDTOTestFactory.liquidacao(recorrenciaTransacaoCriada);

        var sliceMock = new SliceImpl<>(List.of(recorrenciaTransacaoCriada, recorrenciaTransacaoExcluida), pagebleMock, false);

        when(appConfig.getJobProcessamentoLiquidacao().getTamanhoDaConsulta()).thenReturn(10);
        when(appConfig.getJobProcessamentoLiquidacao().isJobHabilitado()).thenReturn(true);
        when(repository.buscarParcelasParaProcessamento(LocalDate.now(), pagebleMock)).thenReturn(sliceMock);
        when(liquidacaoService.consultarTipoProcessamento(anyString(), eq(recorrenciaTransacaoCriada))).thenReturn(tipoProcessamentoDTO);

        processamentoLiquidacaoJob.executar();

        verify(repository).buscarParcelasParaProcessamento(LocalDate.now(), pagebleMock);
        verify(liquidacaoService, times(1)).consultarTipoProcessamento(anyString(), eq(recorrenciaTransacaoCriada));
        verify(processamentoLiquidacaoService, times(1)).processarLiquidacao(tipoProcessamentoDTO);

        verify(processamentoLiquidacaoService).processarLiquidacao(tipoProcessamentoWrapperDTOArgumentCaptor.capture());
        verify(registraMetricaService).registrar(metricaCaptor.capture());
    }

    @Test
    void dadoTipoProcessamentoExclusao_quandoExecutarJob_deveProcessarComoExclusao() {
        var pagebleMock = criarPageable();
        var recorrenciaMock = TestFactory.RecorrenciaTestFactory.criarRecorrencia(LocalDateTime.now());
        var recorrenciaTransacaoMock = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrenciaMock, LocalDate.now(), new BigDecimal(100));
        var tipoProcessamentoDTO = TestFactory.TipoProcessamentoWrapperDTOTestFactory.exclusaoTotal(recorrenciaTransacaoMock);

        var sliceMock = new SliceImpl<>(List.of(recorrenciaTransacaoMock), pagebleMock, false);

        when(appConfig.getJobProcessamentoLiquidacao().isJobHabilitado()).thenReturn(true);
        when(appConfig.getJobProcessamentoLiquidacao().getTamanhoDaConsulta()).thenReturn(10);
        when(repository.buscarParcelasParaProcessamento(LocalDate.now(), pagebleMock)).thenReturn(sliceMock);
        when(liquidacaoService.consultarTipoProcessamento(anyString(), eq(recorrenciaTransacaoMock))).thenReturn(tipoProcessamentoDTO);

        processamentoLiquidacaoJob.executar();

        verify(repository, times(1)).buscarParcelasParaProcessamento(LocalDate.now(),  pagebleMock);
        verify(liquidacaoService, times(1)).consultarTipoProcessamento(anyString(), eq(recorrenciaTransacaoMock));
        verify(processamentoExclusaoService, times(1)).processarExclusaoTotal(tipoProcessamentoDTO);

        verify(processamentoExclusaoService).processarExclusaoTotal(tipoProcessamentoWrapperDTOArgumentCaptor.capture());
        verify(registraMetricaService).registrar(metricaCaptor.capture());

        TipoProcessamentoWrapperDTO captor = tipoProcessamentoWrapperDTOArgumentCaptor.getValue();
        MetricaCounter metricaCapturada = metricaCaptor.getValue();

        Tags tags = metricaCapturada.criarTags();

        assertAll(
                () -> assertEquals(NOME_METRICA, metricaCapturada.getNome()),
                () -> assertEquals(DESCRICAO_METRICA, metricaCapturada.getDescricao()),
                () -> assertTrue(tags.stream().anyMatch(tag -> TIPO_RETORNO.equals(tag.getKey()) && "SUCESSO_EXCLUSAO".equals(tag.getValue()))),
                () -> assertTrue(tags.stream().anyMatch(tag -> TIPO_CANAL.equals(tag.getKey()) && "MOBI".equals(tag.getValue()))),

                () -> assertEquals(tipoProcessamentoDTO.getTipoProcessamentoEnum(), captor.getTipoProcessamentoEnum()),
                () -> assertEquals(recorrenciaTransacaoMock.getIdParcela(), captor.getRecorrenciaTransacao().getIdParcela()),
                () -> assertEquals(recorrenciaTransacaoMock.getValor(), captor.getRecorrenciaTransacao().getValor()),
                () -> assertEquals(recorrenciaTransacaoMock.getDataTransacao(), captor.getRecorrenciaTransacao().getDataTransacao()),
                () -> assertEquals(recorrenciaTransacaoMock.getIdFimAFim(), captor.getRecorrenciaTransacao().getIdFimAFim()),
                () -> assertEquals(recorrenciaTransacaoMock.getInformacoesEntreUsuarios(), captor.getRecorrenciaTransacao().getInformacoesEntreUsuarios()),
                () -> assertEquals(recorrenciaTransacaoMock.getIdConciliacaoRecebedor(), captor.getRecorrenciaTransacao().getIdConciliacaoRecebedor()));
    }

    @Test
    void dadoDadosValidos_quandoExecutarJob_deveOcorrerErroAoProcessarRecorrencia() {
        var pagebleMock = criarPageable();
        var recorrenciaMock = TestFactory.RecorrenciaTestFactory.criarRecorrencia(LocalDateTime.now());
        var recorrenciaTransacaoMock = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrenciaMock, LocalDate.now(), new BigDecimal(100));
        var tipoProcessamentoDTO = TestFactory.TipoProcessamentoWrapperDTOTestFactory.exclusaoParcial(recorrenciaTransacaoMock);

        var sliceMock = new SliceImpl<>(List.of(recorrenciaTransacaoMock), pagebleMock, false);

        when(appConfig.getJobProcessamentoLiquidacao().isJobHabilitado()).thenReturn(true);
        when(appConfig.getJobProcessamentoLiquidacao().getTamanhoDaConsulta()).thenReturn(10);
        when(repository.buscarParcelasParaProcessamento(LocalDate.now(),  pagebleMock)).thenReturn(sliceMock);
        when(liquidacaoService.consultarTipoProcessamento(anyString(), eq(recorrenciaTransacaoMock))).thenReturn(tipoProcessamentoDTO);

        doThrow(new TechnicalException("Erro Generico"))
                .when(processamentoExclusaoService).processarExclusaoParcial(tipoProcessamentoDTO);

        processamentoLiquidacaoJob.executar();

        verify(repository).buscarParcelasParaProcessamento(LocalDate.now(),  pagebleMock);
        verify(liquidacaoService).consultarTipoProcessamento(anyString(), eq(recorrenciaTransacaoMock));
        verify(processamentoExclusaoService).processarExclusaoParcial(tipoProcessamentoDTO);

        verify(registraMetricaService).registrar(metricaCaptor.capture());

        MetricaCounter metricaCapturada = metricaCaptor.getValue();

        Tags tags = metricaCapturada.criarTags();

        assertAll(
                () -> assertEquals(NOME_METRICA, metricaCapturada.getNome()),
                () -> assertEquals(DESCRICAO_METRICA, metricaCapturada.getDescricao()),
                () -> assertTrue(tags.stream().anyMatch(tag -> TIPO_RETORNO.equals(tag.getKey()) && "ERRO".equals(tag.getValue()))),
                () -> assertTrue(tags.stream().anyMatch(tag -> TIPO_CANAL.equals(tag.getKey()) && "MOBI".equals(tag.getValue())))
        );
    }

    @Test
    void dadoErroEmissaoLiquidacao_quandoExecutarJob_deveEmitirProtocoloExclusaoParcial() {
        var pagebleMock = criarPageable();
        var recorrenciaMock = TestFactory.RecorrenciaTestFactory.criarRecorrencia(LocalDateTime.now());

        recorrenciaMock.setTipoCanal(TipoCanalEnum.WEB_OPENBK);

        var recorrenciaTransacaoMock = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrenciaMock, LocalDate.now(), new BigDecimal(100));
        var tipoProcessamentoDTO = TestFactory.TipoProcessamentoWrapperDTOTestFactory.liquidacao(recorrenciaTransacaoMock);

        var sliceMock = new SliceImpl<>(List.of(recorrenciaTransacaoMock), pagebleMock, false);

        when(appConfig.getJobProcessamentoLiquidacao().getTamanhoDaConsulta()).thenReturn(10);
        when(appConfig.getJobProcessamentoLiquidacao().isJobHabilitado()).thenReturn(true);
        when(repository.buscarParcelasParaProcessamento(LocalDate.now(),  pagebleMock)).thenReturn(sliceMock);
        when(liquidacaoService.consultarTipoProcessamento(anyString(), eq(recorrenciaTransacaoMock))).thenReturn(tipoProcessamentoDTO);

        doThrow(new ErroLiquidacaoException("Falha ao emitir protocolo liquidação"))
                .when(processamentoLiquidacaoService).processarLiquidacao(tipoProcessamentoDTO);

        processamentoLiquidacaoJob.executar();

        verify(repository).buscarParcelasParaProcessamento(LocalDate.now(),  pagebleMock);
        verify(liquidacaoService).consultarTipoProcessamento(anyString(), eq(recorrenciaTransacaoMock));
        verify(processamentoExclusaoService).processarExclusaoParcial(tipoProcessamentoWrapperDTOArgumentCaptor.capture());

        verify(registraMetricaService).registrar(metricaCaptor.capture());

        MetricaCounter metricaCapturada = metricaCaptor.getValue();
        TipoProcessamentoWrapperDTO tipoProcessamentoCaptor = tipoProcessamentoWrapperDTOArgumentCaptor.getValue();

        Tags tags = metricaCapturada.criarTags();

        assertAll(
                () -> assertEquals(NOME_METRICA, metricaCapturada.getNome()),
                () -> assertTrue(tags.stream().anyMatch(tag -> TIPO_RETORNO.equals(tag.getKey()) && "ERRO".equals(tag.getValue()))),
                () -> assertTrue(tags.stream().anyMatch(tag -> TIPO_CANAL.equals(tag.getKey()) && "OPBK".equals(tag.getValue()))),

                () -> assertEquals(SPIRECORRENCIA_BU0027.getCode(), tipoProcessamentoCaptor.getTipoProcessamentoErro().getCodigoErro()),
                () -> assertEquals(SPIRECORRENCIA_BU0027.getMessage(), tipoProcessamentoCaptor.getTipoProcessamentoErro().getMensagemErro()),
                () -> assertEquals(recorrenciaTransacaoMock.getIdParcela(), tipoProcessamentoCaptor.getRecorrenciaTransacao().getIdParcela()),
                () -> assertEquals(recorrenciaTransacaoMock.getIdFimAFim(), tipoProcessamentoCaptor.getRecorrenciaTransacao().getIdFimAFim())
        );
    }

    @Test
    void dadoErroEmissaoLiquidacao_quandoExecutarJob_deveNaoEmitirProtocoloExclusaoParcial() {
        var pagebleMock = criarPageable();
        var recorrenciaMock = TestFactory.RecorrenciaTestFactory.criarRecorrencia(LocalDateTime.now());
        var recorrenciaTransacaoMock = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrenciaMock, LocalDate.now(), new BigDecimal(100));
        var tipoProcessamentoDTO = TestFactory.TipoProcessamentoWrapperDTOTestFactory.liquidacao(recorrenciaTransacaoMock);

        var sliceMock = new SliceImpl<>(List.of(recorrenciaTransacaoMock), pagebleMock, false);

        when(appConfig.getJobProcessamentoLiquidacao().getTamanhoDaConsulta()).thenReturn(10);
        when(appConfig.getJobProcessamentoLiquidacao().isJobHabilitado()).thenReturn(true);
        when(appConfig.getRegras().getProcessamento().getHorarioLimiteLiquidacao()).thenReturn(LocalTime.of(23, 59,59));
        when(repository.buscarParcelasParaProcessamento(LocalDate.now(),  pagebleMock)).thenReturn(sliceMock);
        when(liquidacaoService.consultarTipoProcessamento(anyString(), eq(recorrenciaTransacaoMock))).thenReturn(tipoProcessamentoDTO);


        doThrow(new ErroLiquidacaoException("Falha ao emitir protocolo liquidação"))
                .when(processamentoLiquidacaoService).processarLiquidacao(tipoProcessamentoDTO);

        processamentoLiquidacaoJob.executar();

        verify(repository).buscarParcelasParaProcessamento(LocalDate.now(),  pagebleMock);
        verify(liquidacaoService).consultarTipoProcessamento(anyString(), eq(recorrenciaTransacaoMock));
        verify(processamentoLiquidacaoService, times(1)).processarLiquidacao(any());
        verify(processamentoExclusaoService, never()).processarExclusaoParcial(any());
        verify(processamentoExclusaoService, never()).processarExclusaoTotal(any());
        verify(registraMetricaService).registrar(metricaCaptor.capture());

        MetricaCounter metricaCapturada = metricaCaptor.getValue();

        Tags tags = metricaCapturada.criarTags();

        assertAll(
                () -> assertEquals(NOME_METRICA, metricaCapturada.getNome()),
                () -> assertEquals(DESCRICAO_METRICA, metricaCapturada.getDescricao()),
                () -> assertTrue(tags.stream().anyMatch(tag -> TIPO_RETORNO.equals(tag.getKey()) && "ERRO".equals(tag.getValue()))),
                () -> assertTrue(tags.stream().anyMatch(tag -> TIPO_CANAL.equals(tag.getKey()) && "MOBI".equals(tag.getValue())))
        );
    }

    @Test
    void dadoErroEmissaoLiquidacao_quandoExecutarJob_deveLancarExceptionAoTentarExcluirParcela() {
        var pagebleMock = criarPageable();
        var recorrenciaMock = TestFactory.RecorrenciaTestFactory.criarRecorrencia(LocalDateTime.now());

        recorrenciaMock.setTipoCanal(TipoCanalEnum.WEB_OPENBK);

        var recorrenciaTransacaoMock = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrenciaMock, LocalDate.now(), new BigDecimal(100));
        var tipoProcessamentoDTO = TestFactory.TipoProcessamentoWrapperDTOTestFactory.liquidacao(recorrenciaTransacaoMock);

        var sliceMock = new SliceImpl<>(List.of(recorrenciaTransacaoMock), pagebleMock, false);

        when(repository.buscarParcelasParaProcessamento(LocalDate.now(),  pagebleMock)).thenReturn(sliceMock);
        when(appConfig.getJobProcessamentoLiquidacao().getTamanhoDaConsulta()).thenReturn(10);
        when(liquidacaoService.consultarTipoProcessamento(anyString(), eq(recorrenciaTransacaoMock))).thenReturn(tipoProcessamentoDTO);
        when(appConfig.getJobProcessamentoLiquidacao().isJobHabilitado()).thenReturn(true);

        doThrow(new ErroLiquidacaoException("Falha ao emitir protocolo liquidação"))
                .when(processamentoLiquidacaoService).processarLiquidacao(tipoProcessamentoDTO);

        doThrow(new TechnicalException("Falha ao emitir protocolo de cancelamento"))
                .when(processamentoExclusaoService).processarExclusaoParcial(any());

        processamentoLiquidacaoJob.executar();

        verify(repository).buscarParcelasParaProcessamento(LocalDate.now(),  pagebleMock);
        verify(liquidacaoService).consultarTipoProcessamento(anyString(), eq(recorrenciaTransacaoMock));

        verify(registraMetricaService).registrar(metricaCaptor.capture());

        MetricaCounter metricaCapturada = metricaCaptor.getValue();

        Tags tags = metricaCapturada.criarTags();

        assertAll(
                () -> assertEquals(NOME_METRICA, metricaCapturada.getNome()),
                () -> assertTrue(tags.stream().anyMatch(tag -> TIPO_RETORNO.equals(tag.getKey()) && "ERRO".equals(tag.getValue()))),
                () -> assertTrue(tags.stream().anyMatch(tag -> TIPO_CANAL.equals(tag.getKey()) && "OPBK".equals(tag.getValue())))
        );
    }

    @Test
    void dadoTipoProcessamentoIgnorado_quandoExecutarJob_deveRegistrarTentativa() {
        var pagebleMock = criarPageable();
        var recorrenciaMock = TestFactory.RecorrenciaTestFactory.criarRecorrencia(LocalDateTime.now());
        var recorrenciaTransacaoMock = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrenciaMock, LocalDate.now(), new BigDecimal(100));
        var tipoProcessamentoDTO = TestFactory.TipoProcessamentoWrapperDTOTestFactory.ignorada(recorrenciaTransacaoMock);

        var sliceMock = new SliceImpl<>(List.of(recorrenciaTransacaoMock), pagebleMock, false);

        when(appConfig.getJobProcessamentoLiquidacao().getTamanhoDaConsulta()).thenReturn(10);
        when(appConfig.getJobProcessamentoLiquidacao().isJobHabilitado()).thenReturn(true);
        when(repository.buscarParcelasParaProcessamento(LocalDate.now(),  pagebleMock)).thenReturn(sliceMock);
        when(liquidacaoService.consultarTipoProcessamento(anyString(), eq(recorrenciaTransacaoMock))).thenReturn(tipoProcessamentoDTO);

        processamentoLiquidacaoJob.executar();

        verify(repository).buscarParcelasParaProcessamento(LocalDate.now(),  pagebleMock);
        verify(liquidacaoService).consultarTipoProcessamento(anyString(), eq(recorrenciaTransacaoMock));
        verify(tentativaRecorrenciaTransacaoService).registrarRecorrenciaTransacaoTentativa(tipoProcessamentoDTO.getTipoProcessamentoErro().getMensagemErro(), tipoProcessamentoDTO.getTipoProcessamentoErro().getCodigoErro(), recorrenciaTransacaoMock);
        verifyNoInteractions(processamentoExclusaoService, processamentoLiquidacaoService, registraMetricaService);
    }

    private static Pageable criarPageable() {
        return PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "dataCriacaoRegistro"));
    }

}