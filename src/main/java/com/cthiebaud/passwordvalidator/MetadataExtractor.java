package com.cthiebaud.passwordvalidator;

import org.w3c.dom.*;

import javax.xml.parsers.*;
import java.io.File;
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
                // Use filename (without extension) as the project key
                String projectKey = extractArtifactIdFromFileName(pomFile.getName());
                String version = extractVersionFromFileName(pomFile.getName());

                // Parse the POM file
                String artifactId = parsePOMForArtifactId(pomFile);
                String groupId = parsePOMForGroupId(pomFile);
                String scmUrl = parsePOMForScmUrl(pomFile);
                List<Developer> developers = parsePOMForDevelopers(pomFile);

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
                pomInfoByFile.put(projectKey, new PomInfo(artifactId, groupId, version, developers, scmUrl));
            } catch (Exception e) {
                System.out.println("Error processing file: " + pomFile.getName());
                e.printStackTrace();
            }
        }

        // Use the YamlWriter to write developers, SCM URL, and version to a structured
        // text file
        YamlWriter.writePomInfoToYamlFile(pomInfoByFile, "packages_metadata.yaml");
    }

    // Parse the POM file to extract the artifactId
    public static String parsePOMForArtifactId(File pomFile) throws Exception {
        return parseElementFromPOM(pomFile, "artifactId");
    }

    // Parse the POM file to extract the groupId
    public static String parsePOMForGroupId(File pomFile) throws Exception {
        return parseElementFromPOM(pomFile, "groupId");
    }

    // Parse the POM file to extract the version
    public static String parsePOMForVersion(File pomFile) throws Exception {
        return parseElementFromPOM(pomFile, "version");
    }

    // General method to parse a specific element (e.g., artifactId, groupId,
    // version) from the POM file
    public static String parseElementFromPOM(File pomFile, String tagName) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(pomFile);

        // Normalize the XML structure
        doc.getDocumentElement().normalize();

        // Locate the element
        NodeList nodeList = doc.getElementsByTagName(tagName);
        if (nodeList != null && nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }

        return null; // Return null if the element is not found
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

    // Record for developer information
    public record Developer(String id, String name, String email, String pomFile) {
    }

    // Record for storing POM information
    public record PomInfo(String artifactId, String groupId, String version, List<Developer> developers,
            String scmUrl) {
    }
}
