package io.sicredi.spirecorrencia.api.notificacao;

import br.com.sicredi.framework.exception.BusinessException;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@EqualsAndHashCode
@Getter
@ToString
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificacaoDTO {

    private String chave;
    private String canal;
    private String conta;
    private String agencia;
    private String tipoChave;
    private TipoTemplate operacao;
    private Map<String, String> informacoesAdicionais;

    @Builder
    public NotificacaoDTO(
            String chave,
            String canal,
            String conta,
            String agencia,
            String tipoChave,
            TipoTemplate operacao,
            List<InformacaoAdicional> informacoesAdicionais
    ) {
        this.chave = chave;
        this.canal = canal;
        this.conta = conta;
        this.agencia = agencia;
        this.tipoChave = tipoChave;
        this.operacao = operacao;
        this.informacoesAdicionais = Objects.nonNull(informacoesAdicionais) ? Map.ofEntries(informacoesAdicionais.toArray(InformacaoAdicional[]::new)) : Map.of();
    }

    public static class InformacaoAdicional implements Map.Entry<String, String> {
        private static final int TAMANHO_CNPJ = 14;
        private static final int TAMANHO_CPF = 11;

        private final NotificacaoInformacaoAdicional key;
        private final String value;

        InformacaoAdicional(NotificacaoInformacaoAdicional key, String valor) {
            this.key = Objects.requireNonNull(key);
            this.value = Objects.requireNonNull(valor);
        }


        public static InformacaoAdicional of(NotificacaoInformacaoAdicional key, String value) {
            return new InformacaoAdicional(key, value);
        }

        @Override
        public String getKey() {
            return this.key.getNomeVariavel();
        }

        @Override
        public String getValue() {
            if (NotificacaoInformacaoAdicional.DOCUMENTO_RECEBEDOR.equals(key)) {
                return formatarDocumentoRecebedor(this.value);
            }
            return this.value;
        }

        @Override
        public String setValue(String value) {
            return value;
        }

        private static String formatarDocumentoRecebedor(String documento) {
            if (TAMANHO_CNPJ == StringUtils.length(documento)) {
                return RegExUtils.replaceAll(documento, "(\\w{2})(\\w{3})(\\w{3})(\\w{4})(\\d{2})", "$1.$2.$3/$4-$5");
            }

            if (TAMANHO_CPF == StringUtils.length(documento)) {
                return RegExUtils.replaceAll(documento, "(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "###.$2.$3-##");
            }

            throw new BusinessException("Documento inválido");
        }

    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public enum TipoTemplate {
        RECORRENCIA_VENCIMENTO_PROXIMO_DIA,
        RECORRENCIA_CADASTRO_SUCESSO,
        RECORRENCIA_CADASTRO_FALHA,
        RECORRENCIA_FALHA_OPERACIONAL,
        RECORRENCIA_FALHA_MUDANCA_TITULARIDADE_CHAVE,
        RECORRENCIA_SUCESSO_PAGAMENTO_PARCELA,
        RECORRENCIA_SUCESSO_FINALIZACAO,
        RECORRENCIA_FALHA_SALDO_INSUFICIENTE,
        RECORRENCIA_FALHA_SALDO_INSUFICIENTE_NAO_EFETIVADA,
        AUTOMATICO_AUTORIZACAO_PENDENTE_DE_APROVACAO,
        AUTOMATICO_AUTORIZACAO_CONFIRMADA_PAGADOR_FALHA_NAO_RESPONDIDA_OU_CANCELADA_RECEBEDOR,
        AUTOMATICO_AUTORIZACAO_CONFIRMADA_SUCESSO,
        AUTOMATICO_AUTORIZACAO_PEDIDO_CANCELAMENTO_NEGADO,
        AUTOMATICO_AUTORIZACAO_PEDIDO_CANCELAMENTO_SUCESSO
    }
}
