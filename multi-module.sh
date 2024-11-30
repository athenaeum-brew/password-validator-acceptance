#!/bin/bash

PARENT_POM="multi-module-project/pom.xml"
MODULES_DIR="downloaded_sources"

PLUGIN_CONFIG="<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.5.2</version>
    <configuration>
        <failIfNoTests>true</failIfNoTests>
    </configuration>
</plugin>"

echo "Adding modules to parent POM..."

# Create a backup of the parent POM (in case of issues)
cp "$PARENT_POM" "$PARENT_POM.bak"

# Remove all subdirectories of multi-module-project
echo "Cleaning up existing modules in multi-module-project..."
find multi-module-project -mindepth 1 -maxdepth 1 -type d -exec rm -rf {} +

# Remove existing <module> entries inside <modules>...</modules>
if [ "$(uname)" = "Darwin" ]; then
    TMP_POM=$(mktemp)
    sed '/<modules>/,/<\/modules>/c\
    <modules>\
    </modules>' "$PARENT_POM" > "$TMP_POM"
    mv "$TMP_POM" "$PARENT_POM"
else
    # For GNU sed
    sed -i '/<modules>/,/<\/modules>/c <modules>\n</modules>' "$PARENT_POM"
fi

# Add new <module> entries
for project in "$MODULES_DIR"/*; do
    if [ -d "$project" ]; then
        MODULE_NAME=$(basename "$project")
        echo "Adding module: $MODULE_NAME"
        # Copy each module to the parent directory
        cp -r "$project" "multi-module-project/$MODULE_NAME"

        # Add the Surefire plugin configuration to the submodule's POM
        SUBMODULE_POM="multi-module-project/$MODULE_NAME/pom.xml"
        if [ -f "$SUBMODULE_POM" ]; then
            echo "Adding Surefire plugin to $SUBMODULE_POM"
            sed -i '/<\/plugins>/i \
            '"$PLUGIN_CONFIG" "$SUBMODULE_POM"
        else
            echo "WARNING: $SUBMODULE_POM does not exist. Skipping plugin addition."
        fi

        # Add the module to the parent POM
        if [ "$(uname)" = "Darwin" ]; then
            TMP_POM=$(mktemp)
            sed '/<modules>/a\
            <module>'"$MODULE_NAME"'</module>\
            ' "$PARENT_POM" > "$TMP_POM"
            mv "$TMP_POM" "$PARENT_POM"
        else
            sed -i '/<modules>/a <module>'"$MODULE_NAME"'</module>' "$PARENT_POM"
        fi
    fi
done

echo "Modules added successfully!"
