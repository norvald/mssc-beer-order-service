package guru.sfg.beer.order.service.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;


@Slf4j
@Converter(autoApply=true)
public class UuidConverter
        implements AttributeConverter<UUID,String> {
    @Override
    public String convertToDatabaseColumn(UUID attribute) {
        if(attribute == null) {
            return null;
        }
        log.debug("UUID object: "+attribute);
        String str = attribute.toString();
        log.debug("db str: "+str);
        return  str;
    }

    @Override
    public UUID convertToEntityAttribute(String dbData) {
        if(dbData == null) {
            return null;
        }
        log.debug("db str: "+dbData);
        UUID uuid = UUID.fromString(dbData);
        log.debug("UUID object: "+uuid);
        return uuid;
    }
}