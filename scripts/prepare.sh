# This script installs yasgl graph library.

CWD=$( cd $( dirname $0 ) && pwd )

mvn install:install-file -Dfile=${CWD}/yasgl-1.0-SNAPSHOT.jar -DpomFile=${CWD}/yasgl-1.0-SNAPSHOT.pom \
     -DgroupId=edu.illinois -DartifactId=yasgl -Dversion=1.0-SNAPSHOT -Dpackaging=jar
