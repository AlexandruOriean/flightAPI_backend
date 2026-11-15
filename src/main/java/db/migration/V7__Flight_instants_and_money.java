package db.migration;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Flights get real points in time and a price with currency (REFACTORING_ROADMAP task 20).
 *
 * <ul>
 *   <li>{@code airport.time_zone}: IANA zone of each airport.
 *   <li>{@code flight.departure_at/arrival_at} replace {@code departure_date}, {@code departure_time} and
 *       {@code arrival_time}. The old values were local times at the respective airport; an arrival
 *       time not after the departure time means the flight lands on a later day.
 *   <li>{@code flight.price_amount/price_currency} replace {@code price}: same amount, in the currency
 *       of the departure airport's country.
 * </ul>
 *
 * <p>Written in Java because time-zone arithmetic in SQL differs between H2 and PostgreSQL. Zone and
 * currency are only known for the airports below; the migration stops if it meets any other airport
 * rather than guessing - add it to {@link #AIRPORTS} first.
 */
public class V7__Flight_instants_and_money extends BaseJavaMigration {

    private record AirportFacts(String timeZone, String currency) {}

    private static final Map<String, AirportFacts> AIRPORTS = Map.of(
            "OTP", new AirportFacts("Europe/Bucharest", "RON"),
            "TSR", new AirportFacts("Europe/Bucharest", "RON"),
            "LHR", new AirportFacts("Europe/London", "GBP"),
            "CDG", new AirportFacts("Europe/Paris", "EUR"));

    /**
     * Realistic block times for the V2 seed flights, which all "arrive" at 06:30 after departing at
     * 09:30 (see the roadmap follow-ups). Only applied to rows that still hold exactly those values.
     */
    private static final Map<String, Duration> SEED_ROUTE_DURATIONS = Map.of(
            "LHR-OTP", Duration.ofMinutes(195),
            "OTP-LHR", Duration.ofMinutes(215),
            "CDG-OTP", Duration.ofMinutes(180),
            "OTP-CDG", Duration.ofMinutes(195),
            "TSR-LHR", Duration.ofMinutes(175));

    private static final LocalDate SEED_DATE = LocalDate.of(2020, 11, 1);
    private static final LocalTime SEED_DEPARTURE = LocalTime.of(9, 30);
    private static final LocalTime SEED_ARRIVAL = LocalTime.of(6, 30);

    private record AirportRow(String iataCode, ZoneId zone, String currency) {}

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        Map<Long, AirportRow> airports = addAirportTimeZones(connection);
        convertFlights(connection, airports);
    }

    private static Map<Long, AirportRow> addAirportTimeZones(Connection connection) throws SQLException {
        execute(connection, "ALTER TABLE airport ADD COLUMN time_zone VARCHAR(64)");

        Map<Long, AirportRow> airports = new HashMap<>();
        List<String> unknown = new ArrayList<>();
        try (Statement select = connection.createStatement();
                ResultSet rs = select.executeQuery("SELECT airport_id, airport_iata_code FROM airport")) {
            while (rs.next()) {
                String iataCode = rs.getString("airport_iata_code");
                AirportFacts facts = AIRPORTS.get(iataCode);
                if (facts == null) {
                    unknown.add(iataCode);
                } else {
                    airports.put(
                            rs.getLong("airport_id"),
                            new AirportRow(iataCode, ZoneId.of(facts.timeZone()), facts.currency()));
                }
            }
        }
        if (!unknown.isEmpty()) {
            throw new IllegalStateException("V7: no time zone and currency known for airports " + unknown
                    + "; add them to V7__Flight_instants_and_money.AIRPORTS before migrating");
        }

        try (PreparedStatement update =
                connection.prepareStatement("UPDATE airport SET time_zone = ? WHERE airport_id = ?")) {
            for (Map.Entry<Long, AirportRow> airport : airports.entrySet()) {
                update.setString(1, airport.getValue().zone().getId());
                update.setLong(2, airport.getKey());
                update.addBatch();
            }
            update.executeBatch();
        }
        execute(connection, "ALTER TABLE airport ALTER COLUMN time_zone SET NOT NULL");
        return airports;
    }

    private static void convertFlights(Connection connection, Map<Long, AirportRow> airports) throws SQLException {
        execute(connection, "ALTER TABLE flight ADD COLUMN departure_at TIMESTAMP WITH TIME ZONE");
        execute(connection, "ALTER TABLE flight ADD COLUMN arrival_at TIMESTAMP WITH TIME ZONE");
        execute(connection, "ALTER TABLE flight ADD COLUMN price_amount NUMERIC(12, 2)");
        execute(connection, "ALTER TABLE flight ADD COLUMN price_currency VARCHAR(3)");

        try (Statement select = connection.createStatement();
                ResultSet rs = select.executeQuery("SELECT flight_id, departure_date, departure_time, arrival_time,"
                        + " price, departure_airport_id, arrival_airport_id FROM flight");
                PreparedStatement update = connection.prepareStatement("UPDATE flight SET departure_at = ?,"
                        + " arrival_at = ?, price_amount = ?, price_currency = ? WHERE flight_id = ?")) {
            while (rs.next()) {
                LocalDate date = rs.getObject("departure_date", LocalDate.class);
                LocalTime departureTime = rs.getObject("departure_time", LocalTime.class);
                LocalTime arrivalTime = rs.getObject("arrival_time", LocalTime.class);
                AirportRow from = airports.get(rs.getLong("departure_airport_id"));
                AirportRow to = airports.get(rs.getLong("arrival_airport_id"));

                Instant departure =
                        date.atTime(departureTime).atZone(from.zone()).toInstant();
                Instant arrival = arrival(date, departureTime, arrivalTime, departure, from, to);

                update.setObject(1, departure.atOffset(ZoneOffset.UTC));
                update.setObject(2, arrival.atOffset(ZoneOffset.UTC));
                update.setBigDecimal(
                        3, BigDecimal.valueOf(rs.getLong("price"), 0).setScale(2));
                update.setString(4, from.currency());
                update.setLong(5, rs.getLong("flight_id"));
                update.addBatch();
            }
            update.executeBatch();
        }

        for (String column : List.of("departure_at", "arrival_at", "price_amount", "price_currency")) {
            execute(connection, "ALTER TABLE flight ALTER COLUMN " + column + " SET NOT NULL");
        }
        for (String column : List.of("departure_date", "departure_time", "arrival_time", "price")) {
            execute(connection, "ALTER TABLE flight DROP COLUMN " + column);
        }
    }

    private static Instant arrival(
            LocalDate date,
            LocalTime departureTime,
            LocalTime arrivalTime,
            Instant departure,
            AirportRow from,
            AirportRow to) {
        Duration seedDuration = SEED_ROUTE_DURATIONS.get(from.iataCode() + "-" + to.iataCode());
        if (seedDuration != null
                && date.equals(SEED_DATE)
                && departureTime.equals(SEED_DEPARTURE)
                && arrivalTime.equals(SEED_ARRIVAL)) {
            return departure.plus(seedDuration);
        }
        Instant arrival = date.atTime(arrivalTime).atZone(to.zone()).toInstant();
        while (!arrival.isAfter(departure)) {
            arrival = arrival.plus(Duration.ofDays(1)); // overnight: lands on a later day
        }
        return arrival;
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
