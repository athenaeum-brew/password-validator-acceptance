package com.cthiebaud.passwordvalidator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.http.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.PrintWriter;

public class GitHubPackagesLister {

    public static void main(String[] args) throws Exception {
        // Load exclusions from file
        Set<String> exclusions = loadExclusions("exclusions.yaml");

        // Load GitHub token for authentication
        String tokenFilePath = "src/main/resources/github-token.txt";
        String token = loadToken(tokenFilePath);
        if (token == null || token.isBlank()) {
            System.err.println("GitHub token is invalid or missing. Exiting.");
            return;
        }

        String owner = "athenaeum-brew"; // GitHub organization name
        String apiUrl = String.format("https://api.github.com/orgs/%s/packages?package_type=maven", owner);

        // Create HTTP client and send API request for packages
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "token " + token)
                .header("Accept", "application/vnd.github.v3+json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            // Parse the response JSON to list packages
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response.body());

            try (PrintWriter writer = new PrintWriter("packages.txt")) {
                for (JsonNode packageNode : rootNode) {
                    String packageName = packageNode.get("name").asText();

                    // Fetch latest version for the package
                    String version = fetchLatestVersion(client, mapper, owner, packageName, token);

                    // Derive Maven coordinates (groupId:artifactId:version)
                    String[] packageParts = packageName.split("\\.");
                    String groupId = String.join(".", List.of(packageParts).subList(0, packageParts.length - 1));
                    String artifactId = packageParts[packageParts.length - 1];

                    String fullArtifact = String.format("%s:%s", groupId, artifactId);

                    // Check if the package should be excluded
                    if (exclusions.contains(fullArtifact)) {
                        System.out.printf("Skipping package: %s:%s:%s (excluded)\n", groupId, artifactId, version);
                        continue;
                    }

                    String line = String.format("%s:%s:%s", groupId, artifactId, version);
                    writer.println(line); // Write to output file
                }
            }

            System.out.println("Package list written to packages.txt");
        } else {
            // Handle errors in fetching packages
            System.err.println("Failed to fetch packages: " + response.statusCode());
            System.err.println("Response: " + response.body());
        }
    }

    private static Set<String> loadExclusions(String exclusionsFileName) throws IOException {
        Set<String> exclusions = new HashSet<>();
        try (InputStream input = GitHubPackagesLister.class.getClassLoader().getResourceAsStream(exclusionsFileName)) {
            if (input == null) {
                System.err.println("Exclusions file not found. No exclusions will be applied.");
                return exclusions;
            }

            ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
            JsonNode root = yamlMapper.readTree(input);
            JsonNode exclusionsNode = root.get("exclusions");

            if (exclusionsNode != null && exclusionsNode.isArray()) {
                for (JsonNode exclusion : exclusionsNode) {
                    exclusions.add(exclusion.asText());
                }
            }
        }
        return exclusions;
    }

    private static String fetchLatestVersion(HttpClient client, ObjectMapper mapper, String owner,
            String packageName, String token) throws Exception {
        // Fetch versions of a specific package
        String versionsUrl = String.format(
                "https://api.github.com/orgs/%s/packages/maven/%s/versions", owner, packageName);

        HttpRequest versionsRequest = HttpRequest.newBuilder()
                .uri(URI.create(versionsUrl))
                .header("Authorization", "token " + token)
                .header("Accept", "application/vnd.github.v3+json")
                .build();

        HttpResponse<String> versionsResponse = client.send(versionsRequest, HttpResponse.BodyHandlers.ofString());

        if (versionsResponse.statusCode() == 200) {
            // Parse and return the latest version name
            JsonNode versionsNode = mapper.readTree(versionsResponse.body());
            if (versionsNode.size() > 0) {
                return versionsNode.get(0).get("name").asText(); // Latest version
            } else {
                return "no-version"; // No versions available
            }
        } else {
            // Log errors and return fallback value
            System.err.println("Failed to fetch versions for package: " + packageName);
            System.err.println("Response: " + versionsResponse.body());
            return "error-fetching-version";
        }
    }

    private static String loadToken(String filePath) {
        // Load and return GitHub token from a file
        try {
            return Files.readString(Paths.get(filePath)).trim();
        } catch (Exception e) {
            System.err.println("Error reading token file: " + e.getMessage());
            return null; // Return null if token loading fails
        }
    }
}
