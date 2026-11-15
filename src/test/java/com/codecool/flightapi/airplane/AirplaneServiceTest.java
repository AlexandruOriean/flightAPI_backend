package com.codecool.flightapi.airplane;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codecool.flightapi.common.error.ResourceNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AirplaneServiceTest {

    @Mock
    private AirplaneRepository airplaneRepository;

    private AirplaneService airplaneService;

    @BeforeEach
    void setUp() {
        airplaneService = new AirplaneService(airplaneRepository, new AirplaneMapperImpl());
    }

    @Test
    void findByIdThrowsNotFoundForUnknownId() {
        when(airplaneRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> airplaneService.findById(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Airplane with id '42' was not found");
    }

    @Test
    void findByIdReturnsTheAirplaneAsResponse() {
        when(airplaneRepository.findById(1L)).thenReturn(Optional.of(new Airplane(1L, "Boeing", "B737", 180L, 700)));

        assertThat(airplaneService.findById(1L)).isEqualTo(new AirplaneResponse(1L, "Boeing", "B737", 180L, 700));
    }

    @Test
    void createSavesANewEntityWithoutId() {
        when(airplaneRepository.save(any(Airplane.class))).thenAnswer(invocation -> {
            Airplane saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        AirplaneResponse created = airplaneService.create(new AirplaneRequest("Airbus", "A320", 150L, 800.0));

        ArgumentCaptor<Airplane> saved = ArgumentCaptor.forClass(Airplane.class);
        verify(airplaneRepository).save(saved.capture());
        assertThat(saved.getValue().getModel()).isEqualTo("A320");
        assertThat(created).isEqualTo(new AirplaneResponse(5L, "Airbus", "A320", 150L, 800));
    }

    @Test
    void updateCopiesEveryField() {
        Airplane existing = new Airplane(1L, "Boeing", "B737", 180L, 700);
        when(airplaneRepository.findById(1L)).thenReturn(Optional.of(existing));

        AirplaneResponse updated = airplaneService.update(1L, new AirplaneRequest("Airbus", "A350", 300L, 950.0));

        // regression: speed used to be overwritten with numberOfSeats
        assertThat(updated).isEqualTo(new AirplaneResponse(1L, "Airbus", "A350", 300L, 950));
        assertThat(existing.getSpeed()).isEqualTo(950);
    }

    @Test
    void deleteThrowsNotFoundAndDeletesNothingForUnknownId() {
        when(airplaneRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> airplaneService.delete(42L)).isInstanceOf(ResourceNotFoundException.class);
        verify(airplaneRepository, never()).delete(any(Airplane.class));
    }
}
