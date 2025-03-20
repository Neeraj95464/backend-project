package AssetManagement.AssetManagement.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AssetIdGenerator {

    private static JdbcTemplate jdbcTemplate;

    @Autowired
    public AssetIdGenerator(JdbcTemplate jdbcTemplate) {
        AssetIdGenerator.jdbcTemplate = jdbcTemplate;
    }

    public static synchronized int getNextId() {
        jdbcTemplate.update("INSERT INTO asset_sequence VALUES ()");
        Integer nextVal = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
        return (nextVal != null) ? nextVal : 1;
    }
}
