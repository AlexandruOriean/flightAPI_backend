package com.codecool.flightapi.city;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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

@ExtendWith(MockitoExtension.class)
class CityServiceTest {

    @Mock
    private CityRepository cityRepository;

    private CityService cityService;

    private final City london = new City(21L, "London");

    @BeforeEach
    void setUp() {
        cityService = new CityService(cityRepository, new CityMapperImpl());
    }

    @Test
    void findAllReturnsTheRequestedPageOrderedById() {
        // whatever sort the client asks for, the page is read in id order
        Pageable byId = PageRequest.of(0, 2, Sort.by("id"));
        when(cityRepository.findAll(byId))
                .thenReturn(new PageImpl<>(List.of(new City(12L, "Bucuresti"), london), byId, 3));

        Page<CityResponse> page = cityService.findAll(PageRequest.of(0, 2, Sort.by("name")));

        assertThat(page.getContent())
                .containsExactly(new CityResponse(12L, "Bucuresti"), new CityResponse(21L, "London"));
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void findByIdReturnsTheCity() {
        when(cityRepository.findById(21L)).thenReturn(Optional.of(london));

        assertThat(cityService.findById(21L)).isEqualTo(new CityResponse(21L, "London"));
    }

    @Test
    void findByIdThrowsNotFoundForUnknownId() {
        when(cityRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cityService.findById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("City with id '1' was not found");
    }

    @Test
    void getByNameIgnoresCaseAndSurroundingWhitespace() {
        when(cityRepository.findByNameKey("london")).thenReturn(Optional.of(london));

        assertThat(cityService.getByName("  LONDON ")).isSameAs(london);
    }

    @Test
    void getByNameThrowsNotFoundWithTheNameAsGiven() {
        when(cityRepository.findByNameKey("paris")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cityService.getByName("Paris"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("City with name 'Paris' was not found");
    }

    @Test
    void createSavesTheCityWithItsNameKey() {
        when(cityRepository.findByNameKey("paris")).thenReturn(Optional.empty());
        when(cityRepository.save(any(City.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CityResponse created = cityService.create(new CityRequest("Paris"));

        assertThat(created).isEqualTo(new CityResponse(null, "Paris"));
        verify(cityRepository).save(argThat(city -> "paris".equals(city.getNameKey())));
    }

    @Test
    void createRejectsANameThatDiffersOnlyByCase() {
        when(cityRepository.findByNameKey("london")).thenReturn(Optional.of(london));

        assertThatThrownBy(() -> cityService.create(new CityRequest("LONDON")))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessage("City 'LONDON' already exists.");
        verify(cityRepository, never()).save(any(City.class));
    }

    @Test
    void updateMayChangeTheCaseOfItsOwnName() {
        when(cityRepository.findById(21L)).thenReturn(Optional.of(london));
        when(cityRepository.findByNameKey("london")).thenReturn(Optional.of(london));

        CityResponse updated = cityService.update(21L, new CityRequest("LONDON"));

        assertThat(updated).isEqualTo(new CityResponse(21L, "LONDON"));
        assertThat(london.getNameKey()).isEqualTo("london");
    }

    @Test
    void updateRejectsTheNameOfAnotherCity() {
        when(cityRepository.findById(21L)).thenReturn(Optional.of(london));
        when(cityRepository.findByNameKey("bucuresti")).thenReturn(Optional.of(new City(12L, "Bucuresti")));

        assertThatThrownBy(() -> cityService.update(21L, new CityRequest("Bucuresti")))
                .isInstanceOf(ResourceAlreadyExistsException.class);
        assertThat(london.getName()).isEqualTo("London");
    }

    @Test
    void updateThrowsNotFoundForUnknownId() {
        when(cityRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cityService.update(1L, new CityRequest("Paris")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRemovesAnExistingCity() {
        when(cityRepository.findById(21L)).thenReturn(Optional.of(london));

        cityService.delete(21L);

        verify(cityRepository).delete(london);
    }

    @Test
    void deleteThrowsNotFoundAndDeletesNothingForUnknownId() {
        when(cityRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cityService.delete(1L)).isInstanceOf(ResourceNotFoundException.class);
        verify(cityRepository, never()).delete(any(City.class));
    }
}
