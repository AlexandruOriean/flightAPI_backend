package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * City names are unique regardless of case: {@code city.city_name_key} holds the trimmed, lower-case name
 * under a unique constraint, replacing the case-sensitive {@code uk_city_name} from V5. Lookups by name use
 * the key, so they can use its index.
 *
 * <p>Written in Java so existing rows get exactly the key the application computes ({@code City.key}),
 * rather than the database's {@code LOWER()}, which can differ for non-ASCII letters. Stops if two
 * existing cities differ only by case or surrounding whitespace: merge or rename one, then migrate again.
 */
public class V11__City_name_key extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        execute(connection, "ALTER TABLE city ADD COLUMN city_name_key VARCHAR(255)");

        Map<String, List<String>> namesByKey = new HashMap<>();
        try (Statement select = connection.createStatement();
                ResultSet rs = select.executeQuery("SELECT city_id, city_name FROM city");
                PreparedStatement update =
                        connection.prepareStatement("UPDATE city SET city_name_key = ? WHERE city_id = ?")) {
            while (rs.next()) {
                String name = rs.getString("city_name");
                // must match City.key at the time of this migration
                String key = name.strip().toLowerCase(Locale.ROOT);
                namesByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(name);
                update.setString(1, key);
                update.setLong(2, rs.getLong("city_id"));
                update.addBatch();
            }
            List<List<String>> duplicates = namesByKey.values().stream()
                    .filter(names -> names.size() > 1)
                    .toList();
            if (!duplicates.isEmpty()) {
                throw new IllegalStateException("V11: cities differ only by case or whitespace: " + duplicates
                        + "; merge or rename them, then migrate again");
            }
            update.executeBatch();
        }

        execute(connection, "ALTER TABLE city ALTER COLUMN city_name_key SET NOT NULL");
        execute(connection, "ALTER TABLE city ADD CONSTRAINT uk_city_name_key UNIQUE (city_name_key)");
        execute(connection, "ALTER TABLE city DROP CONSTRAINT uk_city_name");
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }
}
