package com.codecool.flightapi.flight;

import com.codecool.flightapi.common.error.ResourceNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlightService {

    private final FlightRepository flightRepository;

    public List<Flight> findAll() {
        return flightRepository.findAll();
    }

    /**
     * Returns the entity with the given id.
     *
     * @throws com.codecool.flightapi.common.error.ResourceNotFoundException if absent
     */
    public Flight findById(Long id) {
        return flightRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.byId("Flight", id));
    }

    /**
     * Flights on {@code date} from one airport to another. Each airport is given either as its IATA code
     * ({@code OTP}) or as part of the airport or city name ({@code henri}, {@code London}), ignoring case.
     */
    public List<Flight> search(String from, String to, LocalDate date) {
        return flightRepository.search(iataCode(from), containsPattern(from), iataCode(to), containsPattern(to), date);
    }

    private static String iataCode(String term) {
        return term.strip().toUpperCase(Locale.ROOT);
    }

    /** {@code %term%} in lower case, with LIKE wildcards in the term escaped so they match literally. */
    private static String containsPattern(String term) {
        String escaped = term.strip()
                .toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    /** Creates a new flight; any client-supplied id is ignored. */
    @Transactional
    public Flight create(Flight flight) {
        flight.setFlight_id(null); // never let a client-supplied id overwrite an existing row
        return flightRepository.save(flight);
    }

    @Transactional
    public Flight update(Long id, Flight changes) {
        Flight flight = findById(id);
        flight.setDepartureDate(changes.getDepartureDate());
        flight.setDepartureTime(changes.getDepartureTime());
        flight.setArrivalTime(changes.getArrivalTime());
        flight.setPrice(changes.getPrice());
        return flight;
    }

    @Transactional
    public void delete(Long id) {
        flightRepository.delete(findById(id));
    }
}
