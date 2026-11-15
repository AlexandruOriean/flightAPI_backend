package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Set;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;

/**
 * Before migrating a database from before V7, checks that its data can go through V5, V6 and V7, and stops with
 * one report of everything to fix otherwise. Without it, the first problem fails in the middle of an upgrade
 * (with a database error), after the earlier migrations were already applied.
 *
 * <ul>
 *   <li>V5 makes airport IATA codes, airline ICAO codes and city names unique.
 *   <li>V6 makes the columns the API requires {@code NOT NULL}.
 *   <li>V7 needs time zone and currency of every airport ({@link V7__Flight_instants_and_money}).
 * </ul>
 *
 * <p>New databases (no schema history yet) and databases at V7 or later are not checked.
 */
public class LegacyDataCheck implements Callback {

    /** Columns V6 makes {@code NOT NULL}, as {@code table.column}. */
    private static final List<String> V6_REQUIRED_COLUMNS = List.of(
            "city.city_name",
            "airport.airport_name",
            "airport.airport_iata_code",
            "airport.city_id",
            "airplane.manufacturer",
            "airplane.model",
            "airplane.number_of_seats",
            "flight.departure_date",
            "flight.departure_time",
            "flight.arrival_time",
            "flight.price",
            "flight.airline_id",
            "flight.departure_airport_id",
            "flight.arrival_airport_id");

    @Override
    public boolean supports(Event event, Context context) {
        return event == Event.BEFORE_MIGRATE;
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return true;
    }

    @Override
    public void handle(Event event, Context context) {
        try {
            Connection connection = context.getConnection();
            OptionalInt version =
                    currentVersion(connection, context.getConfiguration().getTable());
            if (version.isEmpty() || version.getAsInt() >= 7) {
                return;
            }
            List<String> problems = problems(
                    connection,
                    version.getAsInt(),
                    V7__Flight_instants_and_money.knownAirportCodes(
                            context.getConfiguration().getPlaceholders()));
            if (!problems.isEmpty()) {
                throw new IllegalStateException("The database is at V" + version.getAsInt()
                        + " and holds data the pending migrations cannot convert; nothing was migrated."
                        + " Fix the data (see README, \"Upgrading an old database\"), then start again:\n - "
                        + String.join("\n - ", problems));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not check the existing data before migrating", e);
        }
    }

    @Override
    public String getCallbackName() {
        return "LegacyDataCheck";
    }

    private static List<String> problems(Connection connection, int version, Set<String> knownAirports)
            throws SQLException {
        List<String> problems = new ArrayList<>();
        if (version < 5) {
            duplicates(
                    connection, "airport", "airport_iata_code", "V5: airport IATA codes used more than once", problems);
            duplicates(
                    connection, "airline", "iso", "V5: airline ICAO codes (column iso) used more than once", problems);
            duplicates(connection, "city", "city_name", "V5: city names used more than once", problems);
        }
        if (version < 6) {
            for (String column : V6_REQUIRED_COLUMNS) {
                long rows = count(connection, "SELECT COUNT(*) FROM " + column.replace(".", " WHERE ") + " IS NULL");
                if (rows > 0) {
                    problems.add("V6: " + column + " is empty in " + rows + " row(s); it becomes required");
                }
            }
        }
        List<String> unknown = new ArrayList<>();
        for (String code :
                strings(connection, "SELECT DISTINCT airport_iata_code FROM airport ORDER BY airport_iata_code")) {
            if (code != null && !knownAirports.contains(code)) {
                unknown.add(code);
            }
        }
        if (!unknown.isEmpty()) {
            problems.add("V7: no time zone and currency for airports " + unknown + "; set LEGACY_AIRPORTS, e.g. "
                    + unknown.getFirst() + ":Europe/Berlin:EUR");
        }
        return problems;
    }

    /** The highest successfully applied version, or empty for a database Flyway has not migrated yet. */
    private static OptionalInt currentVersion(Connection connection, String historyTable) throws SQLException {
        if (!tableExists(connection, historyTable)) {
            return OptionalInt.empty();
        }
        return strings(connection, "SELECT \"version\" FROM \"" + historyTable + "\" WHERE \"success\" = TRUE").stream()
                .filter(version -> version != null && version.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max();
    }

    /** Through the metadata, because a failed query would abort the transaction on PostgreSQL. */
    private static boolean tableExists(Connection connection, String table) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        for (String name : List.of(table, table.toLowerCase(Locale.ROOT), table.toUpperCase(Locale.ROOT))) {
            try (ResultSet tables = metaData.getTables(null, null, name, null)) {
                if (tables.next()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void duplicates(
            Connection connection, String table, String column, String problem, List<String> problems)
            throws SQLException {
        List<String> values = strings(
                connection,
                "SELECT " + column + " FROM " + table + " WHERE " + column + " IS NOT NULL GROUP BY " + column
                        + " HAVING COUNT(*) > 1 ORDER BY " + column);
        if (!values.isEmpty()) {
            problems.add(problem + ": " + values);
        }
    }

    private static long count(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static List<String> strings(Connection connection, String sql) throws SQLException {
        List<String> values = new ArrayList<>();
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                values.add(rs.getString(1));
            }
        }
        return values;
    }
}
