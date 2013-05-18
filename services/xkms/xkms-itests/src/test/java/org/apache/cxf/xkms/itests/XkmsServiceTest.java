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

import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.editConfigurationFilePut;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.logLevel;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.scanFeatures;

import java.io.File;
import java.net.URISyntaxException;
import java.util.UUID;

import javax.inject.Inject;

import junit.framework.Assert;

import org.apache.cxf.xkms.model.xkms.PrototypeKeyBindingType;
import org.apache.cxf.xkms.model.xkms.RegisterRequestType;
import org.apache.cxf.xkms.model.xkms.RegisterResultType;
import org.apache.cxf.xkms.model.xkms.ResultMajorEnum;
import org.apache.cxf.xkms.model.xkms.ResultMinorEnum;
import org.apache.cxf.xkms.model.xmldsig.KeyInfoType;
import org.apache.karaf.tooling.exam.options.LogLevelOption;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.MavenUrlReference;
import org.ops4j.pax.exam.spi.reactors.EagerSingleStagedReactorFactory;
import org.osgi.framework.BundleContext;
import org.w3._2002._03.xkms_wsdl.XKMSPortType;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(EagerSingleStagedReactorFactory.class)
public class XkmsServiceTest {
    private static final String HTTP_PORT = "9191";

    private static final String XKMS_ENDPOINT = "http://localhost:" + HTTP_PORT + "/cxf/XKMS";

    @Inject
    BundleContext bundleContext;

    @Inject
    XKMSPortType xkmsService;

    @Configuration
    public Option[] config() {
        MavenArtifactUrlReference karafUrl = maven()
                .groupId("org.apache.karaf").artifactId("apache-karaf")
                .version("2.3.0").type("tar.gz");
        MavenUrlReference cxfFeatures = maven().groupId("org.apache.cxf.karaf")
                .artifactId("apache-cxf").type("xml").classifier("features")
                .version("2.7.0");
        MavenUrlReference xkmsFeatures = maven()
                .groupId("org.apache.cxf.services.xkms")
                .artifactId("cxf-services-xkms-features").type("xml")
                .version("2.7.1-SNAPSHOT");

        return new Option[] {
                karafDistributionConfiguration().frameworkUrl(karafUrl)
                        .karafVersion("2.3.0").name("Apache Karaf")
                        .unpackDirectory(new File("target/exam")),
                logLevel(LogLevelOption.LogLevel.INFO),
                CoreOptions.
                scanFeatures(cxfFeatures, "cxf"),
                scanFeatures(xkmsFeatures, "cxf-xkms-service", "cxf-xkms-client").start(),
                editConfigurationFilePut("etc/org.ops4j.pax.web.cfg",
                        "org.osgi.service.http.port", HTTP_PORT),
                editConfigurationFilePut("etc/org.apache.cxf.xkms.client.cfg",
                        "xkms.endpoint", XKMS_ENDPOINT),
                        
                editConfigurationFilePut("etc/org.apache.cxf.xkms.cfg",
                                "xkms.filepersistence.storageDir", "data/xkms/keys"),

                        
        // vmOption(
        // "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005" )
        };
    }

    @Test
    public void testEmptyRegister() throws URISyntaxException, Exception {
        RegisterRequestType request = new RegisterRequestType();
        request.setId(UUID.randomUUID().toString());
        RegisterResultType result = xkmsService.register(request);
        Assert.assertEquals(ResultMajorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_SENDER.value(), result.getResultMajor());
        Assert.assertEquals(ResultMinorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_FAILURE.value(), result.getResultMinor());
    }
    
    @Test
    public void testRegisterWithoutKey() throws URISyntaxException, Exception {
        RegisterRequestType request = new RegisterRequestType();
        PrototypeKeyBindingType binding = new PrototypeKeyBindingType();
        KeyInfoType keyInfo = new KeyInfoType();
        binding.setKeyInfo(keyInfo);
        request.setPrototypeKeyBinding(binding );
        request.setId(UUID.randomUUID().toString());
        RegisterResultType result = xkmsService.register(request);
        Assert.assertEquals(ResultMajorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_SENDER.value(), result.getResultMajor());
        Assert.assertEquals(ResultMinorEnum.HTTP_WWW_W_3_ORG_2002_03_XKMS_FAILURE.value(), result.getResultMinor());
    }


}
