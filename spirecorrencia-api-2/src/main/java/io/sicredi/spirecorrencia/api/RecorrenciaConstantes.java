package io.sicredi.spirecorrencia.api;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RecorrenciaConstantes {

    public static final String RECORRENCIA_EXCLUSAO_SISTEMA = "REC_EXCL_0002";
    public static final String CODIGO_PROTOCOLO_EXCLUSAO = "432";
    public static final String CODIGO_PROTOCOLO_EXCLUSAO_INTEGRADA = "434";
    public static final String CODIGO_PROTOCOLO_AUTOMATICO_PAGADOR_CONFIRMACAO_AUTORIZACAO = "438";
    public static final String CODIGO_PROTOCOLO_AUTOMATICO_PAGADOR_CADASTRO_COM_AUTENTICACAO = "439";
    public static final String CODIGO_PROTOCOLO_AUTOMATICO_PAGADOR_CADASTRO_SEM_AUTENTICACAO = "440";
    public static final String CODIGO_PROTOCOLO_AUTOMATICO_PAGADOR_CANCELAMENTO_DE_AUTORIZACAO = "441";
    public static final String DATA_CRIACAO_RECORRENCIA = "dataCriacaoRecorrencia";

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Regex {
        public static final String PATTERN_APENAS_NUMEROS = "^[0-9]*$";
        public static final String PATTERN_AGENCIA = "^\\d{4}$";
        public static final String PATTERN_CONTA = "^\\d{1,20}$";
        public static final String PATTERN_IDENTIFICADOR_PARCELA = "\\S+";
        public static final String PATTERN_CPF_CNPJ = "^([0-9]{11}|[A-Za-z0-9]{12}\\d{2})$";
        public static final String PATTERN_TIPO_PESSOA = "^(PF|PJ)$";
        public static final String PATTERN_TIPO_RECORRENCIA = "^(AGENDADO|AGENDADO_RECORRENTE|AUTOMATICO_RECORRENTE)$";
        public static final String PATTERN_TIPO_STATUS = "^(CRIADO|CONCLUIDO|EXCLUIDO)$";
        public static final String PATTERN_TIPO_STATUS_PARCELA = "^(CRIADO|CONCLUIDO|EXCLUIDO|PENDENTE)$";
        public static final String PATTERN_TIPO_STATUS_SOLICITACAO_AUTORIZACAO = "^(CRIADA|PENDENTE_CONFIRMACAO|CONFIRMADA|ACEITA|REJEITADA|CANCELADA|EXPIRADA)$";
        public static final String PATTERN_TIPO_STATUS_AUTORIZACAO = "^(CRIADA|APROVADA|REJEITADA|CANCELADA|EXPIRADA)$";
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Recorrencia {
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class Validations {
            public static final String PAGADOR_NOTNULL = "Preenchimento do pagador é obrigatório";
            public static final String RECEBEDOR_NOTNULL = "Preenchimento do recebedor é obrigatório";
            public static final String ID_RECORRENCIA_NOTNULL = "Preenchimento do identificador único da recorrência é obrigatório";
            public static final String TIPO_INICIACAO_CANAL_NOTNULL = "Preenchimento do tipo de iniciação canal é obrigatório";
            public static final String TIPO_FREQUENCIA_NOTNULL = "Preenchimento do tipo de frequência é obrigatório";
            public static final String CONTA_NOT_BLANK = "Número da conta do associado não pode ser nulo ou vazio.";
            public static final String TIPO_RECORRENCIA_PATTERN_MESSAGE = "Lista de tipo de recorrência inválida";
            public static final String TIPO_STATUS_SOLICITACAO_AUTORIZACAO_PATTERN_MESSAGE = "Lista de tipo de status de solicitação de autorização inválida";
            public static final String TIPO_STATUS_AUTORIZACAO_PATTERN_MESSAGE = "Lista de tipo de status de autorização inválida";

        }

        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class Schemas {
            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static final class Titles {
                public static final String RECORRENCIA_RESPONSE = "Response de recorrências Pix";
                public static final String ID = "Identificador da Recorrência";
                public static final String OID_RECORRENCIA = "OID da Recorrência";
                public static final String NOME = "Identificador da recorrência";
                public static final String DESCRICAO = "Campo livre para informações entre pagador e recebedor";
                public static final String NUMERO_TOTAL_PARCELAS = "Número total de parcelas da recorrência";
                public static final String TIPO_CANAL = "Tipo do Canal";
                public static final String TIPO_MARCA = "Tipo da Marca";
                public static final String TIPO_INICIACAO = "Tipo de Iniciação";
                public static final String TIPO_INICIACAO_CANAL = "Tipo de Iniciação Canal";
                public static final String TIPO_FREQUENCIA = "Tipo de Frequência";
                public static final String CPF_CNPJ = "Documento do pagador";
                public static final String EXCLUSAO_RECORRENCIAS = "Request para exclusão de recorrências Pix por documento, agência e conta";
                public static final String RECORRENCIA_TRANSACAO = "Lista das transações de recorrência";
                public static final String TIPO_RECORRENCIA = "Tipo da recorrencia";
                public static final String TIPO_STATUS_SOLICITACAO_AUTORIZACAO = "Status da solicitação de autorização para pix automático.";
                public static final String TIPO_STATUS_AUTORIZACAO = "Status da autorização para pix automático.";
            }

            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static final class Exemplos {
                public static final String ID = "1";
                public static final String NOME = "Recorrência Academia";
                public static final String DESCRICAO = "Mensalidade academia";
                public static final String NUMERO_TOTAL_PARCELAS = "2";
                public static final String TIPO_CANAL = "MOBI";
                public static final String TIPO_MARCA = "SICREDI";
                public static final String TIPO_INICIACAO = "PIX_PAYMENT_BY_KEY";
                public static final String TIPO_INICIACAO_CANAL = "CHAVE";
                public static final String TIPO_FREQUENCIA = "MENSAL";
                public static final String NUM_INIC_CNPJ = "50685362000135";
                public static final String TIPO_RECORRENCIA = "AGENDADO_RECORRENTE";
                public static final String TIPO_STATUS_SOLICITACAO_AUTORIZACAO = "PENDENTE_CONFIRMACAO";
                public static final String TIPO_STATUS_AUTORIZACAO = "APROVADA";
                public static final String TIPOS_STATUS_SOLICITACAO_AUTORIZACAO = "[\"CRIADA\", \"PENDENTE_CONFIRMACAO\", \"CONFIRMADA\", \"ACEITA\", \"REJEITADA\", \"CANCELADA\", \"EXPIRADA\"]";
                public static final String TIPOS_STATUS_AUTORIZACAO = "[\"CRIADA\", \"APROVADA\", \"REJEITADA\", \"CANCELADA\", \"EXPIRADA\"]";

            }

        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class RecorrenciaTransacao {

        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class Schemas {
            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static final class Titles {

                public static final String ID = "ID da Parcela da Recorrência";
                public static final String RECORRENCIA_TRANSACAO_RESPONSE = "Response com detalhes da parcela da recorrência Pix";
                public static final String ID_FIM_A_FIM = "Identificador da Transação ( EndToEnd )";
                public static final String VALOR = "Valor da parcela";
                public static final String NUMERO_PARCELA = "Número da parcela";
                public static final String DATA_TRANSACAO = "Data para efetivação da parcela";
                public static final String DATA_EXCLUSAO = "Data de exclusão da parcela";
                public static final String IDENTIFICADOR_PARCELA = "Identificador da Parcela";
                public static final String STATUS_PARCELA = "Status da parcela da recorrencia";
                public static final String STATUS_PARCELA_DESCRICAO = "Conjunto de status para filtro na busca";
                public static final String STATUS_PARCELA_PATTERNS = "Status da parcela da recorrencia";
            }

            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static final class Exemplos {
                public static final String IDENTIFICADOR_PARCELA = "E91586982202208151245099rD6AIAa7";
                public static final String ID_FIM_A_FIM = "E91586982202208151245099rD6AIAa7";
                public static final String ID_CONCILIACAO_RECEBEDOR = "E00038166201907261559y6j6";
                public static final String VALOR = "89.90";
                public static final String NUMERO_PARCELA = "1";
                public static final String DATA_TRANSACAO = "2022-10-10";
                public static final String DATA_EXCLUSAO = "2022-10-11";
                public static final String TIPO_STATUS = "CRIADO";
                public static final String TIPOS_STATUS_EXEMPLOS = "[\"CRIADO\", \"CONCLUIDO\", \"EXCLUIDO\", \"PENDENTE\"]";

            }

            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static final class Validations {
                public static final String IDENTIFICADOR_PARCELA_NOTBLANK = "Preenchimento do identificador único da parcela é obrigatório";
                public static final String STATUS_PATTERN_MESSAGE = "Lista de status da parcela da recorrencia inválida";
            }

        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Pagador {
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class Validations {
            public static final String AGENCIA_NOTBLANK = "Preenchimento da agência é obrigatório";
            public static final String AGENCIA_SIZE = "Número da agência deve possuir minimo 1 e máximo 4 caracteres";
            public static final String AGENCIA_PATTERN = "Número da cooperativa do associado não pode ser nulo ou vazio.";
            public static final String AGENCIA_PATTERN_MESSAGE = "Número da cooperativa do associado inválido.";
            public static final String CONTA_NOTBLANK = "Preenchimento da conta é obrigatório";
            public static final String CONTA_PATTERN_MESSAGE = "Número da conta do associado inválido.";
            public static final String CONTA_SIZE = "Número da conta deve possuir minimo 1 e máximo 20 caracteres";
            public static final String TIPO_PESSOA_PATTERN_MESSAGE = "Tipo pessoa associado inválido.";
            public static final String CPF_CNPJ_PATTERN_MESSAGE = "Número do CPF ou CNPJ do pagador inválido.";
            public static final String TIPO_STATUS_NOT_BLANK = "Preenchimento do tipo de status é obrigatório";
        }

        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class Schemas {
            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static final class Titles {
                public static final String AGENCIA = "Agencia do pagador";
                public static final String CONTA = "Conta do pagador";
                public static final String TIPO_PESSOA = "Tipo pessoa associado";
                public static final String CPF_CNPJ = "Número de CPF ou CNPJ do pagador.";
            }

            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static final class Exemplos {
                public static final String CPF_CNPJ = "75519914000162";
                public static final String NOME = "Fulano de Tal";
                public static final String INSTITUICAO = "91586982";
                public static final String AGENCIA = "0101";
                public static final String CONTA = "003039";
                public static final String UA = "02";
                public static final String TIPO_PESSOA = "PF";
            }

        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Recebedor {
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class Validations {
            public static final String TIPO_CHAVE_NOTNULL = "Preenchimento do Tipo de Chave é obrigatório quando a chave for informada";
            public static final String CHAVE_NOTNULL = "Preenchimento da chave é obrigatório quando o Tipo da Chave for informado";
        }

        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class Schemas {
            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static final class Titles {
                public static final String RECEBEDOR_RESPONSE = "Response com dados do recebedor das recorrências Pix";
                public static final String CPF_CNPJ = "CPF/CNPJ do recebedor";
                public static final String NOME = "Nome do recebedor";
                public static final String TIPO_CHAVE = "Tipo de Chave";
                public static final String TIPO_PESSOA = "Tipo de Pessoa";
                public static final String INSTITUICAO = "Instituição Financeira do recebedor (ISPB)";
                public static final String CHAVE_PIX = "Chave Pix";
                public static final String AGENCIA = "Agência do recebedor";
                public static final String CONTA = "Conta do recebedor";
            }

            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static final class Exemplos {
                public static final String NOME = "SUPERMERCADO E ACOUGUE SAO JOSE";
                public static final String CPF_CNPJ = "00248158023";
                public static final String AGENCIA = "0101";
                public static final String CONTA = "052124";
                public static final String TIPO_CHAVE = "EMAIL";
                public static final String TIPO_PESSOA = "PJ";
                public static final String INSTITUICAO = "91586982";
                public static final String CHAVE_PIX = "pix@sicredi.com.br";
            }

        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ExcluirRecorrenciaTransacao {
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class Validations {
            public static final String LIST_PARCELAS_UNIQUE = "Não pode conter parcelas com mesmo Identificador da Transação";

            public static final String CODIGO_MOTIVO_EXCLUSAO_NOTBLANK = "Preenchimento do código do motivo da exclusão é obrigatório";
            public static final String CODIGO_MOTIVO_EXCLUSAO_SIZE = "Código do motivo da exclusão deve possuir minimo 1 e máximo 15 caracteres";
        }

        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class HeadersKafka {
            public static final String TIPO_CANAL = "tipoCanal";
            public static final String TIPO_RECORRENCIA = "tipoRecorrencia";
            public static final String PROCESSO = "processo";
            public static final String GESTAO = "GESTAO";

        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class ConsultaRecorrencia {
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class Validations {
            public static final String TAMANHO_PAGINA_SIZE = "Tamanho da pagina deve possuir minimo 1 e máximo %d";
            public static final String NUMERO_PAGINA_SIZE = "Número da pagina deve possuir minimo 0";
            public static final String TIPO_STATUS_UNIQUE = "Não pode conter status duplicados";
        }

        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class Schemas {
            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static final class Titles {
                public static final String CONSULTA_RECORRENCIA_RESPONSE = "Consulta de todas recorrências Pix";
                public static final String TIPO_STATUS = "Status da Recorrência";
                public static final String TAMANHO_DA_PAGINA = "Tamanho da pagina";
                public static final String NUMERO_DA_PAGINA = "Número da pagina";
                public static final String DATA_CRIACAO = "Data de criação";
                public static final String DATA_EXCLUSAO = "Data de exclusão";
                public static final String DATA_ULTIMO_PAGAMENTO_CONCLUIDO = "Data do último pagamento concluído";
                public static final String DATA_PROXIMO_PAGAMENTO = "Data do próximo pagamento";
                public static final String TIPO_RECORRENCIA = "Tipo de recorrência";
                public static final String VALOR = "Valor da recorrência";
                public static final String TIPO_RECORRENCIA_DESCRICAO = "Conjunto de tipos de recorrência para filtro na busca";
                public static final String TIPO_STATUS_SOLICITACAO_AUTORIZACAO_DESCRICAO = "Conjunto de tipos de status de solicitação de autorizaçao para Pix Automático";
                public static final String TIPO_STATUS_AUTORIZACAO_DESCRICAO = "Conjunto de tipos de status da autorizaçao para Pix Automático";
            }

            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static final class Exemplos {
                public static final String TIPO_STATUS = "CRIADO";
                public static final String TIPO_STATUS_EXCLUIDO = "EXCLUIDO";
                public static final String TAMANHO_DA_PAGINA = "10";
                public static final String NUMERO_DA_PAGINA = "0";
                public static final String DATA_CRIACAO = "2022-05-01 23:22:00";
                public static final String DATA_EXCLUSAO = "2022-06-02 23:22:00";
                public static final String DATA_ULTIMO_PAGAMENTO_CONCLUIDO = "2022-07-03";
                public static final String DATA_PROXIMO_PAGAMENTO = "2022-08-03";
                public static final String VALOR = "100";
                public static final String TIPO_RECORRENCIA = "[\"AGENDADO\", \"AGENDADO_RECORRENTE\", \"AUTOMATICO_RECORRENTE\"]";
            }
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Paginacao {
        @NoArgsConstructor(access = AccessLevel.PRIVATE)
        public static final class Schemas {
            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static final class Titles {
                public static final String PAGINACAO_RESPONSE = "Dados da paginação de recorrência Pix";
                public static final String TOTAL_PAGINA = "Número total de páginas";
                public static final String TAMANHO_PAGINA = "Tamanho da página atual";
                public static final String NUMERO_PAGINA = "Número da página atual";
                public static final String NUMERO_ELEMENTOS = "Número de elementos na página atual";
                public static final String NUMERO_TOTAL_ELEMENTOS = "Número total de elementos";
                public static final String ULTIMA_PAGINA = "Indica se é a ultima página disponível";
                public static final String PRIMEIRA_PAGINA = "Indica se é a primeira página disponível";
                public static final String EXISTE_PROXIMA_PAGINA = "Indica se contém uma próxima página com dados";
                public static final String EXISTE_PAGINA_ANTERIOR = "Indica se contém uma página anterior com dados";
                public static final String PAGINA_VAZIA = "Indica se a página atual está vazia";

            }

            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static final class Validations {
                public static final String TAMANHO_PAGINA_NOTNULL = "Tamanho da pagina não pode ser nulo.";
                public static final String NUMERO_PAGINA_MIN = "Número da página atual deve ser maior ou igual a 0.";
                public static final String NUMERO_PAGINA_NOTNULL = "Número da página atual não pode ser nulo.";


            }

            @NoArgsConstructor(access = AccessLevel.PRIVATE)
            public static final class Exemplos {
                public static final String TAMANHO_PAGINA_EXEMPLO_MIN = "1";
                public static final String TAMANHO_PAGINA_EXEMPLO_MAX = "100";
                public static final String TAMANHO_PAGINA_EXEMPLO = "10";

                public static final String NUMERO_PAGINA_EXEMPLO = "0";
                public static final String NUMERO_ELEMENTOS_EXEMPLO = "1";
                public static final String NUMERO_TOTAL_ELEMENTOS_EXEMPLO = "3";
                public static final String ULTIMA_PAGINA_EXEMPLO = "false";
                public static final String PRIMEIRA_PAGINA_EXEMPLO = "true";
                public static final String EXISTE_PROXIMA_PAGINA_EXEMPLO = "true";
                public static final String EXISTE_PAGINA_ANTERIOR_EXEMPLO = "false";
                public static final String PAGINA_VAZIA_EXEMPLO = "false";
            }
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class RecorrenciaAutorizacao {

        public static final String CRIADA = "CRIADA";
        public static final String AGUARDANDO_ENVIO = "AGUARDANDO_ENVIO";
        public static final String AGUARDANDO_RETORNO = "AGUARDANDO_RETORNO";
        public static final String AGUARDANDO_CANCELAMENTO = "AGUARDANDO_CANCELAMENTO";

    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Headers {
            public static final String TIPO_MENSAGEM = "TIPO_MENSAGEM";
            public static final String OPERACAO = "OPERACAO";
            public static final String ID_RECORRENCIA = "ID_RECORRENCIA";
            public static final String PSP_EMISSOR = "PSP_EMISSOR";
            public static final String PAGADOR = "PAGADOR";
            public static final String ID_IDEMPOTENCIA = "ID_IDEMPOTENCIA";
            public static final String ACAO = "ACAO";
    }
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Path {
        public static final String ID_FIM_A_FIM = "idFimAFim";
    }
}
