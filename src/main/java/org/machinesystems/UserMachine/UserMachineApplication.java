package org.machinesystems.UserMachine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.machinesystems.UserMachine.security.DotEnvUtil.loadDotenv;
@SpringBootApplication
public class UserMachineApplication {

	public static void main(String[] args) {
		loadDotenv();
		SpringApplication.run(UserMachineApplication.class, args);
	}

}

