package com.codecool.flightapi.common.mapping;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.MapperConfig;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * Shared settings of all MapStruct mappers: Spring beans with constructor injection, and a compile
 * error when a target property is neither mapped nor explicitly ignored, so a new entity or DTO field
 * cannot be silently dropped.
 */
@MapperConfig(
        componentModel = MappingConstants.ComponentModel.SPRING,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface MappingConfig {}
