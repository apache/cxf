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

import org.apache.karaf.tooling.exam.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.w3._2002._03.xkms_wsdl.XKMSPortType;

import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.editConfigurationFilePut;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.features;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.keepRuntimeFolder;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.logLevel;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.replaceConfigurationFile;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;

@ExamReactorStrategy(PerClass.class)
public class BasicIntegrationTest {

    private static final String HTTP_PORT = "9191";
    private static final String XKMS_ENDPOINT = "http://localhost:" + HTTP_PORT + "/cxf/XKMS";

    @Inject
    protected XKMSPortType xkmsService;

    @Configuration
    public Option[] getConfig() {

        String projectVersion = System.getProperty("project.version");
        String karafVersion = System.getProperty("karaf.version");
        MavenArtifactUrlReference karafUrl = maven().groupId("org.apache.karaf").artifactId("apache-karaf")
            .version(karafVersion).type("zip");
        MavenUrlReference cxfFeatures = maven().groupId("org.apache.cxf.karaf").artifactId("apache-cxf")
            .version(projectVersion).type("xml").classifier("features");
        MavenUrlReference xkmsFeatures = maven().groupId("org.apache.cxf.services.xkms")
            .artifactId("cxf-services-xkms-features").version(projectVersion).type("xml");

        return new Option[] {
            karafDistributionConfiguration().frameworkUrl(karafUrl).karafVersion(karafVersion)
                .unpackDirectory(new File("target/paxexam/unpack/")).useDeployFolder(false),
            /*
             * Timeout is set to 15 minutes because installation of cxf and xkms takes ages. The reason should
             * be investigated in the near future. One problem is the usage of pax exam snapshot build which
             * makes maven scan the snapshot repositories for each dependency but that should not be the main
             * reason.
             */
            systemTimeout(900000),
            logLevel(LogLevel.ERROR),
            keepRuntimeFolder(),

            replaceConfigurationFile("data/xkms/certificates/trusted_cas/root.cer",
                                     new File(
                                              "src/test/resources/data/xkms/certificates/trusted_cas/root.cer")),
            replaceConfigurationFile("data/xkms/certificates/cas/alice.cer",
                                     new File("src/test/resources/data/xkms/certificates/cas/alice.cer")),
            replaceConfigurationFile("etc/org.apache.cxf.xkms.cfg",
                                     new File("src/test/resources/etc/org.apache.cxf.xkms.cfg")),

            features(cxfFeatures, "cxf"), features(xkmsFeatures, "cxf-xkms-service", "cxf-xkms-client"),

            editConfigurationFilePut("etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port", HTTP_PORT),
            editConfigurationFilePut("etc/org.apache.cxf.xkms.client.cfg", "xkms.endpoint", XKMS_ENDPOINT)
        };
    }

}
