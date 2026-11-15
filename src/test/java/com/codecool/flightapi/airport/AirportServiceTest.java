package com.codecool.flightapi.airport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.codecool.flightapi.city.City;
import com.codecool.flightapi.city.CityMapperImpl;
import com.codecool.flightapi.city.CityResponse;
import com.codecool.flightapi.city.CityService;
import com.codecool.flightapi.common.error.ResourceAlreadyExistsException;
import com.codecool.flightapi.common.error.ResourceNotFoundException;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class AirportServiceTest {

    @Mock
    private AirportRepository airportRepository;

    @Mock
    private CityService cityService;

    @Mock
    private ApplicationEventPublisher events;

    private AirportService airportService;

    private final City bucuresti = new City(12L, "Bucuresti");
    private final City london = new City(21L, "London");
    private final Airport otopeni = new Airport(1L, "OTP", "Henri Coanda", bucuresti, ZoneId.of("Europe/Bucharest"));

    private final AirportResponse otopeniResponse =
            new AirportResponse(1L, "OTP", "Henri Coanda", new CityResponse(12L, "Bucuresti"), "Europe/Bucharest");

    @BeforeEach
    void setUp() {
        airportService =
                new AirportService(airportRepository, new AirportMapperImpl(new CityMapperImpl()), cityService, events);
    }

    @Test
    void findAllReturnsTheRequestedPageOrderedById() {
        Pageable byId = PageRequest.of(0, 5, Sort.by("id"));
        when(airportRepository.findAll(byId)).thenReturn(new PageImpl<>(List.of(otopeni), byId, 1));

        Page<AirportResponse> page = airportService.findAll(PageRequest.of(0, 5, Sort.by("name")));

        assertThat(page.getContent()).containsExactly(otopeniResponse);
    }

    @Test
    void findByIdMapsCityAndTimeZone() {
        when(airportRepository.findById(1L)).thenReturn(Optional.of(otopeni));

        assertThat(airportService.findById(1L)).isEqualTo(otopeniResponse);
    }

    @Test
    void findByIdThrowsNotFoundForUnknownId() {
        when(airportRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> airportService.findById(9L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Airport with id '9' was not found");
    }

    @Test
    void findByIataCodeIgnoresCaseAndWhitespace() {
        when(airportRepository.findByIataCode("OTP")).thenReturn(Optional.of(otopeni));

        assertThat(airportService.findByIataCode(" otp ")).isEqualTo(otopeniResponse);
    }

    @Test
    void getByIataCodeThrowsNotFoundWithTheCodeAsGiven() {
        when(airportRepository.findByIataCode("XXX")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> airportService.getByIataCode("xxx"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Airport with IATA code 'xxx' was not found");
    }

    @Test
    void findAllNamesComesFromTheRepository() {
        when(airportRepository.findAllNames()).thenReturn(List.of("Heathrow", "Henri Coanda"));

        assertThat(airportService.findAllNames()).containsExactly("Heathrow", "Henri Coanda");
    }

    @Test
    void createResolvesTheCityByName() {
        when(airportRepository.findByIataCode("LHR")).thenReturn(Optional.empty());
        when(cityService.getByName("london")).thenReturn(london);
        when(airportRepository.save(any(Airport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AirportResponse created =
                airportService.create(new AirportRequest("LHR", "Heathrow", "london", "Europe/London"));

        assertThat(created)
                .isEqualTo(
                        new AirportResponse(null, "LHR", "Heathrow", new CityResponse(21L, "London"), "Europe/London"));
    }

    @Test
    void createRejectsATakenIataCode() {
        when(airportRepository.findByIataCode("OTP")).thenReturn(Optional.of(otopeni));

        assertThatThrownBy(() ->
                        airportService.create(new AirportRequest("OTP", "Other", "Bucuresti", "Europe/Bucharest")))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessage("An airport with IATA code 'OTP' already exists.");
        verify(airportRepository, never()).save(any(Airport.class));
    }

    @Test
    void createWithUnknownCityThrowsNotFoundAndSavesNothing() {
        when(airportRepository.findByIataCode("CDG")).thenReturn(Optional.empty());
        when(cityService.getByName("Paris")).thenThrow(new ResourceNotFoundException("City", "name", "Paris"));

        assertThatThrownBy(() ->
                        airportService.create(new AirportRequest("CDG", "Charles de Gaulle", "Paris", "Europe/Paris")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(airportRepository, never()).save(any(Airport.class));
    }

    @Test
    void updateMayKeepItsOwnCodeAndMoveToAnotherCity() {
        when(airportRepository.findById(1L)).thenReturn(Optional.of(otopeni));
        when(airportRepository.findByIataCode("OTP")).thenReturn(Optional.of(otopeni));
        when(cityService.getByName("London")).thenReturn(london);

        AirportResponse updated =
                airportService.update(1L, new AirportRequest("OTP", "Otopeni", "London", "Europe/London"));

        assertThat(updated)
                .isEqualTo(new AirportResponse(1L, "OTP", "Otopeni", new CityResponse(21L, "London"), "Europe/London"));
        assertThat(otopeni.getTimeZone()).isEqualTo(ZoneId.of("Europe/London"));
        verify(events).publishEvent(new AirportTimeZoneChanged(1L));
    }

    @Test
    void updateKeepingTheTimeZonePublishesNothing() {
        when(airportRepository.findById(1L)).thenReturn(Optional.of(otopeni));
        when(airportRepository.findByIataCode("OTP")).thenReturn(Optional.of(otopeni));
        when(cityService.getByName("Bucuresti")).thenReturn(bucuresti);

        airportService.update(1L, new AirportRequest("OTP", "Otopeni", "Bucuresti", "Europe/Bucharest"));

        verifyNoInteractions(events);
    }

    @Test
    void updateRejectsTheCodeOfAnotherAirport() {
        Airport heathrow = new Airport(2L, "LHR", "Heathrow", london, ZoneId.of("Europe/London"));
        when(airportRepository.findById(1L)).thenReturn(Optional.of(otopeni));
        when(airportRepository.findByIataCode("LHR")).thenReturn(Optional.of(heathrow));

        assertThatThrownBy(() -> airportService.update(
                        1L, new AirportRequest("LHR", "Otopeni", "Bucuresti", "Europe/Bucharest")))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        assertThat(otopeni.getIataCode()).isEqualTo("OTP");
    }

    @Test
    void deleteRemovesAnExistingAirport() {
        when(airportRepository.findById(1L)).thenReturn(Optional.of(otopeni));

        airportService.delete(1L);

        verify(airportRepository).delete(otopeni);
    }

    @Test
    void deleteThrowsNotFoundAndDeletesNothingForUnknownId() {
        when(airportRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> airportService.delete(9L)).isInstanceOf(ResourceNotFoundException.class);
        verify(airportRepository, never()).delete(any(Airport.class));
    }
}
