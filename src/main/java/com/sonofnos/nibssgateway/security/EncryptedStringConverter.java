package com.sonofnos.nibssgateway.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter for PII fields (account numbers, BVN-equivalent
 * identifiers). Not autoApply - it is attached explicitly per field with
 * @Convert so it is obvious from the entity which columns are encrypted.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return EncryptionService.getInstance().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return EncryptionService.getInstance().decrypt(dbData);
    }
}
