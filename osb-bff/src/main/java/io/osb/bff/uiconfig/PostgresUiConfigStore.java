package io.osb.bff.uiconfig;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

@ApplicationScoped
public class PostgresUiConfigStore implements UiConfigStore {

    private final AgroalDataSource dataSource;

    @Inject
    public PostgresUiConfigStore(AgroalDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<String> find(String userName, String key) {
        final String sql =
                "SELECT payload::text FROM bff_ui_config WHERE user_name = ? AND config_key = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userName);
            statement.setString(2, key);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.ofNullable(rs.getString(1));
            }
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to load UI config for user=" + userName + " key=" + key, e);
        }
    }

    @Override
    public void save(String userName, String key, String json) {
        final String sql =
                """
                INSERT INTO bff_ui_config (user_name, config_key, payload, updated_at)
                VALUES (?, ?, CAST(? AS jsonb), NOW())
                ON CONFLICT (user_name, config_key)
                DO UPDATE SET payload = EXCLUDED.payload, updated_at = NOW()
                """;
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userName);
            statement.setString(2, key);
            statement.setString(3, json);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to save UI config for user=" + userName + " key=" + key, e);
        }
    }

    @Override
    public boolean delete(String userName, String key) {
        final String sql = "DELETE FROM bff_ui_config WHERE user_name = ? AND config_key = ?";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, userName);
            statement.setString(2, key);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Failed to delete UI config for user=" + userName + " key=" + key, e);
        }
    }
}
