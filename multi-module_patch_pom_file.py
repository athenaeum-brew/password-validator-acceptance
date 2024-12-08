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
    Patch the parent POM file by commenting out a specific module.
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

    # Find and comment out the specified module multi-module-project/
    for module in modules_elem.findall("module", ns):
        if module.text == "com.thomxs1.password-validator-main":
            print(f"Found module to update: {module.text}")
            # Update the module text with the new value
            module.text = f"{module.text}/password-validator-main"
            break
    else:
        print(f"Module 'com.zipse.length-password-validator' not found. Skipping patch.")

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
