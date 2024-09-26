package org.machinesystems.UserMachine.security;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DotEnvUtil {
    static Dotenv dotenv = Dotenv.load();
    public static final String EMAIL = dotenv.get("EMAIL");
    public static final String PASSWORD = dotenv.get("PASSWORD");
    public static final String SECRET_KEY = dotenv.get("SECRET_KEY");

    public static void loadDotenv() {
        System.setProperty("spring.mail.username", EMAIL);
        System.setProperty("spring.mail.password", PASSWORD);
    }
}