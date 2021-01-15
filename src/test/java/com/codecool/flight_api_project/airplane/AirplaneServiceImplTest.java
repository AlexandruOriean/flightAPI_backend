package com.codecool.flight_api_project.airplane;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.AssertionErrors;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class AirplaneServiceImplTest {

    @Autowired
    AirplaneService airplaneService;

    @MockBean
    AirplaneRepository airplaneRepository;

    @Test
    @DisplayName("Add new airport successfully")
    public void testAddAirplane() {
            Airplane mockAirplane = new Airplane(1L, "Airbus", "A380", 853L, 1185);
            doReturn(mockAirplane).when(airplaneRepository).save(any());
            Airplane savedAirplane = airplaneService.addAirplane(mockAirplane);
            AssertionErrors.assertTrue("Product should not be null", savedAirplane != null);
            Assertions.assertSame("Airbus", mockAirplane.getManufacturer());
    }

    @Test
    @DisplayName("Find all airplanes")
    void testGetAllAirplanes() {
        Airplane mockAirplaneOne = new Airplane(1L, "Airbus", "A380", 853L, 1185);
        Airplane mockAirplaneTwo =  new Airplane(2L, "Boeing", "737MAX", 180L, 800);
        List<Airplane> addedAirplanes = new ArrayList<>();
        addedAirplanes.add(mockAirplaneOne);
        addedAirplanes.add(mockAirplaneTwo);

        when(airplaneRepository.findAll()).thenReturn(addedAirplanes);
        Assertions.assertEquals(2, airplaneService.getAllAirplanes().size());
    }


    @Test
    @DisplayName("Find airplane by id successfully")
    public void testGetAirplaneById() {
        Long id = 1L;
        Airplane mockAirplane = new Airplane(1L, "Airbus", "A380", 853L, 1185);
        doReturn(mockAirplane).when(airplaneRepository).findAirplaneById(id);
        Airplane foundAirplane = airplaneService.getAirplaneById(id);
        Assertions.assertNotNull(foundAirplane);
        Assertions.assertSame(id, foundAirplane.getId());
    }


    @Test
    @DisplayName("Fail to find airplane with id")
    public void testFailToFindAirplaneById(){
        doReturn(null).when(airplaneRepository).findAirplaneById(1L);
        Airplane foundAirplane = airplaneService.getAirplaneById(1L);
        Assertions.assertNull(foundAirplane);
    }

    @Test
    @DisplayName("Delete an existing airplane successfully")
    void testDeleteAirplaneByID() {
        Long airplaneId = 1L;
        airplaneService.deleteAirplaneByID(airplaneId);
        verify(airplaneRepository, times(1)).deleteById(airplaneId);
    }


    @Test
    @DisplayName("Update an existing airplane successfully")
    void testUpdateAirplaneByIdSuccessfully() {
        Airplane existingAirplane = new Airplane(1L, "Airbus", "A380", 853L, 1185);
        Airplane updatedAirplane = new Airplane(1L, "Boeing", "737MAX", 180L, 800);
        doReturn(existingAirplane).when(airplaneRepository).findAirplaneById(1L);
        doReturn(updatedAirplane).when(airplaneRepository).save(existingAirplane);
        Airplane updateAirplane = airplaneService.updateAirplaneById(existingAirplane.getId(), existingAirplane);
        Assertions.assertEquals("Boeing", updateAirplane.getManufacturer());
    }

    @Test
    @DisplayName("Fail to update an existing airplane")
    public void testFailToUpdateExistingAirplane(){
        Airplane mockAirplane = new Airplane(1L, "Airbus", "A380", 853L, 1185);

        doReturn(null).when(airplaneRepository).findAirplaneById(1L);

        Airplane updatedAirplane = airplaneService.updateAirplaneById(mockAirplane.getId(), mockAirplane);

        AssertionErrors.assertTrue("Airplane should be null", updatedAirplane == null);
    }
}