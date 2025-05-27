package io.sicredi.spirecorrencia.api.liquidacao;

import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import br.com.sicredi.canaisdigitais.enums.OrigemEnum;
import br.com.sicredi.framework.web.spring.exception.BadRequestException;
import br.com.sicredi.framework.web.spring.exception.NotFoundException;
import br.com.sicredi.spi.dto.TransacaoDto;
import br.com.sicredi.spi.entities.type.CodigoErro;
import br.com.sicredi.spi.entities.type.OrdemStatus;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.*;
import br.com.sicredi.spicanais.transacional.transport.lib.config.SpiCanaisTransportContantes;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.ExclusaoRecorrenciaProtocoloRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.dict.ConsultaChaveDictClient;
import io.sicredi.spirecorrencia.api.dict.DictConsultaDTO;
import io.sicredi.spirecorrencia.api.dict.DictException;
import io.sicredi.spirecorrencia.api.dict.TipoContaDictEnum;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoInformacaoAdicional;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoProducer;
import io.sicredi.spirecorrencia.api.protocolo.CanaisDigitaisProtocoloInfoInternalApiClient;
import io.sicredi.spirecorrencia.api.protocolo.SpiCanaisProtocoloApiClient;
import io.sicredi.spirecorrencia.api.recorrencia_tentativa.TentativaRecorrenciaTransacaoService;
import io.sicredi.spirecorrencia.api.repositorio.*;
import io.sicredi.spirecorrencia.api.testconfig.TestFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Recebedor.Schemas.Titles.CHAVE_PIX;
import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO.InformacaoAdicional.of;
import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO.TipoTemplate.RECORRENCIA_FALHA_MUDANCA_TITULARIDADE_CHAVE;
import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO.TipoTemplate.RECORRENCIA_FALHA_OPERACIONAL;
import static io.sicredi.spirecorrencia.api.utils.ConstantesTest.*;
import static io.sicredi.spirecorrencia.api.utils.ConstantesTest.RecorrenciaConstantes.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LiquidacaoServiceTest {

    private static final String CODIGO_ERRO = "REC-XXX";
    private static final String MENSAGEM_ERRO = "ERRO-XXX";
    public static final LocalTime TREZE_HORAS = LocalTime.of(13, 0);
    public static final LocalTime DEZOITO_HORAS = LocalTime.of(18, 0);
    public static final BigDecimal VALOR = new BigDecimal("1500.35");

    @Mock
    private NotificacaoProducer notificacaoProducer;

    @Mock
    private RecorrenciaRepository recorrenciaRepository;

    @Mock
    private SpiCanaisProtocoloApiClient spiCanaisProtocoloApiClient;

    @Mock
    private RecorrenciaTransacaoRepository recorrenciaTransacaoRepository;

    @Mock
    private CanaisDigitaisProtocoloInfoInternalApiClient canaisDigitaisProtocoloInfoInternalApiClient;

    @InjectMocks
    private LiquidacaoService liquidacaoService;

    @Captor
    private ArgumentCaptor<NotificacaoDTO> notificacaoDtoCaptor;

    @Mock
    private ConsultaChaveDictClient consultaChaveDictClient;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AppConfig appConfig;

    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private TentativaRecorrenciaTransacaoService tentativaService;

    @Captor
    private ArgumentCaptor<RecorrenciaTransacao> recorrenciaTransacaoCaptor;

    @Captor
    private ArgumentCaptor<ExclusaoRecorrenciaProtocoloRequest> captureExclusaoRecorrenciaProtocoloRequest;

    @Captor
    private ArgumentCaptor<Recorrencia> recorrenciaCaptor;

    @Nested
    class AtualizacaoRecorrenciaLiquidacaoDaTransacaoComErro {

        @Test
        void dadoOrdemErroProcessamentoResponseValido_quandoAtualizaRecorrenciaLiquidacaoDaTransacaoComErro_deveRegistrarTentativaETratarRetorno() {
            var mockHorarioAtual = LocalTime.of(17, 0, 0);
            try (var mockLocalTime = mockStatic(LocalTime.class)) {
                final var ordemErroProcessamentoResponse = new OrdemErroProcessamentoResponse(CodigoErro.S920.name(), ERRO_FALTA_DE_SALDO)
                        .adicionarIdFimAFim(ID_FIM_A_FIM);

                final var recorrenciaTransacao = buildRecorrenciaTransacao();

                when(appConfig.getRegras().getProcessamento().getHorarioLimiteLiquidacao()).thenReturn(HORARIO_LIMITE_LIQUIDACAO);
                when(recorrenciaTransacaoRepository.findByIdFimAFim(ID_FIM_A_FIM)).thenReturn(Optional.of(recorrenciaTransacao));
                mockLocalTime.when(LocalTime::now).thenReturn(mockHorarioAtual);

                liquidacaoService.atualizaRecorrenciaLiquidacaoDaTransacaoComErro(IDENTIFICADOR_TRANSACAO, ordemErroProcessamentoResponse);

                verify(tentativaService).registrarRecorrenciaTransacaoTentativa(eq(ERRO_FALTA_DE_SALDO), eq(CodigoErro.S920.name()), any());
            }
        }

        @ParameterizedTest
        @EnumSource(value = CodigoErro.class, mode = EnumSource.Mode.EXCLUDE, names = {"AB03", "AB09", "AB11", "AM04", "AM18", "DT02", "ED05", "S910", "S920", "S932", "S940", "S999"})
        void dadoErroProcessamentoNaPrimeiraLiquidacaoSemSerWebOpenBkComCodigoErroNaoPassivelRetry_quandoAtualizaRecorrenciaLiquidacaoDaTransacaoComErro_deveNotificarErroTentativaLiquidacaoEExcluirParcela(CodigoErro codigoErro) {
            var mockHorarioAtual = LocalTime.of(17, 0, 0);
            try (var mockLocalTime = mockStatic(LocalTime.class)) {
                final var ordemErroProcessamentoResponse = new OrdemErroProcessamentoResponse(codigoErro.name(), "Erro operacional")
                        .adicionarIdFimAFim(ID_FIM_A_FIM);

                final var recorrenciaTransacao = buildRecorrenciaTransacao();

                when(appConfig.getRegras().getProcessamento().getHorarioLimiteLiquidacao()).thenReturn(HORARIO_LIMITE_LIQUIDACAO);
                when(recorrenciaTransacaoRepository.findByIdFimAFim(ID_FIM_A_FIM)).thenReturn(Optional.of(recorrenciaTransacao));
                mockLocalTime.when(LocalTime::now).thenReturn(mockHorarioAtual);

                liquidacaoService.atualizaRecorrenciaLiquidacaoDaTransacaoComErro(IDENTIFICADOR_TRANSACAO, ordemErroProcessamentoResponse);

                verify(tentativaService).registrarRecorrenciaTransacaoTentativa(eq("Erro operacional"), eq(codigoErro.name()), any());
                verify(notificacaoProducer).enviarNotificacao(any(NotificacaoDTO.class));
                verify(spiCanaisProtocoloApiClient).emitirProtocoloCancelamentoRecorrencia(any(), eq(TipoExclusaoRecorrencia.EXCLUSAO_INTEGRADA), any());
            }
        }

        @ParameterizedTest
        @EnumSource(value = CodigoErro.class, mode = EnumSource.Mode.INCLUDE, names = {"AB03", "AB09", "AB11", "AM04", "AM18", "DT02", "ED05", "S910", "S932", "S940", "S999"})
        void dadoErroProcessamentoNaPrimeiraLiquidacaoSemSerWebOpenBkComCodigoErroPassivelRetry_quandoAtualizaRecorrenciaLiquidacaoDaTransacaoComErro_deveAlterarParcelaParaStatusCriado(CodigoErro codigoErro) {
            var mockHorarioAtual = LocalTime.of(17, 0, 0);
            try (var mockLocalTime = mockStatic(LocalTime.class)) {
                final var ordemErroProcessamentoResponse = new OrdemErroProcessamentoResponse(codigoErro.name(), "Erro passível de retry")
                        .adicionarIdFimAFim(ID_FIM_A_FIM);
                final var recorrenciaTransacao = buildRecorrenciaTransacao();

                when(appConfig.getRegras().getProcessamento().getHorarioLimiteLiquidacao()).thenReturn(HORARIO_LIMITE_LIQUIDACAO);
                when(recorrenciaTransacaoRepository.findByIdFimAFim(ID_FIM_A_FIM)).thenReturn(Optional.of(recorrenciaTransacao));
                mockLocalTime.when(LocalTime::now).thenReturn(mockHorarioAtual);

                liquidacaoService.atualizaRecorrenciaLiquidacaoDaTransacaoComErro(IDENTIFICADOR_TRANSACAO, ordemErroProcessamentoResponse);

                verify(tentativaService).registrarRecorrenciaTransacaoTentativa(eq("Erro passível de retry"), eq(codigoErro.name()), any());
                verify(notificacaoProducer, never()).enviarNotificacao(any());
                verify(spiCanaisProtocoloApiClient, never()).emitirProtocoloCancelamentoRecorrencia(any(), any(), any());
            }
        }

        @Test
        void dadoErroProcessamentoNaPrimeiraLiquidacaoSemSerWebOpenBkErroForFaltaDeSaldo_quandoAtualizaRecorrenciaLiquidacaoDaTransacaoComErro_deveAlterarParcelaParaStatusCriadoENotificarFaltaDeSaldo() {
            var mockHorarioAtual = LocalTime.of(17, 0, 0);
            try (var mockLocalTime = mockStatic(LocalTime.class)) {
                final var ordemErroProcessamentoResponse = new OrdemErroProcessamentoResponse(CodigoErro.S920.name(), ERRO_FALTA_DE_SALDO)
                        .adicionarIdFimAFim(ID_FIM_A_FIM);
                final var recorrenciaTransacao = buildRecorrenciaTransacao();

                when(appConfig.getRegras().getProcessamento().getHorarioLimiteLiquidacao()).thenReturn(HORARIO_LIMITE_LIQUIDACAO);
                when(recorrenciaTransacaoRepository.findByIdFimAFim(ID_FIM_A_FIM)).thenReturn(Optional.of(recorrenciaTransacao));
                mockLocalTime.when(LocalTime::now).thenReturn(mockHorarioAtual);

                liquidacaoService.atualizaRecorrenciaLiquidacaoDaTransacaoComErro(IDENTIFICADOR_TRANSACAO, ordemErroProcessamentoResponse);

                verify(tentativaService).registrarRecorrenciaTransacaoTentativa(eq(ERRO_FALTA_DE_SALDO), eq(CodigoErro.S920.name()), any());
                verify(spiCanaisProtocoloApiClient, never()).emitirProtocoloCancelamentoRecorrencia(any(), any(), any());
                verify(notificacaoProducer).enviarNotificacao(notificacaoDtoCaptor.capture());
                var notificacao = notificacaoDtoCaptor.getValue();
                assertEquals(NotificacaoDTO.TipoTemplate.RECORRENCIA_FALHA_SALDO_INSUFICIENTE, notificacao.getOperacao());
            }
        }

        private static RecorrenciaTransacao buildRecorrenciaTransacao() {
            return RecorrenciaTransacao.builder()
                    .idFimAFim(ID_FIM_A_FIM)
                    .dataTransacao(LocalDate.now())
                    .recorrencia(Recorrencia.builder()
                            .tipoOrigemSistema(OrigemEnum.LEGADO)
                            .recebedor(Recebedor.builder()
                                    .nome(NOME_RECEBEDOR)
                                    .cpfCnpj(CPF_RECEBEDOR)
                                    .build())
                            .pagador(Pagador.builder()
                                    .cpfCnpj(CPF_PAGADOR)
                                    .tipoConta(TipoContaEnum.CONTA_CORRENTE)
                                    .build())
                            .tipoCanal(TipoCanalEnum.MOBI)
                            .build())
                    .build();
        }

        @ParameterizedTest
        @CsvSource({
                "WEB_OPENBK, AGENDADO_RECORRENTE",
                "WEB_OPENBK, AGENDADO"
        })
        void dadoTipoCanalETipoRecorrencia_quandoAtualizaRecorrenciaLiquidacaoDaTransacaoComErro_deveRegistrarTentativaEExcluirParcela(TipoCanalEnum tipoCanal, TipoRecorrencia tipoRecorrencia) {
            var mockHorarioAtual = LocalTime.of(17, 0, 0);
            try (var mockLocalTime = mockStatic(LocalTime.class)) {
                final var ordemErroProcessamentoResponse = new OrdemErroProcessamentoResponse(CodigoErro.S920.name(), ERRO_FALTA_DE_SALDO)
                        .adicionarIdFimAFim(ID_FIM_A_FIM);

                var recorrenciaMock = TestFactory.RecorrenciaTestFactory.criarRecorrencia(LocalDateTime.now());

                recorrenciaMock.setTipoCanal(tipoCanal);
                recorrenciaMock.setTipoRecorrencia(tipoRecorrencia);

                var recorrenciaTransacaoMock = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrenciaMock, LocalDate.now(), new BigDecimal(100));

                when(appConfig.getRegras().getProcessamento().getHorarioLimiteLiquidacao()).thenReturn(HORARIO_LIMITE_LIQUIDACAO);
                when(recorrenciaTransacaoRepository.findByIdFimAFim(ID_FIM_A_FIM)).thenReturn(Optional.of(recorrenciaTransacaoMock));
                mockLocalTime.when(LocalTime::now).thenReturn(mockHorarioAtual);

                liquidacaoService.atualizaRecorrenciaLiquidacaoDaTransacaoComErro(IDENTIFICADOR_TRANSACAO, ordemErroProcessamentoResponse);

                verify(tentativaService).registrarRecorrenciaTransacaoTentativa(eq(ERRO_FALTA_DE_SALDO), eq(CodigoErro.S920.name()), any());
                verify(notificacaoProducer).enviarNotificacao(any(NotificacaoDTO.class));
                verify(spiCanaisProtocoloApiClient).emitirProtocoloCancelamentoRecorrencia(eq(tipoCanal), eq(TipoExclusaoRecorrencia.EXCLUSAO_INTEGRADA), captureExclusaoRecorrenciaProtocoloRequest.capture());

                var request = captureExclusaoRecorrenciaProtocoloRequest.getValue();

                var pagador = recorrenciaMock.getPagador();

                assertAll("Identificação do Associado",
                        () -> assertEquals(pagador.getTipoConta().getTipoContaCanaisDigitais(), request.getIdentificacaoAssociado().getTipoConta()),
                        () -> assertEquals(pagador.getCpfCnpj(), request.getIdentificacaoAssociado().getCpfUsuario()),
                        () -> assertEquals(pagador.getCpfCnpj(), request.getIdentificacaoAssociado().getCpfCnpjConta()),
                        () -> assertEquals(pagador.getConta(), request.getIdentificacaoAssociado().getConta()),
                        () -> assertEquals(pagador.getCodPosto(), request.getIdentificacaoAssociado().getAgencia()),
                        () -> assertEquals(pagador.getNome(), request.getIdentificacaoAssociado().getNomeAssociadoConta()),
                        () -> assertEquals(pagador.getNome(), request.getIdentificacaoAssociado().getNomeUsuario()),
                        () -> assertEquals(pagador.getAgencia(), request.getIdentificacaoAssociado().getCooperativa()),
                        () -> assertEquals(recorrenciaTransacaoMock.getRecorrencia().getIdRecorrencia(), request.getIdentificadorRecorrencia()),
                        () -> assertEquals(TipoMotivoExclusao.SOLICITADO_SISTEMA, request.getTipoMotivoExclusao()),
                        () -> assertEquals(IDENTIFICADOR_TRANSACAO, request.getIdentificadorTransacao()),
                        () -> assertEquals(1, request.getParcelas().size())
                );
            }
        }

    }

    @ParameterizedTest
    @EnumSource(value = OrdemStatus.class, names = "CONCLUIDO", mode = EnumSource.Mode.EXCLUDE)
    void dadoStatusTransacaoDtoDiferenteConcluido_quandoProcessarRetornoPagamentoComRecorrencia_deveIgnorarProcessamento(OrdemStatus status) {
        final var transacaoDto = TransacaoDto.builder()
                .status(status)
                .build();

        liquidacaoService.processarRetornoPagamentoComRecorrencia(IDENTIFICADOR_TRANSACAO, transacaoDto);

        verifyNoInteractions(canaisDigitaisProtocoloInfoInternalApiClient, spiCanaisProtocoloApiClient, notificacaoProducer, recorrenciaRepository, recorrenciaTransacaoRepository);
    }

    @Test
    void dadoRetornoConsultaProtocoloNull_quandoProcessarRetornoPagamentoComRecorrencia_deveLancarNotFoundException() {
        final var transacaoDto = TransacaoDto.builder()
                .idFimAFim(ID_FIM_A_FIM)
                .status(OrdemStatus.CONCLUIDO)
                .build();

        final var exception = assertThrows(NotFoundException.class, () -> liquidacaoService.processarRetornoPagamentoComRecorrencia(IDENTIFICADOR_TRANSACAO, transacaoDto));

        assertEquals("Protocolo 358 não encontrado para o idFimAFim E91586982202309111724011vuHEvJGT", exception.getMessage());
        verify(canaisDigitaisProtocoloInfoInternalApiClient).consultaProtocoloPorTipoEIdentificador("358", ID_FIM_A_FIM);
        verifyNoInteractions(spiCanaisProtocoloApiClient, notificacaoProducer, recorrenciaRepository, recorrenciaTransacaoRepository);
    }

    @Test
    void dadoProtocoloPagamento_quandoProcessarRetornoPagamentoComRecorrencia_deveEmitirProtocoloRecorrenciaIntegrado() {
        final var transacaoDto = TransacaoDto.builder()
                .idFimAFim(ID_FIM_A_FIM)
                .status(OrdemStatus.CONCLUIDO)
                .build();

        final var protocoloDto = new ProtocoloDTO();
        protocoloDto.setPayloadTransacao(PAYLOAD_TRANSACAO_PAGAMENTO_COM_RECORRENCIA);

        when(canaisDigitaisProtocoloInfoInternalApiClient.consultaProtocoloPorTipoEIdentificador(any(), any())).thenReturn(protocoloDto);

        liquidacaoService.processarRetornoPagamentoComRecorrencia(IDENTIFICADOR_TRANSACAO, transacaoDto);

        verify(canaisDigitaisProtocoloInfoInternalApiClient).consultaProtocoloPorTipoEIdentificador("358", ID_FIM_A_FIM);
        verify(spiCanaisProtocoloApiClient).emitirProtocoloCadastroRecorrenciaIntegrada(eq(TipoCanalEnum.MOBI), eq("CADASTRO_INTEGRADO"), any());
    }

    @Test
    void dadoFalhaAoEmitirProtocoloRecorrenciaIntegrada_quandoProcessarRetornoPagamentoComRecorrencia_deveEnviarNotificacaoErro() {
        final var transacaoDto = TransacaoDto.builder()
                .idFimAFim(ID_FIM_A_FIM)
                .status(OrdemStatus.CONCLUIDO)
                .build();

        final var protocoloDto = new ProtocoloDTO();
        protocoloDto.setPayloadTransacao(PAYLOAD_TRANSACAO_PAGAMENTO_COM_RECORRENCIA);

        when(canaisDigitaisProtocoloInfoInternalApiClient.consultaProtocoloPorTipoEIdentificador(any(), any())).thenReturn(protocoloDto);
        doThrow(BadRequestException.class).when(spiCanaisProtocoloApiClient).emitirProtocoloCadastroRecorrenciaIntegrada(any(), any(), any());

        liquidacaoService.processarRetornoPagamentoComRecorrencia(IDENTIFICADOR_TRANSACAO, transacaoDto);

        verify(canaisDigitaisProtocoloInfoInternalApiClient).consultaProtocoloPorTipoEIdentificador("358", ID_FIM_A_FIM);
        verify(spiCanaisProtocoloApiClient).emitirProtocoloCadastroRecorrenciaIntegrada(eq(TipoCanalEnum.MOBI), eq("CADASTRO_INTEGRADO"), any());
        verify(notificacaoProducer).enviarNotificacao(
                argThat(notificacao -> NotificacaoDTO.TipoTemplate.RECORRENCIA_CADASTRO_FALHA == notificacao.getOperacao())
        );
    }

    @Test
    void dadoProtocoloPagamento_quandoProcessarRetornoPagamentoComAutomatico_deveEmitirProtocoloAutomaticoIntegrado() {
        final var transacaoDto = TransacaoDto.builder()
                .idFimAFim(ID_FIM_A_FIM)
                .status(OrdemStatus.CONCLUIDO)
                .build();

        final var protocoloDto = new ProtocoloDTO();
        protocoloDto.setPayloadTransacao(PAYLOAD_TRANSACAO_PAGAMENTO_COM_AUTOMATICO);

        when(canaisDigitaisProtocoloInfoInternalApiClient.consultaProtocoloPorTipoEIdentificador(any(), any())).thenReturn(protocoloDto);

        liquidacaoService.processarRetornoPagamentoComAutomatico(transacaoDto);

        verify(canaisDigitaisProtocoloInfoInternalApiClient).consultaProtocoloPorTipoEIdentificador("358", ID_FIM_A_FIM);
        verify(spiCanaisProtocoloApiClient).emitirProtocoloCadastroAutorizacaoIntegrada(eq(TipoCanalEnum.MOBI), eq("CADASTRO_INTEGRADO"), any());
    }

    @Test
    void dadoFalhaAoEmitirProtocoloAutomaticoIntegrado_quandoProcessarRetornoPagamentoComAutomatico_deveEnviarNotificacaoErro() {
        final var transacaoDto = TransacaoDto.builder()
                .idFimAFim(ID_FIM_A_FIM)
                .status(OrdemStatus.CONCLUIDO)
                .build();

        final var protocoloDto = new ProtocoloDTO();
        protocoloDto.setPayloadTransacao(PAYLOAD_TRANSACAO_PAGAMENTO_COM_AUTOMATICO);

        when(canaisDigitaisProtocoloInfoInternalApiClient.consultaProtocoloPorTipoEIdentificador(any(), any())).thenReturn(protocoloDto);
        doThrow(BadRequestException.class).when(spiCanaisProtocoloApiClient).emitirProtocoloCadastroAutorizacaoIntegrada(any(), any(), any());

        liquidacaoService.processarRetornoPagamentoComAutomatico(transacaoDto);

        verify(canaisDigitaisProtocoloInfoInternalApiClient).consultaProtocoloPorTipoEIdentificador("358", ID_FIM_A_FIM);
        verify(spiCanaisProtocoloApiClient).emitirProtocoloCadastroAutorizacaoIntegrada(eq(TipoCanalEnum.MOBI), eq("CADASTRO_INTEGRADO"), any());
        verify(notificacaoProducer).enviarNotificacao(
                argThat(notificacao -> NotificacaoDTO.TipoTemplate.AUTOMATICO_AUTORIZACAO_CONFIRMADA_PAGADOR_FALHA_NAO_RESPONDIDA_OU_CANCELADA_RECEBEDOR == notificacao.getOperacao())
        );
    }

    @Test
    void dadoRecorrenciaNaoEncontrada_quandoProcessarRetornoPagamentoAgendadoRecorrente_deveLancarNotFoundException() {
        final var transacaoDto = TransacaoDto.builder()
                .idFimAFim(ID_FIM_A_FIM)
                .build();

        final var exception = assertThrows(NotFoundException.class, () -> liquidacaoService.processarRetornoPagamentoAgendadoRecorrente(IDENTIFICADOR_TRANSACAO, transacaoDto));

        assertEquals("Parcela não encontrada para o idFimAFim E91586982202309111724011vuHEvJGT", exception.getMessage());

        verify(recorrenciaTransacaoRepository).findByIdFimAFim(ID_FIM_A_FIM);
        verifyNoInteractions(recorrenciaRepository, notificacaoProducer);
        verifyNoMoreInteractions(recorrenciaTransacaoRepository);
    }

    @ParameterizedTest
    @EnumSource(value = TipoStatusEnum.class, names = "PENDENTE", mode = EnumSource.Mode.EXCLUDE)
    void dadoTransacaoComStatusDiferenteDePendente_quandoProcessarRetornoPagamentoAgendadoRecorrente_deveIgnorarProcessamento(TipoStatusEnum status) {
        final var transacaoDto = TransacaoDto.builder().idFimAFim(ID_FIM_A_FIM).build();
        final var recorrenciaTransacao = RecorrenciaTransacao.builder()
                .tpoStatus(status)
                .build();

        when(recorrenciaTransacaoRepository.findByIdFimAFim(any())).thenReturn(Optional.of(recorrenciaTransacao));

        liquidacaoService.processarRetornoPagamentoAgendadoRecorrente(IDENTIFICADOR_TRANSACAO, transacaoDto);

        verify(recorrenciaTransacaoRepository).findByIdFimAFim(ID_FIM_A_FIM);
        verifyNoMoreInteractions(recorrenciaTransacaoRepository);
        verifyNoInteractions(recorrenciaRepository, notificacaoProducer);
    }

    @ParameterizedTest
    @EnumSource(value = OrdemStatus.class, names = {"CONCLUIDO", "CANCELADO"}, mode = EnumSource.Mode.EXCLUDE)
    void dadoStatusTransacaoDtoDiferenteConcluidoOuCancelado_quandoProcessarRetornoPagamentoAgendadoRecorrente_deveIgnorarProcessamento(OrdemStatus status) {
        final var transacaoDto = TransacaoDto.builder()
                .status(status)
                .idFimAFim(ID_FIM_A_FIM)
                .build();
        final var recorrenciaTransacao = RecorrenciaTransacao.builder()
                .tpoStatus(TipoStatusEnum.PENDENTE)
                .build();

        when(recorrenciaTransacaoRepository.findByIdFimAFim(any())).thenReturn(Optional.of(recorrenciaTransacao));

        liquidacaoService.processarRetornoPagamentoAgendadoRecorrente(IDENTIFICADOR_TRANSACAO, transacaoDto);

        verify(recorrenciaTransacaoRepository).findByIdFimAFim(ID_FIM_A_FIM);
        verifyNoMoreInteractions(recorrenciaTransacaoRepository);
        verifyNoInteractions(recorrenciaRepository, notificacaoProducer);
    }

    @Test
    void dadoRecorrenciaTransacaoConcluida_quandoProcessarRetornoPagamentoAgendadoRecorrente_deveNotificarLiquidacaoDaTransacao() {
        final var transacaoDto = TransacaoDto.builder()
                .status(OrdemStatus.CONCLUIDO)
                .idFimAFim(ID_FIM_A_FIM)
                .build();
        final var recorrencia = Recorrencia.builder()
                .oidRecorrencia(OID_RECORRENCIA)
                .recebedor(Recebedor.builder()
                        .nome(NOME_RECEBEDOR)
                        .build())
                .pagador(Pagador.builder()
                        .agencia(AGENCIA)
                        .conta(CONTA)
                        .cpfCnpj(CPF_PAGADOR)
                        .build())
                .tipoCanal(TipoCanalEnum.MOBI)
                .build();
        final var recorrenciaTransacaoCriada = RecorrenciaTransacao.builder()
                .idParcela(ID_PARCELA_2)
                .tpoStatus(TipoStatusEnum.CRIADO)
                .build();
        final var recorrenciaTransacaoLiquidada = RecorrenciaTransacao.builder()
                .idParcela(ID_PARCELA_1)
                .idFimAFim(ID_FIM_A_FIM)
                .tpoStatus(TipoStatusEnum.PENDENTE)
                .recorrencia(recorrencia)
                .valor(VALOR)
                .build();

        recorrencia.setRecorrencias(List.of(recorrenciaTransacaoLiquidada, recorrenciaTransacaoCriada));

        when(recorrenciaTransacaoRepository.findByIdFimAFim(any())).thenReturn(Optional.of(recorrenciaTransacaoLiquidada));

        liquidacaoService.processarRetornoPagamentoAgendadoRecorrente(IDENTIFICADOR_TRANSACAO, transacaoDto);

        verify(recorrenciaTransacaoRepository).findByIdFimAFim(ID_FIM_A_FIM);
        verify(recorrenciaTransacaoRepository).save(recorrenciaTransacaoCaptor.capture());
        final var recorrenciaSalva = recorrenciaTransacaoCaptor.getValue();
        assertEquals(TipoStatusEnum.CONCLUIDO, recorrenciaSalva.getTpoStatus());
        assertEquals(ID_PARCELA_1, recorrenciaSalva.getIdParcela());

        verify(notificacaoProducer).enviarNotificacao(notificacaoDtoCaptor.capture());
        assertThat(notificacaoDtoCaptor.getValue())
                .usingRecursiveComparison()
                .isEqualTo(NotificacaoDTO.builder()
                        .agencia(AGENCIA)
                        .conta(CONTA)
                        .canal(CANAL)
                        .operacao(NotificacaoDTO.TipoTemplate.RECORRENCIA_SUCESSO_PAGAMENTO_PARCELA)

                        .informacoesAdicionais(List.of(
                                of(NotificacaoInformacaoAdicional.VALOR, String.valueOf(VALOR)),
                                of(NotificacaoInformacaoAdicional.NOME_RECEBEDOR, recorrenciaTransacaoLiquidada.getNomeRecebedor()),
                                of(NotificacaoInformacaoAdicional.DOCUMENTO_PAGADOR, CPF_PAGADOR)
                        ))
                        .build());

        verifyNoInteractions(recorrenciaRepository);
        verifyNoMoreInteractions(recorrenciaTransacaoRepository, notificacaoProducer);
    }

    @Test
    void dadoRecorrenciaTransacaoConcluidaERecorrenciaPaiSemParcelasPendentes_quandoProcessarRetornoPagamentoAgendadoRecorrente_deveNotificarLiquidacaoDaTransacaoEDaRecorrenciaPai() {
        final var transacaoDto = TransacaoDto.builder()
                .status(OrdemStatus.CONCLUIDO)
                .idFimAFim(ID_FIM_A_FIM)
                .build();
        final var recorrencia = Recorrencia.builder()
                .oidRecorrencia(OID_RECORRENCIA)
                .tipoRecorrencia(TipoRecorrencia.AGENDADO_RECORRENTE)
                .recebedor(Recebedor.builder()
                        .nome(NOME_RECEBEDOR)
                        .build())
                .pagador(Pagador.builder()
                        .agencia(AGENCIA)
                        .conta(CONTA)
                        .cpfCnpj(CPF_PAGADOR)
                        .build())
                .tipoCanal(TipoCanalEnum.MOBI)
                .nome(NOME_RECORRENCIA)
                .build();
        final var recorrenciaTransacaoLiquidada = RecorrenciaTransacao.builder()
                .idParcela(ID_PARCELA_1)
                .idFimAFim(ID_FIM_A_FIM)
                .tpoStatus(TipoStatusEnum.PENDENTE)
                .recorrencia(recorrencia)
                .valor(VALOR)
                .build();
        final var recorrenciaTransacaoExcluida = RecorrenciaTransacao.builder()
                .idParcela(ID_PARCELA_2)
                .idFimAFim(ID_FIM_A_FIM)
                .tpoStatus(TipoStatusEnum.EXCLUIDO)
                .recorrencia(recorrencia)
                .valor(VALOR)
                .build();

        recorrencia.setRecorrencias(List.of(recorrenciaTransacaoLiquidada, recorrenciaTransacaoExcluida));

        when(recorrenciaTransacaoRepository.findByIdFimAFim(any())).thenReturn(Optional.of(recorrenciaTransacaoLiquidada));

        liquidacaoService.processarRetornoPagamentoAgendadoRecorrente(IDENTIFICADOR_TRANSACAO, transacaoDto);

        verify(recorrenciaTransacaoRepository).findByIdFimAFim(ID_FIM_A_FIM);
        verify(recorrenciaTransacaoRepository).save(recorrenciaTransacaoCaptor.capture());
        final var recorrenciaTransacaoSalva = recorrenciaTransacaoCaptor.getValue();
        assertEquals(TipoStatusEnum.CONCLUIDO, recorrenciaTransacaoSalva.getTpoStatus());
        assertEquals(ID_PARCELA_1, recorrenciaTransacaoSalva.getIdParcela());

        verify(notificacaoProducer, times(2)).enviarNotificacao(notificacaoDtoCaptor.capture());

        final var notificacoes = notificacaoDtoCaptor.getAllValues();

        assertEquals(2, notificacoes.size());

        assertThat(notificacoes.getFirst())
                .usingRecursiveComparison()
                .isEqualTo(NotificacaoDTO.builder()
                        .agencia(AGENCIA)
                        .conta(CONTA)
                        .canal(CANAL)
                        .operacao(NotificacaoDTO.TipoTemplate.RECORRENCIA_SUCESSO_FINALIZACAO)
                        .informacoesAdicionais(List.of(
                                of(NotificacaoInformacaoAdicional.NOME_RECORRENCIA, NOME_RECORRENCIA),
                                of(NotificacaoInformacaoAdicional.DOCUMENTO_PAGADOR, CPF_PAGADOR)
                        ))
                        .build());

        assertThat(notificacoes.getLast())
                .usingRecursiveComparison()
                .isEqualTo(NotificacaoDTO.builder()
                        .agencia(AGENCIA)
                        .conta(CONTA)
                        .canal(CANAL)
                        .operacao(NotificacaoDTO.TipoTemplate.RECORRENCIA_SUCESSO_PAGAMENTO_PARCELA)
                        .informacoesAdicionais(List.of(
                                of(NotificacaoInformacaoAdicional.VALOR, String.valueOf(VALOR)),
                                of(NotificacaoInformacaoAdicional.NOME_RECEBEDOR, recorrenciaTransacaoLiquidada.getNomeRecebedor()),
                                of(NotificacaoInformacaoAdicional.DOCUMENTO_PAGADOR, CPF_PAGADOR)
                        ))
                        .build());

        verify(recorrenciaRepository).save(recorrenciaCaptor.capture());
        final var recorrenciaSalva = recorrenciaCaptor.getValue();
        assertEquals(OID_RECORRENCIA, recorrenciaSalva.getOidRecorrencia());
        assertEquals(TipoStatusEnum.CONCLUIDO, recorrenciaSalva.getTipoStatus());

        verifyNoMoreInteractions(recorrenciaTransacaoRepository, recorrenciaRepository, notificacaoProducer);
    }

    @Test
    void dadoRecorrenciaTransacaoConcluidaERecorrenciaPaiSemParcelasPendentes_quandoProcessarRetornoPagamentoAgendadoRecorrente_deveNotificarLiquidacaoDaTransacaoENaoDaRecorrenciaPai() {
        final var transacaoDto = TransacaoDto.builder()
                .status(OrdemStatus.CONCLUIDO)
                .idFimAFim(ID_FIM_A_FIM)
                .build();
        final var recorrencia = Recorrencia.builder()
                .oidRecorrencia(OID_RECORRENCIA)
                .recebedor(Recebedor.builder()
                        .nome(NOME_RECEBEDOR)
                        .build())
                .pagador(Pagador.builder()
                        .conta(CONTA)
                        .cpfCnpj(CPF_PAGADOR)
                        .agencia(AGENCIA)
                        .build())
                .nome(NOME_RECORRENCIA)
                .tipoCanal(TipoCanalEnum.MOBI)
                .build();
        final var recorrenciaTransacaoExcluida = RecorrenciaTransacao.builder()
                .idParcela(ID_PARCELA_2)
                .tpoStatus(TipoStatusEnum.EXCLUIDO)
                .recorrencia(recorrencia)
                .idFimAFim(ID_FIM_A_FIM)
                .valor(VALOR)
                .build();
        final var recorrenciaTransacaoLiquidada = RecorrenciaTransacao.builder()
                .idParcela(ID_PARCELA_1)
                .tpoStatus(TipoStatusEnum.PENDENTE)
                .recorrencia(recorrencia)
                .idFimAFim(ID_FIM_A_FIM)
                .valor(VALOR)
                .build();

        recorrencia.setRecorrencias(List.of(recorrenciaTransacaoLiquidada, recorrenciaTransacaoExcluida));

        when(recorrenciaTransacaoRepository.findByIdFimAFim(any())).thenReturn(Optional.of(recorrenciaTransacaoLiquidada));

        liquidacaoService.processarRetornoPagamentoAgendadoRecorrente(IDENTIFICADOR_TRANSACAO, transacaoDto);

        verify(recorrenciaTransacaoRepository).save(recorrenciaTransacaoCaptor.capture());
        verify(recorrenciaTransacaoRepository).findByIdFimAFim(ID_FIM_A_FIM);

        final var recorrenciaTransacaoSalva = recorrenciaTransacaoCaptor.getValue();

        verify(notificacaoProducer, times(1)).enviarNotificacao(notificacaoDtoCaptor.capture());

        final var notificacoes = notificacaoDtoCaptor.getAllValues();

        assertEquals(1, notificacoes.size());
        assertEquals(TipoStatusEnum.CONCLUIDO, recorrenciaTransacaoSalva.getTpoStatus());
        assertEquals(ID_PARCELA_1, recorrenciaTransacaoSalva.getIdParcela());

        assertThat(notificacoes.getLast())
                .usingRecursiveComparison()
                .isEqualTo(NotificacaoDTO.builder()
                        .agencia(AGENCIA)
                        .conta(CONTA)
                        .canal(CANAL)
                        .operacao(NotificacaoDTO.TipoTemplate.RECORRENCIA_SUCESSO_PAGAMENTO_PARCELA)
                        .informacoesAdicionais(List.of(
                                of(NotificacaoInformacaoAdicional.VALOR, String.valueOf(VALOR)),
                                of(NotificacaoInformacaoAdicional.NOME_RECEBEDOR, recorrenciaTransacaoLiquidada.getNomeRecebedor()),
                                of(NotificacaoInformacaoAdicional.DOCUMENTO_PAGADOR, CPF_PAGADOR)
                        ))
                        .build());

        verify(recorrenciaRepository).save(recorrenciaCaptor.capture());

        final var recorrenciaSalva = recorrenciaCaptor.getValue();

        assertEquals(TipoStatusEnum.CONCLUIDO, recorrenciaSalva.getTipoStatus());
        assertEquals(OID_RECORRENCIA, recorrenciaSalva.getOidRecorrencia());

        verifyNoMoreInteractions(recorrenciaTransacaoRepository, recorrenciaRepository, notificacaoProducer);
    }

    @Nested
    class ConsultarTipoProcessamentoLiquidacao {

        @Test
        void dadoDadosDictsDiferentes_quandoConsultarTipoProcessamento_deveRetornarTipoProcessamentoExclusaoTotal() throws JsonProcessingException {
            var recorrenciaMock = TestFactory.RecorrenciaTestFactory.criarRecorrencia(LocalDateTime.now());
            var recorrenciaTransacaoMock = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrenciaMock, LocalDate.now(), new BigDecimal(100));
            var channelDict = recorrenciaTransacaoMock.criarChannelData();

            when(objectMapper.writeValueAsString(any())).thenReturn(channelDict.toString());
            when(consultaChaveDictClient.consultaChaveDict(any(), any(), any(), any(), any(), anyBoolean())).thenReturn(Optional.empty());

            var responseTipoProcessamento = liquidacaoService.consultarTipoProcessamento(IDENTIFICADOR_TRANSACAO, recorrenciaTransacaoMock);

            assertThat(responseTipoProcessamento)
                    .usingRecursiveComparison()
                    .isEqualTo(TipoProcessamentoWrapperDTO.builder()
                            .identificadorTransacao(IDENTIFICADOR_TRANSACAO)
                            .tipoProcessamentoEnum(TipoProcessamentoEnum.EXCLUSAO_TOTAL)
                            .recorrenciaTransacao(recorrenciaTransacaoMock)
                            .tipoProcessamentoErro(TipoProcessamentoWrapperDTO.TipoProcessamentoErro.builder()
                                    .tipoMotivoExclusao(TipoMotivoExclusao.SOLICITADO_SISTEMA)
                                    .codigoErro(AppExceptionCode.REC_PROC_BU0001.getCode())
                                    .mensagemErro(AppExceptionCode.REC_PROC_BU0001.getMessage())
                                    .build())
                            .templateNotificacao(RECORRENCIA_FALHA_MUDANCA_TITULARIDADE_CHAVE)
                            .build());

            verify(consultaChaveDictClient).consultaChaveDict(recorrenciaMock.getRecebedor().getChave(), recorrenciaMock.getPagador().getCpfCnpj(), recorrenciaMock.getPagador().getAgencia(), recorrenciaMock.getTipoCanal().name(), channelDict.toString(), false);
        }


        @Test
        void dadoDadosDictsNaoExistentes_quandoConsultarTipoProcessamento_deveRetornarTipoProcessamentoExclusaoTotal() throws JsonProcessingException {
            var recorrenciaMock = TestFactory.RecorrenciaTestFactory.criarRecorrencia(LocalDateTime.now());
            var recorrenciaTransacaoMock = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrenciaMock, LocalDate.now(), new BigDecimal(100));
            var channelDict = recorrenciaTransacaoMock.criarChannelData();

            when(objectMapper.writeValueAsString(any())).thenReturn(channelDict.toString());

            doThrow(new DictException(HttpStatus.NOT_FOUND, CODIGO_ERRO, MENSAGEM_ERRO)).when(consultaChaveDictClient).consultaChaveDict(recorrenciaMock.getRecebedor().getChave(), recorrenciaMock.getPagador().getCpfCnpj(), recorrenciaMock.getPagador().getAgencia(), recorrenciaMock.getTipoCanal().name(), channelDict.toString(), false);

            var responseTipoProcessamento = liquidacaoService.consultarTipoProcessamento(IDENTIFICADOR_TRANSACAO, recorrenciaTransacaoMock);

            assertThat(responseTipoProcessamento)
                    .usingRecursiveComparison()
                    .isEqualTo(TipoProcessamentoWrapperDTO.builder()
                            .identificadorTransacao(IDENTIFICADOR_TRANSACAO)
                            .tipoProcessamentoEnum(TipoProcessamentoEnum.EXCLUSAO_TOTAL)
                            .recorrenciaTransacao(recorrenciaTransacaoMock)
                            .templateNotificacao(RECORRENCIA_FALHA_MUDANCA_TITULARIDADE_CHAVE)
                            .tipoProcessamentoErro(TipoProcessamentoWrapperDTO.TipoProcessamentoErro.builder()
                                    .tipoMotivoExclusao(TipoMotivoExclusao.SOLICITADO_SISTEMA)
                                    .codigoErro(AppExceptionCode.REC_PROC_BU0002.getCode())
                                    .mensagemErro(AppExceptionCode.REC_PROC_BU0002.getMessage())
                                    .build())
                            .build());

            verify(consultaChaveDictClient).consultaChaveDict(recorrenciaMock.getRecebedor().getChave(), recorrenciaMock.getPagador().getCpfCnpj(), recorrenciaMock.getPagador().getAgencia(), recorrenciaMock.getTipoCanal().name(), channelDict.toString(), false);
        }

        @Test
        void dadoDadosDictForIguais_quandoConsultarTipoProcessamento_deveRetornarTipoProcessamentoLiquidacao() throws JsonProcessingException {
            var recorrenciaMock = TestFactory.RecorrenciaTestFactory.criarRecorrencia(LocalDateTime.now());
            var recorrenciaTransacaoMock = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrenciaMock, LocalDate.now(), new BigDecimal(100));
            var channelDict = recorrenciaTransacaoMock.criarChannelData();

            var dictConsultaMock = criarDictConsultaDTO(TipoChaveEnum.EMAIL, CHAVE_PIX, recorrenciaMock.getPagador().getCpfCnpj());

            when(objectMapper.writeValueAsString(any())).thenReturn(channelDict.toString());
            when(consultaChaveDictClient.consultaChaveDict(recorrenciaMock.getRecebedor().getChave(), recorrenciaMock.getPagador().getCpfCnpj(), recorrenciaMock.getPagador().getAgencia(), recorrenciaMock.getTipoCanal().name(), channelDict.toString(), false)).thenReturn(Optional.of(dictConsultaMock));

            var responseTipoProcessamento = liquidacaoService.consultarTipoProcessamento(IDENTIFICADOR_TRANSACAO, recorrenciaTransacaoMock);

            assertThat(responseTipoProcessamento)
                    .usingRecursiveComparison()
                    .isEqualTo(TipoProcessamentoWrapperDTO.builder()
                            .identificadorTransacao(IDENTIFICADOR_TRANSACAO)
                            .tipoProcessamentoEnum(TipoProcessamentoEnum.LIQUIDACAO)
                            .recorrenciaTransacao(recorrenciaTransacaoMock)
                            .build());

            verify(consultaChaveDictClient).consultaChaveDict(recorrenciaMock.getRecebedor().getChave(), recorrenciaMock.getPagador().getCpfCnpj(), recorrenciaMock.getPagador().getAgencia(), recorrenciaMock.getTipoCanal().name(), channelDict.toString(), false);
        }

        @ParameterizedTest
        @EnumSource(value = HttpStatus.class, mode = EnumSource.Mode.INCLUDE, names = {"INTERNAL_SERVER_ERROR", "BAD_REQUEST"})
        void dadoDadosDictRetornarErro5xxOu4xx_quandoConsultarTipoProcessamento_deveRetornarTipoProcessamentoExclusaoParcial(HttpStatus httpStatus) throws JsonProcessingException {
            var horarioExecucao = LocalTime.of(18, 1);
            try (var mockLocalTime = mockStatic(LocalTime.class)) {
                var recorrenciaMock = TestFactory.RecorrenciaTestFactory.criarRecorrencia(LocalDateTime.now());
                recorrenciaMock.setTipoCanal(TipoCanalEnum.MOBI);
                var recorrenciaTransacaoMock = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrenciaMock, LocalDate.now(), new BigDecimal(100));
                var channelDict = recorrenciaTransacaoMock.criarChannelData();

                when(appConfig.getRegras().getProcessamento().getHorarioLimiteLiquidacao()).thenReturn(DEZOITO_HORAS);
                when(objectMapper.writeValueAsString(any())).thenReturn(channelDict.toString());
                mockLocalTime.when(LocalTime::now).thenReturn(horarioExecucao);
                doThrow(new DictException(httpStatus, CODIGO_ERRO, MENSAGEM_ERRO)).when(consultaChaveDictClient).consultaChaveDict(recorrenciaMock.getRecebedor().getChave(), recorrenciaMock.getPagador().getCpfCnpj(), recorrenciaMock.getPagador().getAgencia(), recorrenciaMock.getTipoCanal().name(), channelDict.toString(), false);

                var responseTipoProcessamento = liquidacaoService.consultarTipoProcessamento(IDENTIFICADOR_TRANSACAO, recorrenciaTransacaoMock);

                assertThat(responseTipoProcessamento)
                        .usingRecursiveComparison()
                        .isEqualTo(TipoProcessamentoWrapperDTO.builder()
                                .identificadorTransacao(IDENTIFICADOR_TRANSACAO)
                                .tipoProcessamentoEnum(TipoProcessamentoEnum.EXCLUSAO_PARCIAL)
                                .recorrenciaTransacao(recorrenciaTransacaoMock)
                                .templateNotificacao(RECORRENCIA_FALHA_OPERACIONAL)
                                .tipoProcessamentoErro(TipoProcessamentoWrapperDTO.TipoProcessamentoErro.builder()
                                        .tipoMotivoExclusao(TipoMotivoExclusao.SOLICITADO_SISTEMA)
                                        .codigoErro(CODIGO_ERRO)
                                        .mensagemErro(MENSAGEM_ERRO)
                                        .build())
                                .build());

                verify(consultaChaveDictClient).consultaChaveDict(recorrenciaMock.getRecebedor().getChave(), recorrenciaMock.getPagador().getCpfCnpj(), recorrenciaMock.getPagador().getAgencia(), recorrenciaMock.getTipoCanal().name(), channelDict.toString(), false);
            }
        }

        @Test
        void dadoErro5xxDictParaCanalWebOpenBk_quandoConsultarTipoProcessamento_deveRetornarTipoProcessamentoExclusaoParcial() throws JsonProcessingException {
            try (var mockLocalTime = mockStatic(LocalTime.class)) {
                var recorrenciaMock = TestFactory.RecorrenciaTestFactory.criarRecorrencia(LocalDateTime.now());
                recorrenciaMock.setTipoCanal(TipoCanalEnum.WEB_OPENBK);
                var recorrenciaTransacaoMock = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrenciaMock, LocalDate.now(), new BigDecimal(100));
                var channelDict = recorrenciaTransacaoMock.criarChannelData();

                when(appConfig.getRegras().getProcessamento().getHorarioLimiteLiquidacao()).thenReturn(DEZOITO_HORAS);
                when(objectMapper.writeValueAsString(any())).thenReturn(channelDict.toString());
                mockLocalTime.when(LocalTime::now).thenReturn(TREZE_HORAS);
                doThrow(new DictException(HttpStatus.INTERNAL_SERVER_ERROR, CODIGO_ERRO, MENSAGEM_ERRO)).when(consultaChaveDictClient).consultaChaveDict(recorrenciaMock.getRecebedor().getChave(), recorrenciaMock.getPagador().getCpfCnpj(), recorrenciaMock.getPagador().getAgencia(), recorrenciaMock.getTipoCanal().name(), channelDict.toString(), false);

                var responseTipoProcessamento = liquidacaoService.consultarTipoProcessamento(IDENTIFICADOR_TRANSACAO, recorrenciaTransacaoMock);

                assertThat(responseTipoProcessamento)
                        .usingRecursiveComparison()
                        .isEqualTo(TipoProcessamentoWrapperDTO.builder()
                                .identificadorTransacao(IDENTIFICADOR_TRANSACAO)
                                .tipoProcessamentoEnum(TipoProcessamentoEnum.EXCLUSAO_PARCIAL)
                                .recorrenciaTransacao(recorrenciaTransacaoMock)
                                .templateNotificacao(RECORRENCIA_FALHA_OPERACIONAL)
                                .tipoProcessamentoErro(TipoProcessamentoWrapperDTO.TipoProcessamentoErro.builder()
                                        .tipoMotivoExclusao(TipoMotivoExclusao.SOLICITADO_SISTEMA)
                                        .codigoErro(CODIGO_ERRO)
                                        .mensagemErro(MENSAGEM_ERRO)
                                        .build())
                                .build());

                verify(consultaChaveDictClient).consultaChaveDict(recorrenciaMock.getRecebedor().getChave(), recorrenciaMock.getPagador().getCpfCnpj(), recorrenciaMock.getPagador().getAgencia(), recorrenciaMock.getTipoCanal().name(), channelDict.toString(), false);
            }
        }

        @Test
        void dadoErro5xxDictPassivelRetry_quandoConsultarTipoProcessamento_deveRetornarTipoProcessamentoIgnoradal() throws JsonProcessingException {
            try (var mockLocalTime = mockStatic(LocalTime.class)) {
                var recorrenciaMock = TestFactory.RecorrenciaTestFactory.criarRecorrencia(LocalDateTime.now());
                recorrenciaMock.setTipoCanal(TipoCanalEnum.MOBI);
                var recorrenciaTransacaoMock = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrenciaMock, LocalDate.now(), new BigDecimal(100));
                var channelDict = recorrenciaTransacaoMock.criarChannelData();

                when(appConfig.getRegras().getProcessamento().getHorarioLimiteLiquidacao()).thenReturn(DEZOITO_HORAS);
                when(objectMapper.writeValueAsString(any())).thenReturn(channelDict.toString());
                mockLocalTime.when(LocalTime::now).thenReturn(TREZE_HORAS);
                doThrow(new DictException(HttpStatus.INTERNAL_SERVER_ERROR, CODIGO_ERRO, MENSAGEM_ERRO)).when(consultaChaveDictClient).consultaChaveDict(recorrenciaMock.getRecebedor().getChave(), recorrenciaMock.getPagador().getCpfCnpj(), recorrenciaMock.getPagador().getAgencia(), recorrenciaMock.getTipoCanal().name(), channelDict.toString(), false);

                var responseTipoProcessamento = liquidacaoService.consultarTipoProcessamento(IDENTIFICADOR_TRANSACAO, recorrenciaTransacaoMock);

                assertThat(responseTipoProcessamento)
                        .usingRecursiveComparison()
                        .isEqualTo(TipoProcessamentoWrapperDTO.builder()
                                .identificadorTransacao(IDENTIFICADOR_TRANSACAO)
                                .tipoProcessamentoEnum(TipoProcessamentoEnum.IGNORADA)
                                .recorrenciaTransacao(recorrenciaTransacaoMock)
                                .tipoProcessamentoErro(TipoProcessamentoWrapperDTO.TipoProcessamentoErro.builder()
                                        .codigoErro(CODIGO_ERRO)
                                        .mensagemErro(MENSAGEM_ERRO)
                                        .build())
                                .templateNotificacao(null)
                                .build());

                verify(consultaChaveDictClient).consultaChaveDict(recorrenciaMock.getRecebedor().getChave(), recorrenciaMock.getPagador().getCpfCnpj(), recorrenciaMock.getPagador().getAgencia(), recorrenciaMock.getTipoCanal().name(), channelDict.toString(), false);
            }
        }

        @ParameterizedTest
        @CsvSource({
                "WEB_OPENBK, AGENDADO_RECORRENTE, NOVO_ID_FIM_A_FIM",
                "MOBI, AGENDADO_RECORRENTE, END_TO_END_BACEN"
        })
        void dadoDiferentesCenarios_quandoConsultarTipoProcessamento_deveManterOuAtualizarIdFimAFim(TipoCanalEnum tipoCanal, TipoRecorrencia tipoRecorrencia, String idFimAFimEsperado) throws JsonProcessingException {
            var recorrenciaMock = TestFactory.RecorrenciaTestFactory.criarRecorrencia(LocalDateTime.now());
            var recorrenciaTransacaoMock = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrenciaMock, LocalDate.now(), new BigDecimal(100));
            var channelDict = recorrenciaTransacaoMock.criarChannelData();

            recorrenciaMock.setTipoCanal(tipoCanal);
            recorrenciaMock.setTipoRecorrencia(tipoRecorrencia);
            recorrenciaTransacaoMock.setIdFimAFim("NOVO_ID_FIM_A_FIM");

            var dictConsultaMock = criarDictConsultaDTO(TipoChaveEnum.EMAIL, CHAVE_PIX, recorrenciaMock.getPagador().getCpfCnpj());
            dictConsultaMock.setEndToEndBacen("END_TO_END_BACEN");

            when(objectMapper.writeValueAsString(any())).thenReturn(channelDict.toString());
            when(consultaChaveDictClient.consultaChaveDict(any(), any(), any(), any(), any(), anyBoolean()))
                    .thenReturn(Optional.of(dictConsultaMock));

            var responseTipoProcessamento = liquidacaoService.consultarTipoProcessamento(IDENTIFICADOR_TRANSACAO, recorrenciaTransacaoMock);

            assertEquals(idFimAFimEsperado, responseTipoProcessamento.getRecorrenciaTransacao().getIdFimAFim());
            assertEquals(TipoProcessamentoEnum.LIQUIDACAO, responseTipoProcessamento.getTipoProcessamentoEnum());

            verify(consultaChaveDictClient).consultaChaveDict(any(), any(), any(), any(), any(), anyBoolean());
        }

        @Test
        void dadoTipoIniciacaoForDadosBancarios_quandoConsultarTipoProcessamento_deveAtualizarIdFimAFim() {
            var recorrenciaMock = TestFactory.RecorrenciaTestFactory.criarRecorrencia(LocalDateTime.now());
            recorrenciaMock.setTipoIniciacao(TipoPagamentoPixEnum.PIX_PAYMENT_MANUAL);
            var recorrenciaTransacaoMock = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrenciaMock, LocalDate.now(), new BigDecimal(100));

            recorrenciaTransacaoMock.setIdFimAFim(null);

            recorrenciaTransacaoMock.getRecorrencia().setTipoIniciacaoCanal(TipoIniciacaoCanal.DADOS_BANCARIOS);

            var responseTipoProcessamento = liquidacaoService.consultarTipoProcessamento(IDENTIFICADOR_TRANSACAO, recorrenciaTransacaoMock);

            assertEquals(TipoProcessamentoEnum.LIQUIDACAO, responseTipoProcessamento.getTipoProcessamentoEnum());
            assertNotNull(responseTipoProcessamento.getRecorrenciaTransacao().getIdFimAFim());
            verify(consultaChaveDictClient, never()).consultaChaveDict(any(), any(), any(), any(), any(), anyBoolean());
        }

        @ParameterizedTest
        @CsvSource({
                "WEB_OPENBK, AGENDADO_RECORRENTE",
                "MOBI, AGENDADO",
                "WEB_OPENBK, AGENDADO"
        })
        void dadoTipoIniciacaoForDadosBancarios_quandoConsultarTipoProcessamento_devePermanecerMesmoIdFimAFimTransacao(TipoCanalEnum tipoCanal, TipoRecorrencia tipoRecorrencia) {
            var recorrenciaMock = TestFactory.RecorrenciaTestFactory.criarRecorrencia(LocalDateTime.now());
            recorrenciaMock.setTipoIniciacao(TipoPagamentoPixEnum.PIX_PAYMENT_MANUAL);
            var recorrenciaTransacaoMock = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrenciaMock, LocalDate.now(), new BigDecimal(100));

            recorrenciaMock.setTipoCanal(tipoCanal);
            recorrenciaMock.setTipoRecorrencia(tipoRecorrencia);

            recorrenciaTransacaoMock.getRecorrencia().setTipoIniciacaoCanal(TipoIniciacaoCanal.DADOS_BANCARIOS);

            var responseTipoProcessamento = liquidacaoService.consultarTipoProcessamento(IDENTIFICADOR_TRANSACAO, recorrenciaTransacaoMock);

            assertEquals(TipoProcessamentoEnum.LIQUIDACAO, responseTipoProcessamento.getTipoProcessamentoEnum());
            assertEquals(recorrenciaTransacaoMock.getIdFimAFim(), responseTipoProcessamento.getRecorrenciaTransacao().getIdFimAFim());
            verify(consultaChaveDictClient, never()).consultaChaveDict(any(), any(), any(), any(), any(), anyBoolean());
        }

        public static DictConsultaDTO criarDictConsultaDTO(TipoChaveEnum tipoChave, String chave, String cpfCnpj) {
            var dictConsulta = new DictConsultaDTO();
            dictConsulta.setAgencia(SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Exemplos.AGENCIA);
            dictConsulta.setConta(SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Exemplos.CONTA);
            dictConsulta.setChave(chave);
            dictConsulta.setTipoChave(tipoChave.name());
            dictConsulta.setCpfCnpj(cpfCnpj);
            dictConsulta.setEndToEndBacen(ID_FIM_A_FIM);
            dictConsulta.setNome(SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Exemplos.NOME);
            dictConsulta.setTipoConta(TipoContaDictEnum.CORRENTE);
            dictConsulta.setTipoPessoa(TipoPessoaEnum.PJ);
            return dictConsulta;
        }

    }

}