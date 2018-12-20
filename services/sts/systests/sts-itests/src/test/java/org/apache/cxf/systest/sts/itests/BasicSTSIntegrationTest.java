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

import org.apache.cxf.testutil.common.TestUtil;

import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.container.internal.JavaVersionUtil;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.extra.VMOption;
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

    @Configuration
    public Option[] getConfig() {
        String port = TestUtil.getPortNumber(BasicSTSIntegrationTest.class);
        System.setProperty("BasicSTSIntegrationTest.PORT", port);
        
        String karafVersion = System.getProperty("karaf.version", "4.0.8");
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
        
        if (JavaVersionUtil.getMajorVersion() >= 9) {
            return new Option[] {
                                 karafDistributionConfiguration().frameworkUrl(karafUrl)
                                     .karafVersion(karafVersion)
                                     .unpackDirectory(new File("target/paxexam/unpack/"))
                                     .useDeployFolder(false),
                                 systemProperty("java.awt.headless").value("true"),
                                 systemProperty("BasicSTSIntegrationTest.PORT").value(port),

                copy("clientKeystore.properties"), copy("clientstore.jks"),
                                 copy("etc/org.ops4j.pax.logging.cfg"),
                                 editConfigurationFilePut("etc/org.ops4j.pax.web.cfg",
                                                          "org.osgi.service.http.port", port),
                                 when(localRepository != null)
                                     .useOptions(editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg",
                                                                          "org.ops4j.pax.url.mvn.localRepository",
                                                                          localRepository)),
                                 features(stsFeatures, "cxf-sts-service"),
                                 configureConsole().ignoreLocalConsole().ignoreRemoteShell(),
            new VMOption("--add-reads=java.xml=java.logging"),
            new VMOption("--add-exports=java.base/"
                + "org.apache.karaf.specs.locator=java.xml,ALL-UNNAMED"),
            new VMOption("--patch-module"),
            new VMOption("java.base=lib/endorsed/org.apache.karaf.specs.locator-"
                + System.getProperty("karaf.version", "4.2.2")
                + ".jar"),
            new VMOption("--patch-module"),
            new VMOption("java.xml=lib/endorsed/org.apache.karaf.specs.java.xml-"
                + System.getProperty("karaf.version", "4.2.2")
                + ".jar"),
            new VMOption("--add-opens"),
            new VMOption("java.base/java.security=ALL-UNNAMED"),
            new VMOption("--add-opens"), new VMOption("java.base/java.net=ALL-UNNAMED"),
            new VMOption("--add-opens"), new VMOption("java.base/java.lang=ALL-UNNAMED"),
            new VMOption("--add-opens"), new VMOption("java.base/java.util=ALL-UNNAMED"),
            new VMOption("--add-opens"),
            new VMOption("java.naming/javax.naming.spi=ALL-UNNAMED"),
            new VMOption("--add-opens"),
            new VMOption("java.rmi/sun.rmi.transport.tcp=ALL-UNNAMED"),
            new VMOption("--add-exports=java.base/sun.net.www.protocol.http=ALL-UNNAMED"),
            new VMOption("--add-exports=java.base/sun.net.www.protocol.https=ALL-UNNAMED"),
            new VMOption("--add-exports=java.base/sun.net.www.protocol.jar=ALL-UNNAMED"),
            new VMOption("--add-exports=jdk.naming.rmi/com.sun.jndi.url.rmi=ALL-UNNAMED"),
            new VMOption("-classpath"),
            new VMOption("lib/jdk9plus/*" + File.pathSeparator + "lib/boot/*"),

            };
        } else {

            return new Option[] {
                                 karafDistributionConfiguration().frameworkUrl(karafUrl)
                                     .karafVersion(karafVersion)
                                     .unpackDirectory(new File("target/paxexam/unpack/"))
                                     .useDeployFolder(false),
                                 systemProperty("java.awt.headless").value("true"),
                                 systemProperty("BasicSTSIntegrationTest.PORT").value(port),

                copy("clientKeystore.properties"), copy("clientstore.jks"),
                                 copy("etc/org.ops4j.pax.logging.cfg"),
                                 editConfigurationFilePut("etc/org.ops4j.pax.web.cfg",
                                                          "org.osgi.service.http.port", port),
                                 when(localRepository != null)
                                     .useOptions(editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg",
                                                                          "org.ops4j.pax.url.mvn.localRepository",
                                                                          localRepository)),
                                 features(stsFeatures, "cxf-sts-service"),
                                 configureConsole().ignoreLocalConsole().ignoreRemoteShell(),

            };
        }
    }

    protected Option copy(String path) {
        return replaceConfigurationFile(path, new File("src/test/resources/" + path));
    }

}
