package com.codecool.flightapi.common.money;

import com.codecool.flightapi.common.mapping.MappingConfig;
import org.mapstruct.Mapper;

/** Currency is mapped to and from its ISO 4217 code by MapStruct's built-in conversion. */
@Mapper(config = MappingConfig.class)
public interface MoneyMapper {

    MoneyDto toDto(Money money);

    Money toMoney(MoneyDto dto);
}
