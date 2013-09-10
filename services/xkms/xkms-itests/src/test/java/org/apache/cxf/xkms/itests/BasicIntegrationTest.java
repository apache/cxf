/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.xkms.itests;

import java.io.File;

import javax.inject.Inject;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.util.Filter;
import org.w3._2002._03.xkms_wsdl.XKMSPortType;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;

@ExamReactorStrategy(PerClass.class)
public class BasicIntegrationTest {

    private static final String HTTP_PORT = "9191";
    private static final String XKMS_ENDPOINT = "http://localhost:" + HTTP_PORT + "/cxf/XKMS";
    
    // Adding apache snapshots as cxf trunk may contain snapshot dependencies
    private static final String REPOS = "http://repo1.maven.org/maven2@id=central, " 
//        + "http://svn.apache.org/repos/asf/servicemix/m2-repo@id=servicemix, "
        + "http://repository.apache.org/content/groups/snapshots-group@snapshots@noreleases@id=apache-snapshots ";
//        + "http://repository.springsource.com/maven/bundles/release@id=springsource.release, "
//        + "http://repository.springsource.com/maven/bundles/external@id=springsource.external, "
//        + "http://oss.sonatype.org/content/repositories/releases/@id=sonatype"; 

    @Inject
    @Filter(timeout = 20000)
    protected XKMSPortType xkmsService;

    @Configuration
    public Option[] getConfig() {

        String projectVersion = System.getProperty("project.version");
        String karafVersion = System.getProperty("karaf.version");
        MavenArtifactUrlReference karafUrl = maven().groupId("org.apache.karaf").artifactId("apache-karaf")
            .version(karafVersion).type("tar.gz");
        MavenUrlReference xkmsFeatures = maven().groupId("org.apache.cxf.services.xkms")
            .artifactId("cxf-services-xkms-features").version(projectVersion).type("xml");

        return new Option[] {
            karafDistributionConfiguration().frameworkUrl(karafUrl).karafVersion(karafVersion)
                .unpackDirectory(new File("target/paxexam/unpack/")).useDeployFolder(false),
            logLevel(LogLevel.INFO),
            systemProperty("java.awt.headless").value("true"),

            replaceConfigurationFile("data/xkms/certificates/trusted_cas/root.cer",
                                     new File("src/test/resources/data/xkms/certificates/trusted_cas/root.cer")),
            replaceConfigurationFile("data/xkms/certificates/trusted_cas/wss40CA.cer",
                                     new File("src/test/resources/data/xkms/certificates/trusted_cas/wss40CA.cer")),
            replaceConfigurationFile("data/xkms/certificates/cas/alice.cer",
                                     new File("src/test/resources/data/xkms/certificates/cas/alice.cer")),
            replaceConfigurationFile("data/xkms/certificates/dave.cer",
                                     new File("src/test/resources/data/xkms/certificates/dave.cer")),
            replaceConfigurationFile("data/xkms/certificates/crls/wss40CACRL.cer",
                                     new File("src/test/resources/data/xkms/certificates/crls/wss40CACRL.cer")),
            replaceConfigurationFile("etc/org.apache.cxf.xkms.cfg", getConfigFile()),

            editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg", "org.ops4j.pax.url.mvn.repositories", REPOS), 
            editConfigurationFilePut("etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port", HTTP_PORT),
            editConfigurationFilePut("etc/org.apache.cxf.xkms.client.cfg", "xkms.endpoint", XKMS_ENDPOINT),
            features(xkmsFeatures, "cxf-xkms-service", "cxf-xkms-client"),
            //CoreOptions.vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005")
        };
    }

    protected File getConfigFile() {
        return new File("src/test/resources/etc/org.apache.cxf.xkms.cfg");
    }

}
