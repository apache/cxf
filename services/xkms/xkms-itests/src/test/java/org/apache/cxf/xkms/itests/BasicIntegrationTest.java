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

import javax.inject.Inject;

import org.apache.cxf.xkms.model.extensions.ResultDetails;
import org.apache.cxf.xkms.model.xkms.LocateResultType;
import org.apache.cxf.xkms.model.xkms.MessageExtensionAbstractType;
import org.apache.cxf.xkms.model.xkms.ResultMajorEnum;
import org.junit.Assert;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.w3._2002._03.xkms_wsdl.XKMSPortType;

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

    private static final String HTTP_PORT = "9191";
    private static final String XKMS_ENDPOINT = "http://localhost:" + HTTP_PORT + "/cxf/XKMS";

    // Adding apache snapshots as cxf trunk may contain snapshot dependencies
    //private static final String REPOS = "http://repo1.maven.org/maven2@id=central, "
    //    + "http://repository.apache.org/content/groups/snapshots-group@snapshots@noreleases@id=apache-snapshots ";

    @Inject
    protected XKMSPortType xkmsService;

    @Configuration
    public Option[] getConfig() {
        String karafVersion = System.getProperty("karaf.version", "3.0.4");
        String localRepository = System.getProperty("localRepository");
        MavenArtifactUrlReference karafUrl = maven() //
            .groupId("org.apache.karaf") //
            .artifactId("apache-karaf") //
            .version(karafVersion)
            .type("tar.gz");
        MavenArtifactUrlReference xkmsFeatures = maven() //
            .groupId("org.apache.cxf.services.xkms") //
            .artifactId("cxf-services-xkms-features") //
            .versionAsInProject() //
            .type("xml");

        return new Option[] {
            karafDistributionConfiguration().frameworkUrl(karafUrl).karafVersion(karafVersion)
                .unpackDirectory(new File("target/paxexam/unpack/")).useDeployFolder(false),
            systemProperty("java.awt.headless").value("true"),

            copy("data/xkms/certificates/trusted_cas/root.cer"),
            copy("data/xkms/certificates/trusted_cas/wss40CA.cer"),
            copy("data/xkms/certificates/cas/alice.cer"),
            copy("data/xkms/certificates/dave.cer"),
            copy("data/xkms/certificates/http___localhost_8080_services_TestService.cer"),
            copy("etc/org.ops4j.pax.logging.cfg"),
            //editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg", "org.ops4j.pax.url.mvn.repositories", REPOS),
            editConfigurationFilePut("etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port", HTTP_PORT),
            editConfigurationFilePut("etc/org.apache.cxf.xkms.client.cfg", "xkms.endpoint", XKMS_ENDPOINT),
            when(localRepository != null)
                .useOptions(editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg",
                            "org.ops4j.pax.url.mvn.localRepository",
                            localRepository)),
            features(xkmsFeatures, "cxf-xkms-service", "cxf-xkms-client", "cxf-xkms-ldap"),
            configureConsole().ignoreLocalConsole(),

            //org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder(),
            //CoreOptions.vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005")
        };
    }

    protected Option copy(String path) {
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
