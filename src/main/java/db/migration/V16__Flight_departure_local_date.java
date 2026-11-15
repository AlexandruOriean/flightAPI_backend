package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * {@code flight.departure_local_date}: the calendar day a flight departs in its departure airport's time zone,
 * indexed, so the flight search is one exact query instead of a window of UTC instants filtered afterwards.
 *
 * <p>Written in Java so existing rows get exactly the date the application computes ({@code Flight}), using the
 * JDK's time-zone rules rather than the database's, which H2 and PostgreSQL implement differently.
 */
public class V16__Flight_departure_local_date extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        execute(connection, "ALTER TABLE flight ADD COLUMN departure_local_date DATE");

        try (Statement select = connection.createStatement();
                ResultSet rs = select.executeQuery("SELECT f.flight_id, f.departure_at, a.time_zone FROM flight f"
                        + " JOIN airport a ON a.airport_id = f.departure_airport_id");
                PreparedStatement update =
                        connection.prepareStatement("UPDATE flight SET departure_local_date = ? WHERE flight_id = ?")) {
            while (rs.next()) {
                OffsetDateTime departure = rs.getObject("departure_at", OffsetDateTime.class);
                ZoneId zone = ZoneId.of(rs.getString("time_zone"));
                update.setObject(1, localDate(departure, zone));
                update.setLong(2, rs.getLong("flight_id"));
                update.addBatch();
            }
            update.executeBatch();
        }

        execute(connection, "ALTER TABLE flight ALTER COLUMN departure_local_date SET NOT NULL");
        execute(connection, "CREATE INDEX idx_flight_departure_local_date ON flight (departure_local_date)");
    }

    /** Must match {@code Flight}'s computation at the time of this migration. */
    static LocalDate localDate(OffsetDateTime departure, ZoneId zone) {
        return departure.atZoneSameInstant(zone).toLocalDate();
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
