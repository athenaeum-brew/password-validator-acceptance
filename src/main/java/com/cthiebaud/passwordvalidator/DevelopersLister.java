package com.cthiebaud.passwordvalidator;

import org.w3c.dom.*;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.xml.parsers.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class DevelopersLister {

    public static void main(String[] args) {
        // Directory containing the POM files
        String pomDirPath = "downloaded_packages";

        // Get the list of all .pom files in the directory
        File pomDir = new File(pomDirPath);
        File[] pomFiles = pomDir.listFiles((dir, name) -> name.endsWith(".pom"));

        if (pomFiles == null || pomFiles.length == 0) {
            System.out.println("No .pom files found in the directory.");
            return;
        }

        // Map to store developers and SCM URL grouped by their POM file
        Map<String, PomInfo> pomInfoByFile = new LinkedHashMap<>();

        // Process each .pom file
        for (File pomFile : pomFiles) {
            System.out.println("Processing: " + pomFile.getName());
            try {
                // Parse the POM file for developers and SCM URL
                List<Developer> developers = parsePOMForDevelopers(pomFile);
                String scmUrl = parsePOMForScmUrl(pomFile);

                // Store the POM information
                pomInfoByFile.put(pomFile.getName(), new PomInfo(developers, scmUrl));
            } catch (Exception e) {
                System.out.println("Error processing file: " + pomFile.getName());
                e.printStackTrace();
            }
        }

        // Write developers and SCM URL to a structured text file
        // writePomInfoToStructuredTextFile(pomInfoByFile, "developers.txt");
        writePomInfoToYamlFile(pomInfoByFile, "developers.yaml");
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
                String url = getElementValue(developerElement, "url");

                // Create a Developer record and add it to the list
                developers.add(new Developer(id, name, email, url, pomFile.getName()));
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
            String pomFile = entry.getKey();
            PomInfo info = entry.getValue();

            Map<String, Object> pomDetails = new HashMap<>();
            pomDetails.put("scmUrl", info.scmUrl() != null ? info.scmUrl() : "N/A");

            List<Map<String, String>> developers = info.developers().stream()
                    .map(dev -> {
                        Map<String, String> devDetails = new HashMap<>();
                        devDetails.put("id", dev.id() != null ? dev.id() : "N/A");
                        devDetails.put("name", dev.name() != null ? dev.name() : "N/A");
                        devDetails.put("email", dev.email() != null ? dev.email() : "N/A");
                        devDetails.put("url", dev.url() != null ? dev.url() : "N/A");
                        return devDetails;
                    })
                    .toList();
            pomDetails.put("developers", developers);

            pomEntries.put(pomFile, pomDetails);
        }

        yamlData.put("poms", pomEntries);

        // Write YAML to file
        try (FileWriter writer = new FileWriter(outputPath)) {
            yaml.dump(yamlData, writer);
            System.out.println("Developers and SCM URLs written to YAML file: " + outputPath);
        } catch (IOException e) {
            System.out.println("Error writing to YAML file.");
            e.printStackTrace();
        }
    }

    // Record for developer information
    public record Developer(String id, String name, String email, String url, String pomFile) {
    }

    // Record for storing POM information
    public record PomInfo(List<Developer> developers, String scmUrl) {
    }
}
