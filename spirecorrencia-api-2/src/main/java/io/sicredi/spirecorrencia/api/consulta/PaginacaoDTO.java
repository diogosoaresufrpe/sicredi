package io.sicredi.spirecorrencia.api.consulta;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Builder
@Getter
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Schema(title = "Dados da paginação de recorrências do Pix")
public class PaginacaoDTO {

    @Schema(title = "Número total de páginas", example = "3")
    private Integer totalPaginas;

    @Schema(title = "Tamanho da página atual", example = "1")
    private Integer tamanhoPagina;

    @Schema(title = "Número da página atual", example = "0")
    private Integer numeroPagina;

    @Schema(title = "Número de elementos na página atual", example = "1")
    private Integer numeroElementos;

    @Schema(title = "Número total de elementos", example = "3")
    private Long numeroTotalElementos;

    @Schema(title = "Indica se é a ultima página disponível", example = "false")
    private Boolean ultimaPagina;

    @Schema(title = "Indica se é a primeira página disponível", example = "true")
    private Boolean primeiraPagina;

    @Schema(title = "Indica se contém uma próxima página com dados", example = "true")
    private Boolean existeProximaPagina;

    @Schema(title = "Indica se contém uma página anterior com dados", example = "false")
    private Boolean existePaginaAnterior;

    @Schema(title = "Indica se a página atual está vazia", example = "false")
    private Boolean paginaVazia;

    public static PaginacaoDTO fromPage(Page<?> page) {
        return PaginacaoDTO.builder()
                .numeroPagina(page.getNumber())
                .tamanhoPagina(page.getSize())
                .totalPaginas(page.getTotalPages())
                .paginaVazia(page.isEmpty())
                .primeiraPagina(page.isFirst())
                .existeProximaPagina(page.hasNext())
                .existePaginaAnterior(page.hasPrevious())
                .numeroElementos(page.getNumberOfElements())
                .ultimaPagina(page.isLast())
                .numeroTotalElementos(page.getTotalElements())
                .build();
    }
}
