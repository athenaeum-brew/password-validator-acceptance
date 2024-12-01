# password-validator-acceptance

0. run GitHubPackagesLister to create packages.txt
1. run clear_maven_cache.sh to clear local maven repo for previously downloaded packages, just in case
2. run download_packages.sh to download packages listed in packages.txt to downloaded_packages
3. run MetadataExtractor to parse scmUrl, developers, and version from downloaded *.pom files into packages_metadata.yaml
4. run test_all.sh to test every packages sequentially. Results should be hand-written to results.txt
5. run download_sources.py to download sources, either with git clone, or downloading zip artifact
6. run multi-module.py to create a multi-module project with all packages as sub modules
7. cd multi-module-project, then run mvn clean verify to generate aggregated javadoc
8. cd target/site/apidocs, then run python -m http.server, then open browser at http://locahost:8000, inspect javadoc

## Criteria

| Criterion                                         | Points |
|--------------------------------------------------|--------|
| The JAR file containing the implementation is downloaded from athenaeum-brew maven repo, and is executed ok by the test program | 3      |
| Validation criteria is described in a README file at the root of the project           | 3      |
| Source code contains javadoc comments                                                | 3      |
| Implementation is unit tested                                                       | 3      |
| Usability evaluation                                                                 | 2      |
| Coding evaluation                                                                    | 2      |
| Test evaluation                                                                   | 2      |
| Documentation evaluation                                                            | 2      |
