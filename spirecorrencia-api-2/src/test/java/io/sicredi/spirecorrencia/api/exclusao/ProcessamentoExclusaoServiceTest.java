package io.sicredi.spirecorrencia.api.exclusao;

import br.com.sicredi.canaisdigitais.enums.OrigemEnum;
import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoCanalEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoContaEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoMotivoExclusao;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.ExclusaoRecorrenciaParcelaRequest;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.ExclusaoRecorrenciaProtocoloRequest;
import io.sicredi.spirecorrencia.api.liquidacao.TipoExclusaoRecorrencia;
import io.sicredi.spirecorrencia.api.liquidacao.TipoProcessamentoEnum;
import io.sicredi.spirecorrencia.api.liquidacao.TipoProcessamentoWrapperDTO;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoInformacaoAdicional;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoProducer;
import io.sicredi.spirecorrencia.api.protocolo.SpiCanaisProtocoloApiClient;
import io.sicredi.spirecorrencia.api.recorrencia_tentativa.TentativaRecorrenciaTransacaoService;
import io.sicredi.spirecorrencia.api.repositorio.Pagador;
import io.sicredi.spirecorrencia.api.repositorio.Recorrencia;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacao;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacaoRepository;
import io.sicredi.spirecorrencia.api.testconfig.TestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode.REC_PROC_BU0001;
import static io.sicredi.spirecorrencia.api.testconfig.TestFactory.RecorrenciaTestFactory.criarRecorrencia;
import static io.sicredi.spirecorrencia.api.utils.ConstantesTest.IDENTIFICADOR_TRANSACAO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessamentoExclusaoServiceTest {

    @Mock
    private NotificacaoProducer producer;
    @Mock
    private RecorrenciaTransacaoRepository recorrenciaTransacaoRepository;
    @Mock
    private SpiCanaisProtocoloApiClient spiCanaisProtocoloApiClient;
    @Mock
    private TentativaRecorrenciaTransacaoService tentativaService;
    @InjectMocks
    private ProcessamentoExclusaoService processamentoExclusaoService;
    @Captor
    private ArgumentCaptor<ExclusaoRecorrenciaProtocoloRequest> captureExclusaoRecorrenciaProtocoloRequest;
    @Captor
    private ArgumentCaptor<NotificacaoDTO> captureNotificacaoDTO;

    private Recorrencia recorrenciaMock;
    private RecorrenciaTransacao recorrenciaTransacaoMock;

    @BeforeEach
    void setUp() {
        recorrenciaMock = criarRecorrencia(LocalDateTime.now());
        recorrenciaTransacaoMock = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrenciaMock, LocalDate.now(), new BigDecimal(100));
    }

    @Test
    void dadoExclusaoTotal_quandoProcessarExclusaoTotal_deveExcluirTodasParcelas() {
        var tipoProcessamentoMock = TestFactory.TipoProcessamentoWrapperDTOTestFactory.exclusaoTotal(recorrenciaTransacaoMock);

        mocksExclusaoTotal();

        processamentoExclusaoService.processarExclusaoTotal(tipoProcessamentoMock);

        verify(spiCanaisProtocoloApiClient).emitirProtocoloCancelamentoRecorrencia(eq(recorrenciaTransacaoMock.getRecorrencia().getTipoCanal()), eq(TipoExclusaoRecorrencia.EXCLUSAO_INTEGRADA), captureExclusaoRecorrenciaProtocoloRequest.capture());
        verify(producer).enviarNotificacao(captureNotificacaoDTO.capture());
        verify(tentativaService).registrarRecorrenciaTransacaoTentativa(REC_PROC_BU0001.getMessage(), REC_PROC_BU0001.getCode(), recorrenciaTransacaoMock);

        var valueExclusaoRecorrenciaProtocoloRequest = captureExclusaoRecorrenciaProtocoloRequest.getValue();
        var notificacaoDTO = captureNotificacaoDTO.getValue();

        var pagador = recorrenciaTransacaoMock.getRecorrencia().getPagador();

        assertAll("Identificação do Associado",
                () -> assertEquals(pagador.getTipoConta().getTipoContaCanaisDigitais(), valueExclusaoRecorrenciaProtocoloRequest.getIdentificacaoAssociado().getTipoConta()),
                () -> assertEquals(pagador.getCpfCnpj(), valueExclusaoRecorrenciaProtocoloRequest.getIdentificacaoAssociado().getCpfUsuario()),
                () -> assertEquals(pagador.getCpfCnpj(), valueExclusaoRecorrenciaProtocoloRequest.getIdentificacaoAssociado().getCpfCnpjConta()),
                () -> assertEquals(pagador.getConta(), valueExclusaoRecorrenciaProtocoloRequest.getIdentificacaoAssociado().getConta()),
                () -> assertEquals(pagador.getCodPosto(), valueExclusaoRecorrenciaProtocoloRequest.getIdentificacaoAssociado().getAgencia()),
                () -> assertEquals(pagador.getNome(), valueExclusaoRecorrenciaProtocoloRequest.getIdentificacaoAssociado().getNomeAssociadoConta()),
                () -> assertEquals(pagador.getAgencia(), valueExclusaoRecorrenciaProtocoloRequest.getIdentificacaoAssociado().getCooperativa()),
                () -> assertEquals(recorrenciaTransacaoMock.getRecorrencia().getTipoOrigemSistema(), valueExclusaoRecorrenciaProtocoloRequest.getIdentificacaoAssociado().getOrigemConta())
        );

        assertAll("Detalhes da Exclusão",
                () -> assertEquals(recorrenciaTransacaoMock.getRecorrencia().getIdRecorrencia(), valueExclusaoRecorrenciaProtocoloRequest.getIdentificadorRecorrencia()),
                () -> assertEquals(TipoMotivoExclusao.SOLICITADO_SISTEMA, valueExclusaoRecorrenciaProtocoloRequest.getTipoMotivoExclusao()),
                () -> assertEquals(2, valueExclusaoRecorrenciaProtocoloRequest.getParcelas().size())
        );

        var recebedor = recorrenciaTransacaoMock.getRecorrencia().getRecebedor();

        assertAll("Notificação",
                () -> assertEquals(pagador.getAgencia(), notificacaoDTO.getAgencia()),
                () -> assertEquals(pagador.getConta(), notificacaoDTO.getConta()),
                () -> assertEquals(recebedor.getChave(), notificacaoDTO.getChave()),
                () -> assertEquals(recebedor.getTipoChave().name(), notificacaoDTO.getTipoChave()),
                () -> assertEquals(NotificacaoDTO.TipoTemplate.RECORRENCIA_FALHA_MUDANCA_TITULARIDADE_CHAVE, notificacaoDTO.getOperacao()),
                () -> assertEquals(recorrenciaTransacaoMock.getRecorrencia().getTipoCanal().getTipoCanalPix().name(), notificacaoDTO.getCanal()),
                () -> assertEquals(recebedor.getNome(), notificacaoDTO.getInformacoesAdicionais().get(NotificacaoInformacaoAdicional.NOME_RECEBEDOR.getNomeVariavel())),
                () -> assertEquals(recorrenciaTransacaoMock.getValor().toString(), notificacaoDTO.getInformacoesAdicionais().get(NotificacaoInformacaoAdicional.VALOR.getNomeVariavel()))
        );
    }

    @Test
    void dadoExcecao_quandoProcessarExclusaoTotal_deveLancarTechnicalException() {
        var tipoProcessamentoMock = TestFactory.TipoProcessamentoWrapperDTOTestFactory.exclusaoTotal(recorrenciaTransacaoMock);

        mocksExclusaoTotal();

        doThrow(new TechnicalException("Erro ao consultar DICT")).when(spiCanaisProtocoloApiClient)
                .emitirProtocoloCancelamentoRecorrencia(eq(recorrenciaTransacaoMock.getRecorrencia().getTipoCanal()), eq(TipoExclusaoRecorrencia.EXCLUSAO_INTEGRADA), any());

        var exception = assertThrows(TechnicalException.class, () ->
                processamentoExclusaoService.processarExclusaoTotal(tipoProcessamentoMock)
        );

        assertEquals("Erro ao consultar DICT", exception.getCause().getMessage());

        verify(tentativaService).registrarRecorrenciaTransacaoTentativa(REC_PROC_BU0001.getMessage(), REC_PROC_BU0001.getCode(), recorrenciaTransacaoMock);
        verify(recorrenciaTransacaoRepository, times(1)).findByRecorrenciaAndStatus(anyString(), any());
        verify(producer, never()).enviarNotificacao(any());
    }

    @ParameterizedTest
    @MethodSource("recorrenciasInvalidasParaExclusaoTotal")
    void dadoRecorrenciaNullOuListaParcelasVazia_quandoEmitirProtocoloExclusaoTotal_deveIgnorarProcessamento(Recorrencia recorrencia) {
        processamentoExclusaoService.emitirProtocoloExclusaoTotal(IDENTIFICADOR_TRANSACAO, recorrencia);

        verify(spiCanaisProtocoloApiClient, never()).emitirProtocoloCancelamentoRecorrencia(
                any(), any(),
                argThat(request -> IDENTIFICADOR_TRANSACAO.equals(request.getIdentificadorTransacao()))
        );
    }

    private static Stream<Recorrencia> recorrenciasInvalidasParaExclusaoTotal() {
        return Stream.of(
                null,
                new Recorrencia(),
                Recorrencia.builder().recorrencias(List.of()).build()
        );
    }

    @Test
    void dadoRecorrenciaComVariasParcelas_quandoProcessarExclusaoTotal_deveExcluirSomenteParcelasComStatusCriado() {
        final var parcelas = List.of(
                RecorrenciaTransacao.builder().idParcela("1").dataTransacao(LocalDate.of(2025, 1, 1)).tpoStatus(TipoStatusEnum.CONCLUIDO).build(),
                RecorrenciaTransacao.builder().idParcela("2").dataTransacao(LocalDate.of(2025, 2, 1)).tpoStatus(TipoStatusEnum.CONCLUIDO).build(),
                RecorrenciaTransacao.builder().idParcela("3").dataTransacao(LocalDate.of(2025, 3, 1)).tpoStatus(TipoStatusEnum.EXCLUIDO).build(),
                RecorrenciaTransacao.builder().idParcela("4").dataTransacao(LocalDate.of(2025, 4, 1)).tpoStatus(TipoStatusEnum.EXCLUIDO).build(),
                RecorrenciaTransacao.builder().idParcela("5").dataTransacao(LocalDate.of(2025, 5, 1)).tpoStatus(TipoStatusEnum.PENDENTE).build(),
                RecorrenciaTransacao.builder().idParcela("6").dataTransacao(LocalDate.of(2025, 6, 1)).tpoStatus(TipoStatusEnum.CRIADO).build(),
                RecorrenciaTransacao.builder().idParcela("7").dataTransacao(LocalDate.of(2025, 7, 1)).tpoStatus(TipoStatusEnum.CRIADO).build()
        );
        final var recorrencia = Recorrencia.builder()
                .recorrencias(parcelas)
                .tipoCanal(TipoCanalEnum.MOBI)
                .tipoOrigemSistema(OrigemEnum.LEGADO)
                .pagador(Pagador.builder().tipoConta(TipoContaEnum.CONTA_CORRENTE).build())
                .build();

        processamentoExclusaoService.emitirProtocoloExclusaoTotal(IDENTIFICADOR_TRANSACAO, recorrencia);

        verify(spiCanaisProtocoloApiClient).emitirProtocoloCancelamentoRecorrencia(eq(TipoCanalEnum.MOBI), eq(TipoExclusaoRecorrencia.EXCLUSAO_INTEGRADA), captureExclusaoRecorrenciaProtocoloRequest.capture());
        var requestExclusao = captureExclusaoRecorrenciaProtocoloRequest.getValue();
        assertEquals(2, requestExclusao.getParcelas().size());
        assertEquals(List.of("6", "7"), requestExclusao.getParcelas().stream().map(ExclusaoRecorrenciaParcelaRequest::getIdentificadorParcela).toList());
        assertEquals(IDENTIFICADOR_TRANSACAO, requestExclusao.getIdentificadorTransacao());
    }

    private void mocksExclusaoTotal() {
        recorrenciaMock.setRecorrencias(List.of(recorrenciaTransacaoMock, recorrenciaTransacaoMock));
        when(recorrenciaTransacaoRepository.findByRecorrenciaAndStatus(recorrenciaMock.getIdRecorrencia(), List.of(TipoStatusEnum.CRIADO, TipoStatusEnum.PENDENTE))).thenReturn(recorrenciaMock.getRecorrencias());
    }

    @Test
    void dadoExclusaoParcial_quandoProcessarExclusaoParcial_deveEmitirProtocoloComParcelaUnica() {
        var tipoProcessamentoMock =  TestFactory.TipoProcessamentoWrapperDTOTestFactory.exclusaoParcial(recorrenciaTransacaoMock);

        processamentoExclusaoService.processarExclusaoParcial(tipoProcessamentoMock);

        verify(tentativaService).registrarRecorrenciaTransacaoTentativa(REC_PROC_BU0001.getMessage(), REC_PROC_BU0001.getCode(), tipoProcessamentoMock.getRecorrenciaTransacao());
        verify(spiCanaisProtocoloApiClient).emitirProtocoloCancelamentoRecorrencia(eq(TipoCanalEnum.MOBI), eq(TipoExclusaoRecorrencia.EXCLUSAO_INTEGRADA), captureExclusaoRecorrenciaProtocoloRequest.capture());
        verify(producer).enviarNotificacao(captureNotificacaoDTO.capture());

        var valueExclusaoRecorrenciaProtocoloRequest = captureExclusaoRecorrenciaProtocoloRequest.getValue();
        var notificacaoDTO = captureNotificacaoDTO.getValue();

        var pagador = recorrenciaTransacaoMock.getRecorrencia().getPagador();

        assertAll("Identificação do Associado",
                () -> assertEquals(pagador.getTipoConta().getTipoContaCanaisDigitais(), valueExclusaoRecorrenciaProtocoloRequest.getIdentificacaoAssociado().getTipoConta()),
                () -> assertEquals(pagador.getCpfCnpj(), valueExclusaoRecorrenciaProtocoloRequest.getIdentificacaoAssociado().getCpfUsuario()),
                () -> assertEquals(pagador.getCpfCnpj(), valueExclusaoRecorrenciaProtocoloRequest.getIdentificacaoAssociado().getCpfCnpjConta()),
                () -> assertEquals(pagador.getConta(), valueExclusaoRecorrenciaProtocoloRequest.getIdentificacaoAssociado().getConta()),
                () -> assertEquals(pagador.getCodPosto(), valueExclusaoRecorrenciaProtocoloRequest.getIdentificacaoAssociado().getAgencia()),
                () -> assertEquals(pagador.getNome(), valueExclusaoRecorrenciaProtocoloRequest.getIdentificacaoAssociado().getNomeAssociadoConta()),
                () -> assertEquals(pagador.getAgencia(), valueExclusaoRecorrenciaProtocoloRequest.getIdentificacaoAssociado().getCooperativa()),
                () -> assertEquals(recorrenciaTransacaoMock.getRecorrencia().getTipoOrigemSistema(), valueExclusaoRecorrenciaProtocoloRequest.getIdentificacaoAssociado().getOrigemConta())
        );

        assertAll("Detalhes da Exclusão",
                () -> assertEquals(recorrenciaTransacaoMock.getRecorrencia().getIdRecorrencia(), valueExclusaoRecorrenciaProtocoloRequest.getIdentificadorRecorrencia()),
                () -> assertEquals(TipoMotivoExclusao.SOLICITADO_SISTEMA, valueExclusaoRecorrenciaProtocoloRequest.getTipoMotivoExclusao()),
                () -> assertEquals(1, valueExclusaoRecorrenciaProtocoloRequest.getParcelas().size())
        );

        var recebedor = recorrenciaTransacaoMock.getRecorrencia().getRecebedor();

        assertAll("Notificação",
                () -> assertEquals(pagador.getAgencia(), notificacaoDTO.getAgencia()),
                () -> assertEquals(pagador.getConta(), notificacaoDTO.getConta()),
                () -> assertEquals(recebedor.getChave(), notificacaoDTO.getChave()),
                () -> assertEquals(recebedor.getTipoChave().name(), notificacaoDTO.getTipoChave()),
                () -> assertEquals(NotificacaoDTO.TipoTemplate.RECORRENCIA_FALHA_MUDANCA_TITULARIDADE_CHAVE, notificacaoDTO.getOperacao()),
                () -> assertEquals(recorrenciaTransacaoMock.getRecorrencia().getTipoCanal().getTipoCanalPix().name(), notificacaoDTO.getCanal()),
                () -> assertEquals(recebedor.getNome(), notificacaoDTO.getInformacoesAdicionais().get(NotificacaoInformacaoAdicional.NOME_RECEBEDOR.getNomeVariavel())),
                () -> assertEquals(recorrenciaTransacaoMock.getValor().toString(), notificacaoDTO.getInformacoesAdicionais().get(NotificacaoInformacaoAdicional.VALOR.getNomeVariavel()))
        );
    }

    @Test
    void dadoException_quandoProcessarExclusaoParcial_deveMapearParaTechnicalException() {
        var tipoProcessamentoErroDTO = TipoProcessamentoWrapperDTO.TipoProcessamentoErro.builder()
                .codigoErro(REC_PROC_BU0001.getCode())
                .mensagemErro(REC_PROC_BU0001.getMessage())
                .tipoMotivoExclusao(TipoMotivoExclusao.SOLICITADO_SISTEMA)
                .build();

        var parcela = RecorrenciaTransacao.builder()
                .oidRecorrenciaTransacao(1L)
                .recorrencia(recorrenciaMock)
                .tpoStatus(TipoStatusEnum.CRIADO)
                .idParcela("b022ec30-0bc7-4ac5-b4dd-8f3fc28de3ad")
                .idFimAFim("E91586982202208151245099rD6AIAa7")
                .valor(new BigDecimal("100"))
                .dataTransacao(LocalDate.now())
                .build();

        var tipoProcessamentoMock =  TipoProcessamentoWrapperDTO.builder()
                .identificadorTransacao(IDENTIFICADOR_TRANSACAO)
                .tipoProcessamentoEnum(TipoProcessamentoEnum.EXCLUSAO_PARCIAL)
                .recorrenciaTransacao(parcela)
                .tipoProcessamentoErro(tipoProcessamentoErroDTO)
                .build();

        var runtimeException = new RuntimeException("Erro ao tentar registrar tentativa");
        doThrow(runtimeException).when(tentativaService).registrarRecorrenciaTransacaoTentativa(any(), any(), any());

        var exception = assertThrows(TechnicalException.class, () -> processamentoExclusaoService.processarExclusaoParcial(tipoProcessamentoMock));

        assertEquals(runtimeException, exception.getCause());
        verify(tentativaService).registrarRecorrenciaTransacaoTentativa(REC_PROC_BU0001.getMessage(), REC_PROC_BU0001.getCode(), parcela);
    }

}