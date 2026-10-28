package br.com.candidaturas.repository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.*;
class PlataformaMigrationTest {
    @Test void migracaoPreservaCodigosENovasContasRecebemCatalogo() throws Exception {
        String url="jdbc:h2:mem:migracao-plataformas;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url,"sa","").target("5").load().migrate();
        try(var c=DriverManager.getConnection(url,"sa","");var s=c.createStatement()) {
            s.execute("INSERT INTO usuario(id,nome,email,senha_hash,criado_em) VALUES(1,'Pessoa','p@teste.com','x',CURRENT_TIMESTAMP)");
            s.execute("INSERT INTO candidatura(empresa,cargo,plataforma,status,prioridade,origem,criado_em,atualizado_em,usuario_id) VALUES('Acme','Dev','LINKEDIN_VAGAS','CANDIDATADO','MEDIA','MANUAL',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1)");
            Flyway.configure().dataSource(url,"sa","").load().migrate();
            try(var r=s.executeQuery("SELECT plataforma FROM candidatura")) { r.next(); assertThat(r.getString(1)).isEqualTo("LINKEDIN_VAGAS"); }
            try(var r=s.executeQuery("SELECT COUNT(*) FROM plataforma WHERE usuario_id=1")) { r.next(); assertThat(r.getInt(1)).isEqualTo(13); }
            try(var r=s.executeQuery("SELECT nome FROM plataforma WHERE codigo='SOLIDES'")) { r.next(); assertThat(r.getString(1)).isEqualTo("Sólides"); }
        }
    }
}
