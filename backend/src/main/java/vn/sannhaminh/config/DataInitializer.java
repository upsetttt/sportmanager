package vn.sannhaminh.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {
    private final JdbcTemplate db; private final PasswordEncoder encoder;
    @Value("${app.admin.email:admin@sannhaminh.local}") private String email;
    @Value("${app.admin.password:Admin@123456}") private String password;
    @Value("${app.admin.name:Quản trị viên}") private String name;
    public DataInitializer(JdbcTemplate db, PasswordEncoder encoder){this.db=db;this.encoder=encoder;}
    @Override public void run(String... args){
        Integer count=db.queryForObject("SELECT COUNT(*) FROM accounts WHERE role='ADMIN'",Integer.class);
        if(count!=null&&count==0) {
            String normalizedEmail = email.trim().toLowerCase();
            Integer emailExists = db.queryForObject("SELECT COUNT(*) FROM accounts WHERE email=?", Integer.class, normalizedEmail);
            if (emailExists == null || emailExists == 0) {
                db.update("INSERT INTO accounts(full_name,email,password_hash,role,status) VALUES(?,?,?,'ADMIN','ACTIVE')", name, normalizedEmail, encoder.encode(password));
            }
        }
    }
}
