package com.codecool.flight_api_project.airline;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
public class AirlineServiceTests {

    @Autowired
    private AirlineService airlineService;
    @MockBean
    private AirlineRepository airlineRepository;


    @Test
    @DisplayName("Test all airlines found - Service")
    void getAllAirlinesTest() {
        when(airlineRepository.findAll()).thenReturn(
                Stream
                        .of(new Airline(10000L, "Tarom", "TRO"),
                            new Airline(10001L, "Wizz", "WIZZ"),
                            new Airline(10002L, "BlueAir", "BLA"))
                        .collect(Collectors.toList()));
        assertEquals(3, airlineService.getAllAirlines().size());
    }

    @Test
    @DisplayName("Test find airplane by ID - Service")
    void getAirlineByIdTest(){
        Airline airline = new Airline(10000L, "Tarom", "TRO");
        Long airlineId = airline.getId();
        when(airlineRepository.getOne(airlineId)).thenReturn(airline);
        assertEquals(airline, airlineService.findById(airlineId));
    }

    @Test
    @DisplayName("Test add an airline  - Service")
    void addAirlineToDbTest(){
        Airline airline = new Airline(123L,"British", "BRT");
        when(airlineRepository.save(airline)).thenReturn(airline);
        assertEquals(airline, airlineService.saveAirline(airline));
    }

    @Test
    @DisplayName("Test update an existing airline  - Service")
    public void testUpdateAirlineById(){
        Airline existingAirline = new Airline(10000L, "Tarom", "TRO");
        Airline updatedAirline = new Airline(10000L, "Blue Air", "BLU");

        doReturn(existingAirline).when(airlineRepository).getOne((long) 10000);
        doReturn(updatedAirline).when(airlineRepository).save(existingAirline);

        updatedAirline = airlineService.updateAirlineById((long) 10000, existingAirline);

        assertEquals("Blue Air", updatedAirline.getName());
    }

    @Test
    @DisplayName("Test delete an existing airline - Service")
    void deleteAirlineFromDbTest(){
        Long airlineId = 10000L;
        airlineService.deleteAirlineById(airlineId);
        verify(airlineRepository, times(1)).deleteById(airlineId);
    }

}


