package com.codecool.flight_api_project.airplane;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;

@ExtendWith({SpringExtension.class})
@SpringBootTest
public class AirplaneRepositoryTest {

        @Autowired
        private AirplaneRepository airplaneRepository;

        private static File DATA_JSON = Paths.get("src", "test", "resources", "airplanes.json").toFile();

        @BeforeEach
        public void setup() throws IOException {
            // Deserialize products from JSON file to Product array
            Airplane[] airplanes = new ObjectMapper().readValue(DATA_JSON, Airplane[].class);

            // Save each product to database
            Arrays.stream(airplanes).forEach(airplaneRepository::save);
            for(Airplane air : airplaneRepository.findAll())
            {
                System.out.println("*****" + air + "****");
            }
        }

        @AfterEach
        public void cleanup(){
            // Cleanup the database after each test
            airplaneRepository.deleteAll();
        }

        @Test
        @DisplayName("Test airplane not found with non-existing id")
        public void testAirplaneNotFoundForNonExistingId(){
            // Given five products in the database

            // When
            Airplane retrievedAirplane = airplaneRepository.findAirplaneById(1L);

            // Then
            Assertions.assertNull(retrievedAirplane, "Airplane with id 999 should not exist");
        }

        @Test
        @DisplayName("Test airplane saved successfully")
        public void testAirplaneSavedSuccessfully(){
            // Prepare mock product
            Airplane newAirplane = new Airplane(6L, "Boeing", "737MAX", 180L, 800);

            // When
            Airplane savedAirplane = airplaneRepository.save(newAirplane);

            // Then
            Assertions.assertNotNull(savedAirplane, "Product should be saved");
            Assertions.assertNotNull(savedAirplane.getId(), "Product should have an id when saved");
            Assertions.assertEquals(newAirplane.getManufacturer(), savedAirplane.getManufacturer());
        }

        @Test
        @DisplayName("Test product updated successfully")
        public void testProductUpdatedSuccessfully(){
            // Prepare the product
            Airplane airplaneToUpdate = new Airplane(1L, "Boeing", "737MAX", 180L, 800);

            // When
            Airplane updatedAirplane = airplaneRepository.save(airplaneToUpdate);

            // Then
            Assertions.assertEquals(airplaneToUpdate.getModel(), updatedAirplane.getModel());
            Assertions.assertEquals(800, updatedAirplane.getSpeed());
            Assertions.assertEquals(180L, updatedAirplane.getNumberOfSeats());
        }

        @Test
        @DisplayName("Test product deleted successfully")
        public void testProductDeletedSuccessfully(){
            // Given five products in the database

            // When
            airplaneRepository.deleteById(1L);

            // Then
            Assertions.assertEquals(4, airplaneRepository.findAll().size());
        }
    }
