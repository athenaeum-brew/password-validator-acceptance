package com.cthiebaud.passwordvalidator;

import java.net.http.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.net.URI;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.PrintWriter;
import java.util.Arrays;

public class GitHubPackagesLister {
    public static void main(String[] args) throws Exception {
        // Load the GitHub token from a file in src/main/resources
        String tokenFilePath = "src/main/resources/github-token.txt";
        String token = loadToken(tokenFilePath);
        String owner = "athenaeum-brew"; // GitHub organization
        String apiUrl = String.format("https://api.github.com/orgs/%s/packages?package_type=maven", owner);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "token " + token)
                .header("Accept", "application/vnd.github.v3+json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response.body());

            try (PrintWriter writer = new PrintWriter("packages.txt")) {
                for (JsonNode packageNode : rootNode) {
                    String packageName = packageNode.get("name").asText();

                    // Fetch the versions for this package
                    String version = fetchLatestVersion(client, mapper, owner, packageName, token);

                    String[] packageParts = packageName.split("\\.");
                    String groupId = String.join(".", Arrays.copyOfRange(packageParts, 0, packageParts.length - 1));
                    String artifactId = packageParts[packageParts.length - 1];

                    String line = String.format("%s:%s:%s", groupId, artifactId, version);
                    writer.println(line);
                }
            }

            System.out.println("Package list written to packages.txt");
        } else {
            System.out.println("Failed to fetch packages: " + response.statusCode());
            System.out.println("Response: " + response.body());
        }
    }

    private static String fetchLatestVersion(HttpClient client, ObjectMapper mapper, String owner,
            String packageName, String token) throws Exception {
        String versionsUrl = String.format(
                "https://api.github.com/orgs/%s/packages/maven/%s/versions", owner, packageName);

        HttpRequest versionsRequest = HttpRequest.newBuilder()
                .uri(URI.create(versionsUrl))
                .header("Authorization", "token " + token)
                .header("Accept", "application/vnd.github.v3+json")
                .build();

        HttpResponse<String> versionsResponse = client.send(versionsRequest, HttpResponse.BodyHandlers.ofString());

        if (versionsResponse.statusCode() == 200) {
            JsonNode versionsNode = mapper.readTree(versionsResponse.body());
            if (versionsNode.size() > 0) {
                // Return the name of the first (latest) version
                return versionsNode.get(0).get("name").asText();
            } else {
                return "no-version";
            }
        } else {
            System.out.println("Failed to fetch versions for package: " + packageName);
            System.out.println("Response: " + versionsResponse.body());
            return "error-fetching-version";
        }
    }

    private static String loadToken(String filePath) {
        try {
            return Files.readString(Paths.get(filePath)).trim();
        } catch (Exception e) {
            System.out.println("Error reading token file: " + e.getMessage());
            return null;
        }
    }
}
