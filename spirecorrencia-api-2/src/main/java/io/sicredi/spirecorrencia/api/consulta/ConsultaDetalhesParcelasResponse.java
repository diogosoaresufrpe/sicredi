package io.sicredi.spirecorrencia.api.consulta;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoCanalEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoFrequencia;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoIniciacaoCanal;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoRecorrencia;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.sicredi.spirecorrencia.api.commons.PagadorResponse;
import io.sicredi.spirecorrencia.api.commons.RecebedorResponse;
import io.sicredi.spirecorrencia.api.repositorio.Recorrencia;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.Optional;

@Builder
@Getter
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Schema(title = "Consulta de detalhes de uma parcela de recorrência Pix")
class ConsultaDetalhesParcelasResponse {

    @Schema(title = "Identificador da Recorrência", example = "2f335153-4d6a-4af1-92a0-c52c5c827af9")
    private String identificadorRecorrencia;

    @Schema(title = "Nome da recorrência", example = "Recorrência Academia")
    private String nome;

    @Schema(title = "Tipo de recorrência")
    private TipoRecorrencia tipoRecorrencia;

    @Schema(title = "Tipo de frequencia", example = "SEMANAL")
    private TipoFrequencia tipoFrequencia;

    @Schema(title = "Tipo da iniciação de canal utilizada na criação do registro da recorrência.", example = "DADOS_BANCARIOS")
    private TipoIniciacaoCanal tipoIniciacaoCanal;

    @Schema(title = "Tipo do canal utilizado na criação da recorrência.", example = "MOBI")
    private TipoCanalEnum tipoCanal;

    @Schema(title = "Número total de parcelas", example = "10")
    private Long numeroTotalParcelas;

    @Schema(title = "Dados do recebedor")
    private RecebedorResponse recebedor;

    @Schema(title = "Dados da parcela")
    private ParcelaRecorrenciaResponse parcela;

    @Schema(title = "Dados do pagador")
    private PagadorResponse pagador;

    public static ConsultaDetalhesParcelasResponse fromRecorrencia(Recorrencia recorrencia, String identificadorParcela) {
        return Optional.ofNullable(recorrencia)
                .map(recorrenciaFilter -> {
                            var parcelas = ParcelaRecorrenciaResponse.gerarDetalhesParcelas(recorrenciaFilter.getRecorrencias());
                            var parcelaEspecifica = parcelas.stream()
                                    .filter(parcela -> parcela.getIdentificadorParcela().equals(identificadorParcela))
                                    .findFirst().orElse(null);
                            return ConsultaDetalhesParcelasResponse.builder()
                                    .identificadorRecorrencia(recorrenciaFilter.getIdRecorrencia())
                                    .nome(recorrenciaFilter.getNome())
                                    .tipoIniciacaoCanal(recorrenciaFilter.getTipoIniciacaoCanal())
                                    .tipoCanal(recorrenciaFilter.getTipoCanal())
                                    .tipoRecorrencia(recorrenciaFilter.getTipoRecorrencia())
                                    .tipoFrequencia(recorrenciaFilter.getTipoFrequencia())
                                    .numeroTotalParcelas(ParcelaRecorrenciaResponse.obterNumeroTotalParcelas(parcelas))
                                    .recebedor(RecebedorResponse.fromDetalhesParcelas(recorrenciaFilter.getRecebedor()))
                                    .parcela(parcelaEspecifica)
                                    .pagador(PagadorResponse.fromDetalhesParcelas(recorrenciaFilter.getPagador()))
                                    .build();
                        }
                ).orElse(null);
    }

}
