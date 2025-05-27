package io.sicredi.spirecorrencia.api.automatico.pain;

import br.com.sicredi.spi.dto.DetalheRecorrenciaPain011Dto;
import br.com.sicredi.spi.dto.DetalheRecorrenciaPain012Dto;
import br.com.sicredi.spi.dto.Pain009Dto;
import br.com.sicredi.spi.dto.Pain011Dto;
import br.com.sicredi.spi.dto.Pain012Dto;
import br.com.sicredi.spi.entities.type.MotivoRejeicaoPain012;
import br.com.sicredi.spi.entities.type.StatusRecorrenciaPain012;
import br.com.sicredi.spi.entities.type.TipoRecorrencia;
import br.com.sicredi.spi.entities.type.TipoSituacaoRecorrenciaPain012;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoJornada;
import io.sicredi.spirecorrencia.api.accountdata.DadosContaResponseDTO;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrencia;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.CadastroAutorizacaoRequest;
import io.sicredi.spirecorrencia.api.automatico.autorizacao.ConfirmacaoAutorizacaoRequestDTO;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static br.com.sicredi.spi.entities.type.TipoSituacaoPain009.DATA_CRIACAO_RECORRENCIA;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Pain012ResponseFactory {

    private static final Map<String, TipoSituacaoRecorrenciaPain012> MAP_JORNADA_PARA_SITUACAO = Map.of(
            TipoJornada.JORNADA_2.name(), TipoSituacaoRecorrenciaPain012.AUTORIZACAO_PAGAMENTO_JORNADA_2,
            TipoJornada.JORNADA_3.name(), TipoSituacaoRecorrenciaPain012.AUTORIZACAO_PAGAMENTO_JORNADA_3,
            TipoJornada.JORNADA_4.name(), TipoSituacaoRecorrenciaPain012.AUTORIZACAO_PAGAMENTO_JORNADA_4
    );

    public static Pain012Dto fromPain009Erro(Pain009Dto dto,
                                             MotivoRejeicaoPain012 motivoRejeicao,
                                             String codigoIbge,
                                             String idInformacaoStatus) {
        return Pain012Dto.builder()
                .status(Boolean.FALSE)
                .codMunIBGE(codigoIbge)
                .motivoRejeicao(motivoRejeicao.name())
                .idRecorrencia(dto.getIdRecorrencia())
                .tipoRecorrencia(dto.getTipoRecorrencia())
                .tipoFrequencia(dto.getTipoFrequencia())
                .dataInicialRecorrencia(dto.getDataInicialRecorrencia())
                .dataFinalRecorrencia(dto.getDataFinalRecorrencia())
                .indicadorObrigatorioOriginal(dto.getIndicadorObrigatorio())
                .valor(dto.getValor())
                .nomeUsuarioRecebedor(dto.getNomeUsuarioRecebedor())
                .cpfCnpjUsuarioRecebedor(dto.getCpfCnpjUsuarioRecebedor())
                .participanteDoUsuarioRecebedor(dto.getParticipanteDoUsuarioRecebedor())
                .cpfCnpjUsuarioPagador(dto.getCpfCnpjUsuarioPagador())
                .contaUsuarioPagador(dto.getContaUsuarioPagador())
                .agenciaUsuarioPagador(dto.getAgenciaUsuarioPagador())
                .participanteDoUsuarioPagador(dto.getParticipanteDoUsuarioPagador())
                .nomeDevedor(dto.getNomeDevedor())
                .cpfCnpjDevedor(dto.getCpfCnpjDevedor())
                .numeroContrato(dto.getNumeroContrato())
                .descricao(dto.getDescricao())
                .idInformacaoStatus(idInformacaoStatus)
                .build();
    }

    public static Pain012Dto fromPain009Sucesso(Pain009Dto dto,
                                                DadosContaResponseDTO conta,
                                                Map<String, LocalDateTime> datasPain009,
                                                String codigoIbge,
                                                String idInformacaoStatus) {

        var agencia = Optional.ofNullable(dto.getAgenciaUsuarioPagador())
                .filter(StringUtils::isNotBlank)
                .orElseGet(conta::coop);

        return Pain012Dto.builder()
                .status(Boolean.TRUE)
                .statusRecorrencia(StatusRecorrenciaPain012.PENDENTE_CONFIRMACAO.name())
                .idRecorrencia(dto.getIdRecorrencia())
                .tipoRecorrencia(dto.getTipoRecorrencia())
                .tipoFrequencia(dto.getTipoFrequencia())
                .dataInicialRecorrencia(dto.getDataInicialRecorrencia())
                .dataFinalRecorrencia(dto.getDataFinalRecorrencia())
                .indicadorObrigatorioOriginal(dto.getIndicadorObrigatorio())
                .valor(dto.getValor())
                .nomeUsuarioRecebedor(dto.getNomeUsuarioRecebedor())
                .cpfCnpjUsuarioRecebedor(dto.getCpfCnpjUsuarioRecebedor())
                .participanteDoUsuarioRecebedor(dto.getParticipanteDoUsuarioRecebedor())
                .cpfCnpjUsuarioPagador(dto.getCpfCnpjUsuarioPagador())
                .contaUsuarioPagador(dto.getContaUsuarioPagador())
                .agenciaUsuarioPagador(agencia)
                .participanteDoUsuarioPagador(dto.getParticipanteDoUsuarioPagador())
                .nomeDevedor(dto.getNomeDevedor())
                .cpfCnpjDevedor(dto.getCpfCnpjDevedor())
                .numeroContrato(dto.getNumeroContrato())
                .descricao(dto.getDescricao())
                .codMunIBGE(codigoIbge)
                .idInformacaoStatus(idInformacaoStatus)
                .detalhesRecorrencias(List.of(
                        criarDetalhe(TipoSituacaoRecorrenciaPain012.CRIACAO_RECORRENCIA.name(), datasPain009.get(DATA_CRIACAO_RECORRENCIA.name())),
                        criarDetalhe(TipoSituacaoRecorrenciaPain012.ATUALIZACAO_STATUS_RECORRENCIA.name(), LocalDateTime.now())
                ))
                .build();
    }

    public static Pain012Dto fromConfirmacaoAceita(ConfirmacaoAutorizacaoRequestDTO request, SolicitacaoAutorizacaoRecorrencia solicitacaoAutorizacaoRecorrencia) {
        return Pain012Dto.builder()
                .status(Boolean.TRUE)
                .idRecorrencia(request.getIdRecorrencia())
                .idInformacaoStatus(request.getIdInformacaoStatus())
                .tipoRecorrencia(TipoRecorrencia.RECORRENTE.name())
                .tipoFrequencia(solicitacaoAutorizacaoRecorrencia.getTipoFrequencia())
                .dataInicialRecorrencia(solicitacaoAutorizacaoRecorrencia.getDataInicialRecorrencia())
                .dataFinalRecorrencia(solicitacaoAutorizacaoRecorrencia.getDataFinalRecorrencia())
                .indicadorObrigatorioOriginal(Boolean.FALSE)
                .valor(solicitacaoAutorizacaoRecorrencia.getValor())
                .nomeUsuarioRecebedor(solicitacaoAutorizacaoRecorrencia.getNomeRecebedor())
                .cpfCnpjUsuarioRecebedor(solicitacaoAutorizacaoRecorrencia.getCpfCnpjRecebedor())
                .participanteDoUsuarioRecebedor(solicitacaoAutorizacaoRecorrencia.getInstituicaoRecebedor())
                .codMunIBGE(solicitacaoAutorizacaoRecorrencia.getCodigoMunicipioIBGE())
                .cpfCnpjUsuarioPagador(request.getCpfCnpjPagador())
                .contaUsuarioPagador(solicitacaoAutorizacaoRecorrencia.getContaPagador())
                .agenciaUsuarioPagador(solicitacaoAutorizacaoRecorrencia.getAgenciaPagador())
                .participanteDoUsuarioPagador(solicitacaoAutorizacaoRecorrencia.getInstituicaoPagador())
                .nomeDevedor(solicitacaoAutorizacaoRecorrencia.getNomeDevedor())
                .cpfCnpjDevedor(solicitacaoAutorizacaoRecorrencia.getCpfCnpjDevedor())
                .numeroContrato(solicitacaoAutorizacaoRecorrencia.getNumeroContrato())
                .descricao(solicitacaoAutorizacaoRecorrencia.getDescricao())
                .detalhesRecorrencias(List.of(
                        criarDetalhe(TipoSituacaoRecorrenciaPain012.CRIACAO_RECORRENCIA.name(), solicitacaoAutorizacaoRecorrencia.getDataCriacaoRecorrencia()),
                        criarDetalhe(TipoSituacaoRecorrenciaPain012.ATUALIZACAO_STATUS_RECORRENCIA.name(), request.getDataHoraInicioCanal()),
                        criarDetalhe(TipoSituacaoRecorrenciaPain012.AUTORIZACAO_PAGAMENTO_JORNADA_1.name(), request.getDataHoraInicioCanal())
                ))
                .statusRecorrencia(StatusRecorrenciaPain012.CONFIRMADO_USUARIO_PAGADOR.name())
                .build();
    }

    public static Pain012Dto fromConfirmacaoRejeitada(ConfirmacaoAutorizacaoRequestDTO request, MotivoRejeicaoPain012 motivoRejeicao, SolicitacaoAutorizacaoRecorrencia solicitacaoAutorizacaoRecorrencia) {
        return Pain012Dto.builder()
                .status(Boolean.FALSE)
                .motivoRejeicao(motivoRejeicao.name())
                .idRecorrencia(request.getIdRecorrencia())
                .idInformacaoStatus(request.getIdInformacaoStatus())
                .tipoRecorrencia(TipoRecorrencia.RECORRENTE.name())
                .tipoFrequencia(solicitacaoAutorizacaoRecorrencia.getTipoFrequencia())
                .dataInicialRecorrencia(solicitacaoAutorizacaoRecorrencia.getDataInicialRecorrencia())
                .dataFinalRecorrencia(solicitacaoAutorizacaoRecorrencia.getDataFinalRecorrencia())
                .indicadorObrigatorioOriginal(Boolean.FALSE)
                .valor(solicitacaoAutorizacaoRecorrencia.getValor())
                .nomeUsuarioRecebedor(solicitacaoAutorizacaoRecorrencia.getNomeRecebedor())
                .cpfCnpjUsuarioRecebedor(solicitacaoAutorizacaoRecorrencia.getCpfCnpjRecebedor())
                .participanteDoUsuarioRecebedor(solicitacaoAutorizacaoRecorrencia.getInstituicaoRecebedor())
                .codMunIBGE(solicitacaoAutorizacaoRecorrencia.getCodigoMunicipioIBGE())
                .cpfCnpjUsuarioPagador(request.getCpfCnpjPagador())
                .contaUsuarioPagador(solicitacaoAutorizacaoRecorrencia.getContaPagador())
                .agenciaUsuarioPagador(solicitacaoAutorizacaoRecorrencia.getAgenciaPagador())
                .participanteDoUsuarioPagador(solicitacaoAutorizacaoRecorrencia.getInstituicaoPagador())
                .nomeDevedor(solicitacaoAutorizacaoRecorrencia.getNomeDevedor())
                .cpfCnpjDevedor(solicitacaoAutorizacaoRecorrencia.getCpfCnpjDevedor())
                .numeroContrato(solicitacaoAutorizacaoRecorrencia.getNumeroContrato())
                .descricao(solicitacaoAutorizacaoRecorrencia.getDescricao())
                .build();
    }

    public static Pain012Dto fromPain011Error(Pain011Dto dto,
                                              MotivoRejeicaoPain012 motivoRejeicao,
                                              String idInformacaoStatus,
                                              String codigoIbge) {

        return buildPain012Dto(
                dto,
                motivoRejeicao.name(),
                idInformacaoStatus,
                codigoIbge,
                null,
                null,
                Boolean.FALSE);
    }

    public static Pain012Dto fromPain011ComSucesso(Pain011Dto dto,
                                                   String idInformacaoStatus,
                                                   String codigoIbge,
                                                   LocalDateTime dataAlteracaoRegistro) {
        var dataCriacaoRecorrencia = dto.getDetalhesRecorrencias()
                .stream()
                .filter(r ->
                        TipoSituacaoRecorrenciaPain012.CRIACAO_RECORRENCIA.name().equals(r.getTipoSituacao()))
                .map(DetalheRecorrenciaPain011Dto::getDataHoraRecorrencia)
                .findFirst()
                .orElseGet(LocalDateTime::now);

        var dataAtualizada = List.of(
                criarDetalhe(TipoSituacaoRecorrenciaPain012.CRIACAO_RECORRENCIA.name(), dataCriacaoRecorrencia),
                criarDetalhe(TipoSituacaoRecorrenciaPain012.ATUALIZACAO_STATUS_RECORRENCIA.name(), dataAlteracaoRegistro)
        );

        return buildPain012Dto(
                dto,
                null,
                idInformacaoStatus,
                codigoIbge,
                StatusRecorrenciaPain012.CANCELADA.name(),
                dataAtualizada,
                Boolean.TRUE
        );
    }

    private static Pain012Dto buildPain012Dto(Pain011Dto dto,
                                              String motivoRejeicao,
                                              String idInformacaoStatus,
                                              String codigoIbge,
                                              String statusRecorrencia,
                                              List<DetalheRecorrenciaPain012Dto> detalhesRecorrencias,
                                              Boolean indicadorObrigatorio) {

        return Pain012Dto.builder()
                .status(indicadorObrigatorio)
                .motivoRejeicao(motivoRejeicao)
                .idRecorrencia(dto.getIdRecorrencia())
                .idInformacaoStatus(idInformacaoStatus)
                .tipoRecorrencia(dto.getTipoRecorrencia())
                .tipoFrequencia(dto.getTipoFrequencia())
                .dataInicialRecorrencia(dto.getDataInicialRecorrencia())
                .dataFinalRecorrencia(dto.getDataFinalRecorrencia())
                .indicadorObrigatorioOriginal(dto.getIndicadorObrigatorio())
                .valor(dto.getValor())
                .nomeUsuarioRecebedor(dto.getNomeUsuarioRecebedor())
                .cpfCnpjUsuarioRecebedor(dto.getCpfCnpjUsuarioRecebedor())
                .participanteDoUsuarioRecebedor(dto.getParticipanteDoUsuarioRecebedor())
                .codMunIBGE(codigoIbge)
                .cpfCnpjUsuarioPagador(dto.getCpfCnpjUsuarioPagador())
                .contaUsuarioPagador(dto.getContaUsuarioPagador())
                .agenciaUsuarioPagador(dto.getAgenciaUsuarioPagador())
                .participanteDoUsuarioPagador(dto.getParticipanteDoUsuarioPagador())
                .nomeDevedor(dto.getNomeDevedor())
                .cpfCnpjDevedor(dto.getCpfCnpjDevedor())
                .numeroContrato(dto.getNumeroContrato())
                .descricao(dto.getDescricao())
                .statusRecorrencia(statusRecorrencia)
                .detalhesRecorrencias(detalhesRecorrencias)
                .build();
    }

    public static Pain012Dto fromCadastroAutorizacaoRequest(CadastroAutorizacaoRequest autorizacaoRequest) {
        var dataAtualizacaoRecorrencia = autorizacaoRequest.getTipoJornada() == TipoJornada.JORNADA_3 ? autorizacaoRequest.getDataRecebimentoConfirmacaoPacs002PagamentoImediato() : autorizacaoRequest.getDataHoraInicioCanal();
        TipoSituacaoRecorrenciaPain012 tipoSituacaoDaRecorrenciaAut = MAP_JORNADA_PARA_SITUACAO.get(autorizacaoRequest.getTipoJornada().name());

        return Pain012Dto.builder()
                .status(Boolean.TRUE)
                .statusRecorrencia(StatusRecorrenciaPain012.CONFIRMADO_USUARIO_PAGADOR.name())
                .idRecorrencia(autorizacaoRequest.getIdRecorrencia())
                .tipoRecorrencia(TipoRecorrencia.RECORRENTE.name())
                .tipoFrequencia(autorizacaoRequest.getTipoFrequencia().name())
                .dataInicialRecorrencia(autorizacaoRequest.getDataInicialRecorrencia())
                .dataFinalRecorrencia(autorizacaoRequest.getDataFinalRecorrencia())
                .indicadorObrigatorioOriginal(Boolean.FALSE)
                .valor(autorizacaoRequest.getValor())
                .nomeUsuarioRecebedor(autorizacaoRequest.getNomeRecebedor())
                .cpfCnpjUsuarioRecebedor(autorizacaoRequest.getCpfCnpjRecebedor())
                .participanteDoUsuarioRecebedor(autorizacaoRequest.getInstituicaoRecebedor())
                .cpfCnpjUsuarioPagador(autorizacaoRequest.getCpfCnpjPagador())
                .contaUsuarioPagador(autorizacaoRequest.getContaPagador())
                .agenciaUsuarioPagador(autorizacaoRequest.getAgenciaPagador())
                .participanteDoUsuarioPagador(autorizacaoRequest.getInstituicaoPagador())
                .nomeDevedor(autorizacaoRequest.getNomeDevedor())
                .cpfCnpjDevedor(autorizacaoRequest.getCpfCnpjDevedor())
                .numeroContrato(autorizacaoRequest.getContrato())
                .descricao(autorizacaoRequest.getObjeto())
                .codMunIBGE(autorizacaoRequest.getCodigoMunicipioIbge())
                .idInformacaoStatus(autorizacaoRequest.getIdInformacaoStatus())
                .detalhesRecorrencias(List.of(
                        criarDetalhe(TipoSituacaoRecorrenciaPain012.CRIACAO_RECORRENCIA.name(), autorizacaoRequest.getDataCriacaoRecorrencia()),
                        criarDetalhe(TipoSituacaoRecorrenciaPain012.ATUALIZACAO_STATUS_RECORRENCIA.name(),dataAtualizacaoRecorrencia),
                        criarDetalhe(tipoSituacaoDaRecorrenciaAut.name(), autorizacaoRequest.getDataHoraInicioCanal())
                ))
                .build();
    }

    private static DetalheRecorrenciaPain012Dto criarDetalhe(String tipo, LocalDateTime dataHora) {
        return DetalheRecorrenciaPain012Dto.builder()
                .tipoSituacaoDaRecorrencia(tipo)
                .dataHoraTipoSituacaoDaRecorrencia(dataHora)
                .build();
    }
}