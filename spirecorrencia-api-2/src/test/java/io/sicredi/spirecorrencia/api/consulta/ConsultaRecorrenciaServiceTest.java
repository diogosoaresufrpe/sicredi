package io.sicredi.spirecorrencia.api.consulta;

import br.com.sicredi.framework.web.spring.exception.NotFoundException;
import br.com.sicredi.framework.web.spring.exception.UnprocessableEntityException;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;
import io.sicredi.spirecorrencia.api.repositorio.Recorrencia;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaRepository;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacao;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacaoRepository;
import io.sicredi.spirecorrencia.api.testconfig.TestFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static br.com.sicredi.spicanais.transacional.transport.lib.config.SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaTransacao.Schemas.Exemplos.ID_PARCELA;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class ConsultaRecorrenciaServiceTest {

    @InjectMocks
    private ConsultaRecorrenciaService consultaRecorrenciaService;

    @Mock
    private RecorrenciaRepository recorrenciaRepository;

    @Mock
    private RecorrenciaTransacaoRepository recorrenciaTransacaoRepository;

    private static final String COOPERATIVA = "0101";
    private static final String CONTA = "000023";
    private static final Integer NUMERO_PAGINA_ZERO = 0;
    private static final Integer TAMANHO_PAGINA_DEZ = 10;


    @Nested
    class ConsultaTodas {

        @Test
        void dadoFiltros_quandoConsultarTodas_deveConsultarTodasNoBanco() {
            var primeiraRecorrenciaMock = TestFactory.RecorrenciaTestFactory.criarRecorrencia(LocalDateTime.now());
            primeiraRecorrenciaMock.setOidRecorrencia(1L);
            var primeiraParcelaDaPrimeiraRecorrenciaMock = criarMockParcela(primeiraRecorrenciaMock);
            var segundaParcelaDaPrimeiraRecorrenciaMock = criarMockParcela(primeiraRecorrenciaMock);

            var segundaRecorrenciaMock = TestFactory.RecorrenciaTestFactory.criarRecorrencia(LocalDateTime.now());
            segundaRecorrenciaMock.setOidRecorrencia(2L);
            var primeiraParcelaDaSegundaRecorrenciaMock = criarMockParcela(segundaRecorrenciaMock);

            var listaParcelas = List.of(primeiraParcelaDaPrimeiraRecorrenciaMock, segundaParcelaDaPrimeiraRecorrenciaMock, primeiraParcelaDaSegundaRecorrenciaMock);


            var pageable = PageRequest.of(
                    NUMERO_PAGINA_ZERO,
                    TAMANHO_PAGINA_DEZ,
                    Sort.by(Sort.Direction.ASC, "dataCriacao", "tipoStatus")
            );

            var request = ConsultaRecorrenciaRequest.builder()
                    .contaPagador(CONTA)
                    .agenciaPagador(COOPERATIVA)
                    .tamanhoPagina(TAMANHO_PAGINA_DEZ)
                    .numeroPagina(NUMERO_PAGINA_ZERO)
                    .build();

            when(recorrenciaRepository.findAllByFiltros(eq(request), Mockito.any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(primeiraRecorrenciaMock, segundaRecorrenciaMock), pageable, 2));

            when(recorrenciaTransacaoRepository.consultarPorRecorrenciaIn(List.of(1L, 2L)))
                    .thenReturn(listaParcelas);


            var response = consultaRecorrenciaService.consultarTodas(request);
            assertEquals(2, response.recorrencias().size());
            assertNotNull(response.recorrencias().getFirst().getDataProximoPagamento());
            assertNotNull(response.recorrencias().getLast().getDataProximoPagamento());

            verify(recorrenciaRepository, Mockito.times(1)).findAllByFiltros(eq(request), Mockito.any(Pageable.class));
            verify(recorrenciaTransacaoRepository, Mockito.times(1)).consultarPorRecorrenciaIn(List.of(1L, 2L));
        }

        private static RecorrenciaTransacao criarMockParcela(Recorrencia recorrenciaMock) {
            return TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(
                    recorrenciaMock,
                    LocalDate.now(),
                    BigDecimal.TEN
            );
        }
    }

    @Nested
    class ConsultaDetalhes {

        private static final String IDENTIFICADOR_RECORRENCIA= "2f335153-4d6a-4af1-92a0-c52c5c827af9";

        @Test
        void dadoIdentificadorTransacaoExistente_quandoConsultarDetalhes_deveRetornarRecorrencia() {
            var recorrenciaMock = TestFactory.RecorrenciaTestFactory.criarRecorrencia(LocalDateTime.now());
            recorrenciaMock.setIdRecorrencia(IDENTIFICADOR_RECORRENCIA);

            when(recorrenciaRepository.consultarPorIdentificadorRecorrencia(IDENTIFICADOR_RECORRENCIA))
                    .thenReturn(Optional.of(recorrenciaMock));

            var recorrenciaResponse = consultaRecorrenciaService.consultarDetalhes(IDENTIFICADOR_RECORRENCIA);

            assertEquals(IDENTIFICADOR_RECORRENCIA, recorrenciaResponse.getIdentificadorRecorrencia());

            verify(recorrenciaRepository).consultarPorIdentificadorRecorrencia(IDENTIFICADOR_RECORRENCIA);
        }

        @Test
        void dadoIdentificadorTransacaoInexistente_quandoConsultarDetalhes_deveLancarNotFoundException() {
            when(recorrenciaRepository.consultarPorIdentificadorRecorrencia(IDENTIFICADOR_RECORRENCIA))
                    .thenReturn(Optional.empty());

            assertThrows(NotFoundException.class, () -> consultaRecorrenciaService.consultarDetalhes(IDENTIFICADOR_RECORRENCIA));

            verify(recorrenciaRepository).consultarPorIdentificadorRecorrencia(IDENTIFICADOR_RECORRENCIA);
        }

    }

    @Nested
    class ConsultarParcelas {

        @Test
        void dadoFiltrosExistentes_quandoConsultarParcelas_deveConsultarParcelasNoBanco() {
            var recorrenciaMock = TestFactory.RecorrenciaTestFactory.criarRecorrencia(LocalDateTime.now());

            var parcelaMock1 = ParcelaRecorrenciaMock.builder()
                    .nomeRecebedor(recorrenciaMock.getRecebedor().getNome())
                    .identificadorParcela("123")
                    .status(TipoStatusEnum.CRIADO)
                    .dataTransacao(LocalDate.now())
                    .tipoRecorrencia(recorrenciaMock.getTipoRecorrencia())
                    .valor(BigDecimal.TEN)
                    .build();

            var parcelaMock2 = ParcelaRecorrenciaMock.builder()
                    .nomeRecebedor(recorrenciaMock.getRecebedor().getNome())
                    .identificadorParcela("456")
                    .status(TipoStatusEnum.CRIADO)
                    .dataTransacao(LocalDate.now().plusDays(1))
                    .tipoRecorrencia(recorrenciaMock.getTipoRecorrencia())
                    .valor(BigDecimal.valueOf(20))
                    .build();

            var pageable = PageRequest.of(
                    NUMERO_PAGINA_ZERO,
                    TAMANHO_PAGINA_DEZ
            );

            var request = ConsultaParcelasRecorrenciaRequest.builder()
                    .contaPagador(CONTA)
                    .agenciaPagador(COOPERATIVA)
                    .tamanhoPagina(TAMANHO_PAGINA_DEZ)
                    .numeroPagina(NUMERO_PAGINA_ZERO)
                    .build();

            List<ListagemParcelaRecorrenciaProjection> parcelaResponseMock = List.of(parcelaMock1, parcelaMock2);

            var pageParcelas = new PageImpl<>(parcelaResponseMock, pageable, 2);

            when(recorrenciaTransacaoRepository.findParcelasByFiltros(eq(request), Mockito.any(Pageable.class)))
                    .thenReturn(pageParcelas);

            var response = consultaRecorrenciaService.consultarParcelas(request);

            assertEquals(2, response.parcelas().size());
            assertEquals("123", response.parcelas().get(0).getIdentificadorParcela());
            assertEquals("456", response.parcelas().get(1).getIdentificadorParcela());

            verify(recorrenciaTransacaoRepository, Mockito.times(1)).findParcelasByFiltros(eq(request), eq(pageable));
        }

    }

    @Nested
    class ConsultarDetalhesParcelas {

        @Test
        void dadoFiltrosExistentes_quandoConsultarDetalhesParcelas_deveConsultarDetalhesParcelasNoBanco() {
            var recorrenciaMock = TestFactory.RecorrenciaTestFactory.criarRecorrencia(LocalDateTime.now());

            var parcelaMock1 = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrenciaMock, LocalDate.now(), BigDecimal.TEN);
            parcelaMock1.setIdParcela(ID_PARCELA);
            var parcelaMock2 = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrenciaMock, LocalDate.now().plusDays(1), BigDecimal.valueOf(20));
            parcelaMock2.setIdParcela("456");

            recorrenciaMock.setRecorrencias(List.of(parcelaMock1, parcelaMock2));

            when(recorrenciaRepository.findRecorrenciaByIdParcelaAndAgenciaAndConta(ID_PARCELA, recorrenciaMock.getPagador().getAgencia(), recorrenciaMock.getPagador().getConta())).thenReturn(Optional.of(recorrenciaMock));

            var response = consultaRecorrenciaService.consultarDetalhesParcelas(ID_PARCELA, recorrenciaMock.getPagador().getAgencia(), recorrenciaMock.getPagador().getConta());

            assertEquals(1, response.getParcela().getNumeroParcela());
            assertEquals(2, response.getNumeroTotalParcelas());
            assertEquals(ID_PARCELA, response.getParcela().getIdentificadorParcela());
            assertEquals(recorrenciaMock.getRecebedor().getNome(), response.getRecebedor().getNome());
            assertEquals(2, response.getNumeroTotalParcelas());

            verify(recorrenciaRepository, Mockito.times(1)).findRecorrenciaByIdParcelaAndAgenciaAndConta(ID_PARCELA, recorrenciaMock.getPagador().getAgencia(), recorrenciaMock.getPagador().getConta());
        }

        @Test
        void dadoRecorrenciaComIdParcelaNaoExistente_quandoConsultarDetalhesParcelas_deveConsultarDetalhesParcelasNoBanco() {
            var recorrenciaMock = TestFactory.RecorrenciaTestFactory.criarRecorrencia(LocalDateTime.now());

            var parcelaMock1 = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrenciaMock, LocalDate.now(), BigDecimal.TEN);
            parcelaMock1.setIdParcela(ID_PARCELA);
            var parcelaMock2 = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrenciaMock, LocalDate.now().plusDays(1), BigDecimal.valueOf(20));
            parcelaMock2.setIdParcela("456");

            recorrenciaMock.setRecorrencias(List.of(parcelaMock1, parcelaMock2));

            when(recorrenciaRepository.findRecorrenciaByIdParcelaAndAgenciaAndConta(ID_PARCELA, recorrenciaMock.getPagador().getAgencia(), recorrenciaMock.getPagador().getConta())).thenReturn(Optional.empty());

            UnprocessableEntityException exception = assertThrows(UnprocessableEntityException.class, () ->
                    consultaRecorrenciaService.consultarDetalhesParcelas(ID_PARCELA, recorrenciaMock.getPagador().getAgencia(), recorrenciaMock.getPagador().getConta())
            );

            assertNotNull(exception);
            assertEquals(AppExceptionCode.SPIRECORRENCIA_BU0025.getMessage(), exception.getMessage());
            verify(recorrenciaRepository, Mockito.times(1)).findRecorrenciaByIdParcelaAndAgenciaAndConta(ID_PARCELA, recorrenciaMock.getPagador().getAgencia(), recorrenciaMock.getPagador().getConta());
        }

    }

}
