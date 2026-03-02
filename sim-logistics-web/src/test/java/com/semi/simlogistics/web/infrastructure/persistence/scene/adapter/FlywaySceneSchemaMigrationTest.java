package com.semi.simlogistics.web.infrastructure.persistence.scene.adapter;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies Flyway scene migrations align with target schema.
 *
 * @author shentw
 * @version 1.0
 * @since 2026-02-10
 */
class FlywaySceneSchemaMigrationTest {

    /**
     * H2-compatible V2 migration test using Flyway.migrate().
     *
     * This test runs in the default CI/local test chain and verifies:
     * 1) Creating V1 tables (scene, scene_draft) with sample data
     * 2) Running Flyway.migrate() to execute H2-compatible V2 script
     *    (from src/test/resources/db/migration/V2__align_scene_tables_with_spec.sql)
     * 3) Asserting migration results:
     *    - scenes/scene_drafts tables exist
     *    - scene_backup_v1 and scene_draft_backup_v1 backup tables exist
     *    - All draft.scene_id map to valid scenes.id (no orphaned drafts)
     *    - Draft count is preserved
     *
     * NOTE: This validates the H2-compatible migration script, NOT the production
     * MySQL script. Production script (src/main/resources/db/migration/) uses
     * MySQL-specific syntax (JSON_OBJECT, DATETIME(3)) and requires MySQL environment.
     */
    @Test
    void shouldRunH2CompatibleV2MigrationAndPreserveDraftMapping() throws Exception {
        String url = "jdbc:h2:mem:v2_h2_flyway;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=true";

        // Step 1: Create V1 tables with sample data (including non-UUID id)
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {

            // Create V1 scene table
            statement.execute("""
                    CREATE TABLE scene (
                        id VARCHAR(64) PRIMARY KEY,
                        scene_id VARCHAR(64) NOT NULL UNIQUE,
                        name VARCHAR(255) NOT NULL,
                        description TEXT,
                        version INT NOT NULL DEFAULT 1,
                        entities TEXT,
                        paths TEXT,
                        process_flows TEXT,
                        entity_count INT NOT NULL DEFAULT 0,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);

            // Create V1 scene_draft table
            statement.execute("""
                    CREATE TABLE scene_draft (
                        id VARCHAR(64) PRIMARY KEY,
                        scene_id VARCHAR(64) NOT NULL UNIQUE,
                        content TEXT NOT NULL,
                        version INT NOT NULL,
                        saved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);

            // Insert V1 test data (non-UUID and UUID formats)
            statement.execute("""
                    INSERT INTO scene (id, scene_id, name, description, version, entities, paths, process_flows, entity_count)
                    VALUES ('SCENE-OLD-001', 'REF-001', 'Test Scene 1', 'Description 1', 1, '[]', '[]', '[]', 0)
                    """);

            statement.execute("""
                    INSERT INTO scene (id, scene_id, name, description, version, entities, paths, process_flows, entity_count)
                    VALUES ('550e8400-e29b-41d4-a716-446655440000', 'REF-002', 'Test Scene 2', 'Description 2', 1, '[]', '[]', '[]', 0)
                    """);

            // Insert drafts with UUID format to avoid conflicts
            statement.execute("""
                    INSERT INTO scene_draft (id, scene_id, content, version, saved_at)
                    VALUES ('d0000001-0000-0000-0000-000000000001', 'REF-001', '{"sceneId":"REF-001","name":"Test Scene 1"}', 1, CURRENT_TIMESTAMP)
                    """);

            statement.execute("""
                    INSERT INTO scene_draft (id, scene_id, content, version, saved_at)
                    VALUES ('d0000002-0000-0000-0000-000000000002', 'REF-002', '{"sceneId":"REF-002","name":"Test Scene 2"}', 1, CURRENT_TIMESTAMP)
                    """);
        }

        // Step 2: Run Flyway migration with H2-compatible script
        Flyway flyway = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("filesystem:src/test/resources/db/migration")
                .baselineOnMigrate(true)
                .load();

        flyway.migrate();

        // Step 3: Verify migration results
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {

            // 3.1) Verify new tables exist
            assertThat(tableExists(statement, "SCENES")).isTrue();
            assertThat(tableExists(statement, "SCENE_DRAFTS")).isTrue();

            // 3.2) Verify backup tables exist (rollback protection)
            assertThat(tableExists(statement, "SCENE_BACKUP_V1")).isTrue();
            assertThat(tableExists(statement, "SCENE_DRAFT_BACKUP_V1")).isTrue();

            // 3.3) Verify old tables are dropped
            assertThat(tableExists(statement, "SCENE")).isFalse();
            assertThat(tableExists(statement, "SCENE_DRAFT")).isFalse();

            // 3.4) Verify data counts
            int sceneCount = getCount(statement, "scenes");
            int draftCount = getCount(statement, "scene_drafts");
            assertThat(sceneCount).isEqualTo(2);  // Both scenes migrated
            assertThat(draftCount).isEqualTo(2);   // Both drafts preserved

            // 3.5) Verify ALL draft.scene_id map to valid scenes.id (critical validation)
            try (ResultSet rs = statement.executeQuery(
                    "SELECT COUNT(*) FROM scene_drafts sd WHERE NOT EXISTS (SELECT 1 FROM scenes s WHERE s.id = sd.scene_id)"
            )) {
                rs.next();
                int orphanedDrafts = rs.getInt(1);
                assertThat(orphanedDrafts).isZero();  // No orphaned drafts
            }
        }
    }

    /**
     * Production V2 migration test for MySQL environment.
     *
     * This test validates the ACTUAL production V2 migration script:
     * src/main/resources/db/migration/V2__align_scene_tables_with_spec.sql
     *
     * IMPORTANT: This test is NOT executed in default CI pipeline.
     * - Purpose: For manual testing or dedicated MySQL validation pipeline only
     * - The default CI chain uses H2-compatible migration tests, which do NOT prove
     *   production script executability in real MySQL environment
     * - To run this test:
     *   mvn -pl sim-logistics-web -Dtest=FlywaySceneSchemaMigrationTest
     *   -Dit.mysql.flyway=true
     *   -Dit.mysql.url=jdbc:mysql://localhost:3306/plant_simulation
     *   -Dit.mysql.user=root
     *   -Dit.mysql.password=your_password
     *   test
     */
    @Test
    @EnabledIfSystemProperty(named = "it.mysql.flyway", matches = "true")
    void shouldRunProductionV2MigrationInMySql() throws Exception {
        String url = System.getProperty("it.mysql.url", "jdbc:mysql://localhost:3306/plant_simulation");
        String user = System.getProperty("it.mysql.user", "root");
        String password = System.getProperty("it.mysql.password", "password");

        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement()) {

            // Step 1: Create V1 tables with sample data
            statement.execute("""
                    CREATE TABLE scene (
                        id VARCHAR(64) PRIMARY KEY,
                        scene_id VARCHAR(64) NOT NULL UNIQUE,
                        name VARCHAR(255) NOT NULL,
                        description TEXT,
                        version INT NOT NULL DEFAULT 1,
                        entities TEXT,
                        paths TEXT,
                        process_flows TEXT,
                        entity_count INT NOT NULL DEFAULT 0,
                        created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
                        updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);

            statement.execute("""
                    CREATE TABLE scene_draft (
                        id VARCHAR(64) PRIMARY KEY,
                        scene_id VARCHAR(64) NOT NULL UNIQUE,
                        content TEXT NOT NULL,
                        version INT NOT NULL,
                        saved_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);

            // Insert test data
            statement.execute("""
                    INSERT INTO scene (id, scene_id, name, description, version, entities, paths, process_flows, entity_count)
                    VALUES ('SCENE-OLD-001', 'REF-001', 'Test Scene 1', 'Description 1', 1, '[]', '[]', '[]', 0)
                    """);

            statement.execute("""
                    INSERT INTO scene_draft (id, scene_id, content, version, saved_at)
                    VALUES ('d0000001-0000-0000-0000-000000000001', 'REF-001', '{"sceneId":"REF-001"}', 1, NOW(3))
                    """);
        }

        // Step 2: Run Flyway migration with PRODUCTION script
        Flyway flyway = Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();

        flyway.migrate();

        // Step 3: Verify migration results
        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement()) {

            // Verify new tables exist
            assertThat(tableExists(statement, "scenes")).isTrue();
            assertThat(tableExists(statement, "scene_drafts")).isTrue();

            // Verify backup tables exist
            assertThat(tableExists(statement, "scene_backup_v1")).isTrue();
            assertThat(tableExists(statement, "scene_draft_backup_v1")).isTrue();

            // Verify old tables are dropped
            assertThat(tableExists(statement, "scene")).isFalse();
            assertThat(tableExists(statement, "scene_draft")).isFalse();

            // Verify no orphaned drafts
            try (ResultSet rs = statement.executeQuery(
                    "SELECT COUNT(*) FROM scene_drafts sd WHERE NOT EXISTS (SELECT 1 FROM scenes s WHERE s.id = sd.scene_id)"
            )) {
                rs.next();
                int orphanedDrafts = rs.getInt(1);
                assertThat(orphanedDrafts).isZero();
            }
        }
    }

    /**
     * Test V2 migration fails when V1 tables are missing (early failure validation).
     *
     * This test verifies the "强前提 + 早失败" strategy:
     * 1) When `scene` table is missing, migration should fail with readable error
     * 2) When `scene_draft` table is missing, migration should fail with readable error
     * 3) The failure should occur early in the migration process
     *
     * Note: This test uses H2-compatible validation script from src/test/resources/db/migration/
     */
    @Test
    void shouldFailV2MigrationWhenV1TablesMissing() throws Exception {
        String url = "jdbc:h2:mem:v2_missing_tables;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=true";

        // Test Case 1: Missing scene table
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {

            // Only create scene_draft, not scene
            statement.execute("""
                    CREATE TABLE scene_draft (
                        id VARCHAR(64) PRIMARY KEY,
                        scene_id VARCHAR(64) NOT NULL UNIQUE,
                        content TEXT NOT NULL,
                        version INT NOT NULL,
                        saved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);

            // Attempt to run Flyway migration - should fail because scene table is missing
            Flyway flyway = Flyway.configure()
                    .dataSource(url, "sa", "")
                    .locations("filesystem:src/test/resources/db/migration")
                    .baselineOnMigrate(true)
                    .load();

            // Migration should fail because V1 `scene` table is missing.
            assertThatThrownBy(() -> flyway.migrate())
                    .isInstanceOf(Exception.class)
                    .satisfies(e -> {
                        assertThat(isMissingTableFailure(e, "SCENE"))
                                .withFailMessage(
                                        "Expected missing SCENE table failure, root cause was: %s",
                                        rootCauseMessage(e)
                                )
                                .isTrue();
                    });
        }
    }

    /**
     * Test V2 migration fails when only scene_draft table is missing.
     */
    @Test
    void shouldFailV2MigrationWhenSceneDraftTableMissing() throws Exception {
        String url = "jdbc:h2:mem:v2_missing_draft;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=true";

        // Test Case 2: Missing scene_draft table
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {

            // Only create scene, not scene_draft
            statement.execute("""
                    CREATE TABLE scene (
                        id VARCHAR(64) PRIMARY KEY,
                        scene_id VARCHAR(64) NOT NULL UNIQUE,
                        name VARCHAR(255) NOT NULL,
                        description TEXT,
                        version INT NOT NULL DEFAULT 1,
                        entities TEXT,
                        paths TEXT,
                        process_flows TEXT,
                        entity_count INT NOT NULL DEFAULT 0,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);

            // Attempt to run Flyway migration - should fail because scene_draft table is missing
            Flyway flyway = Flyway.configure()
                    .dataSource(url, "sa", "")
                    .locations("filesystem:src/test/resources/db/migration")
                    .baselineOnMigrate(true)
                    .load();

            // Migration should fail because V1 `scene_draft` table is missing.
            assertThatThrownBy(() -> flyway.migrate())
                    .isInstanceOf(Exception.class)
                    .satisfies(e -> {
                        assertThat(isMissingTableFailure(e, "SCENE_DRAFT"))
                                .withFailMessage(
                                        "Expected missing SCENE_DRAFT table failure, root cause was: %s",
                                        rootCauseMessage(e)
                                )
                                .isTrue();
                    });
        }
    }

    /**
     * Test V2 migration fails when both V1 tables are missing.
     */
    @Test
    void shouldFailV2MigrationWhenBothV1TablesMissing() throws Exception {
        String url = "jdbc:h2:mem:v2_missing_both;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=true";

        // Test Case 3: Both tables missing (fresh database scenario)
        Flyway flyway = Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("filesystem:src/test/resources/db/migration")
                .baselineOnMigrate(true)
                .load();

        // Migration should fail because at least one required V1 table is missing.
        assertThatThrownBy(() -> flyway.migrate())
                .isInstanceOf(Exception.class)
                .satisfies(e -> {
                    boolean missingScene = isMissingTableFailure(e, "SCENE");
                    boolean missingSceneDraft = isMissingTableFailure(e, "SCENE_DRAFT");

                    assertThat(missingScene || missingSceneDraft)
                            .withFailMessage(
                                    "Expected missing SCENE/SCENE_DRAFT failure, root cause was: %s",
                                    rootCauseMessage(e)
                            )
                            .isTrue();

                    assertThat(isV2MigrationStageFailure(e))
                            .withFailMessage(
                                    "Expected failure to occur in V2 stage, but exception did not indicate V2 migration: %s",
                                    collectAllMessages(e)
                            )
                            .isTrue();
                });
    }

    /**
     * Test V2 migration core logic with draft mapping validation (H2 compatible).
     *
     * This test runs in default CI/local test chain and verifies:
     * 1) V1 → V2 table structure migration works correctly
     * 2) Draft mapping via temporary table preserves all drafts
     * 3) scene_drafts.scene_id correctly maps to scenes.id
     *
     * Note: Uses manual SQL execution to simulate V2 script core behavior in H2.
     */
    @Test
    void shouldVerifyV2CoreLogicWithDraftMapping() throws Exception {
        String url = "jdbc:h2:mem:v2_core_logic;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=true";

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {

            // Step 1: Create V1 tables (old schema)
            statement.execute("""
                    CREATE TABLE scene (
                        id VARCHAR(64) PRIMARY KEY,
                        scene_id VARCHAR(64) NOT NULL UNIQUE,
                        name VARCHAR(255) NOT NULL,
                        description TEXT,
                        version INT NOT NULL DEFAULT 1,
                        entities TEXT,
                        paths TEXT,
                        process_flows TEXT,
                        entity_count INT NOT NULL DEFAULT 0,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);

            statement.execute("""
                    CREATE TABLE scene_draft (
                        id VARCHAR(64) PRIMARY KEY,
                        scene_id VARCHAR(64) NOT NULL UNIQUE,
                        content TEXT NOT NULL,
                        version INT NOT NULL,
                        saved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);

            // Step 2: Insert test data (including non-UUID id to test mapping logic)
            statement.execute("""
                    INSERT INTO scene (id, scene_id, name, description, version, entities, paths, process_flows, entity_count)
                    VALUES ('SCENE-OLD-001', 'REF-001', 'Test Scene 1', 'Description 1', 1, '[]', '[]', '[]', 0)
                    """);

            statement.execute("""
                    INSERT INTO scene (id, scene_id, name, description, version, entities, paths, process_flows, entity_count)
                    VALUES ('550e8400-e29b-41d4-a716-446655440000', 'REF-002', 'Test Scene 2', 'Description 2', 1, '[]', '[]', '[]', 0)
                    """);

            // Insert drafts (these should be preserved after migration)
            statement.execute("""
                    INSERT INTO scene_draft (id, scene_id, content, version, saved_at)
                    VALUES ('d0000001-0000-0000-0000-000000000001', 'REF-001', '{"sceneId":"REF-001","name":"Test Scene 1"}', 1, CURRENT_TIMESTAMP)
                    """);

            statement.execute("""
                    INSERT INTO scene_draft (id, scene_id, content, version, saved_at)
                    VALUES ('d0000002-0000-0000-0000-000000000002', 'REF-002', '{"sceneId":"REF-002","name":"Test Scene 2"}', 1, CURRENT_TIMESTAMP)
                    """);

            // Step 3: Execute V2 migration logic (simulating V2 script core behavior)
            // Create temporary migration table
            statement.execute("""
                    CREATE TEMPORARY TABLE temp_scene_migration_data (
                        old_id VARCHAR(64),
                        old_scene_id VARCHAR(64),
                        new_id CHAR(36),
                        name VARCHAR(255),
                        description TEXT,
                        version INT,
                        definition VARCHAR(1000),
                        metadata VARCHAR(500),
                        created_at TIMESTAMP,
                        updated_at TIMESTAMP,
                        PRIMARY KEY (old_scene_id)
                    )
                    """);

            // Populate migration table (simulating UUID generation logic)
            statement.execute("""
                    INSERT INTO temp_scene_migration_data (old_id, old_scene_id, new_id, name, description, version, definition, metadata, created_at, updated_at)
                    SELECT
                        s.id as old_id,
                        s.scene_id as old_scene_id,
                        CASE
                            WHEN LENGTH(s.id) = 36 AND s.id LIKE '%-%-%-%-%' THEN s.id
                            ELSE '00000000-0000-0000-0000-000000000001'
                        END as new_id,
                        s.name,
                        s.description,
                        s.version,
                        '{"entities":[],"paths":[],"processFlows":[]}' as definition,
                        '{"entityCount":0}' as metadata,
                        s.created_at,
                        s.updated_at
                    FROM scene s
                    """);

            // Create new scenes table
            statement.execute("""
                    CREATE TABLE scenes (
                        id CHAR(36) PRIMARY KEY,
                        tenant_id CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
                        name VARCHAR(255) NOT NULL,
                        description TEXT,
                        version INT NOT NULL DEFAULT 1,
                        schema_version INT NOT NULL DEFAULT 1,
                        definition VARCHAR(1000) NOT NULL,
                        metadata VARCHAR(500),
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        created_by VARCHAR(100),
                        updated_by VARCHAR(100)
                    )
                    """);

            // Migrate to scenes
            statement.execute("""
                    INSERT INTO scenes (id, tenant_id, name, description, version, schema_version, definition, metadata, created_at, updated_at, created_by, updated_by)
                    SELECT new_id, '00000000-0000-0000-0000-000000000000', name, description, version, 1, definition, metadata, created_at, updated_at, NULL, NULL
                    FROM temp_scene_migration_data
                    """);

            // Create new scene_drafts table
            statement.execute("""
                    CREATE TABLE scene_drafts (
                        id CHAR(36) PRIMARY KEY,
                        tenant_id CHAR(36) NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
                        scene_id CHAR(36) NOT NULL,
                        content VARCHAR(2000) NOT NULL,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);

            // Migrate drafts using temp mapping table
            statement.execute("""
                    INSERT INTO scene_drafts (id, tenant_id, scene_id, content, updated_at, created_at)
                    SELECT
                        CASE
                            WHEN LENGTH(d.id) = 36 AND d.id LIKE '%-%-%-%-%' THEN d.id
                            ELSE '00000000-0000-0000-0000-000000000001'
                        END as id,
                        '00000000-0000-0000-0000-000000000000' as tenant_id,
                        temp.new_id as scene_id,
                        d.content as content,
                        d.saved_at as updated_at,
                        d.saved_at as created_at
                    FROM scene_draft d
                    INNER JOIN temp_scene_migration_data temp ON temp.old_scene_id = d.scene_id
                    """);

            // Step 4: Verify migration results (core assertions)
            assertThat(tableExists(statement, "SCENES")).isTrue();
            assertThat(tableExists(statement, "SCENE_DRAFTS")).isTrue();

            int sceneCount = getCount(statement, "scenes");
            int draftCount = getCount(statement, "scene_drafts");
            assertThat(sceneCount).isEqualTo(2);  // Both scenes migrated
            assertThat(draftCount).isEqualTo(2);   // Both drafts preserved

            // Verify ALL draft.scene_id map to valid scenes.id (critical validation)
            try (ResultSet rs = statement.executeQuery(
                    "SELECT COUNT(*) FROM scene_drafts sd WHERE NOT EXISTS (SELECT 1 FROM scenes s WHERE s.id = sd.scene_id)"
            )) {
                rs.next();
                int orphanedDrafts = rs.getInt(1);
                assertThat(orphanedDrafts).isZero();  // No orphaned drafts
            }
        }
    }

    private boolean tableExists(Statement statement, String tableName) throws Exception {
        try (ResultSet rs = statement.executeQuery(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = '" + tableName + "'"
        )) {
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private boolean columnExists(Statement statement, String tableName, String columnName) throws Exception {
        try (ResultSet rs = statement.executeQuery(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_NAME = '" + tableName + "' AND COLUMN_NAME = '" + columnName + "'"
        )) {
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private int getCount(Statement statement, String tableName) throws Exception {
        try (ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage() == null ? "" : root.getMessage();
    }

    private boolean containsTableNotFound(String message, String tableName) {
        if (message == null || message.isBlank()) {
            return false;
        }

        String normalized = message.toLowerCase(Locale.ROOT);
        String normalizedTable = tableName.toLowerCase(Locale.ROOT);

        Pattern[] patterns = new Pattern[]{
                Pattern.compile("(?s).*table\\s+\"?" + Pattern.quote(normalizedTable) + "\"?\\s+not\\s+found.*"),
                Pattern.compile("(?s).*table\\s+or\\s+view\\s+\"?" + Pattern.quote(normalizedTable) + "\"?\\s+not\\s+found.*"),
                Pattern.compile("(?s).*object\\s+\"?" + Pattern.quote(normalizedTable) + "\"?\\s+not\\s+found.*"),
                Pattern.compile("(?s).*42s02.*\\b" + Pattern.quote(normalizedTable) + "\\b.*"),
                Pattern.compile("(?s).*\\b" + Pattern.quote(normalizedTable) + "\\b.*(not\\s+found|does\\s+not\\s+exist|doesn't\\s+exist).*")
        };

        for (Pattern pattern : patterns) {
            if (pattern.matcher(normalized).matches()) {
                return true;
            }
        }
        return false;
    }

    private boolean isMissingTableFailure(Throwable throwable, String tableName) {
        String root = rootCauseMessage(throwable);
        if (containsTableNotFound(root, tableName)) {
            return true;
        }
        return containsTableNotFound(collectAllMessages(throwable), tableName);
    }

    private boolean isV2MigrationStageFailure(Throwable throwable) {
        String all = collectAllMessages(throwable).toLowerCase(Locale.ROOT);
        if (all.contains("v2__align_scene_tables_with_spec.sql")) {
            return true;
        }
        if (all.contains("2 - align scene tables with spec")) {
            return true;
        }
        return all.contains("version \"2 - align scene tables with spec")
                || all.contains("migration to version \"2")
                || all.contains("version 2");
    }

    private String collectAllMessages(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(current.getMessage());
            }
            current = current.getCause();
        }
        return builder.toString();
    }
}
