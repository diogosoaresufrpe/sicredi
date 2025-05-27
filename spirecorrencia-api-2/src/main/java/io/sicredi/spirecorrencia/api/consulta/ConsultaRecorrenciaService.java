package io.sicredi.spirecorrencia.api.consulta;

import br.com.sicredi.framework.web.spring.exception.NotFoundException;
import br.com.sicredi.framework.web.spring.exception.UnprocessableEntityException;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;
import io.sicredi.spirecorrencia.api.repositorio.Recorrencia;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaRepository;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

import static io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode.SPIRECORRENCIA_BU0025;

@Service
@RequiredArgsConstructor
@Slf4j
class ConsultaRecorrenciaService {

    private static final String ATRIBUTO_DEFAULT_ORDENACAO_TIPO_STATUS = "tipoStatus";
    private static final String ATRIBUTO_DEFAULT_ORDENACAO_DATA_CRIACAO = "dataCriacao";
    private static final String ATRIBUTO_DEFAULT_ORDENACAO_DATA_TRANSACAO = "dataTransacao";

    private final RecorrenciaRepository repository;
    private final RecorrenciaTransacaoRepository recorrenciaTransacaoRepository;

    public RecorrenciaResponseWrapper consultarTodas(ConsultaRecorrenciaRequest request) {
        var pageable = PageRequest.of(
                request.getNumeroPagina(),
                request.getTamanhoPagina(),
                Sort.by(Sort.Direction.ASC, ATRIBUTO_DEFAULT_ORDENACAO_DATA_CRIACAO, ATRIBUTO_DEFAULT_ORDENACAO_TIPO_STATUS)
        );

        var pageRecorrencia = repository.findAllByFiltros(request, pageable);

        var listOidRecorrencia = pageRecorrencia.getContent().stream()
                .map(Recorrencia::getOidRecorrencia)
                .toList();

        var mapaRecorrenciaComParcelas = recorrenciaTransacaoRepository.consultarPorRecorrenciaIn(listOidRecorrencia).stream()
                .collect(Collectors.groupingBy(parcela -> parcela.getRecorrencia().getOidRecorrencia()));

        var listaRecorrenciaResponse = pageRecorrencia.getContent().stream()
                .map(recorrencia -> {
                    recorrencia.setRecorrencias(mapaRecorrenciaComParcelas.get(recorrencia.getOidRecorrencia()));
                    return RecorrenciaResponse.fromListagemRecorrencia(recorrencia);
                }).toList();

        return new RecorrenciaResponseWrapper(
                listaRecorrenciaResponse,
                PaginacaoDTO.fromPage(pageRecorrencia)
        );
    }

    public RecorrenciaResponse consultarDetalhes(String identificadorRecorrencia) {
        var recorrencia = repository.consultarPorIdentificadorRecorrencia(identificadorRecorrencia)
                .orElseThrow(() -> new NotFoundException(AppExceptionCode.SPIRECORRENCIA_BU0010));

        return RecorrenciaResponse.fromDetalhesRecorrencia(
                recorrencia
        );
    }

    public ParcelaRecorrenciaResponseWrapper consultarParcelas(ConsultaParcelasRecorrenciaRequest request) {
        var pageable = PageRequest.of(
                request.getNumeroPagina(),
                request.getTamanhoPagina()
        );

        var pageParcelas = recorrenciaTransacaoRepository.findParcelasByFiltros(request, pageable);

        var parcelasResponse = pageParcelas.getContent().stream()
                .map(ListagemParcelaRecorrenciaResponse::fromTransacaoRecorrencia)
                .toList();

        return new ParcelaRecorrenciaResponseWrapper(parcelasResponse,  PaginacaoDTO.fromPage(pageParcelas));
    }

    public ConsultaDetalhesParcelasResponse consultarDetalhesParcelas(String identificadorParcela, String agenciaPagador, String contaPagador) {
        var recorrencias = repository.findRecorrenciaByIdParcelaAndAgenciaAndConta(identificadorParcela, agenciaPagador, contaPagador)
                .orElseThrow(() -> new UnprocessableEntityException(SPIRECORRENCIA_BU0025));

        return ConsultaDetalhesParcelasResponse.fromRecorrencia(recorrencias, identificadorParcela);
    }
}
