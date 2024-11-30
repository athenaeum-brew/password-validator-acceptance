# password-validator-acceptance

0. run GitHubPackagesLister to create packages.txt
1. run clear_maven_cache.sh to clear local maven repo for previously downloaded packages, just in case
2. run download_packages.sh to download packages listed in packages.txt to downloaded_packages
3. run MetadataExtractor to parse scmUrl, developers, and version from downloaded *.pom files into packages_metadata.yaml
4. run test_all.sh to test every packages sequentially. Results should be hand-written to results.txt
