package org.viators.personal_finance_app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@SpringBootApplication
public class PersonalFinanceAppApplication {

	public static void main(String[] args) {
		// Load .env file
		loadEnvFile();

		SpringApplication.run(PersonalFinanceAppApplication.class, args);
	}

	private static void loadEnvFile() {
		Path envPath = Paths.get(".env");
		if (Files.exists(envPath)) {
			try (Stream<String> lines = Files.lines(envPath)) {
				lines.filter(line -> !line.trim().isEmpty() && !line.trim().startsWith("#"))
					.forEach(line -> {
						String[] parts = line.split("=", 2);
						if (parts.length == 2) {
							String key = parts[0].trim();
							String value = parts[1].trim();
							System.setProperty(key, value);
						}
					});
			} catch (IOException e) {
				System.err.println("Warning: Could not load .env file: " + e.getMessage());
			}
		}
	}

}
