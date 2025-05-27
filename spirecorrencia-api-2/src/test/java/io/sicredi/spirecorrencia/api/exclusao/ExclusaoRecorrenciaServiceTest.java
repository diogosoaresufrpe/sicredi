package io.sicredi.spirecorrencia.api.exclusao;

import br.com.sicredi.canaisdigitais.enums.TipoRetornoTransacaoEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoRecorrencia;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentRequest;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;
import io.sicredi.spirecorrencia.api.idempotente.*;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoInformacaoAdicional;
import io.sicredi.spirecorrencia.api.repositorio.Recorrencia;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaRepository;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacao;
import jakarta.validation.Validator;
import org.hibernate.validator.internal.engine.ConstraintViolationImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static io.sicredi.spirecorrencia.api.utils.ConstantesTest.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExclusaoRecorrenciaServiceTest {

    @InjectMocks
    private ExclusaoService exclusaoService;

    @Mock
    private AppConfig appConfig;
    @Mock
    private RecorrenciaRepository recorrenciaRepository;
    @Mock
    private Validator validator;
    @Mock
    private IdempotentRequest<ExclusaoRequisicaoDTO> idempotentRequest;
    @Mock
    private CriaResponseStrategyFactory<ExclusaoRequisicaoDTO> criaResponseStrategyFactory;
    @Mock
    private CriaResponseStrategy<ExclusaoRequisicaoDTO> criaResponseStrategy;
    @Mock
    private EventoResponseFactory eventoResponseFactory;
    @Captor
    private ArgumentCaptor<ErroDTO> captorErroDTO;
    @Captor
    private ArgumentCaptor<Recorrencia> captorRecorrencia;
    @Captor
    private ArgumentCaptor<NotificacaoDTO> captureNotificacaoDTO;

    @Nested
    class ProcessarProtocolo {

        @Test
        @DisplayName("Deve excluir recorrência com sucesso quando status for CRIADO")
        void dadoRecorrenciaValidaComStatusCriado_quandoProcessarProtocolo_deveExecutarComSucesso() {
            var recorrencia = TestExclusaoFactory.RecorrenciaTest.criarRecorrencia(LocalDateTime.now());
            var exclusaoRecorrenciaRequestDTO = TestExclusaoFactory.ExcluirRecorrenciaTest.criarExcluirRecorrenciaTransacao(recorrencia.getIdRecorrencia(), List.of(ID_PARCELA_1));
            var excluirParcela = criarRecorrencias(ID_PARCELA_1, TipoStatusEnum.CRIADO, ID_RECORRENCIA_1, LocalDate.now().plusMonths(1));

            recorrencia.setRecorrencias(List.of(excluirParcela,
                    criarRecorrencias(ID_PARCELA_2, TipoStatusEnum.PENDENTE, ID_RECORRENCIA_2, LocalDate.now().plusMonths(2)),
                    criarRecorrencias(ID_PARCELA_3, TipoStatusEnum.CRIADO, ID_RECORRENCIA_3, LocalDate.now().plusMonths(3))));

            var regras = mock(AppConfig.Regras.class);

            mockRecorrenciaRegrasEIdempotent(recorrencia, regras, exclusaoRecorrenciaRequestDTO);

            exclusaoService.processarProtocolo(idempotentRequest);

            verify(appConfig, times(1)).getRegras();
            verify(appConfig.getRegras(), times(1)).getExclusaoHorarioLimite();
            verify(validator, times(1)).validate(exclusaoRecorrenciaRequestDTO);

            verify(criaResponseStrategy, times(1)).criarResponseIdempotentSucesso(any(ExclusaoRequisicaoDTO.class), any(), any(), any());
            verify(recorrenciaRepository, times(1)).findByIdRecorrencia(exclusaoRecorrenciaRequestDTO.getIdentificadorRecorrencia());
            verify(recorrenciaRepository, times(1)).save(captorRecorrencia.capture());
            verify(criaResponseStrategy, never()).criarResponseIdempotentErro(any(), any(), any());
            verify(criaResponseStrategy, never()).criarResponseIdempotentErro(any(), any(), any());

            var retornoRecorrencia = captorRecorrencia.getValue();
            assertNotNull(retornoRecorrencia);
            assertEquals(TipoStatusEnum.CRIADO, retornoRecorrencia.getTipoStatus());
        }

        @Test
        @DisplayName("Quando o fluxo de liquidação estiver ativo, deve excluir recorrência com sucesso quando fluxo de liquidação for ativo e status for CRIADO. Não deve gerar noificação")
        void dadoRecorrenciaValidaComFluxoLiquidacaoAtivo_quandoProcessarProtocolo_deveExecutarComSucesso() {
            var recorrencia = TestExclusaoFactory.RecorrenciaTest.criarRecorrencia(LocalDateTime.now());
            var exclusaoRecorrenciaRequestDTO = TestExclusaoFactory.ExcluirRecorrenciaTest.criarExcluirRecorrenciaTransacao(recorrencia.getIdRecorrencia(), List.of(ID_PARCELA_1));
            var excluirParcela = criarRecorrencias(ID_PARCELA_1, TipoStatusEnum.CRIADO, ID_RECORRENCIA_1, LocalDate.now().plusMonths(1));

            exclusaoRecorrenciaRequestDTO.setFluxoLiquidacao(true);

            recorrencia.setRecorrencias(List.of(excluirParcela,
                    criarRecorrencias(ID_PARCELA_2, TipoStatusEnum.PENDENTE, ID_RECORRENCIA_2, LocalDate.now().plusMonths(2)),
                    criarRecorrencias(ID_PARCELA_3, TipoStatusEnum.CRIADO, ID_RECORRENCIA_3, LocalDate.now().plusMonths(3))));

            when(recorrenciaRepository.findByIdRecorrencia(any())).thenReturn(Optional.of(recorrencia));
            when(idempotentRequest.getValue()).thenReturn(exclusaoRecorrenciaRequestDTO);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);
            when(recorrenciaRepository.save(any())).thenReturn(recorrencia);

            exclusaoService.processarProtocolo(idempotentRequest);

            verify(criaResponseStrategy, times(1)).criarResponseIdempotentSucesso(any(ExclusaoRequisicaoDTO.class), any(), any(), any());
            verify(recorrenciaRepository, times(1)).findByIdRecorrencia(exclusaoRecorrenciaRequestDTO.getIdentificadorRecorrencia());
            verify(validator, times(1)).validate(exclusaoRecorrenciaRequestDTO);
            verify(recorrenciaRepository, times(1)).findByIdRecorrencia(exclusaoRecorrenciaRequestDTO.getIdentificadorRecorrencia());
            verify(recorrenciaRepository, times(1)).save(captorRecorrencia.capture());
            verify(criaResponseStrategy, never()).criarResponseIdempotentErro(any(), any(), any());
            verify(criaResponseStrategy, never()).criarResponseIdempotentErro(any(), any(), any());

            var retornoRecorrencia = captorRecorrencia.getValue();

            assertNotNull(retornoRecorrencia);
            assertEquals(TipoStatusEnum.CRIADO, retornoRecorrencia.getTipoStatus());

            verify(eventoResponseFactory, never()).criarEventoNotificacao(any());
        }


        @Test
        @DisplayName("Deve lançar exceção se a data e hora atual forem anteriores à data de liquidação da parcela")
        void dadoRecorrenciaEDataHoraForAnteriorLimiteDeCancelamento_quandoProcessarProtocolo_deveLancarExcecao() {
            var recorrencia = TestExclusaoFactory.RecorrenciaTest.criarRecorrencia(LocalDateTime.now());
            var exclusaoRecorrenciaRequestDTO = TestExclusaoFactory.ExcluirRecorrenciaTest.criarExcluirRecorrenciaTransacao(recorrencia.getIdRecorrencia(), List.of(ID_PARCELA_1));
            var excluirParcela = criarRecorrencias(ID_PARCELA_1, TipoStatusEnum.CRIADO, ID_RECORRENCIA_1, LocalDate.now().minusDays(1));

            recorrencia.setRecorrencias(List.of(excluirParcela,
                    criarRecorrencias(ID_PARCELA_2, TipoStatusEnum.PENDENTE, ID_RECORRENCIA_2, LocalDate.now().plusMonths(2)),
                    criarRecorrencias(ID_PARCELA_3, TipoStatusEnum.CRIADO, ID_RECORRENCIA_3, LocalDate.now().plusMonths(3))));
            var regras = mock(AppConfig.Regras.class);

            mockRecorrenciaRegrasEIdempotent(recorrencia, regras, exclusaoRecorrenciaRequestDTO);

            assertDoesNotThrow(() ->
                    exclusaoService.processarProtocolo(idempotentRequest)
            );

            verify(criaResponseStrategy, times(1)).criarResponseIdempotentErro(any(), any(), captorErroDTO.capture());
            verify(criaResponseStrategy, never()).criarResponseIdempotentSucesso(any(ExclusaoRequisicaoDTO.class), any(), any(), any());
            verify(recorrenciaRepository, never()).save(any());

            var retornoErro = captorErroDTO.getValue();

            assertNotNull(retornoErro);
            assertEquals(AppExceptionCode.SPIRECORRENCIA_BU0026, retornoErro.codigoErro());
            assertEquals(AppExceptionCode.SPIRECORRENCIA_BU0026.getMessage(), retornoErro.mensagemErro());
            assertEquals(TipoRetornoTransacaoEnum.ERRO_NEGOCIO, retornoErro.tipoRetornoTransacaoEnum());
        }

        @Test
        @DisplayName("Deve lançar exceção quando recorrência não existir")
        void dadoRecorrenciaInexistente_quandoProcessarProtocolo_deveRetornarExcecao() {
            var exclusaoRecorrenciaRequestDTO = TestExclusaoFactory.ExcluirRecorrenciaTest.criarExcluirRecorrenciaTransacao(UUID.randomUUID().toString(), List.of(ID_PARCELA_1));

            when(recorrenciaRepository.findByIdRecorrencia(any())).thenReturn(Optional.empty());
            when(idempotentRequest.getValue()).thenReturn(exclusaoRecorrenciaRequestDTO);

            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO))
                    .thenReturn(criaResponseStrategy);

            assertDoesNotThrow(() -> exclusaoService.processarProtocolo(idempotentRequest));

            verify(criaResponseStrategy, times(1)).criarResponseIdempotentErro(any(ExclusaoRequisicaoDTO.class), any(), captorErroDTO.capture());
            verify(criaResponseStrategy, never()).criarResponseIdempotentSucesso(any(ExclusaoRequisicaoDTO.class), any(), any(), any());

            verify(recorrenciaRepository, never()).save(any());
            var retornoErro = captorErroDTO.getValue();
            assertNotNull(retornoErro);
            assertEquals(AppExceptionCode.SPIRECORRENCIA_BU0010, retornoErro.codigoErro());
            assertEquals(AppExceptionCode.SPIRECORRENCIA_BU0010.getMessage(), retornoErro.mensagemErro());
            assertEquals(TipoRetornoTransacaoEnum.ERRO_NEGOCIO, retornoErro.tipoRetornoTransacaoEnum());
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = TipoStatusEnum.class, mode = EnumSource.Mode.EXCLUDE, names = {"CRIADO"})
        @DisplayName("Deve lançar exceção quando status da recorrência for diferente de CRIADO")
        void dadoRecorrenciaComStatusDiferenteDeCriado_quandoProcessarProtocolo_deveRetornarExcecao(TipoStatusEnum tipoStatus) {
            var recorrencia = TestExclusaoFactory.RecorrenciaTest.criarRecorrencia(LocalDateTime.now());
            recorrencia.setTipoStatus(tipoStatus);
            var exclusaoRecorrenciaRequestDTO = TestExclusaoFactory.ExcluirRecorrenciaTest.criarExcluirRecorrenciaTransacao(recorrencia.getIdRecorrencia(), List.of(ID_PARCELA_1));
            var excluirParcela = criarRecorrencias(ID_PARCELA_1, TipoStatusEnum.CRIADO, ID_RECORRENCIA_1, LocalDate.now().plusMonths(1));

            recorrencia.setRecorrencias(List.of(excluirParcela,
                    criarRecorrencias(ID_PARCELA_2, TipoStatusEnum.PENDENTE, ID_RECORRENCIA_2, LocalDate.now().plusMonths(2)),
                    criarRecorrencias(ID_PARCELA_3, TipoStatusEnum.CRIADO, ID_RECORRENCIA_3, LocalDate.now().plusMonths(3))));

            when(recorrenciaRepository.findByIdRecorrencia(any())).thenReturn(Optional.of(recorrencia));
            when(idempotentRequest.getValue()).thenReturn(exclusaoRecorrenciaRequestDTO);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO))
                    .thenReturn(criaResponseStrategy);

            assertDoesNotThrow(() ->
                    exclusaoService.processarProtocolo(idempotentRequest)
            );

            verify(criaResponseStrategy, times(1)).criarResponseIdempotentErro(any(), any(), captorErroDTO.capture());
            verify(criaResponseStrategy, never()).criarResponseIdempotentSucesso(any(ExclusaoRequisicaoDTO.class), any(), any(), any());
            verify(recorrenciaRepository, never()).save(any());

            var retornoErro = captorErroDTO.getValue();

            assertNotNull(retornoErro);
            assertEquals(AppExceptionCode.SPIRECORRENCIA_BU0008, retornoErro.codigoErro());
            assertEquals(AppExceptionCode.SPIRECORRENCIA_BU0008.getMessage(), retornoErro.mensagemErro());
            assertEquals(TipoRetornoTransacaoEnum.ERRO_NEGOCIO, retornoErro.tipoRetornoTransacaoEnum());
        }

        @Test
        @DisplayName("Deve lançar exceção quando parcela não pertence à recorrência")
        void dadoParcelaNaoPertencenteARecorrencia_quandoProcessarProtocolo_deveRetornarExcecao() {
            var recorrencia = TestExclusaoFactory.RecorrenciaTest.criarRecorrencia(LocalDateTime.now());
            var exclusaoRecorrenciaRequestDTO = TestExclusaoFactory.ExcluirRecorrenciaTest.criarExcluirRecorrenciaTransacao(recorrencia.getIdRecorrencia(), List.of(ID_PARCELA_1));

            recorrencia.setRecorrencias(List.of(criarRecorrencias(ID_PARCELA_2, TipoStatusEnum.PENDENTE, ID_RECORRENCIA_2, LocalDate.now().plusMonths(2)),
                    criarRecorrencias(ID_PARCELA_3, TipoStatusEnum.CRIADO, ID_RECORRENCIA_3, LocalDate.now().plusMonths(3))));

            var mensagemEsperada = MessageFormat.format(AppExceptionCode.SPIRECORRENCIA_BU0007.getMessage(), 0, ID_PARCELA_1);

            when(recorrenciaRepository.findByIdRecorrencia(any())).thenReturn(Optional.of(recorrencia));
            when(idempotentRequest.getValue()).thenReturn(exclusaoRecorrenciaRequestDTO);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO))
                    .thenReturn(criaResponseStrategy);

            assertDoesNotThrow(() ->
                    exclusaoService.processarProtocolo(idempotentRequest)
            );

            verify(criaResponseStrategy, times(1)).criarResponseIdempotentErro(any(), any(), captorErroDTO.capture());
            verify(criaResponseStrategy, never()).criarResponseIdempotentSucesso(any(ExclusaoRequisicaoDTO.class), any(), any(), any());

            verify(recorrenciaRepository, never()).save(any());
            var retornoErro = captorErroDTO.getValue();
            assertNotNull(retornoErro);
            assertEquals(AppExceptionCode.SPIRECORRENCIA_BU0007, retornoErro.codigoErro());
            assertEquals(mensagemEsperada, retornoErro.mensagemErro());
            assertEquals(TipoRetornoTransacaoEnum.ERRO_NEGOCIO, retornoErro.tipoRetornoTransacaoEnum());
        }

        @Test
        @DisplayName("Deve lançar exceção quando parcela tem status diferente de CRIADO ou PENDENTE")
        void dadoParcelaComStatusDiferenteDeCriadoOuPendente_quandoProcessarProtocolo_deveRetornarExcecao() {
            var recorrencia = TestExclusaoFactory.RecorrenciaTest.criarRecorrencia(LocalDateTime.now());
            var exclusaoRecorrenciaRequestDTO = TestExclusaoFactory.ExcluirRecorrenciaTest.criarExcluirRecorrenciaTransacao(recorrencia.getIdRecorrencia(), List.of(ID_PARCELA_1));

            var excluirParcela = criarRecorrencias(ID_PARCELA_1, TipoStatusEnum.CONCLUIDO, ID_RECORRENCIA_1, LocalDate.now().plusMonths(1));
            recorrencia.setRecorrencias(List.of(excluirParcela,
                    criarRecorrencias(ID_PARCELA_2, TipoStatusEnum.PENDENTE, ID_RECORRENCIA_2, LocalDate.now().plusMonths(2)),
                    criarRecorrencias(ID_PARCELA_3, TipoStatusEnum.CRIADO, ID_RECORRENCIA_3, LocalDate.now().plusMonths(3))));

            when(recorrenciaRepository.findByIdRecorrencia(any())).thenReturn(Optional.of(recorrencia));
            when(idempotentRequest.getValue()).thenReturn(exclusaoRecorrenciaRequestDTO);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO))
                    .thenReturn(criaResponseStrategy);

            var mensagemEsperada = MessageFormat.format(AppExceptionCode.SPIRECORRENCIA_BU0006.getMessage(), excluirParcela.getTpoStatus(), 0, excluirParcela.getIdParcela());

            assertDoesNotThrow(() ->
                    exclusaoService.processarProtocolo(idempotentRequest)
            );

            verify(criaResponseStrategy, times(1)).criarResponseIdempotentErro(any(), any(), captorErroDTO.capture());
            verify(criaResponseStrategy, never()).criarResponseIdempotentSucesso(any(ExclusaoRequisicaoDTO.class), any(), any(), any());
            verify(recorrenciaRepository, never()).save(any());

            var retornoErro = captorErroDTO.getValue();

            assertNotNull(retornoErro);
            assertEquals(AppExceptionCode.SPIRECORRENCIA_BU0006, retornoErro.codigoErro());
            assertEquals(mensagemEsperada, retornoErro.mensagemErro());
            assertEquals(TipoRetornoTransacaoEnum.ERRO_NEGOCIO, retornoErro.tipoRetornoTransacaoEnum());
        }

        @Test
        @DisplayName("Deve lançar exceção quando uma constraint for violada")
        void dadoViolacaoDeConstraints_quandoProcessarProtocolo_deveRetornarExcecao() {
            var exclusaoRecorrenciaRequestDTO = TestExclusaoFactory.ExcluirRecorrenciaTest.criarExcluirRecorrenciaTransacao(UUID.randomUUID().toString(), List.of(ID_PARCELA_1, ID_PARCELA_2, ID_PARCELA_3));
            var mensagemErro = "Teste de erro de validação de constraints";
            var mockConstraintViolation = Mockito.mock(ConstraintViolationImpl.class);

            when(idempotentRequest.getValue()).thenReturn(exclusaoRecorrenciaRequestDTO);
            when(validator.validate(any())).thenReturn(Set.of(mockConstraintViolation));
            when(mockConstraintViolation.getMessage()).thenReturn(mensagemErro);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO))
                    .thenReturn(criaResponseStrategy);

            assertDoesNotThrow(() ->
                    exclusaoService.processarProtocolo(idempotentRequest)
            );

            verify(criaResponseStrategy, times(1)).criarResponseIdempotentErro(any(), any(), captorErroDTO.capture());
            verify(criaResponseStrategy, never()).criarResponseIdempotentSucesso(any(ExclusaoRequisicaoDTO.class), any(), any(), any());
            verify(recorrenciaRepository, never()).save(any());

            var retornoErro = captorErroDTO.getValue();

            assertNotNull(retornoErro);
            assertEquals(AppExceptionCode.SPIRECORRENCIA_REC0001, retornoErro.codigoErro());
            assertEquals("Algum dado do payload de cadastro está inválido. Erro -> %s" .formatted(mensagemErro), retornoErro.mensagemErro());
            assertEquals(TipoRetornoTransacaoEnum.ERRO_VALIDACAO, retornoErro.tipoRetornoTransacaoEnum());
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = TipoStatusEnum.class, mode = EnumSource.Mode.INCLUDE, names = {"CRIADO", "PENDENTE"})
        @DisplayName("Deve manter status CRIADO quando há parcelas restantes com status CRIADO ou PENDENTE")
        void dadoRecorrenciaComParcelasRestantesCriadasOuPendentes_quandoProcessarProtocolo_deveManterStatusCriado(TipoStatusEnum tipoStatus) {
            var recorrencia = TestExclusaoFactory.RecorrenciaTest.criarRecorrencia(LocalDateTime.now());
            var exclusaoRecorrenciaRequestDTO = TestExclusaoFactory.ExcluirRecorrenciaTest.criarExcluirRecorrenciaTransacao(recorrencia.getIdRecorrencia(), List.of(ID_PARCELA_1));
            var excluirParcela = criarRecorrencias(ID_PARCELA_1, TipoStatusEnum.CRIADO, ID_RECORRENCIA_1, LocalDate.now().plusMonths(1));

            recorrencia.setRecorrencias(List.of(criarRecorrencias(ID_PARCELA_2, TipoStatusEnum.CONCLUIDO, ID_RECORRENCIA_3, LocalDate.now().minusMonths(1)),
                    excluirParcela,
                    criarRecorrencias(ID_PARCELA_3, tipoStatus, ID_RECORRENCIA_2, LocalDate.now().plusMonths(2))));

            var regras = mock(AppConfig.Regras.class);
            mockRecorrenciaRegrasEIdempotent(recorrencia, regras, exclusaoRecorrenciaRequestDTO);

            assertDoesNotThrow(() ->
                    exclusaoService.processarProtocolo(idempotentRequest)
            );

            verify(recorrenciaRepository, times(1)).save(captorRecorrencia.capture());
            var retornoRecorrencia = captorRecorrencia.getValue();
            assertNotNull(retornoRecorrencia);
            assertEquals(TipoStatusEnum.CRIADO, retornoRecorrencia.getTipoStatus());
        }

        @Test
        @DisplayName("Deve atualizar status da recorrência para CONCLUIDO quando todas as parcelas restantes estão EXCLUIDAS, exceto uma ou mais CONCLUIDA")
        void dadoRecorrenciaComParcelasRestantes_quandoProcessarProtocolo_deveAtualizarStatusParaConcluido() {
            var recorrencia = TestExclusaoFactory.RecorrenciaTest.criarRecorrencia(LocalDateTime.now());
            var exclusaoRecorrenciaRequestDTO = TestExclusaoFactory.ExcluirRecorrenciaTest.criarExcluirRecorrenciaTransacao(recorrencia.getIdRecorrencia(), List.of(ID_PARCELA_1));
            var excluirParcela = criarRecorrencias(ID_PARCELA_1, TipoStatusEnum.CRIADO, ID_RECORRENCIA_1, LocalDate.now().plusMonths(1));
            recorrencia.setRecorrencias(List.of(excluirParcela,
                    criarRecorrencias(ID_PARCELA_2, TipoStatusEnum.CONCLUIDO, ID_RECORRENCIA_2, LocalDate.now().plusMonths(2)),
                    criarRecorrencias(ID_PARCELA_3, TipoStatusEnum.CONCLUIDO, ID_RECORRENCIA_3, LocalDate.now().plusMonths(3))));
            var regras = mock(AppConfig.Regras.class);

            mockRecorrenciaRegrasEIdempotent(recorrencia, regras, exclusaoRecorrenciaRequestDTO);

            when(recorrenciaRepository.save(any())).thenReturn(recorrencia);

            exclusaoService.processarProtocolo(idempotentRequest);

            verify(recorrenciaRepository, times(1)).save(captorRecorrencia.capture());
            var retornoRecorrencia = captorRecorrencia.getValue();
            assertNotNull(retornoRecorrencia);
            assertEquals(TipoStatusEnum.CONCLUIDO, retornoRecorrencia.getTipoStatus());
        }

        @Test
        @DisplayName("Quando o fluxo de liquidação estiver ativo, deve gerar notificação e atualizar status da recorrência para CONCLUIDO quando todas as parcelas restantes estão EXCLUIDAS, exceto uma ou mais CONCLUIDA.")
        void dadoRecorrenciaComParcelasRestantesParaLiquidacao_quandoProcessarProtocolo_deveAtualizarStatusParaConcluido() {
            var recorrencia = TestExclusaoFactory.RecorrenciaTest.criarRecorrencia(LocalDateTime.now());

            recorrencia.setTipoRecorrencia(TipoRecorrencia.AGENDADO_RECORRENTE);

            var exclusaoRecorrenciaRequestDTO = TestExclusaoFactory.ExcluirRecorrenciaTest.criarExcluirRecorrenciaTransacao(recorrencia.getIdRecorrencia(), List.of(ID_PARCELA_1));
            var excluirParcela = criarRecorrencias(ID_PARCELA_1, TipoStatusEnum.CRIADO, ID_RECORRENCIA_1, LocalDate.now().plusMonths(1));

            exclusaoRecorrenciaRequestDTO.setFluxoLiquidacao(true);

            recorrencia.setRecorrencias(List.of(excluirParcela,
                    criarRecorrencias(ID_PARCELA_2, TipoStatusEnum.CONCLUIDO, ID_RECORRENCIA_2, LocalDate.now().plusMonths(2)),
                    criarRecorrencias(ID_PARCELA_3, TipoStatusEnum.CONCLUIDO, ID_RECORRENCIA_3, LocalDate.now().plusMonths(3))));

            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);
            when(recorrenciaRepository.findByIdRecorrencia(any())).thenReturn(Optional.of(recorrencia));
            when(idempotentRequest.getValue()).thenReturn(exclusaoRecorrenciaRequestDTO);
            when(recorrenciaRepository.save(any())).thenReturn(recorrencia);

            assertDoesNotThrow(() ->
                    exclusaoService.processarProtocolo(idempotentRequest)
            );

            verify(recorrenciaRepository, times(1)).save(captorRecorrencia.capture());
            verify(eventoResponseFactory).criarEventoNotificacao(captureNotificacaoDTO.capture());
            verify(criaResponseStrategy, never()).criarResponseIdempotentErro(any(), any(), any());
            verify(criaResponseStrategy, never()).criarResponseIdempotentErro(any(), any(), any());

            var valueNotificacao = captureNotificacaoDTO.getValue();
            var retornoRecorrencia = captorRecorrencia.getValue();
            assertNotNull(retornoRecorrencia);
            assertEquals(TipoStatusEnum.CONCLUIDO, retornoRecorrencia.getTipoStatus());
            assertAll(
                    () -> assertEquals(recorrencia.getPagador().getAgencia(), valueNotificacao.getAgencia()),
                    () -> assertEquals(recorrencia.getPagador().getConta(), valueNotificacao.getConta()),
                    () -> assertEquals(recorrencia.getRecebedor().getChave(), valueNotificacao.getChave()),
                    () -> assertEquals(recorrencia.getRecebedor().getTipoChave().name(), valueNotificacao.getTipoChave()),
                    () -> assertEquals(NotificacaoDTO.TipoTemplate.RECORRENCIA_SUCESSO_FINALIZACAO, valueNotificacao.getOperacao()),
                    () -> assertEquals(recorrencia.getTipoCanal().getTipoCanalPix().name(), valueNotificacao.getCanal()),
                    () -> assertEquals(2, valueNotificacao.getInformacoesAdicionais().size()),
                    () -> assertEquals(recorrencia.getNome(), valueNotificacao.getInformacoesAdicionais().get(NotificacaoInformacaoAdicional.NOME_RECORRENCIA.getNomeVariavel())),
                    () -> assertEquals(recorrencia.getPagador().getCpfCnpj(), valueNotificacao.getInformacoesAdicionais().get(NotificacaoInformacaoAdicional.DOCUMENTO_PAGADOR.getNomeVariavel())));
        }

        @Test
        @DisplayName("Quando o fluxo de liquidação estiver ativo, não deve gerar notificação e atualizar status da recorrência quando tipoRecorrencia for AGENDADO.")
        void dadoRecorrenciaSemParcelasRestantes_quandoProcessarProtocolo_deveNaoEnviarNotificacao() {
            var excluirParcela = criarRecorrencias(ID_PARCELA_1, TipoStatusEnum.CRIADO, ID_RECORRENCIA_1, LocalDate.now().plusMonths(1));
            var recorrencia = TestExclusaoFactory.RecorrenciaTest.criarRecorrencia(LocalDateTime.now());
            var exclusaoRecorrenciaRequestDTO = TestExclusaoFactory.ExcluirRecorrenciaTest.criarExcluirRecorrenciaTransacao(recorrencia.getIdRecorrencia(), List.of(ID_PARCELA_1));

            recorrencia.setRecorrencias(List.of(excluirParcela,
                    criarRecorrencias(ID_PARCELA_2, TipoStatusEnum.CONCLUIDO, ID_RECORRENCIA_2, LocalDate.now().plusMonths(2)),
                    criarRecorrencias(ID_PARCELA_3, TipoStatusEnum.CONCLUIDO, ID_RECORRENCIA_3, LocalDate.now().plusMonths(3))));

            exclusaoRecorrenciaRequestDTO.setFluxoLiquidacao(true);

            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)).thenReturn(criaResponseStrategy);
            when(recorrenciaRepository.findByIdRecorrencia(any())).thenReturn(Optional.of(recorrencia));
            when(recorrenciaRepository.save(any())).thenReturn(recorrencia);
            when(idempotentRequest.getValue()).thenReturn(exclusaoRecorrenciaRequestDTO);

            assertDoesNotThrow(() ->
                    exclusaoService.processarProtocolo(idempotentRequest)
            );

            verify(criaResponseStrategy, never()).criarResponseIdempotentErro(any(), any(), any());
            verify(criaResponseStrategy, never()).criarResponseIdempotentErro(any(), any(), any());
            verify(recorrenciaRepository, times(1)).save(captorRecorrencia.capture());

            verify(eventoResponseFactory, never()).criarEventoNotificacao(any());

            var retornoRecorrencia = captorRecorrencia.getValue();

            assertNotNull(retornoRecorrencia);
            assertEquals(TipoStatusEnum.CONCLUIDO, retornoRecorrencia.getTipoStatus());
        }

        @Test
        @DisplayName("Deve atualizar status da recorrência para EXCLUIDO quando todas as parcelas restantes estão EXCLUIDAS")
        void dadoRecorrenciaComParcelasRestantes_quandoProcessarProtocolo_deveAtualizarStatusParaExcluido() {
            var recorrencia = TestExclusaoFactory.RecorrenciaTest.criarRecorrencia(LocalDateTime.now());
            var exclusaoRecorrenciaRequestDTO = TestExclusaoFactory.ExcluirRecorrenciaTest.criarExcluirRecorrenciaTransacao(recorrencia.getIdRecorrencia(), List.of(ID_PARCELA_1));

            var excluirParcela = criarRecorrencias(ID_PARCELA_1, TipoStatusEnum.CRIADO, ID_RECORRENCIA_1, LocalDate.now().plusMonths(1));

            recorrencia.setRecorrencias(List.of(excluirParcela,
                    criarRecorrencias(ID_PARCELA_2, TipoStatusEnum.EXCLUIDO, ID_RECORRENCIA_2, LocalDate.now().plusMonths(2)),
                    criarRecorrencias(ID_PARCELA_3, TipoStatusEnum.EXCLUIDO, ID_RECORRENCIA_3, LocalDate.now().plusMonths(3))));

            var regras = mock(AppConfig.Regras.class);

            mockRecorrenciaRegrasEIdempotent(recorrencia, regras, exclusaoRecorrenciaRequestDTO);

            when(recorrenciaRepository.save(any())).thenReturn(recorrencia);

            assertDoesNotThrow(() ->
                    exclusaoService.processarProtocolo(idempotentRequest)
            );

            verify(recorrenciaRepository, times(1)).save(captorRecorrencia.capture());

            var retornoRecorrencia = captorRecorrencia.getValue();
            assertNotNull(retornoRecorrencia);
            assertEquals(TipoStatusEnum.EXCLUIDO, retornoRecorrencia.getTipoStatus());
        }

        private void mockRecorrenciaRegrasEIdempotent(Recorrencia recorrencia, AppConfig.Regras regras, ExclusaoRequisicaoDTO exclusaoRequisicaoDTO) {
            when(recorrenciaRepository.findByIdRecorrencia(any())).thenReturn(Optional.of(recorrencia));
            when(regras.getExclusaoHorarioLimite()).thenReturn(LocalTime.of(23, 59, 59));
            when(appConfig.getRegras()).thenReturn(regras);
            when(idempotentRequest.getValue()).thenReturn(exclusaoRequisicaoDTO);
            when(criaResponseStrategyFactory.criar(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO))
                    .thenReturn(criaResponseStrategy);
        }

        private RecorrenciaTransacao criarRecorrencias(String idParcela, TipoStatusEnum tipoStatus, Long idRecorrencia, LocalDate dataTransacao) {
            return RecorrenciaTransacao.builder()
                    .idParcela(idParcela)
                    .tpoStatus(tipoStatus)
                    .oidRecorrenciaTransacao(idRecorrencia)
                    .dataTransacao(dataTransacao)
                    .build();
        }
    }
}
