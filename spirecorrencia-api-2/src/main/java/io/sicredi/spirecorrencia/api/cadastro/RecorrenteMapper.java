package io.sicredi.spirecorrencia.api.cadastro;

import br.com.sicredi.canaisdigitais.dto.IdentificacaoAssociadoDTO;
import br.com.sicredi.canaisdigitais.enums.MarcaEnum;
import br.com.sicredi.canaisdigitais.enums.TipoPessoaEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoCanalEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoMarcaEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoRecorrencia;
import br.com.sicredi.spicanais.transacional.transport.lib.pagamento.CadastroOrdemPagamentoTransacaoDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.CadastroRecorrenciaProtocoloRequest;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.PagadorRequestDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.RecebedorRequestDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.RecorrenciaParcelaRequest;

import java.time.LocalDate;

public final class RecorrenteMapper {

    private RecorrenteMapper() {
    }

    public static CadastroRecorrenciaProtocoloRequest toCadastroRecorrenciaProtocoloRequest(CadastroRequest cadastroRequest) {
        var listParcelaRequest = cadastroRequest.getParcelas().stream()
                .map(parcela -> RecorrenciaParcelaRequest.builder()
                        .valor(parcela.getValor())
                        .dataTransacao(parcela.getDataTransacao())
                        .idFimAFim(parcela.getIdFimAFim())
                        .informacoesEntreUsuarios(parcela.getInformacoesEntreUsuarios())
                        .idConciliacaoRecebedor(parcela.getIdConciliacaoRecebedor())
                        .identificadorParcela(parcela.getIdentificadorParcela())
                        .build())
                .toList();

        var pagadorDTO = cadastroRequest.getPagador();
        var pagadorRequest = PagadorRequestDTO.builder()
                .cpfCnpj(pagadorDTO.getCpfCnpj())
                .nome(pagadorDTO.getNome())
                .instituicao(pagadorDTO.getInstituicao())
                .agencia(pagadorDTO.getAgencia())
                .conta(pagadorDTO.getConta())
                .posto(pagadorDTO.getPosto())
                .tipoConta(pagadorDTO.getTipoConta())
                .tipoPessoa(pagadorDTO.getTipoPessoa())
                .build();

        var recebedorDTO = cadastroRequest.getRecebedor();
        var recebedorRequest = RecebedorRequestDTO.builder()
                .cpfCnpj(recebedorDTO.getCpfCnpj())
                .nome(recebedorDTO.getNome())
                .agencia(recebedorDTO.getAgencia())
                .conta(recebedorDTO.getConta())
                .instituicao(recebedorDTO.getInstituicao())
                .tipoConta(recebedorDTO.getTipoConta())
                .tipoPessoa(recebedorDTO.getTipoPessoa())
                .tipoChave(recebedorDTO.getTipoChave())
                .chave(recebedorDTO.getChave())
                .build();

        var dataPrimeiraParcela = cadastroRequest.getParcelas().stream()
                .filter(parcela -> parcela.getDataTransacao() != null)
                .map(parcela -> parcela.getDataTransacao().toLocalDate())
                .min(LocalDate::compareTo)
                .stream()
                .findFirst()
                .orElse(null);

        var cadastroRecorrenciaProtocoloRequest = CadastroRecorrenciaProtocoloRequest.builder()
                .tipoIniciacao(cadastroRequest.getTipoIniciacao())
                .tipoMarca(getTipoMarca(cadastroRequest.getTipoCanal()))
                .tipoCanal(cadastroRequest.getTipoCanal())
                .pagador(pagadorRequest)
                .recebedor(recebedorRequest)
                .nome(cadastroRequest.getNome())
                .identificadorRecorrencia(cadastroRequest.getIdentificadorRecorrencia())
                .dataPrimeiraParcela(dataPrimeiraParcela)
                .tipoIniciacaoCanal(cadastroRequest.getTipoIniciacaoCanal())
                .tipoFrequencia(cadastroRequest.getTipoFrequencia())
                .tipoRecorrencia(TipoRecorrencia.AGENDADO_RECORRENTE)
                .parcelas(listParcelaRequest)
                .build();

        var identificacaoAssociado = new IdentificacaoAssociadoDTO();
        identificacaoAssociado.setMarca(getMarcaCanaisDigitais(cadastroRequest.getTipoCanal()));
        identificacaoAssociado.setOrigemConta(cadastroRequest.getTipoOrigemSistema());
        identificacaoAssociado.setCooperativa(pagadorDTO.getAgencia());
        identificacaoAssociado.setAgencia(pagadorDTO.getPosto());
        identificacaoAssociado.setConta(pagadorDTO.getConta());
        identificacaoAssociado.setTipoConta(pagadorDTO.getTipoConta().getTipoContaCanaisDigitais());
        identificacaoAssociado.setTipoPessoaConta(getTipoPessoa(pagadorDTO.getTipoPessoa()));
        identificacaoAssociado.setCpfCnpjConta(pagadorDTO.getCpfCnpj());
        identificacaoAssociado.setCpfUsuario(pagadorDTO.getCpfCnpj());
        identificacaoAssociado.setNomeUsuario(pagadorDTO.getNome());
        identificacaoAssociado.setNomeAssociadoConta(pagadorDTO.getNome());

        cadastroRecorrenciaProtocoloRequest.setIdentificadorTransacao(cadastroRequest.getIdentificadorTransacao());
        cadastroRecorrenciaProtocoloRequest.setIdentificadorSimulacaoLimite(cadastroRequest.getIdentificadorSimulacaoLimites());
        cadastroRecorrenciaProtocoloRequest.setIdentificacaoAssociado(identificacaoAssociado);
        return cadastroRecorrenciaProtocoloRequest;
    }

    private static TipoPessoaEnum getTipoPessoa(br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoPessoaEnum tipoPessoa) {
        return TipoPessoaEnum.valueOf(tipoPessoa.name());
    }

    public static TipoMarcaEnum getTipoMarca(TipoCanalEnum tipoCanal) {
        return TipoCanalEnum.WOOP == tipoCanal
                ? TipoMarcaEnum.WOOP
                : TipoMarcaEnum.SICREDI;
    }

    private static MarcaEnum getMarcaCanaisDigitais(TipoCanalEnum tipoCanal) {
        return TipoCanalEnum.WOOP == tipoCanal
                ? MarcaEnum.WOOP
                : MarcaEnum.SICREDI;
    }

}
