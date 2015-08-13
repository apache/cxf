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
package org.apache.cxf.systest.sts.itests;

import java.io.File;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;

@ExamReactorStrategy(PerClass.class)
public class BasicSTSIntegrationTest {

    protected static final String HTTP_PORT = "9191";
    protected static final String STS_ENDPOINT = "http://localhost:" + HTTP_PORT + "/cxf/X509";

    @Configuration
    public Option[] getConfig() {
        String karafVersion = System.getProperty("karaf.version", "3.0.4");
        String localRepository = System.getProperty("localRepository");
        
        MavenArtifactUrlReference karafUrl = maven() //
                        .groupId("org.apache.karaf") //
                        .artifactId("apache-karaf") //
                        .version(karafVersion)
                        .type("tar.gz");
        MavenArtifactUrlReference stsFeatures = maven() //
            .groupId("org.apache.cxf.services.sts.systests") //
            .artifactId("cxf-services-sts-systests-features") //
            .versionAsInProject() //
            .type("xml");

        return new Option[] {
            karafDistributionConfiguration().frameworkUrl(karafUrl).karafVersion(karafVersion)
                .unpackDirectory(new File("target/paxexam/unpack/")).useDeployFolder(false),
            systemProperty("java.awt.headless").value("true"),

            copy("clientKeystore.properties"),
            copy("clientstore.jks"),
            copy("etc/org.ops4j.pax.logging.cfg"),
            editConfigurationFilePut("etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port", HTTP_PORT),
            when(localRepository != null)
                .useOptions(editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg",
                            "org.ops4j.pax.url.mvn.localRepository",
                            localRepository)),
            features(stsFeatures, "cxf-sts-service"),
            configureConsole().ignoreLocalConsole(),
        };
    }

    protected Option copy(String path) {
        return replaceConfigurationFile(path, new File("src/test/resources/" + path));
    }

}
