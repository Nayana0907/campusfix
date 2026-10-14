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
		validateEnvironment();
		SpringApplication.run(CampusFixBackendApplication.class, args);
	}

	private static void validateEnvironment() {
		String profile = System.getenv("SPRING_PROFILES_ACTIVE");
		if (profile == null || profile.isBlank()) {
			profile = System.getProperty("spring.profiles.active", "default");
		}
		if (profile.contains("prod")) {
			String dbUrl = System.getenv("DB_URL");
			if (dbUrl == null || dbUrl.isBlank()) {
				dbUrl = System.getProperty("DB_URL");
			}
			if (dbUrl == null || dbUrl.isBlank() || dbUrl.equals("${DB_URL}")) {
				System.err.println("================================================================================");
				System.err.println("CAMPUSFIX STARTUP CONFIGURATION ERROR:");
				System.err.println("The 'DB_URL' environment variable is NOT configured for production ('prod' profile)!");
				System.err.println("To fix this, go to your Render Dashboard -> Service -> Environment and add:");
				System.err.println("  1. DB_URL (e.g. jdbc:mysql://<host>:4000/campusfix?sslMode=VERIFY_IDENTITY)");
				System.err.println("  2. DB_USERNAME (e.g. <prefix>.root)");
				System.err.println("  3. DB_PASSWORD (your database password)");
				System.err.println("  4. JWT_SECRET (256-bit secret key)");
				System.err.println("================================================================================");
				System.err.flush();
			} else if (dbUrl.contains("localhost") || dbUrl.contains("127.0.0.1")) {
				System.err.println("================================================================================");
				System.err.println("CAMPUSFIX STARTUP WARNING: 'DB_URL' points to 'localhost' / '127.0.0.1'!");
				System.err.println("Current DB_URL: " + dbUrl);
				System.err.println("In Render cloud containers, MySQL does not run locally on 127.0.0.1.");
				System.err.println("Please provide a cloud database connection string (e.g. TiDB Cloud Serverless or Aiven).");
				System.err.println("================================================================================");
				System.err.flush();
			}
		}
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
