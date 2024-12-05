#!/usr/bin/env python3

import os
import subprocess
import yaml

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
        maven_command = (
            f"mvn dependency:copy \\\n"
            f"  -U \\\n"
            f"  -Dartifact={artifact_identifier}:zip:project-zip \\\n"
            f"  -Dmdep.prependGroupId=true \\\n"
            f"  -Dmdep.useBaseVersion=false \\\n"
            f"  -Drepositories=\"github-repo::default::{maven_repo_url}\" \\\n"
            f"  -DoutputDirectory={destination_dir}"
        )
        print(f"No valid scmUrl for {project_name}. Use this Maven command:\n{maven_command}")
