package io.sicredi.spirecorrencia.api.repositorio;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class NotificacaoDiaAnteriorConverter implements AttributeConverter<Boolean, String> {

    @Override
    public String convertToDatabaseColumn(Boolean attribute) {
        return Boolean.TRUE.equals(attribute) ? "S" : "N";
    }

    @Override
    public Boolean convertToEntityAttribute(String dbData) {
        return "S".equals(dbData) ? Boolean.TRUE : Boolean.FALSE;
    }
}
