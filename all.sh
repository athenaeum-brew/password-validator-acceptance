#!/bin/bash

mvn clean package
mvn exec:java -Dexec.mainClass=com.cthiebaud.passwordvalidator.GitHubPackagesLister
./clear_maven_cache.sh
./download_packages.sh
mvn exec:java -Dexec.mainClass=com.cthiebaud.passwordvalidator.MetadataExtractor
