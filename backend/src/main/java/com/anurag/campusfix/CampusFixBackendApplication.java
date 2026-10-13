package com.anurag.campusfix;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CampusFixBackendApplication {

	public static void main(String[] args) {
		loadDotEnvIfPresent();
		SpringApplication.run(CampusFixBackendApplication.class, args);
	}

	private static void loadDotEnvIfPresent() {
		try {
			Path envPath = Paths.get(".env");
			if (Files.exists(envPath)) {
				List<String> lines = Files.readAllLines(envPath);
				for (String line : lines) {
					String trimmed = line.trim();
					if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
						continue;
					}
					int eqIdx = trimmed.indexOf('=');
					String key = trimmed.substring(0, eqIdx).trim();
					String val = trimmed.substring(eqIdx + 1).trim();
					if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
						val = val.substring(1, val.length() - 1);
					}
					if (System.getProperty(key) == null && System.getenv(key) == null) {
						System.setProperty(key, val);
					}
				}
			}
		} catch (Exception ignored) {
		}
	}

}
