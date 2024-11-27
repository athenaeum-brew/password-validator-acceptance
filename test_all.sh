#!/bin/bash

# <groupId>com.cthiebaud</groupId>
# <artifactId>password-validator</artifactId>
# <version>1.0.0-SNAPSHOT</version>

# Define paths and variables
TARGET_DIR="target"
DOWNLOADED_DIR="downloaded_packages"
GROUP_ID="com.cthiebaud"
ARTIFACT_ID="password-validator"
VERSION="2.0.0-SNAPSHOT"

# Ensure the target and downloaded directories exist
mkdir -p "$TARGET_DIR"
mkdir -p "$DOWNLOADED_DIR"

# Step 1: Download password-validator.jar
echo "Downloading $ARTIFACT_ID..."
mvn dependency:copy \
    -U \
    -Dartifact="$GROUP_ID:$ARTIFACT_ID:$VERSION:jar" \
    -DoutputDirectory="$TARGET_DIR" \
    -Dmdep.prependGroupId=true \
    -Dmdep.useBaseVersion=false \
    -Dmdep.stripVersion=false

# Locate the downloaded JAR and rename it
DOWNLOADED_JAR=$(find "$TARGET_DIR" -name "$GROUP_ID.$ARTIFACT_ID-*.jar" | head -n 1)

# Step 2: Run the Tester with each student's JAR in the downloaded_packages directory
for STUDENT_JAR in "$DOWNLOADED_DIR"/*.jar; do
    if [[ -f "$STUDENT_JAR" ]]; then
        echo -e "\n"
        echo "Running Tester:"
        echo "java -jar \"$DOWNLOADED_JAR\" \"$STUDENT_JAR\""
        java -jar "$DOWNLOADED_JAR" "$STUDENT_JAR"
    else
        echo "No student JAR files found in $DOWNLOADED_DIR."
    fi
done
