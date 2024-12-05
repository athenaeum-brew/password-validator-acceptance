#!/bin/zsh

mvn clean package
mvn exec:java -Dexec.mainClass=com.cthiebaud.passwordvalidator.GitHubPackagesLister
./clear_maven_cache.sh
./download_packages.sh
mvn exec:java -Dexec.mainClass=com.cthiebaud.passwordvalidator.MetadataExtractor
./download_sources.py 
./multi-module.py
./multi-module_patch_pom_file.py
cd multi-module-project
mvn clean verify
