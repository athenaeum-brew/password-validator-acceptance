package com.cthiebaud.passwordvalidator;

import com.cthiebaud.passwordvalidator.MetadataExtractor.Developer;
import com.cthiebaud.passwordvalidator.MetadataExtractor.PomInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
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
                                        .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES); // Suppress quotes when not
                                                                                        // needed
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
                                                        Map<String, Object> projectMap = new LinkedHashMap<>();
                                                        put2map(info, "groupId", "Unknown", projectMap);
                                                        put2map(info, "artifactId", "Unknown", projectMap);
                                                        put2map(info, "version", "Unknown", projectMap);
                                                        put2map(info, "scmUrl", "N/A", projectMap);
                                                        projectMap.put("developers",
                                                                        prepareDevelopersList(info.developers()));
                                                        return projectMap;
                                                }));
        }

        /**
         * Dynamically adds a key-value pair to the map using reflection.
         *
         * @param info         The object containing the field.
         * @param fieldName    The name of the field (or getter method) to invoke.
         * @param defaultValue The default value to use if the field value is null.
         * @param map          The map to which the key-value pair is added.
         */
        private static void put2map(Object info, String fieldName, Object defaultValue, Map<String, Object> map) {
                try {
                        // Use the field name directly as the method name
                        Method method = info.getClass().getMethod(fieldName);
                        Object value = method.invoke(info); // Invoke the method on the object

                        // If defaultValue is null, enforce strict validation
                        if (defaultValue == null) {
                                if (value == null || (value instanceof String && ((String) value).isBlank())) {
                                        throw new IllegalArgumentException("Field '" + fieldName
                                                        + "' must not be null, blank, or missing.");
                                }
                        }

                        // Put the value or default value into the map
                        map.put(fieldName, value != null ? value : defaultValue);
                } catch (Exception e) {
                        // Handle any exception and rethrow as RuntimeException with details
                        throw new RuntimeException(
                                        "Error accessing field '" + fieldName + "' in record: "
                                                        + info.getClass().getName(),
                                        e);
                }
        }

        /**
         * Prepares the developers list for the YAML structure.
         *
         * @param developers A list of developers associated with a POM file.
         * @return A list of maps representing developer details.
         */
        private static List<Map<String, String>> prepareDevelopersList(List<Developer> developers) {
                return developers.stream()
                                .map(dev -> {
                                        Map<String, String> devMap = new LinkedHashMap<>();
                                        devMap.put("id", dev.id() != null ? dev.id() : "N/A");
                                        devMap.put("name", dev.name() != null ? dev.name() : "N/A");
                                        devMap.put("email", dev.email() != null ? dev.email() : "N/A");
                                        return devMap;
                                })
                                .toList();
        }
}
