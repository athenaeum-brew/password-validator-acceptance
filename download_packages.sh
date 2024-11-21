#!/bin/bash

# Path to packages.txt
PACKAGE_FILE="packages.txt"

# Directory to store downloaded files
OUTPUT_DIR="downloaded_packages"
REPO_URL="https://maven.pkg.github.com/athenaeum-brew"

# Clean the output directory
echo "Cleaning the output directory: $OUTPUT_DIR"
rm -rf "$OUTPUT_DIR"/* 2>/dev/null || true
mkdir -p "$OUTPUT_DIR"

resolve_snapshot_version() {
    local groupId=$1
    local artifactId=$2
    local version=$3
    local repoUrl=$4

    # Convert groupId to folder structure
    local groupPath="${groupId//.//}"
    local metadataUrl="${repoUrl}/${groupPath}/${artifactId}/${version}/maven-metadata.xml"

    # Fetch maven-metadata.xml
    local metadata=$(curl -s "$metadataUrl")

    # Extract timestamp and buildNumber using grep and sed
    local timestamp=$(echo "$metadata" | grep -o "<timestamp>[^<]*" | sed 's/<timestamp>//')
    local buildNumber=$(echo "$metadata" | grep -o "<buildNumber>[^<]*" | sed 's/<buildNumber>//')

    # Construct the timestamped version
    if [[ -n $timestamp && -n $buildNumber ]]; then
        echo "${version%-SNAPSHOT}-${timestamp}-${buildNumber}"
    else
        echo "$version" # Fallback to base version if metadata is incomplete
    fi
}

while IFS= read -r package; do
    IFS=":" read -r groupId artifactId version <<< "$package"

    # Ignore packages with groupId starting with 'com.cthiebaud'
    if [[ $groupId == com.cthiebaud* ]]; then
        echo "Skipping package: $groupId:$artifactId:$version (it's yours!)"
        continue
    fi

    echo "Processing $groupId:$artifactId:$version"

    # Resolve the timestamped version if it's a SNAPSHOT
    if [[ $version == *-SNAPSHOT ]]; then
        resolvedVersion=$(resolve_snapshot_version "$groupId" "$artifactId" "$version" "$REPO_URL")
        echo "Resolved $version to $resolvedVersion"
    else
        resolvedVersion=$version
    fi

    # Convert groupId to underscore-separated format
    formattedGroupId="${groupId//./_}"

    # Construct custom file names
    jarFileName="${formattedGroupId}_${artifactId}-${resolvedVersion}.jar"
    pomFileName="${formattedGroupId}_${artifactId}-${resolvedVersion}.pom"

    # Use Maven dependency:copy to download JAR
    mvn dependency:copy \
        -U \
        -Dartifact="$groupId:$artifactId:$resolvedVersion" \
        -DoutputDirectory="$OUTPUT_DIR" \
        -Drepositories="github-repo::default::https://maven.pkg.github.com/athenaeum-brew"

    # Rename the downloaded JAR file
    if [[ -f "$OUTPUT_DIR/$artifactId-$resolvedVersion.jar" ]]; then
        mv "$OUTPUT_DIR/$artifactId-$resolvedVersion.jar" "$OUTPUT_DIR/$jarFileName"
    fi

    # Use Maven dependency:copy to download POM
    mvn -q dependency:copy \
        -U \
        -Dartifact="$groupId:$artifactId:$resolvedVersion:pom" \
        -DoutputDirectory="$OUTPUT_DIR" \
        -Drepositories="github-repo::default::https://maven.pkg.github.com/athenaeum-brew"

    # Rename the downloaded POM file
    if [[ -f "$OUTPUT_DIR/$artifactId-$resolvedVersion.pom" ]]; then
        mv "$OUTPUT_DIR/$artifactId-$resolvedVersion.pom" "$OUTPUT_DIR/$pomFileName"
    fi

done < "$PACKAGE_FILE"

echo "All packages downloaded to $OUTPUT_DIR"
