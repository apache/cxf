import groovy.xml.StreamingMarkupBuilder
import groovy.xml.XmlUtil

import java.io.File
import java.util.HashMap
import java.util.logging.Logger

Logger log = Logger.getLogger("jakarta.groovy")

def outputBuilder = new StreamingMarkupBuilder()
//read javax->artifacts mapping rule
Properties props = new Properties()
File propsFile = new File(gavMap)

props.load(propsFile.newDataInputStream())
def map = new HashMap();
for (Object key : props.keySet()) {
    map.put(key.toString(), props.get(key).toString())
}

File pomDir = new File(pomDir)
File ouputDir = new File(pomOutDir)
if (!ouputDir.exists()) {
    ouputDir.mkdirs()
}
def suffix = "-" + jakartaSuffix

File[] pomFiles = pomDir.listFiles()
for (File pomFile : pomFiles) {
    def pom = new XmlSlurper().parse(pomFile)
    //update artifact name

    pom.artifactId.each {
        it.replaceBody(it.text() + suffix)
    }
    //update parent artifact name
    pom.parent.artifactId.each {
        it.replaceBody(it.text() + suffix)
    }

    //update dependencies
    def deps = pom.depthFirst().findAll { it.name() == 'dependency' }
    deps.each {
        String groupId = it.groupId.text()
        String artifactId = it.artifactId.text();
        String versionId = it.version.text();
        if (groupId.equals("org.apache.cxf")) {
            it.artifactId.replaceBody(artifactId + suffix)
        }
        for (Object key : map.keySet()) {
            String keyString = key.toString()
            String[] gav = keyString.split(";")
            String mapString = map.get(key).toString()
            String[] mapGav = mapString.split(";")
            if (gav.length == 3) {
                if (groupId.equals(gav[0]) && artifactId.equals(gav[1]) && gav[2].equals("*")) {
                    it.groupId.replaceBody(mapGav[0])
                    it.artifactId.replaceBody(mapGav[1])
                    if (!versionId.isEmpty()) {
                        it.version.replaceBody(mapGav[2])
                    }
                }
            }

        }
    }

    String result = outputBuilder.bind {
        mkp.declareNamespace("": "http://maven.apache.org/POM/4.0.0")
        mkp.yield pom
    }
    File outputFile = new File(ouputDir.toString(), pomFile.getName())
    def writer = outputFile.newWriter()
    writer << XmlUtil.serialize(result)
    writer.close()


}








