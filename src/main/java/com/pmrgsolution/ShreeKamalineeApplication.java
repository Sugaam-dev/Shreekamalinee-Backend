package com.pmrgsolution;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
public class ShreeKamalineeApplication {

	public static void main(String[] args) {
		String envDir = new File("ShreeKamalinee_Backend/.env").exists() ? "ShreeKamalinee_Backend" : ".";
		Dotenv.configure().directory(envDir).ignoreIfMissing().load().entries()
				.forEach(e -> System.setProperty(e.getKey(), e.getValue()));

		SpringApplication.run(ShreeKamalineeApplication.class, args);
	}
}
