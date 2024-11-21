#!/bin/bash

# <groupId>com.cthiebaud</groupId>
# <artifactId>password-validator</artifactId>
# <version>1.0.0-SNAPSHOT</version>

# Define paths and variables
TARGET_DIR="target"
DOWNLOADED_DIR="downloaded_packages"
GROUP_ID="com.cthiebaud"
ARTIFACT_ID="password-validator"
VERSION="1.0.0-SNAPSHOT"

# Create a JAR name with groupId included, using '-' as a separator
OUTPUT_JAR="$TARGET_DIR/${GROUP_ID//./-}-$ARTIFACT_ID-$VERSION.jar"

# Ensure the target and downloaded directories exist
mkdir -p "$TARGET_DIR"
mkdir -p "$DOWNLOADED_DIR"

# Step 1: Download password-validator.jar only once if it doesn't already exist
if [[ ! -f "$OUTPUT_JAR" ]]; then
    echo "Downloading $ARTIFACT_ID..."
    mvn dependency:copy \
        -Dartifact="$GROUP_ID:$ARTIFACT_ID:$VERSION" \
        -DoutputDirectory="$TARGET_DIR" \
        -Dmdep.stripVersion=false

    # Locate the downloaded JAR and rename it
    DOWNLOADED_JAR=$(find "$TARGET_DIR" -name "$ARTIFACT_ID-*.jar" | head -n 1)

    if [[ -z "$DOWNLOADED_JAR" ]]; then
        echo "Error: Failed to download $ARTIFACT_ID to the target directory."
        exit 1
    fi

    mv "$DOWNLOADED_JAR" "$OUTPUT_JAR"

    if [[ ! -f "$OUTPUT_JAR" ]]; then
        echo "Error: Failed to rename downloaded JAR to include groupId."
        exit 1
    fi
else
    echo "$ARTIFACT_ID is already downloaded: $OUTPUT_JAR"
fi

# Step 2: Run the Tester with each student's JAR in the downloaded_packages directory
for STUDENT_JAR in "$DOWNLOADED_DIR"/*.jar; do
    if [[ -f "$STUDENT_JAR" ]]; then
        echo "Running Tester with student JAR: $STUDENT_JAR"
        java -jar "$OUTPUT_JAR" "$STUDENT_JAR"
    else
        echo "No student JAR files found in $DOWNLOADED_DIR."
    fi
done
