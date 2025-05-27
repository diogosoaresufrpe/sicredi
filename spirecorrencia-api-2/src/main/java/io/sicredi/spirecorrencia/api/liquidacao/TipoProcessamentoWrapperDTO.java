package io.sicredi.spirecorrencia.api.liquidacao;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoMotivoExclusao;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacao;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

@Builder
@Data
@ToString
public class TipoProcessamentoWrapperDTO {
    private String identificadorTransacao;
    private TipoProcessamentoEnum tipoProcessamentoEnum;
    private RecorrenciaTransacao recorrenciaTransacao;
    private TipoProcessamentoErro tipoProcessamentoErro;
    private NotificacaoDTO.TipoTemplate templateNotificacao;

    @Builder
    @Getter
    @ToString
    public static class TipoProcessamentoErro {
        private String codigoErro;
        private String mensagemErro;
        private TipoMotivoExclusao tipoMotivoExclusao;
    }

    public static TipoProcessamentoWrapperDTO criarTipoLiquidacao(String identificadorTransacao, RecorrenciaTransacao recorrenciaTransacao) {
        return TipoProcessamentoWrapperDTO.builder()
                .identificadorTransacao(identificadorTransacao)
                .tipoProcessamentoEnum(TipoProcessamentoEnum.LIQUIDACAO)
                .recorrenciaTransacao(recorrenciaTransacao)
                .build();
    }

    public static TipoProcessamentoWrapperDTO criarTipoExclusaoParcial(String identificadorTransacao,
                                                                       RecorrenciaTransacao recorrenciaTransacao,
                                                                       String codigoErro,
                                                                       String mensagemErro,
                                                                       TipoMotivoExclusao tipoMotivoExclusao,
                                                                       NotificacaoDTO.TipoTemplate templateNotificacao) {
        var tipoProcessamentoErroDTO = TipoProcessamentoErro.builder()
                .codigoErro(codigoErro)
                .mensagemErro(mensagemErro)
                .tipoMotivoExclusao(tipoMotivoExclusao)
                .build();

        return TipoProcessamentoWrapperDTO.builder()
                .identificadorTransacao(identificadorTransacao)
                .tipoProcessamentoEnum(TipoProcessamentoEnum.EXCLUSAO_PARCIAL)
                .recorrenciaTransacao(recorrenciaTransacao)
                .tipoProcessamentoErro(tipoProcessamentoErroDTO)
                .templateNotificacao(templateNotificacao)
                .build();
    }

    public static TipoProcessamentoWrapperDTO criarTipoIgnoradaComErro(String identificadorTransacao, RecorrenciaTransacao recorrenciaTransacao, String codigoErro, String mensagemErro) {
        var tipoProcessamentoErroDTO = TipoProcessamentoErro.builder()
                .codigoErro(codigoErro)
                .mensagemErro(mensagemErro)
                .build();

        return TipoProcessamentoWrapperDTO.builder()
                .identificadorTransacao(identificadorTransacao)
                .tipoProcessamentoEnum(TipoProcessamentoEnum.IGNORADA)
                .recorrenciaTransacao(recorrenciaTransacao)
                .tipoProcessamentoErro(tipoProcessamentoErroDTO)
                .build();
    }

    public static TipoProcessamentoWrapperDTO criarTipoExclusaoTotal(String identificadorTransacao,
                                                                     RecorrenciaTransacao recorrenciaTransacao,
                                                                     String codigoErro,
                                                                     String mensagemErro,
                                                                     TipoMotivoExclusao tipoMotivoExclusao,
                                                                     NotificacaoDTO.TipoTemplate templateNotificacao) {
        var tipoProcessamentoErroDTO = TipoProcessamentoErro.builder()
                .codigoErro(codigoErro)
                .mensagemErro(mensagemErro)
                .tipoMotivoExclusao(tipoMotivoExclusao)
                .build();

        return TipoProcessamentoWrapperDTO.builder()
                .identificadorTransacao(identificadorTransacao)
                .tipoProcessamentoEnum(TipoProcessamentoEnum.EXCLUSAO_TOTAL)
                .recorrenciaTransacao(recorrenciaTransacao)
                .tipoProcessamentoErro(tipoProcessamentoErroDTO)
                .templateNotificacao(templateNotificacao)
                .build();
    }

}
