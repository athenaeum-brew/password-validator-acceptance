#!/bin/bash

# Save the initial directory
initial_dir=$(pwd)

# Check if the script received a parameter
if [ -z "$1" ]; then
    echo "Usage: $0 <root_directory>"
    exit 1
fi

# Change to the specified root directory
root_dir="$1"
if ! cd "$root_dir"; then
    echo "Error: Unable to access directory '$root_dir'"
    exit 1
fi

# Iterate through each first-level subdirectory
for dir in */ ; do
    if [ -d "$dir" ]; then
        # Enter the directory
        cd "$dir" || continue
        
        # Check if it's a Git repository
        if [ -d .git ]; then
            echo "Pulling updates in $dir"
            # Perform git pull and ignore errors
            # git reset --hard HEAD
            git pull || echo "Failed to pull in $dir"
            # mvn clean verify
        else
            echo "$dir is not a Git repository."
        fi
        
        # Go back to the root directory
        cd ..
    fi
done

# Restore the initial directory
cd "$initial_dir"
echo "Restored to initial directory: $initial_dir"
