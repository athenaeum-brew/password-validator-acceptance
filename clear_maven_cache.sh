#!/bin/zsh

# Path to the packages.txt file
PACKAGE_FILE="packages.txt"

# Local Maven repository path
M2_REPO="$HOME/.m2/repository"

# Iterate through each line in the packages.txt file
while IFS= read -r package; do
    IFS=":" read -r groupId artifactId version <<< "$package"

    # Convert groupId to folder structure (dots replaced with slashes)
    groupPath="${groupId//.//}"

    # Construct the full path to the artifact directory
    artifactDir="$M2_REPO/$groupPath/$artifactId/$version"

    # Remove the directory if it exists
    if [[ -d "$artifactDir" ]]; then
        echo "Removing cached directory: $artifactDir"
        rm -rf "$artifactDir"
    else
        echo "No cached directory found for: $artifactDir"
    fi
done < "$PACKAGE_FILE"

echo "Local Maven repository cleanup completed."
