package io.sicredi.spirecorrencia.api.cadastro;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoRecorrencia;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TipoFrequenciaConstraintValidatorTest {

    private final TipoFrequenciaConstraintValidator tipoFrequenciaRecorrenciaConstraintValidator = new TipoFrequenciaConstraintValidator();

    @Test
    @DisplayName("Dado que o campo 'tipoFrequencia' foi informado e existe mais de uma parcela, então deve retornar que a informação de Tipo Frequencia foi informado corretamente")
    void dadoTipoFrequenciaEParcelas_quandoIsValid_deveRetornarTrue() {
        var listRecorrenteParcelaRequisicaoDTO = List.of(new RecorrenteParcelaRequisicaoDTO(), new RecorrenteParcelaRequisicaoDTO());
        var recorrenteRequisicaoDTO = CadastroRequest.builder()
                .parcelas(listRecorrenteParcelaRequisicaoDTO)
                .tipoRecorrencia(TipoRecorrencia.AGENDADO)
                .build();
        var constraintValidatorContext = mock(ConstraintValidatorContext.class);

        var retornoIsValid = tipoFrequenciaRecorrenciaConstraintValidator.isValid(recorrenteRequisicaoDTO, constraintValidatorContext);

        assertTrue(retornoIsValid);
    }

    @Test
    @DisplayName("Dado que o campo 'tipoFrequencia' foi informado e existe apenas uma parcela, então deve retornar que a informação de Tipo Frequencia foi informado incorretamente")
    void dadoTipoRecorrenciaAgendadoRecorrente_comTipoFrequenciaNula_quandoisValid_entaoRetornaErro() {
        var recorrenteRequisicaoDTO = CadastroRequest.builder()
                .tipoRecorrencia(TipoRecorrencia.AGENDADO_RECORRENTE)
                .build();
        var constraintValidatorContext = mock(ConstraintValidatorContext.class);
        var constraintViolationBuilder = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        var nodeBuilderCustomizableContext = mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(constraintViolationBuilder);
        when(constraintViolationBuilder.addPropertyNode(anyString())).thenReturn(nodeBuilderCustomizableContext);

        var retornoIsValid = tipoFrequenciaRecorrenciaConstraintValidator.isValid(recorrenteRequisicaoDTO, constraintValidatorContext);

        assertFalse(retornoIsValid);

        verify(constraintValidatorContext, times(1)).buildConstraintViolationWithTemplate(
                argThat("O tipo de frequência deve ser preenchido quando o tipo de recorrência for AGENDADO_RECORRENTE."::equals));
        verify(constraintViolationBuilder, times(1)).addPropertyNode(
                argThat("tipoFrequencia"::equals)
        );
    }

}