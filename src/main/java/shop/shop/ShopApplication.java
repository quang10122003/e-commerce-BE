package shop.shop;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class ShopApplication {
	public static void main(String[] args) {
		loadEnvFile();
		SpringApplication.run(ShopApplication.class, args);
	}

	private static void loadEnvFile() {
		Path envPath = Files.exists(Path.of(".env")) ? Path.of(".env") : Path.of("/.env");
		if (!Files.exists(envPath)) {
			return;
		}

		try {
			for (String line : Files.readAllLines(envPath)) {
				String trimmedLine = line.trim();
				if (trimmedLine.isBlank() || trimmedLine.startsWith("#")) {
					continue;
				}

				int separatorIndex = trimmedLine.indexOf('=');
				if (separatorIndex <= 0) {
					continue;
				}

				String key = trimmedLine.substring(0, separatorIndex).trim();
				String value = trimmedLine.substring(separatorIndex + 1).trim();
				if ((value.startsWith("\"") && value.endsWith("\""))
						|| (value.startsWith("'") && value.endsWith("'"))) {
					value = value.substring(1, value.length() - 1);
				}

				if (System.getenv(key) == null && System.getProperty(key) == null) {
					System.setProperty(key, value);
				}
			}
		} catch (IOException exception) {
			throw new IllegalStateException("Could not load environment file: " + envPath, exception);
		}
	}

}
