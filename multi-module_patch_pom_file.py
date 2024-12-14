#!/usr/bin/env python3

import os
import shutil
import xml.etree.ElementTree as ET
from xml.dom import minidom

# Define the parent POM file location
PARENT_POM = "multi-module-project/pom.xml"

def backup_file(file_path):
    """Create a backup of the specified file."""
    backup_path = file_path + ".bak"
    shutil.copy(file_path, backup_path)
    print(f"Backup created: {backup_path}")

def prettify_xml(tree):
    """Prettify XML output with proper indentation and minimal whitespace."""
    rough_string = ET.tostring(tree.getroot(), encoding="utf-8", method="xml")
    parsed = minidom.parseString(rough_string)
    pretty_xml = parsed.toprettyxml(indent="    ")

    # Remove extra blank lines
    lines = [line for line in pretty_xml.splitlines() if line.strip()]
    return "\n".join(lines)

def patch_pom_file(parent_pom):
    """
    Patch the parent POM file by updating or commenting out specific modules.
    """
    print(f"Patching {parent_pom}...")

    if not os.path.exists(parent_pom):
        print(f"Error: File '{parent_pom}' does not exist. Exiting.")
        return

    # Parse the XML file
    tree = ET.parse(parent_pom)
    root = tree.getroot()

    # Register Maven namespace
    ns = {'': "http://maven.apache.org/POM/4.0.0"}
    ET.register_namespace('', "http://maven.apache.org/POM/4.0.0")

    # Locate the <modules> section
    modules_elem = root.find("modules", ns)
    if modules_elem is None:
        print(f"No <modules> section found in {parent_pom}. Exiting.")
        return

    # Update or comment out specific modules
    for module in list(modules_elem):  # Use list to safely modify while iterating
        if module.text == "com.thomxs1.password-validator-main":
            print(f"Found module to update: {module.text}")
            module.text = f"{module.text}/password-validator-main"
        elif module.text == "com.sinanotc.passwordvalidator":
            print(f"Found module to comment out: {module.text}")
            # Replace the module with a comment
            comment = ET.Comment(f"module>{module.text}</module")
            modules_elem.remove(module)
            modules_elem.append(comment)
        elif module.text == "com.timo.password-validator":
            print(f"Found module to comment out: {module.text}")
            # Replace the module with a comment
            comment = ET.Comment(f"module>{module.text}</module")
            modules_elem.remove(module)
            modules_elem.append(comment)

    # Write the modified XML back to the file
    with open(parent_pom, "w", encoding="utf-8") as f:
        f.write(prettify_xml(tree))
    print(f"Patching completed for {parent_pom}.")

def main():
    print("Starting the patching process...")

    # Backup the parent POM
    backup_file(PARENT_POM)

    # Apply the patch
    patch_pom_file(PARENT_POM)

    print("Patching process completed!")

if __name__ == "__main__":
    main()
