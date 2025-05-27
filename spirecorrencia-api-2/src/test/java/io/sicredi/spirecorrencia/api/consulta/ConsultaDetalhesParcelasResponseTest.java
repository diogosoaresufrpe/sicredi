package io.sicredi.spirecorrencia.api.consulta;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.*;
import br.com.sicredi.spicanais.transacional.transport.lib.config.SpiCanaisTransportContantes;
import io.sicredi.spirecorrencia.api.commons.PagadorResponse;
import io.sicredi.spirecorrencia.api.commons.RecebedorResponse;
import io.sicredi.spirecorrencia.api.testconfig.TestFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static java.math.BigDecimal.TEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConsultaDetalhesParcelasResponseTest {

    private static final String ID_PARCELA = "17b1caba-a6ab-4d25-ad8a-2b30bc17218a";
    private static final LocalDateTime DATA_CRIACAO = LocalDateTime.now().minusDays(1);
    private static final LocalDate DATA_PRIMEIRA_PARCELA = LocalDate.now();
    private static final LocalDate DATA_SEGUNDA_PARCELA = LocalDate.now().plusMonths(1);
    private static final LocalDate DATA_TERCEIRA_PARCELA = LocalDate.now().plusMonths(2);

    @Test
    void dadoRecorrencia_quandoFromListagemRecorrencia_deveRetornarRecorrencia() {
        var recorrencia = TestFactory.RecorrenciaTestFactory.criarRecorrencia(DATA_CRIACAO);

        var parcelaMock1 = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrencia, DATA_PRIMEIRA_PARCELA, BigDecimal.valueOf(20));
        parcelaMock1.setIdParcela("4526");
        var parcelaMock2 = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrencia, DATA_SEGUNDA_PARCELA, BigDecimal.valueOf(20));
        parcelaMock2.setIdParcela("456");
        var parcelaMock3 = TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao(recorrencia, DATA_TERCEIRA_PARCELA, TEN);
        parcelaMock3.setIdParcela(ID_PARCELA);
        parcelaMock3.setIdConciliacaoRecebedor("identificador");
        parcelaMock3.setInformacoesEntreUsuarios("informações");
        parcelaMock3.setDataCriacaoRegistro(DATA_CRIACAO);

        recorrencia.setRecorrencias(List.of(parcelaMock1, parcelaMock2, parcelaMock3));

        var consultaDetalhesParcelasResponse = ConsultaDetalhesParcelasResponse.fromRecorrencia(recorrencia, ID_PARCELA);

        assertThat(consultaDetalhesParcelasResponse)
                .usingRecursiveComparison()
                .isEqualTo(ConsultaDetalhesParcelasResponseTest.ProviderTest.criarConsultaDetalhesParcelasResponseEsperado());
    }

    @Test
    void dadoRecorrenciaNula_quandoFromListagemRecorrencia_deveRetornarNulo() {
        assertNull(ConsultaDetalhesParcelasResponse.fromRecorrencia(null, null));
    }

    private static final class ProviderTest {
        public static ConsultaDetalhesParcelasResponse criarConsultaDetalhesParcelasResponseEsperado() {
            var recebedor = RecebedorResponse.builder()
                    .nome("SUPERMERCADO E ACOUGUE SAO JOSE")
                    .chave("pix@sicredi.com.br")
                    .agencia("0101")
                    .conta("052124")
                    .tipoChave(TipoChaveEnum.EMAIL)
                    .cpfCnpj("00248158023")
                    .instituicao("91586982")
                    .build();

            var pagador = PagadorResponse.builder()
                    .nome("Fulano de Tal")
                    .agencia("0101")
                    .conta("003039")
                    .cpfCnpj("00248158023")
                    .instituicao("91586982")
                    .tipoConta(TipoContaEnum.CONTA_CORRENTE)
                    .build();

            var parcela = ParcelaRecorrenciaResponse.builder()
                    .identificadorParcela(ID_PARCELA)
                    .valor(BigDecimal.TEN)
                    .numeroParcela(3L)
                    .dataTransacao(DATA_TERCEIRA_PARCELA)
                    .idFimAFim("E91586982202208151245099rD6AIAa7")
                    .status(TipoStatusEnum.CRIADO)
                    .dataCriacaoRegistro(DATA_CRIACAO)
                    .idConciliacaoRecebedor("identificador")
                    .informacoesEntreUsuarios("informações")
                    .status(TipoStatusEnum.CRIADO)
                    .build();

            return ConsultaDetalhesParcelasResponse.builder()
                    .identificadorRecorrencia("d02aefe4-91be-404e-a5c6-7dff4e8b05cb")
                    .tipoFrequencia(TipoFrequencia.MENSAL)
                    .tipoIniciacaoCanal(TipoIniciacaoCanal.CHAVE)
                    .tipoCanal(TipoCanalEnum.MOBI)
                    .tipoRecorrencia(TipoRecorrencia.AGENDADO_RECORRENTE)
                    .numeroTotalParcelas(3L)
                    .nome(SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Exemplos.NOME)
                    .recebedor(recebedor)
                    .parcela(parcela)
                    .pagador(pagador)
                    .build();
        }
    }
}