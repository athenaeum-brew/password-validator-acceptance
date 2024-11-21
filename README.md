# password-validator-acceptance

0. run GitHubPackagesLister to create packages.txt
1. run clear_maven_cache.sh to clear local maven repo for previously downloaded packages, just in case
2. run download_packages.sh to download packages listed in packages.txt to downloaded_packages
3. run DevelopersLister to parse developers from *.pom fileS in the downloaded_packages directory into developers.txt 
4. run test_all.sh to test every packages sequentially. Results are hand-written to results.txt.
