package io.sicredi.spirecorrencia.api.exceptions;

import br.com.sicredi.framework.exception.ExceptionError;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AppExceptionCode implements ExceptionError {

    SPIRECORRENCIA_BU0001("SPIRECORRENCIA_BU0001", "Dados do pagador e do recebedor inválidos. Informe uma conta de origem diferente da conta de destino."),
    SPIRECORRENCIA_BU0003("SPIRECORRENCIA_BU0003", "Data da recorrência inválida. Informe uma data a partir do dia %s"),
    SPIRECORRENCIA_BU0004("SPIRECORRENCIA_BU0004", "Quantidade total de parcelas inválido. Informe quantidade de parcelas menor que %s"),
    SPIRECORRENCIA_BU0006("SPIRECORRENCIA_BU0006","Não é permitido exclusão da transação da recorrência com status {0}. Verifique {1, choice,0#a recorrência|1#as recorrências} com identificador único {2}"),
    SPIRECORRENCIA_BU0007("SPIRECORRENCIA_BU0007","{0, choice,0#Parcela|1#Parcelas} da recorrência inválida. Verifique {0, choice,0#a parcela|1#as parcelas} {1}"),
    SPIRECORRENCIA_BU0008("SPIRECORRENCIA_BU0008", "Não é possível a exclusão da transação da recorrência com status %s."),
    SPIRECORRENCIA_BU0009("SPIRECORRENCIA_BU0009","Tipo de motivo da exclusão não existe. Verifique o código (%s) informado"),
    SPIRECORRENCIA_BU0010("SPIRECORRENCIA_BU0010","Não foi possível localizar sua recorrência. Por favor, revise as informações e tente novamente."),
    SPIRECORRENCIA_BU0011("SPIRECORRENCIA_BU0011", "Não é permitido finalizar a recorrência com status %s."),
    SPIRECORRENCIA_BU0012("SPIRECORRENCIA_BU0012", "Recorrência já cadastrada para este recebedor, com mesma data e valor"),
    SPIRECORRENCIA_BU0013("SPIRECORRENCIA_BU0013", "Pedido não autorizado ou proibido"),
    SPIRECORRENCIA_BU0014("SPIRECORRENCIA_BU0014", "Pedido mal formatado ou inválido"),
    SPIRECORRENCIA_BU0015("SPIRECORRENCIA_BU0015", "Pedido improcessável"),
    SPIRECORRENCIA_BU0016("SPIRECORRENCIA_BU0016", "Erro interno da aplicação"),
    SPIRECORRENCIA_BU0017("SPIRECORRENCIA_BU0017", "Erro não mapeado"),
    SPIRECORRENCIA_BU0018("SPIRECORRENCIA_BU0018", "Não foi possível obter o status da ordem de pagamento com o id fim a fim = %s"),
    SPIRECORRENCIA_BU0019("SPIRECORRENCIA_BU0019", "A ordem de pagamento com o id fim a fim = %s, ainda se encontra em processamento com o status = %s"),
    SPIRECORRENCIA_BU0020("SPIRECORRENCIA_BU0020", "Foi lançado um erro de exceção que pode ser repetida ao buscar o status da ordem de pagamento com o id fim a fim = %s"),
    SPIRECORRENCIA_BU0021("SPIRECORRENCIA_BU0021", "Não existe uma recorrência transação (parcela) com id = %d"),
    SPIRECORRENCIA_BU0022("SPIRECORRENCIA_BU0022", "A parcela com id = %d, e status = %s, não pode ser ajustada pois não está PENDENTE"),
    SPIRECORRENCIA_BU0023("SPIRECORRENCIA_BU0023", "A parcela com id = %d, não pode ser ajustada pois não tem id fim a fim"),
    SPIRECORRENCIA_BU0024("SPIRECORRENCIA_BU0024", "Houve um erro ao atualizar a parcela com id = %d, para o status = %s. Por favor verificar na base de dados se a atualização ficou feita."),
    SPIRECORRENCIA_BU0025("SPIRECORRENCIA_BU0025", "Parcela não encontrada. Verifique o identificador único da parcela"),
    SPIRECORRENCIA_BU0026("SPIRECORRENCIA_BU0026","Não é possível cancelar pagamentos no dia da data do vencimento"),
    SPIRECORRENCIA_BU0027("SPIRECORRENCIA_BU0027","Houve um erro durante a emissão de protocolo de liquidação"),
    SPIRECORRENCIA_BU9001("SPIRECORRENCIA_BU9001","Erro durante o processamento."),
    SPIRECORRENCIA_BU9003("SPIRECORRENCIA_BU9003","Identificador único da transação já processado anteriormente com outros dados."),
    SPIRECORRENCIA_REC0001("SPIRECORRENCIA_REC0001", "Algum dado do payload de cadastro está inválido. Erro -> %s"),
    SPIRECORRENCIA_REC0002("SPIRECORRENCIA_REC0002", "Não foi possível processar o cadastro do agendado recorrente. Erro desconhecido -> %s"),
    SPIRECORRENCIA_REC0003("SPIRECORRENCIA_REC0003", "Não foi possível processar a exclusão do agendado recorrente. Erro desconhecido -> %s"),
    SPIRECORRENCIA_REC0004("SPIRECORRENCIA_REC0004", "Não foi possível salvar os dados da recorrência no banco de dados. Erro -> %s"),
    SPIRECORRENCIA_REC9999("SPIRECORRENCIA_REC9999", "Não foi possível processar o cadastro do agendado recorrente por erro de negocio."),
    SPIRECORRENCIA_PROT001("SPIRECORRENCIA_PROT001", "Não foi autorizado a requisição de protocolo para a recorrencia integrada."),
    SPIRECORRENCIA_PROT002("SPIRECORRENCIA_PROT002", "Não foi possivel processar o protocolo por erro na requisição."),
    SPIRECORRENCIA_PROT003("SPIRECORRENCIA_PROT003", "Não foi possível localizar o caminho para realizar a requisição."),
    SPIRECORRENCIA_PROT004("SPIRECORRENCIA_PROT004", "Não foi possível processar o protocolo da recorrencia integrada"),
    SPIRECORRENCIA_PROT005("SPIRECORRENCIA_PROT005", "Houve um erro interno na API de protocolo."),
    SPIRECORRENCIA_PROT006("SPIRECORRENCIA_PROT006", "Serviço de protocolo esta indisponivel."),
    SPIRECORRENCIA_PROT999("SPIRECORRENCIA_PROT999", "Houve um erro desconhecido ao processar o protocolo da recorrencia integrada."),
    SPIRECORRENCIA_PART0001("SPIRECORRENCIA_PART0001", "Dados da requisição inválidos."),
    SPIRECORRENCIA_PART0002("SPIRECORRENCIA_PART0002", "Não foi possível localizar o registro."),
    SPIRECORRENCIA_PART0003("SPIRECORRENCIA_PART0003", "Erro de negócio."),
    SPIRECORRENCIA_PART0004("SPIRECORRENCIA_PART0004", "Erro interno no servidor."),
    SPIRECORRENCIA_PART0005("SPIRECORRENCIA_PART0005", "Serviço não disponível."),
    SPIRECORRENCIA_PART0006("SPIRECORRENCIA_PART0006", "Não foi possível interpretar a resposta do erro."),
    REC_PROC_BU0001("REC_PROC_BU0001","Dados do recebedor difere dos dados no DICT."),
    REC_PROC_BU0002("REC_PROC_BU0002","Dados da chave não localizado no DICT."),
    REC_PROC_BU0003("REC_PROC_BU0003","Erro ao tentar consultar dados da chave no DICT. Detalhes: %s"),
    REC_PROC_BU0004("REC_PROC_BU0004","Tipo de processamento inválido."),
    REC_PROC_BU0005("REC_PROC_BU0005","Tipo de motivo de exclusão inválido."),
    REC_PROC_BU0006("REC_PROC_BU0006","Limite máximo de tentativas de processamento excedido"),

    //FLUXO DE ERRORS AUTORIZAÇÃO
    SOLICITANTE_DO_CANCELAMENTO_DIFERENTE("SOLICITANTE_DO_CANCELAMENTO_DIFERENTE", "O CPF/CNPJ do solicitante do cancelamento não coincidem com as informações fornecidas na autorização do Pix Automático. Por favor, revise as informações e tente novamente."),
    SOLICITACAO_DE_CANCELAMENTO_COM_DADOS_INVALIDA("SPIRECORRENCIA_REC1000", "Algum dado do payload de pedido de cancelamento está inválido. Erro -> %s"),
    RECORRENCIA_COM_STATUS_DIFERENTE_DE_APROVADA("RECORRENCIA_COM_STATUS_DIFERENTE_DE_APROVADA", "A situção da autorização do Pix Automátivo não está APROVADA. Por favor, revise as informações e tente novamente."),

    AUTORIZACAO_NAO_ENCONTRADA("AUTORIZACAO_NAO_ENCONTRADA", "Não foi possível localizar sua autorização do Pix Automático. Por favor, revise as informações e tente novamente."),
    AUTORIZACAO_NAO_ENCONTRADA_RETORNO_BACEN("AUTORIZACAO_NAO_ENCONTRADA","Não foi encontrada nenhuma autorização para realizar o processamento de resposta do BACEN."),

    DADOS_PAGADOR_INVALIDO("DADOS_PAGADOR_INVALIDO", "Os dados do usuário pagador não coincidem com as informações fornecidas na solicitação de autorização do Pix Automático. Por favor, revise as informações e tente novamente."),
    VALOR_MAXIMO_INVALIDO("VALOR_MAXIMO_INVALIDO", "O valor máximo não pode ser menor que o valor mínimo estabelecido pelo recebedor. Por favor, revise as informações e tente novamente."),
    VALOR_MAXIMO_COM_VALOR_FIXO("VALOR_MAXIMO_COM_VALOR_FIXO", "O valor maxímo não deve ser informado para autorizações de valores fixos. Por favor, revise as informações e tente novamente."),
    MOTIVO_REJEICAO_INVALIDO("MOTIVO_REJEICAO_INVALIDO", "Motivo de rejeição inválido para esta solicitação. Por favor, verifique as informações e tente novamente."),
    ERRO_PERSISTENCIA("ERRO_PERSISTENCIA", "Não foi possível persistir os dados da solicitação no banco de dados. Erro -> %s"),
    SOLICITACAO_NAO_ENCONTRADA("SOLICITACAO_NAO_ENCONTRADA", "Não foi possível localizar sua solicitação de autorização do Pix Automático. Por favor, revise as informações e tente novamente."),
    AUTORIZACAO_JA_APROVADA_ANTERIORMENTE("AUTORIZACAO_JA_APROVADA_ANTERIORMENTE", "Você ja possui uma autorização aprovada para esse contrato. Por favor, revise as informações e tente novamente."),
    PERMISSAO_RETENTATIVA_INVALIDA("PERMISSAO_RETENTATIVA_INVALIDA", "A permissão de retentativa está inconsistente com os dados da recorrência. Por favor, revise as informações e tente novamente."),

    INSTRUCAO_PAGAMENTO_DIFERENTE_ATIVA("INSTRUCAO_PAGAMENTO_DIFERENTE_ATIVA", "A instrução de pagamento não está ativa ou já foi cancelada. Por favor, verifique o status da instrução e tente novamente."),
    HORARIO_CANCELAMENTO_FORA_DO_PERMITIDO("HORARIO_CANCELAMENTO_FORA_DO_PERMITIDO", "O cancelamento só é permitido até as 23h59 do dia anterior ao vencimento da instrução de pagamento."),
    INSTRUCAO_PAGAMENTO_NAO_ENCONTRADA("INSTRUCAO_PAGAMENTO_NAO_ENCONTRADA", "Não foi possível localizar os dados do seu agendamento do Pix Automático. Por favor, revise as informações e tente novamente."),
    DADOS_SOLICITANTE_DIFERENTE_DA_INSTRUCAO_PAGAMENTO("DADOS_SOLICITANTE_DIFERENTE_DA_INSTRUCAO_PAGAMENTO", "Os dados do solicitante não correspondem à instrução de pagamento. Por favor, revise as informações e tente novamente.");


    private final String code;
    private final String message;

    @Override
    public String getError() {
        return this.code;
    }

    public String getMensagemFormatada(String arg) {
        return String.format(this.getMessage(), arg);
    }
}
