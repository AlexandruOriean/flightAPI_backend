package com.codecool.flightapi.airline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codecool.flightapi.common.error.ResourceAlreadyExistsException;
import com.codecool.flightapi.common.error.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Revives the airline tests that were commented out in FlightApiProjectApplicationTests, as unit tests. */
@ExtendWith(MockitoExtension.class)
class AirlineServiceTest {

    @Mock
    private AirlineRepository airlineRepository;

    private AirlineService airlineService;

    private final Airline tarom = new Airline(10000L, "Tarom", "ROT");

    @BeforeEach
    void setUp() {
        airlineService = new AirlineService(airlineRepository, new AirlineMapperImpl());
    }

    @Test
    void findAllReturnsTheRequestedPageOrderedById() {
        // whatever sort the client asks for, the page is read in id order
        Pageable byId = PageRequest.of(1, 3, Sort.by("id"));
        when(airlineRepository.findAll(byId))
                .thenReturn(new PageImpl<>(
                        List.of(tarom, new Airline(10001L, "Wizz", "WZZ"), new Airline(10002L, "BlueAir", "BMS")),
                        byId,
                        7));

        Page<AirlineResponse> page = airlineService.findAll(PageRequest.of(1, 3, Sort.by("name")));

        assertThat(page.getContent()).extracting(AirlineResponse::name).containsExactly("Tarom", "Wizz", "BlueAir");
        assertThat(page.getTotalElements()).isEqualTo(7);
    }

    @Test
    void findByIdExposesIsoAsIcaoCode() {
        when(airlineRepository.findById(10000L)).thenReturn(Optional.of(tarom));

        assertThat(airlineService.findById(10000L)).isEqualTo(new AirlineResponse(10000L, "Tarom", "ROT"));
    }

    @Test
    void findByIdThrowsNotFoundForUnknownId() {
        when(airlineRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> airlineService.findById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Airline with id '1' was not found");
    }

    @Test
    void getByIcaoCodeThrowsNotFoundForUnknownCode() {
        when(airlineRepository.findByIcaoCode("XXX")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> airlineService.getByIcaoCode("XXX"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Airline with ICAO code 'XXX' was not found");
    }

    @Test
    void createSavesTheAirline() {
        when(airlineRepository.findByIcaoCode("BAW")).thenReturn(Optional.empty());
        when(airlineRepository.save(any(Airline.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AirlineResponse created = airlineService.create(new AirlineRequest("British Airways", "BAW"));

        assertThat(created).isEqualTo(new AirlineResponse(null, "British Airways", "BAW"));
    }

    @Test
    void createRejectsATakenIcaoCode() {
        when(airlineRepository.findByIcaoCode("ROT")).thenReturn(Optional.of(tarom));

        assertThatThrownBy(() -> airlineService.create(new AirlineRequest("Other", "ROT")))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        verify(airlineRepository, never()).save(any(Airline.class));
    }

    @Test
    void updateCopiesNameAndCodeAndMayKeepItsOwnCode() {
        when(airlineRepository.findById(10000L)).thenReturn(Optional.of(tarom));
        when(airlineRepository.findByIcaoCode("ROT")).thenReturn(Optional.of(tarom));

        AirlineResponse updated =
                airlineService.update(10000L, new AirlineRequest("TAROM Romanian Air Transport", "ROT"));

        assertThat(updated).isEqualTo(new AirlineResponse(10000L, "TAROM Romanian Air Transport", "ROT"));
    }

    @Test
    void deleteRemovesAnExistingAirline() {
        when(airlineRepository.findById(10000L)).thenReturn(Optional.of(tarom));

        airlineService.delete(10000L);

        verify(airlineRepository).delete(tarom);
    }

    @Test
    void deleteThrowsNotFoundAndDeletesNothingForUnknownId() {
        when(airlineRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> airlineService.delete(1L)).isInstanceOf(ResourceNotFoundException.class);
        verify(airlineRepository, never()).delete(any(Airline.class));
    }
}
