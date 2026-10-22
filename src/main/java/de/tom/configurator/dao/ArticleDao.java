package de.tom.configurator.dao;

import de.tom.configurator.config.DatabaseConfig;
import de.tom.configurator.model.Article;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DAO for article master data records.
 */
public class ArticleDao extends BaseDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArticleDao.class);

    public ArticleDao() {
        super();
    }

    public ArticleDao(DatabaseConfig databaseConfig) {
        super(databaseConfig);
    }

    public Optional<Article> findBySku(String sku) {
        if (sku == null || sku.isBlank()) return Optional.empty();

        String sql = "SELECT sku, name, standard_weight_g, category, is_active FROM articles WHERE sku = ?";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, sku.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to query article for SKU: " + sku, e);
        }
        return Optional.empty();
    }

    public List<Article> findAll() {
        List<Article> articles = new ArrayList<>();
        String sql = "SELECT sku, name, standard_weight_g, category, is_active FROM articles ORDER BY sku ASC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                articles.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to query articles", e);
        }
        return articles;
    }

    public boolean save(Article article) {
        String sql = "MERGE INTO articles (sku, name, standard_weight_g, category, is_active) KEY (sku) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, article.getSku());
            ps.setString(2, article.getName());
            ps.setInt(3, article.getStandardWeightG());
            ps.setString(4, article.getCategory());
            ps.setBoolean(5, article.isActive());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("Failed to save article: " + article.getSku(), e);
        }
        return false;
    }

    private Article mapResultSet(ResultSet rs) throws SQLException {
        return new Article(
                rs.getString("sku"),
                rs.getString("name"),
                rs.getInt("standard_weight_g"),
                rs.getString("category"),
                rs.getBoolean("is_active")
        );
    }
}
