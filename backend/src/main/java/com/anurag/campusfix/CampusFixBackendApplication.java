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

	public static String maskUrlCredentials(String url) {
		if (url == null) return "";
		return url.replaceAll("://([^:]+):([^@]+)@", "://$1:***@")
		          .replaceAll("(?i)(password=)[^;&]*", "$1***");
	}

	private static void validateEnvironment() {
		String profile = System.getenv("SPRING_PROFILES_ACTIVE");
		if (profile == null || profile.isBlank()) {
			profile = System.getProperty("spring.profiles.active", "default");
		}

		// Handle cloud providers that inject DATABASE_URL (e.g. Render / Railway)
		String rawDbUrl = System.getenv("DATABASE_URL");
		if (rawDbUrl == null || rawDbUrl.isBlank()) {
			rawDbUrl = System.getenv("DB_URL");
		}
		if (rawDbUrl == null || rawDbUrl.isBlank()) {
			rawDbUrl = System.getProperty("DATABASE_URL");
		}
		if (rawDbUrl == null || rawDbUrl.isBlank()) {
			rawDbUrl = System.getProperty("DB_URL");
		}

		if (rawDbUrl != null && rawDbUrl.startsWith("mysql://")) {
			try {
				java.net.URI uri = java.net.URI.create(rawDbUrl);
				String userInfo = uri.getUserInfo();
				if (userInfo != null && userInfo.contains(":")) {
					String[] parts = userInfo.split(":", 2);
					if (System.getProperty("spring.datasource.username") == null && System.getenv("DB_USERNAME") == null) {
						System.setProperty("spring.datasource.username", parts[0]);
					}
					if (System.getProperty("spring.datasource.password") == null && System.getenv("DB_PASSWORD") == null) {
						System.setProperty("spring.datasource.password", parts[1]);
					}
				}
				int port = uri.getPort() > 0 ? uri.getPort() : 3306;
				String path = uri.getPath() != null ? uri.getPath() : "/campusfix";
				String query = uri.getQuery() != null ? "?" + uri.getQuery() : "";
				String convertedJdbc = "jdbc:mysql://" + uri.getHost() + ":" + port + path + query;
				System.setProperty("spring.datasource.url", convertedJdbc);
				rawDbUrl = convertedJdbc;
			} catch (Exception ex) {
				System.err.println("CAMPUSFIX: Failed to parse DATABASE_URL URI: " + ex.getMessage());
			}
		}

		if (profile.contains("prod")) {
			String dbUrl = rawDbUrl;
			if (dbUrl == null || dbUrl.isBlank() || dbUrl.equals("${DB_URL}") || dbUrl.equals("${DATABASE_URL}")) {
				System.err.println("================================================================================");
				System.err.println("CAMPUSFIX STARTUP CONFIGURATION ERROR:");
				System.err.println("Neither 'DB_URL' nor 'DATABASE_URL' is configured for production ('prod' profile)!");
				System.err.println("To fix this, go to your hosting dashboard (e.g. Render) -> Environment and configure:");
				System.err.println("  1. DB_URL or DATABASE_URL (e.g. jdbc:mysql://<host>:4000/campusfix?sslMode=VERIFY_IDENTITY)");
				System.err.println("  2. DB_USERNAME");
				System.err.println("  3. DB_PASSWORD");
				System.err.println("  4. JWT_SECRET (strong 256-bit key)");
				System.err.println("================================================================================");
				System.err.flush();
			} else if (dbUrl.contains("localhost") || dbUrl.contains("127.0.0.1")) {
				System.err.println("================================================================================");
				System.err.println("CAMPUSFIX STARTUP WARNING: Database URL points to 'localhost' / '127.0.0.1' in production!");
				System.err.println("Configured Database URL: " + maskUrlCredentials(dbUrl));
				System.err.println("In cloud containers, MySQL does not run locally on 127.0.0.1.");
				System.err.println("Please provide a cloud MySQL connection (e.g. TiDB Cloud, Aiven, or AWS RDS).");
				System.err.println("================================================================================");
				System.err.flush();
			} else {
				System.out.println("CAMPUSFIX: Production database target: " + maskUrlCredentials(dbUrl));
			}

			String jwtSecret = System.getenv("JWT_SECRET");
			if (jwtSecret == null || jwtSecret.isBlank()) {
				jwtSecret = System.getProperty("JWT_SECRET");
			}
			if (jwtSecret == null || jwtSecret.isBlank() || "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970".equals(jwtSecret)) {
				System.err.println("================================================================================");
				System.err.println("CAMPUSFIX SECURITY WARNING: Production is using an unconfigured or default JWT_SECRET!");
				System.err.println("Please configure a strong, unique 256-bit key via the JWT_SECRET environment variable.");
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
