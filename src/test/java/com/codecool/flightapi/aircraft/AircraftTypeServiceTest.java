package com.codecool.flightapi.aircraft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codecool.flightapi.common.error.ResourceAlreadyExistsException;
import com.codecool.flightapi.common.error.ResourceNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AircraftTypeServiceTest {

    @Mock
    private AircraftTypeRepository aircraftTypeRepository;

    private AircraftTypeService aircraftTypeService;

    private final AircraftType a320 = new AircraftType(1L, "A320", "Airbus", "A320", 840, 180);

    @BeforeEach
    void setUp() {
        aircraftTypeService = new AircraftTypeService(aircraftTypeRepository, new AircraftTypeMapperImpl());
    }

    @Test
    void findByIdThrowsNotFoundForUnknownId() {
        when(aircraftTypeRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aircraftTypeService.findById(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Aircraft type with id '42' was not found");
    }

    @Test
    void findByIdReturnsTheAircraftTypeAsResponse() {
        when(aircraftTypeRepository.findById(1L)).thenReturn(Optional.of(a320));

        assertThat(aircraftTypeService.findById(1L))
                .isEqualTo(new AircraftTypeResponse(1L, "A320", "Airbus", "A320", 840, 180));
    }

    @Test
    void getByIcaoCodeThrowsNotFoundForUnknownCode() {
        when(aircraftTypeRepository.findByIcaoCode("XXXX")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aircraftTypeService.getByIcaoCode("XXXX"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Aircraft type with ICAO code 'XXXX' was not found");
    }

    @Test
    void createSavesANewEntityWithoutId() {
        when(aircraftTypeRepository.findByIcaoCode("A21N")).thenReturn(Optional.empty());
        when(aircraftTypeRepository.save(any(AircraftType.class))).thenAnswer(invocation -> {
            AircraftType saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        AircraftTypeResponse created =
                aircraftTypeService.create(new AircraftTypeRequest("A21N", "Airbus", "A321neo", 840, null));

        ArgumentCaptor<AircraftType> saved = ArgumentCaptor.forClass(AircraftType.class);
        verify(aircraftTypeRepository).save(saved.capture());
        assertThat(saved.getValue().getModel()).isEqualTo("A321neo");
        assertThat(created).isEqualTo(new AircraftTypeResponse(5L, "A21N", "Airbus", "A321neo", 840, null));
    }

    @Test
    void createRejectsAnExistingIcaoCode() {
        when(aircraftTypeRepository.findByIcaoCode("A320")).thenReturn(Optional.of(a320));

        assertThatThrownBy(
                        () -> aircraftTypeService.create(new AircraftTypeRequest("A320", "Airbus", "A320", 840, 180)))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(aircraftTypeRepository, never()).save(any(AircraftType.class));
    }

    @Test
    void updateCopiesEveryFieldAndMayKeepItsOwnCode() {
        when(aircraftTypeRepository.findById(1L)).thenReturn(Optional.of(a320));
        when(aircraftTypeRepository.findByIcaoCode("A320")).thenReturn(Optional.of(a320));

        AircraftTypeResponse updated =
                aircraftTypeService.update(1L, new AircraftTypeRequest("A320", "Airbus", "A320ceo", 830, 186));

        assertThat(updated).isEqualTo(new AircraftTypeResponse(1L, "A320", "Airbus", "A320ceo", 830, 186));
        assertThat(a320.getCruiseSpeedKmh()).isEqualTo(830);
    }

    @Test
    void deleteThrowsNotFoundAndDeletesNothingForUnknownId() {
        when(aircraftTypeRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aircraftTypeService.delete(42L)).isInstanceOf(ResourceNotFoundException.class);
        verify(aircraftTypeRepository, never()).delete(any(AircraftType.class));
    }
}
