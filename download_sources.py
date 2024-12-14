#!/usr/bin/env python3

import os
import subprocess
import yaml
import zipfile
import shutil

# File containing the metadata
metadata_file = "packages_metadata.yaml"

# Directory to store the cloned or downloaded projects
destination_dir = "downloaded_sources"

# Ensure the directory exists
os.makedirs(destination_dir, exist_ok=True)

# Base Maven repository URL for projects without scmUrl
maven_repo_url = "https://maven.pkg.github.com/athenaeum-brew/maven-repo"

# Read data from the YAML file
with open(metadata_file, "r") as file:
    project_data = yaml.safe_load(file)

# Iterate through projects
for project_name, project_info in project_data["projects"].items():
    scm_url = project_info.get("scmUrl")
    project_dir = os.path.join(destination_dir, project_name)
    
    if scm_url and scm_url != "N/A":
        # Check if the directory already exists
        if os.path.exists(project_dir):
            print(f"Directory for {project_name} already exists. Resetting and pulling latest changes...")
            try:
                # Navigate to the project directory and reset + pull
                subprocess.run(
                    ["git", "-C", project_dir, "reset", "--hard", "HEAD"],
                    check=True,
                )
                subprocess.run(
                    ["git", "-C", project_dir, "pull"],
                    check=True,
                )
            except subprocess.CalledProcessError as e:
                print(f"Failed to reset or pull for {project_name}: {e}")
        else:
            # Clone the repository
            try:
                print(f"Cloning {scm_url} for project {project_name}...")
                subprocess.run(
                    ["git", "clone", scm_url, project_dir],
                    check=True,
                )
            except subprocess.CalledProcessError as e:
                print(f"Failed to clone {scm_url}: {e}")
    else:
        # Extract artifactId, groupId, and version from metadata
        group_id = project_info.get("groupId", "unknown.groupId")
        artifact_id = project_info.get("artifactId", "unknown.artifactId")
        version = project_info.get("version", "unknown.version")
        
        # Generate Maven command for projects without a valid scmUrl
        artifact_identifier = f"{group_id}:{artifact_id}:{version}"
        maven_command = [
            "mvn", "dependency:copy",
            "-U",
            f"-Dartifact={artifact_identifier}:zip:project-zip",
            "-Dmdep.prependGroupId=true",
            "-Dmdep.useBaseVersion=false",
            f"-Drepositories=github-repo::default::{maven_repo_url}",
            f"-DoutputDirectory={destination_dir}"
        ]

        print(f"No valid scmUrl for {project_name}. Running Maven command...")
        try:
            subprocess.run(maven_command, check=True)
            
            # Locate the downloaded ZIP file
            zip_filename = os.path.join(destination_dir, f"{group_id}.{artifact_id}-{version}-project-zip.zip")

            if os.path.exists(zip_filename):
                print(f"Decompressing {zip_filename}...")
                with zipfile.ZipFile(zip_filename, 'r') as zip_ref:
                    temp_extract_dir = os.path.join(destination_dir, f"{project_name}_temp")
                    zip_ref.extractall(temp_extract_dir)

                    # Check if the extracted content contains a single directory
                    extracted_items = os.listdir(temp_extract_dir)
                    if len(extracted_items) == 1 and os.path.isdir(os.path.join(temp_extract_dir, extracted_items[0])):
                        inner_dir = os.path.join(temp_extract_dir, extracted_items[0])
                        for item in os.listdir(inner_dir):
                            src_path = os.path.join(inner_dir, item)
                            dest_path = os.path.join(destination_dir, project_name, item)
                            shutil.move(src_path, dest_path)
                    else:
                        for item in extracted_items:
                            src_path = os.path.join(temp_extract_dir, item)
                            dest_path = os.path.join(destination_dir, project_name, item)
                            shutil.move(src_path, dest_path)

                    # Remove the temporary directory
                    shutil.rmtree(temp_extract_dir)

                # Delete the ZIP file after extraction
                os.remove(zip_filename)
                print(f"Deleted ZIP file {zip_filename}")
            else:
                print(f"Expected ZIP file not found: {zip_filename}")

        except subprocess.CalledProcessError as e:
            print(f"Failed to run Maven command for {project_name}: {e}")
