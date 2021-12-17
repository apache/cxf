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
import java.util.Iterator;

import jakarta.inject.Inject;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.xkms.model.extensions.ResultDetails;
import org.apache.cxf.xkms.model.xkms.LocateResultType;
import org.apache.cxf.xkms.model.xkms.MessageExtensionAbstractType;
import org.apache.cxf.xkms.model.xkms.ResultMajorEnum;
import org.w3._2002._03.xkms_wsdl.XKMSPortType;

import org.junit.Assert;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
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
public class BasicIntegrationTest {

    // Adding apache snapshots as cxf trunk may contain snapshot dependencies
//    private static final String REPOS = "https://repo1.maven.org/maven2@id=central,"
//        + "https://repository.apache.org/content/groups/snapshots-group@id=apache@snapshots@noreleases";

    @Inject
    protected XKMSPortType xkmsService;

    @Configuration
    public Option[] getConfig() {
        String port = TestUtil.getPortNumber(BasicIntegrationTest.class);
        System.setProperty("BasicIntegrationTest.PORT", port);
        String xkmsEndpoint = "http://localhost:" + port + "/cxf/XKMS";

        String localRepository = System.getProperty("localRepository");
        MavenArtifactUrlReference xkmsFeatures = maven() //
            .groupId("org.apache.cxf.services.xkms") //
            .artifactId("cxf-services-xkms-features") //
            .versionAsInProject() //
            .type("xml");

        final Option[] basicOptions = new Option[] {
             karafDistributionConfiguration()
                 .frameworkUrl(
                     maven().groupId("org.apache.karaf").artifactId("apache-karaf-minimal").versionAsInProject()
                         .type("tar.gz"))
                 .unpackDirectory(new File("target/paxexam/"))
                 .useDeployFolder(false),
             systemProperty("java.awt.headless").value("true"),
             systemProperty("BasicIntegrationTest.PORT").value(port),

             copy("data/xkms/certificates/trusted_cas/root.cer"),
             copy("data/xkms/certificates/trusted_cas/wss40CA.cer"),
             copy("data/xkms/certificates/cas/alice.cer"),
             copy("data/xkms/certificates/dave.cer"),
             copy("data/xkms/certificates/http___localhost_8080_services_TestService.cer"),
//             editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg",
//                 "org.ops4j.pax.url.mvn.repositories", REPOS),
             editConfigurationFilePut("etc/org.ops4j.pax.web.cfg",
                                      "org.osgi.service.http.port", port),
             editConfigurationFilePut("etc/org.apache.cxf.xkms.client.cfg",
                                      "xkms.endpoint", xkmsEndpoint),
             when(localRepository != null)
                 .useOptions(editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg",
                                                      "org.ops4j.pax.url.mvn.localRepository",
                                                      localRepository)),
             features(xkmsFeatures, "cxf-xkms-service", "cxf-xkms-client",
                      "cxf-xkms-ldap"),
             configureConsole().ignoreLocalConsole().ignoreRemoteShell(),

            // org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder(),
            // CoreOptions.vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005")
        };
        if (JavaVersionUtil.getMajorVersion() >= 9) {
            final String karafVersion = MavenUtils.getArtifactVersion("org.apache.karaf", "apache-karaf-minimal");
            return OptionUtils.combine(basicOptions,
                new VMOption("--add-reads=java.xml=java.logging"),
                new VMOption("--add-exports=java.base/"
                    + "org.apache.karaf.specs.locator=java.xml,ALL-UNNAMED"),
                new VMOption("--patch-module"),
                new VMOption("java.base=lib/endorsed/org.apache.karaf.specs.locator-" + karafVersion + ".jar"),
                new VMOption("--patch-module"),
                new VMOption("java.xml=lib/endorsed/org.apache.karaf.specs.java.xml-" + karafVersion + ".jar"),
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
                new VMOption("lib/jdk9plus/*" + File.pathSeparator + "lib/boot/*")
            );
        }
        return basicOptions;
    }

    protected static Option copy(String path) {
        return replaceConfigurationFile(path, new File("src/test/resources/" + path));
    }

    protected void assertSuccess(LocateResultType result) {
        Iterator<MessageExtensionAbstractType> it = result.getMessageExtension().iterator();
        String error = "";
        if (it.hasNext()) {
            ResultDetails details = (ResultDetails)it.next();
            error = details.getDetails();
        }
        Assert.assertEquals("Expecting success but got error " + error,
                            ResultMajorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_SUCCESS.value(),
                            result.getResultMajor());
    }

}
