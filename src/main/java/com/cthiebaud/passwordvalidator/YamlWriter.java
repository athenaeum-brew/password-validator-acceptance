package com.cthiebaud.passwordvalidator;

import com.cthiebaud.passwordvalidator.MetadataExtractor.Developer;
import com.cthiebaud.passwordvalidator.MetadataExtractor.PomInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class YamlWriter {

    /**
     * Writes the given POM information to a YAML file.
     *
     * @param pomInfoByFile A map containing artifact IDs and their corresponding
     *                      POM information.
     * @param outputPath    The path of the output YAML file.
     */
    public static void writePomInfoToYamlFile(Map<String, PomInfo> pomInfoByFile, String outputPath) {
        try {
            // Prepare the data structure for YAML
            Map<String, Object> yamlData = Map.of(
                    "totalDevelopers", pomInfoByFile.values().stream()
                            .mapToInt(info -> info.developers().size())
                            .sum(),
                    "projects", prepareProjectsMap(pomInfoByFile));

            // Configure YAMLFactory to suppress unnecessary quotes
            YAMLFactory yamlFactory = new YAMLFactory()
                    .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES); // Suppress quotes when not needed
            ObjectMapper yamlMapper = new ObjectMapper(yamlFactory);

            // Write YAML data to the specified output file
            yamlMapper.writeValue(new File(outputPath), yamlData);

            System.out.println("Metadata written to YAML file: " + outputPath);
        } catch (IOException e) {
            System.out.println("Error writing to YAML file.");
            e.printStackTrace();
        }
    }

    /**
     * Prepares the "projects" section of the YAML structure.
     *
     * @param pomInfoByFile A map containing artifact IDs and their corresponding
     *                      POM information.
     * @return A map representing the "projects" section of the YAML.
     */
    private static Map<String, Object> prepareProjectsMap(Map<String, PomInfo> pomInfoByFile) {
        return pomInfoByFile.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            PomInfo info = entry.getValue();
                            return Map.of(
                                    "scmUrl", info.scmUrl() != null ? info.scmUrl() : "N/A",
                                    "version", info.version() != null ? info.version() : "Unknown",
                                    "developers", prepareDevelopersList(info.developers()));
                        }));
    }

    /**
     * Prepares the developers list for the YAML structure.
     *
     * @param developers A list of developers associated with a POM file.
     * @return A list of maps representing developer details.
     */
    private static List<Map<String, String>> prepareDevelopersList(List<Developer> developers) {
        return developers.stream()
                .map(dev -> Map.of(
                        "id", dev.id() != null ? dev.id() : "N/A",
                        "name", dev.name() != null ? dev.name() : "N/A",
                        "email", dev.email() != null ? dev.email() : "N/A"))
                .toList();
    }
}
