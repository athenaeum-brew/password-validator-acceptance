#!/bin/zsh

# Path to packages.txt
PACKAGE_FILE="packages.txt"

# Directory to store downloaded files
OUTPUT_DIR="downloaded_packages"
REPO_URL="https://maven.pkg.github.com/athenaeum-brew"

# Clean the output directory
echo "Cleaning the output directory: $OUTPUT_DIR"
rm -rf "$OUTPUT_DIR"/* 2>/dev/null || true
mkdir -p "$OUTPUT_DIR"

# Path to the GitHub token file
TOKEN_FILE="src/main/resources/github-token.txt"

# Check if the token file exists
if [[ ! -f $TOKEN_FILE ]]; then
    echo "Error: Token file not found at $TOKEN_FILE"
    exit 1
fi

# Read the token from the file
GITHUB_PAT=$(<"$TOKEN_FILE")

# Check if the token is non-empty
if [[ -z $GITHUB_PAT ]]; then
    echo "Error: Token file is empty"
    exit 1
fi

resolve_snapshot_version() {
    local groupId=$1
    local artifactId=$2
    local version=$3
    local repoUrl=$4

    # Convert groupId to folder structure
    local groupPath="${groupId//.//}"
    local metadataUrl="${repoUrl}/boh/${groupPath}/${artifactId}/${version}/maven-metadata.xml"


    # Fetch maven-metadata.xml
    # echo "Fetch maven-metadata.xml from $metadataUrl" >&2
    local metadata=$(curl -u "cthiebaud:$GITHUB_PAT" -s "$metadataUrl")
    # echo "$metadata" >&2

    # Extract timestamp and buildNumber using grep and sed
    local timestamp=$(echo "$metadata" | grep -o "<timestamp>[^<]*" | sed 's/<timestamp>//')
    local buildNumber=$(echo "$metadata" | grep -o "<buildNumber>[^<]*" | sed 's/<buildNumber>//')

    # Construct the timestamped version
    if [[ -n $timestamp && -n $buildNumber ]]; then
        echo "${version%-SNAPSHOT}-${timestamp}-${buildNumber}"
    else
        echo "Warning: Unable to resolve timestamped version for $groupId:$artifactId:$version from $metadata. Falling back to base version: $version" >&2
        echo "$version" # Fallback to base version if metadata is incomplete
    fi
}

while IFS= read -r package; do
    IFS=":" read -r groupId artifactId version <<< "$package"

    # Ignore packages with groupId starting with 'com.cthiebaud', except 'password-validator-impl'
    if [[ $groupId == com.cthiebaud* && $artifactId != "password-validator-impl" ]]; then
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

    # Use Maven dependency:copy to download JAR
    mvn dependency:copy \
        -U \
        -Dartifact="$groupId:$artifactId:$resolvedVersion" \
        -Dmdep.prependGroupId=true \
        -Dmdep.useBaseVersion=false \
        -DoutputDirectory="$OUTPUT_DIR" \
        -Drepositories="github-repo::default::https://maven.pkg.github.com/athenaeum-brew"

    # Use Maven dependency:copy to download POM
    mvn -q dependency:copy \
        -U \
        -Dartifact="$groupId:$artifactId:$resolvedVersion:pom" \
        -Dmdep.prependGroupId=true \
        -Dmdep.useBaseVersion=false \
        -DoutputDirectory="$OUTPUT_DIR" \
        -Drepositories="github-repo::default::https://maven.pkg.github.com/athenaeum-brew"

done < "$PACKAGE_FILE"

echo "All packages downloaded to $OUTPUT_DIR"
