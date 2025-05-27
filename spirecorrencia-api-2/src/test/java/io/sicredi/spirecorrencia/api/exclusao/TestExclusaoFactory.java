package io.sicredi.spirecorrencia.api.exclusao;

import br.com.sicredi.canaisdigitais.enums.OrigemEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.*;
import br.com.sicredi.spicanais.transacional.transport.lib.config.SpiCanaisTransportContantes;
import io.sicredi.spirecorrencia.api.idempotente.TipoResponseIdempotente;
import io.sicredi.spirecorrencia.api.repositorio.Recorrencia;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacao;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static io.sicredi.spirecorrencia.api.testconfig.TestFactory.PagadorTestFactory.criarPagador;
import static io.sicredi.spirecorrencia.api.testconfig.TestFactory.RecebedorTestFactory.criarRecebedor;
import static io.sicredi.spirecorrencia.api.testconfig.TestFactory.RecorrenciaTransacaoTestFactory.criarRecorrenciaTransacao;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class TestExclusaoFactory {

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class RecorrenciaTest {

        public static Recorrencia criarRecorrencia(LocalDateTime dataCriacao) {

            var recorrencia = Recorrencia.builder()
                    .idRecorrencia(UUID.randomUUID().toString())
                    .pagador(criarPagador())
                    .recebedor(criarRecebedor())
                    .nome(SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Exemplos.NOME)
                    .tipoCanal(TipoCanalEnum.MOBI)
                    .tipoOrigemSistema(OrigemEnum.LEGADO)
                    .recorrencias(List.of(new RecorrenciaTransacao()))
                    .tipoIniciacao(TipoPagamentoPixEnum.PIX_PAYMENT_BY_KEY)
                    .tipoStatus(TipoStatusEnum.CRIADO)
                    .dataCriacao(dataCriacao)
                    .tipoIniciacaoCanal(TipoIniciacaoCanal.CHAVE)
                    .tipoFrequencia(TipoFrequencia.MENSAL)
                    .build();

            recorrencia.setRecorrencias(List.of(
                    criarRecorrenciaTransacao(recorrencia, LocalDate.now().plusMonths(1), BigDecimal.TEN),
                    criarRecorrenciaTransacao(recorrencia, LocalDate.now().plusMonths(2), BigDecimal.ONE),
                    criarRecorrenciaTransacao(recorrencia, LocalDate.now().plusMonths(3), BigDecimal.TWO)
            ));

            return recorrencia;
        }

    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ExcluirRecorrenciaTest {

        public static ExclusaoRequisicaoDTO criarExcluirRecorrenciaTransacao(String idRecorrencia, List<String> listIdentificadoresParcelas) {
            var exclusaoRecorrenciaRequestDTO = new ExclusaoRequisicaoDTO();
            exclusaoRecorrenciaRequestDTO.setIdentificadorRecorrencia(idRecorrencia);
            exclusaoRecorrenciaRequestDTO.setIdentificadoresParcelas(listIdentificadoresParcelas);
            exclusaoRecorrenciaRequestDTO.setTipoMotivoExclusao(TipoMotivoExclusao.SOLICITADO_USUARIO);
            exclusaoRecorrenciaRequestDTO.setTipoResponse(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO);
            return exclusaoRecorrenciaRequestDTO;
        }

    }
}
