package org.machinesystems.UserMachine.security;

import lombok.experimental.UtilityClass;

@UtilityClass
public class DotEnvUtil {
    public static final String EMAIL = System.getenv("SMTP_EMAIL");
    public static final String PASSWORD = System.getenv("SMTP_PASSWORD");
    public static final String JWT_SECRET_KEY = System.getenv("JWT_SECRET_KEY");

    public static void loadDotenv() {
        System.setProperty("spring.mail.username", EMAIL);
        System.setProperty("spring.mail.password", PASSWORD);
    }
}