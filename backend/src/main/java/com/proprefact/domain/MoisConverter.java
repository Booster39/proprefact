package com.proprefact.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.YearMonth;

/**
 * Enregistre un mois sous la forme « AAAA-MM » (colonnes {@code periode}, varchar(7)). Appliqué automatiquement à
 * tout attribut {@link YearMonth} des entités, y compris aux paramètres des requêtes qui le comparent.
 */
@Converter(autoApply = true)
public class MoisConverter implements AttributeConverter<YearMonth, String> {

    @Override
    public String convertToDatabaseColumn(YearMonth mois) {
        return mois == null ? null : mois.toString();
    }

    @Override
    public YearMonth convertToEntityAttribute(String colonne) {
        return colonne == null ? null : YearMonth.parse(colonne);
    }
}
