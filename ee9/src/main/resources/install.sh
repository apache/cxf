#!/bin/sh
#install.sh outputDir
outputDir=$1/../output
pomDir=$1
for entry in $1/*
do
  fname="$(basename -- $entry)"
  sname="$(basename -s .pom $fname)"
  versionId=$(echo $sname | rev | cut -d'-' -f 1 | rev)
  artifactId=${sname%%"-$versionId"}
  groupId=org.apache.cxf
  if [[ $sname == cxf-[0-9]* ]] || [[ $sname == cxf-parent-* ]] || [[ $sname == cxf-bom-* ]];then
    mvn -N install:install-file -DgroupId=org.apache.cxf \
                             -DartifactId=$artifactId-jakarta \
                             -Dversion=$versionId \
                             -Dfile=$pomDir/$artifactId-$versionId.pom \
                             -Dpackaging=pom
  else
    if [[ $artifactId == cxf-services-sts-core ]]; then
      groupId=org.apache.cxf.services.sts
    fi
    if [[ $artifactId == cxf-services-ws-discovery-api ]]; then
      groupId=org.apache.cxf.services.ws-discovery
    fi
    mvn install:install-file -DgroupId=$groupId \
                             -DartifactId=$artifactId-jakarta \
                             -Dversion=$versionId \
                             -Dfile=$outputDir/$artifactId-$versionId.jar \
                             -Dsources=$outputDir/$artifactId-$versionId-sources.jar \
                             -Djavadoc=$outputDir/$artifactId-$versionId-javadoc.jar \
                             -DpomFile=$pomDir/$artifactId-$versionId.pom \
                             -Dpackaging=jar
  fi
done