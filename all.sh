#!/bin/bash

mvn clean package
mvn exec:java -Dexec.mainClass=com.cthiebaud.passwordvalidator.GitHubPackagesLister
./clear_maven_cache.sh
./download_packages.sh
mvn exec:java -Dexec.mainClass=com.cthiebaud.passwordvalidator.MetadataExtractor
./download_sources.py 
./multi-module.py
cd multi-module-project
mvn clean verify
cd target/site/apidocs 
python -m http.server
