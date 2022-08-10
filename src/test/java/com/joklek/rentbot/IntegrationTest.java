package com.joklek.rentbot;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

@TestPropertySource(properties = {
        "TELEGRAM_TOKEN=my_token;my_password"
})
@ActiveProfiles("test")
@SpringBootTest
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:db/testdata/sqlite/clear_db.sql"})
@Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
        scripts = {"classpath:db/testdata/sqlite/clear_db.sql"})
public abstract class IntegrationTest {
}
