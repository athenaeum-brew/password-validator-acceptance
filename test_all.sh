#!/bin/bash

# <groupId>com.cthiebaud</groupId>
# <artifactId>password-validator</artifactId>
# <version>1.0.0-SNAPSHOT</version>

# Define paths and variables
TARGET_DIR="target"
DOWNLOADED_DIR="downloaded_packages"
GROUP_ID="com.cthiebaud"
ARTIFACT_ID="password-validator"
VERSION="1.1.0-SNAPSHOT"

# Function to decorate a string with a dynamic border
decorate_text() {
    local text="$1"
    local len=${#text}
    local border=$(printf '+%*s+' $((len + 2)) '' | tr ' ' '-')
    echo "$border"
    echo "| $text |"
    echo "$border"
}

# Ensure the target and downloaded directories exist
mkdir -p "$TARGET_DIR"
mkdir -p "$DOWNLOADED_DIR"

# Step 1: Clean up the target directory
echo "Cleaning up existing JARs in $TARGET_DIR..."
rm -f "$TARGET_DIR/$GROUP_ID.$ARTIFACT_ID-"*.jar

# Step 2: Download password-validator.jar
echo "Downloading $ARTIFACT_ID..."
mvn dependency:copy \
    -U \
    -Dartifact="$GROUP_ID:$ARTIFACT_ID:$VERSION:jar" \
    -DoutputDirectory="$TARGET_DIR" \
    -Dmdep.prependGroupId=true \
    -Dmdep.useBaseVersion=false \
    -Dmdep.stripVersion=false

# Step 3: Locate the downloaded JAR
DOWNLOADED_JAR=$(find "$TARGET_DIR" -name "$GROUP_ID.$ARTIFACT_ID-*.jar" | head -n 1)

if [ -n "$DOWNLOADED_JAR" ]; then
    echo "Downloaded JAR located at: $DOWNLOADED_JAR"
else
    echo "Error: JAR not found in $TARGET_DIR. Exiting."
    exit 1
fi

# Step 4: Run the Tester with each student's JAR in the downloaded_packages directory
for STUDENT_JAR in "$DOWNLOADED_DIR"/*.jar; do
    if [[ -f "$STUDENT_JAR" ]]; then
        echo -e "\n"
        basename="${STUDENT_JAR##*/}"  # Removes everything before the last '/'
        trimmed="${basename%.*}"       # Removes everything from the last '.' onward

        # Call the decoration function
        decorate_text "$trimmed"
        
        # Execute the Java program
        java -jar "$DOWNLOADED_JAR" "$STUDENT_JAR"
    else
        echo "No student JAR files found in $DOWNLOADED_DIR."
    fi
done
