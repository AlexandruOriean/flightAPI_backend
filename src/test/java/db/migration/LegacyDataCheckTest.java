package db.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** {@link LegacyDataCheck} on H2 databases migrated up to V4, as old deployments would be. */
class LegacyDataCheckTest {

    @Test
    @DisplayName("data V5, V6 and V7 cannot convert stops the upgrade with one report, before any migration")
    void reportsEveryProblemBeforeMigrating() throws SQLException {
        String url = database("legacy-broken");
        flyway(url, Map.of()).target(MigrationVersion.fromVersion("4")).load().migrate();
        execute(url, "INSERT INTO airport (airport_name, airport_iata_code, city_id) VALUES ('Nowhere', 'XYZ', 1)");
        execute(url, "INSERT INTO airport (airport_name, airport_iata_code, city_id) VALUES ('Nowhere 2', 'XYZ', 1)");
        execute(url, "INSERT INTO city (city_name) VALUES ('London')");
        execute(url, "UPDATE flight SET price = NULL WHERE flight_id = 1");

        assertThatThrownBy(() -> flyway(url, Map.of()).load().migrate())
                .rootCause()
                .hasMessageContaining("The database is at V4")
                .hasMessageContaining("nothing was migrated")
                .hasMessageContaining("V5: airport IATA codes used more than once: [XYZ]")
                .hasMessageContaining("V5: city names used more than once: [London]")
                .hasMessageContaining("V6: flight.price is empty in 1 row(s)")
                .hasMessageContaining("V7: no time zone and currency for airports [XYZ]");
        assertThat(version(url)).isEqualTo("4");
    }

    @Test
    @DisplayName("airports V7 does not know can be given in LEGACY_AIRPORTS; the upgrade then runs to the end")
    void extraAirportsLetV7ConvertThem() throws SQLException {
        String url = database("legacy-extra-airport");
        flyway(url, Map.of()).target(MigrationVersion.fromVersion("4")).load().migrate();
        execute(
                url,
                "INSERT INTO airport (airport_name, airport_iata_code, city_id) VALUES ('Berlin Brandenburg', 'BER', 1)");

        assertThatThrownBy(() -> flyway(url, Map.of()).load().migrate())
                .rootCause()
                .hasMessageContaining("V7: no time zone and currency for airports [BER]");

        flyway(url, Map.of(V7__Flight_instants_and_money.EXTRA_AIRPORTS, "BER:Europe/Berlin:EUR"))
                .load()
                .migrate();

        assertThat(single(url, "SELECT time_zone FROM airport WHERE airport_iata_code = 'BER'"))
                .isEqualTo("Europe/Berlin");
        assertThat(version(url)).isNotEqualTo("4");
    }

    @Test
    @DisplayName("a mistyped LEGACY_AIRPORTS entry stops V7 instead of storing an unusable zone")
    void invalidExtraAirportIsRejected() throws SQLException {
        String url = database("legacy-typo");
        flyway(url, Map.of()).target(MigrationVersion.fromVersion("4")).load().migrate();

        assertThatThrownBy(() -> flyway(url, Map.of(V7__Flight_instants_and_money.EXTRA_AIRPORTS, "BER-Europe/Berlin"))
                        .load()
                        .migrate())
                .rootCause()
                .hasMessageContaining("is not IATA:zone:currency");
    }

    @Test
    @DisplayName("new databases and clean old ones migrate as before")
    void cleanDatabasesMigrate() throws SQLException {
        String fresh = database("legacy-fresh");
        flyway(fresh, Map.of()).load().migrate();

        String old = database("legacy-clean");
        flyway(old, Map.of()).target(MigrationVersion.fromVersion("4")).load().migrate();
        flyway(old, Map.of()).load().migrate();

        assertThat(version(old)).isEqualTo(version(fresh));
    }

    private static FluentConfiguration flyway(String url, Map<String, String> placeholders) {
        return Flyway.configure()
                .dataSource(url, "sa", "")
                .placeholders(placeholders)
                .callbacks(new LegacyDataCheck());
    }

    private static String database(String name) {
        return "jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1";
    }

    private static String version(String url) throws SQLException {
        return single(
                url,
                "SELECT \"version\" FROM \"flyway_schema_history\" WHERE \"success\""
                        + " ORDER BY \"installed_rank\" DESC LIMIT 1");
    }

    private static String single(String url, String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getString(1);
        }
    }

    private static void execute(String url, String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
