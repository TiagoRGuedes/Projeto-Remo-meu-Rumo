package com.ibmec.remomeurumo.config;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AjusteBancoDados implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public AjusteBancoDados(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbcTemplate.execute("ALTER TABLE matriculas DROP CONSTRAINT IF EXISTS uk_matricula_participante_atividade");
        List<String[]> colunasEnum = jdbcTemplate.query("""
                select table_name, column_name
                from information_schema.columns
                where table_schema = 'PUBLIC' and data_type = 'ENUM'
                """, (linha, indice) -> new String[] {linha.getString(1), linha.getString(2)});
        for (String[] coluna : colunasEnum) {
            jdbcTemplate.execute("ALTER TABLE " + coluna[0] + " ALTER COLUMN " + coluna[1]
                    + " SET DATA TYPE VARCHAR(40)");
        }
    }
}
