#!/usr/bin/env python3

import os
import shutil
import xml.etree.ElementTree as ET

from xml.dom import minidom

PARENT_POM = "multi-module-project/pom.xml"
MODULES_DIR = "downloaded_sources"

PLUGIN_CONFIG = """<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.5.2</version>
    <configuration>
        <failIfNoTests>true</failIfNoTests>
    </configuration>
</plugin>"""

# Register Maven namespace
ET.register_namespace('', "http://maven.apache.org/POM/4.0.0")

def backup_file(file_path):
    backup_path = file_path + ".bak"
    shutil.copy(file_path, backup_path)
    print(f"Backup created: {backup_path}")

def clean_subdirectories(directory):
    print(f"Cleaning up existing subdirectories in {directory}...")
    for item in os.listdir(directory):
        item_path = os.path.join(directory, item)
        if os.path.isdir(item_path):
            shutil.rmtree(item_path)
            print(f"Removed: {item_path}")

def reset_modules_section(parent_pom):
    print(f"Resetting <modules> section in {parent_pom}...")
    tree = ET.parse(parent_pom)
    root = tree.getroot()

    ns = {'': "http://maven.apache.org/POM/4.0.0"}
    modules_elems = root.findall("modules", ns)  # Find all <modules> sections

    # Remove all existing <modules> sections
    for modules_elem in modules_elems:
        root.remove(modules_elem)

    # Add a fresh, empty <modules> section
    ET.SubElement(root, "{http://maven.apache.org/POM/4.0.0}modules")

    tree.write(parent_pom, encoding="utf-8", xml_declaration=True)
    print(f"<modules> section reset.")

def add_module_to_parent_pom(parent_pom, module_name):
    print(f"Adding module: {module_name} to parent POM...")
    tree = ET.parse(parent_pom)
    root = tree.getroot()

    ns = {'': "http://maven.apache.org/POM/4.0.0"}
    modules_elem = root.find("modules", ns)
    if modules_elem is None:
        modules_elem = ET.SubElement(root, "{http://maven.apache.org/POM/4.0.0}modules")

    module_elem = ET.SubElement(modules_elem, "{http://maven.apache.org/POM/4.0.0}module")
    module_elem.text = module_name

    # Write prettified XML
    with open(parent_pom, "w", encoding="utf-8") as f:
        f.write(prettify_xml(tree))
    print(f"Module {module_name} added to parent POM.")

def add_surefire_plugin_to_submodule(submodule_pom):
    print(f"Adding Surefire plugin to {submodule_pom}...")
    tree = ET.parse(submodule_pom)
    root = tree.getroot()

    ns = {'': "http://maven.apache.org/POM/4.0.0"}
    build_elem = root.find("build", ns)
    if build_elem is None:
        build_elem = ET.SubElement(root, "{http://maven.apache.org/POM/4.0.0}build")

    plugins_elem = build_elem.find("plugins", ns)
    if plugins_elem is None:
        plugins_elem = ET.SubElement(build_elem, "{http://maven.apache.org/POM/4.0.0}plugins")

    # Check if the plugin already exists
    for plugin in plugins_elem.findall("plugin", ns):
        artifact_id = plugin.find("artifactId", ns)
        if artifact_id is not None and artifact_id.text == "maven-surefire-plugin":
            print(f"Surefire plugin already exists in {submodule_pom}. Skipping.")
            return

    # Parse the PLUGIN_CONFIG and append it
    plugin_elem = ET.fromstring(PLUGIN_CONFIG)
    plugins_elem.append(plugin_elem)

    tree.write(submodule_pom, encoding="utf-8", xml_declaration=True)
    print(f"Surefire plugin added to {submodule_pom}.")

def prettify_xml(tree):
    """Prettify XML output with proper indentation and minimal whitespace."""
    rough_string = ET.tostring(tree.getroot(), encoding="utf-8", method="xml")
    parsed = minidom.parseString(rough_string)
    pretty_xml = parsed.toprettyxml(indent="    ")

    # Remove extra blank lines
    lines = [line for line in pretty_xml.splitlines() if line.strip()]
    return "\n".join(lines)


def reset_modules_section(parent_pom):
    print(f"Resetting <modules> section in {parent_pom}...")
    tree = ET.parse(parent_pom)
    root = tree.getroot()

    ns = {'': "http://maven.apache.org/POM/4.0.0"}
    modules_elems = root.findall("modules", ns)  # Find all <modules> sections

    # Remove all existing <modules> sections
    for modules_elem in modules_elems:
        root.remove(modules_elem)

    # Add a fresh, empty <modules> section
    ET.SubElement(root, "{http://maven.apache.org/POM/4.0.0}modules")

    # Write prettified XML
    with open(parent_pom, "w", encoding="utf-8") as f:
        f.write(prettify_xml(tree))
    print(f"<modules> section reset.")    

def main():
    print("Adding modules to parent POM...")

    # Backup the parent POM
    backup_file(PARENT_POM)

    # Remove all subdirectories of multi-module-project
    clean_subdirectories("multi-module-project")

    # Reset <modules> section in parent POM
    reset_modules_section(PARENT_POM)

    # Add new modules
    for project in os.listdir(MODULES_DIR):
        project_path = os.path.join(MODULES_DIR, project)
        if os.path.isdir(project_path):
            # Copy the project to the multi-module-project directory
            dest_path = os.path.join("multi-module-project", project)
            shutil.copytree(project_path, dest_path)
            print(f"Copied {project_path} to {dest_path}")

            # Add the module to the parent POM
            add_module_to_parent_pom(PARENT_POM, project)

            # Add Surefire plugin to the submodule POM
            submodule_pom = os.path.join(dest_path, "pom.xml")
            if os.path.exists(submodule_pom):
                add_surefire_plugin_to_submodule(submodule_pom)
            else:
                print(f"WARNING: {submodule_pom} does not exist. Skipping plugin addition.")

    print("Modules added successfully!")

if __name__ == "__main__":
    main()
