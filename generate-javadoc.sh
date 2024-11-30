#!/bin/bash

BASE_DIR="downloaded_sources"
OUTPUT_DIR="merged-docs"

# Clean output directory
rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

# Iterate through each subdirectory
for project in "$BASE_DIR"/*; do
    if [ -d "$project" ]; then
        echo "Generating Javadoc for $project..."
        (
            cd "$project" || exit
            mvn javadoc:javadoc
        )
        # Copy the generated Javadoc to the merged directory
        # PROJECT_NAME=$(basename "$project")
        # mkdir -p "$OUTPUT_DIR/$PROJECT_NAME"
        # cp -r "$project/target/site/apidocs/"* "$OUTPUT_DIR/$PROJECT_NAME"
    fi
done

