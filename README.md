# password-validator-acceptance

0. run GitHubPackagesLister to create packages.txt
1. run clear_maven_cache.sh to clear local maven repo for previously downloaded packages, just in case
2. run download_packages.sh to download packages listed in packages.txt to downloaded_packages
3. run MetadataExtractor to parse scmUrl, developers, and version from downloaded *.pom files into packages_metadata.yaml
4. run test_all.sh to test every packages sequentially. Results should be hand-written to results.txt
5. run download_sources.py to download sources, either with git clone, or downloading zip artifact
6. run multi-module.py to create a multi-module project with all packages as sub modules
7. cd multi-module-project, then run mvn clean verify to generate aggregated javadoc
8. cd multi-module-project/target/site/apidocs, then run python -m http.server, then open browser at http://localhost:8000

## Criteria

| Criterion                                        | Points |
|--------------------------------------------------|--------|
| - The JAR file containing the implementation is downloaded from the Athenaeum-Brew Maven repository and is successfully executed by the test program  | 3 |
| - The validation criteria are outlined in a README file located at the root of the project                                                            | 3 |
| - The source code includes Javadoc comments                                                                                                           | 3 |
| - The implementation is accompanied by unit tests                                                                                                     | 3 |
| - Usability assessment                                                                                                                                | 2 |
| - Code quality assessment                                                                                                                             | 2 |
| - Test coverage assessment                                                                                                                            | 2 |
| - Documentation quality assessment                                                                                                                    | 2 |


java -jar "/Users/christophe.thiebaud/github.com/athenaeum-brew/password-validator/target/password-validator-1.1.0-SNAPSHOT.jar" 










