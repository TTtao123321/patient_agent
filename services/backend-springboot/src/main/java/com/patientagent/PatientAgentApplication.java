package com.patientagent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
@Slf4j
public class PatientAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(PatientAgentApplication.class, args);
    }

    @Bean
    public CommandLineRunner updateOldSessionTitles(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                String sql = """
                    UPDATE chat_session cs
                    INNER JOIN (
                        SELECT 
                            cm.session_id,
                            cm.content
                        FROM chat_message cm
                        WHERE cm.sequence_no = 1 
                          AND cm.sender_type = 'USER'
                          AND cm.is_deleted = 0
                    ) first_message ON cs.id = first_message.session_id
                    SET cs.title = first_message.content
                    WHERE (cs.title = '新对话' OR cs.title IS NULL)
                      AND cs.is_deleted = 0
                    """;
                int affectedRows = jdbcTemplate.update(sql);
                log.info("Successfully updated {} old session titles", affectedRows);
            } catch (Exception e) {
                log.error("Failed to update old session titles", e);
            }
        };
    }
}
