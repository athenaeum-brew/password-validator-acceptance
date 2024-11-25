package com.cthiebaud.passwordvalidator;

import org.w3c.dom.*;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.xml.parsers.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetadataExtractor {

    public static void main(String[] args) {
        // Directory containing the POM files
        String pomDirPath = "downloaded_packages";

        // Get the list of all .pom files in the directory
        File pomDir = new File(pomDirPath);
        File[] pomFiles = pomDir.listFiles((_, name) -> name.endsWith(".pom"));

        if (pomFiles == null || pomFiles.length == 0) {
            System.out.println("No .pom files found in the directory.");
            return;
        }

        // Map to store developers, SCM URL, and version grouped by their artifact ID
        Map<String, PomInfo> pomInfoByFile = new LinkedHashMap<>();

        // Map to track developers across projects
        Map<String, String> developerToProjectMap = new HashMap<>();

        // ANSI escape code for orange (bright yellow)
        final String ORANGE = "\u001B[33m"; // Bright yellow color
        final String RESET = "\u001B[0m"; // Reset to default console color

        // Process each .pom file
        for (File pomFile : pomFiles) {
            System.out.println("Processing: " + pomFile.getName());
            try {
                // Extract artifact ID and version
                String artifactId = extractArtifactIdFromFileName(pomFile.getName());
                String version = extractVersionFromFileName(pomFile.getName());

                // Parse the POM file
                List<Developer> developers = parsePOMForDevelopers(pomFile);
                String scmUrl = parsePOMForScmUrl(pomFile);

                // Check for duplicate developers across projects
                for (Developer developer : developers) {
                    String uniqueKey = developer.id() != null ? developer.id() : developer.email();
                    if (uniqueKey != null && developerToProjectMap.containsKey(uniqueKey)) {
                        String existingProject = developerToProjectMap.get(uniqueKey);
                        System.out.printf(
                                ORANGE + "Warning: Developer %s (ID: %s) appears in multiple projects: %s and %s%n"
                                        + RESET,
                                developer.name(), developer.id(), existingProject, artifactId);
                    } else if (uniqueKey != null) {
                        developerToProjectMap.put(uniqueKey, artifactId);
                    }
                }

                // Store the POM information
                pomInfoByFile.put(artifactId, new PomInfo(developers, scmUrl, version));
            } catch (Exception e) {
                System.out.println("Error processing file: " + pomFile.getName());
                e.printStackTrace();
            }
        }

        // Write developers, SCM URL, and version to a structured text file
        writePomInfoToYamlFile(pomInfoByFile, "packages_metadata.yaml");
    }

    // Extract artifact ID (base name without version or extension) from the POM
    // file name
    public static String extractArtifactIdFromFileName(String fileName) {
        // Regex pattern to match the artifact ID
        Pattern pattern = Pattern.compile("^(.*?)-\\d+\\.\\d+(\\.\\d+)?(?:-[A-Za-z0-9.-]+)?\\.pom$");
        Matcher matcher = pattern.matcher(fileName);

        if (matcher.find()) {
            return matcher.group(1); // Return the artifact ID (base name)
        }

        return fileName.replace(".pom", ""); // Fallback to removing the extension
    }

    // Extract version from the POM file name
    public static String extractVersionFromFileName(String fileName) {
        // Regex pattern to match the version in the file name
        Pattern pattern = Pattern.compile("-(\\d+\\.\\d+(\\.\\d+)?(?:-[A-Za-z0-9.-]+)?)\\.pom$");
        Matcher matcher = pattern.matcher(fileName);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return "Unknown"; // Return "Unknown" if the version can't be determined
    }

    // Parse the POM file to extract developer information
    public static List<Developer> parsePOMForDevelopers(File pomFile) throws Exception {
        List<Developer> developers = new ArrayList<>();

        // Set up the XML parser
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(pomFile);

        // Normalize the XML structure
        doc.getDocumentElement().normalize();

        // Get all <developer> elements
        NodeList developerNodes = doc.getElementsByTagName("developer");

        for (int i = 0; i < developerNodes.getLength(); i++) {
            Node developerNode = developerNodes.item(i);

            if (developerNode.getNodeType() == Node.ELEMENT_NODE) {
                Element developerElement = (Element) developerNode;

                // Extract developer details
                String id = getElementValue(developerElement, "id");
                String name = getElementValue(developerElement, "name");
                String email = getElementValue(developerElement, "email");

                // Create a Developer record and add it to the list
                developers.add(new Developer(id, name, email, pomFile.getName()));
            }
        }

        return developers;
    }

    // Parse the POM file to extract the SCM URL
    public static String parsePOMForScmUrl(File pomFile) throws Exception {
        // Set up the XML parser
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(pomFile);

        // Normalize the XML structure
        doc.getDocumentElement().normalize();

        // Locate the <scm> element
        NodeList scmNodes = doc.getElementsByTagName("scm");
        if (scmNodes != null && scmNodes.getLength() > 0) {
            Element scmElement = (Element) scmNodes.item(0);
            return getElementValue(scmElement, "url");
        }

        return null; // Return null if <scm><url> is not found
    }

    // Helper method to get the text content of an element
    private static String getElementValue(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList != null && nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }

    public static void writePomInfoToYamlFile(Map<String, PomInfo> pomInfoByFile, String outputPath) {
        // Prepare YAML configuration
        DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        Yaml yaml = new Yaml(options);

        // Prepare the data structure for YAML
        Map<String, Object> yamlData = new HashMap<>();
        yamlData.put("totalDevelopers", pomInfoByFile.values().stream()
                .mapToInt(info -> info.developers().size())
                .sum());

        Map<String, Object> pomEntries = new HashMap<>();
        for (Map.Entry<String, PomInfo> entry : pomInfoByFile.entrySet()) {
            String artifactId = entry.getKey();
            PomInfo info = entry.getValue();

            Map<String, Object> pomDetails = new HashMap<>();
            pomDetails.put("scmUrl", info.scmUrl() != null ? info.scmUrl() : "N/A");
            pomDetails.put("version", info.version() != null ? info.version() : "Unknown");

            List<Map<String, String>> developers = info.developers().stream()
                    .map(dev -> {
                        Map<String, String> devDetails = new HashMap<>();
                        devDetails.put("id", dev.id() != null ? dev.id() : "N/A");
                        devDetails.put("name", dev.name() != null ? dev.name() : "N/A");
                        devDetails.put("email", dev.email() != null ? dev.email() : "N/A");
                        return devDetails;
                    })
                    .toList();
            pomDetails.put("developers", developers);

            pomEntries.put(artifactId, pomDetails);
        }

        yamlData.put("projects", pomEntries);

        // Write YAML to file
        try (FileWriter writer = new FileWriter(outputPath)) {
            yaml.dump(yamlData, writer);
            System.out.println("Metadata written to YAML file: " + outputPath);
        } catch (IOException e) {
            System.out.println("Error writing to YAML file.");
            e.printStackTrace();
        }
    }

    // Record for developer information
    public record Developer(String id, String name, String email, String pomFile) {
    }

    // Record for storing POM information
    public record PomInfo(List<Developer> developers, String scmUrl, String version) {
    }
}
