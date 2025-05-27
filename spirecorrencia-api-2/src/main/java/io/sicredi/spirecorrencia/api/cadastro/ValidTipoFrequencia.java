package io.sicredi.spirecorrencia.api.cadastro;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoRecorrencia;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


@Constraint(validatedBy = TipoFrequenciaConstraintValidator.class)
@Target({ METHOD, CONSTRUCTOR, PARAMETER, ANNOTATION_TYPE, FIELD, TYPE })
@Retention(RUNTIME)
@Documented
@interface ValidTipoFrequencia {
    String message() default "";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

class TipoFrequenciaConstraintValidator implements ConstraintValidator<ValidTipoFrequencia, CadastroRequest> {
    @Override
    public boolean isValid(CadastroRequest cadastroRequest, ConstraintValidatorContext context) {

        if (TipoRecorrencia.AGENDADO_RECORRENTE == cadastroRequest.getTipoRecorrencia() && cadastroRequest.getTipoFrequencia() == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("O tipo de frequência deve ser preenchido quando o tipo de recorrência for AGENDADO_RECORRENTE.")
                    .addPropertyNode("tipoFrequencia")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}