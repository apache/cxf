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
package org.apache.cxf.osgi.itests;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.SSLContext;

import org.apache.cxf.helpers.JavaUtils;
import org.apache.http.ssl.SSLContextBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.when;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class NoAriesBlueprintTest extends OSGiTestSupport {
    /**
     * Make sure cxf bundles start up without aries blueprint
     *
     * @throws Exception
     */
    @Test
    public void testCXFBundles() throws Exception {
        assertBundleStarted("org.apache.cxf.cxf-core");
        assertBundleStarted("org.apache.cxf.cxf-rt-frontend-simple");
        assertBundleStarted("org.apache.cxf.cxf-rt-frontend-jaxws");
    }

    @Configuration
    public Option[] config() throws NoSuchAlgorithmException, NoSuchPaddingException, KeyManagementException {
        String localRepo = System.getProperty("localRepository");
        if (localRepo == null) {
            localRepo = "";
        }

        final Option[] basicOptions = new Option[] {
            junitBundles(),
            systemProperty("java.awt.headless").value("true"),
            when(!"".equals(localRepo))
                .useOptions(systemProperty("org.ops4j.pax.url.mvn.localRepository").value(localRepo)),
            mavenBundle("org.apache.ws.xmlschema", "xmlschema-core").versionAsInProject(),
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.wsdl4j").versionAsInProject(),
            mavenBundle("org.apache.felix", "org.apache.felix.configadmin").versionAsInProject(),
            mavenBundle("org.apache.cxf", "cxf-core").versionAsInProject(),
            mavenBundle("org.apache.cxf", "cxf-rt-wsdl").versionAsInProject(),
            mavenBundle("org.apache.cxf", "cxf-rt-databinding-jaxb").versionAsInProject(),
            mavenBundle("org.apache.cxf", "cxf-rt-bindings-xml").versionAsInProject(),
            mavenBundle("org.apache.cxf", "cxf-rt-bindings-soap").versionAsInProject(),
            mavenBundle("org.apache.cxf", "cxf-rt-frontend-simple").versionAsInProject(),
            mavenBundle("jakarta.servlet", "jakarta.servlet-api").versionAsInProject(),
            mavenBundle("org.apache.cxf", "cxf-rt-transports-http").versionAsInProject(),
            mavenBundle("org.apache.cxf", "cxf-rt-frontend-jaxws").versionAsInProject(),
            junitBundles(),
            systemPackages(
                "jakarta.annotation;version=\"2.0\"",
                "jakarta.xml.soap;version=\"2.0\""
            )
        };
        if (JavaUtils.isJava9Compatible()) {
            // Pre-create SSL context (on JDK16+, the HTTP/HTTPS URL handlers are not registered for some reason)
            final SSLContext sslContext = new SSLContextBuilder().setProtocol("TLS").build();
            assertThat(sslContext, not(nullValue()));

            return OptionUtils.combine(basicOptions,
                mavenBundle("jakarta.annotation", "jakarta.annotation-api").versionAsInProject(),
                mavenBundle("com.sun.activation", "jakarta.activation").versionAsInProject(),
                mavenBundle("jakarta.xml.ws", "jakarta.xml.ws-api").versionAsInProject(),
                mavenBundle("jakarta.xml.bind", "jakarta.xml.bind-api").versionAsInProject(),
                mavenBundle("jakarta.xml.soap", "jakarta.xml.soap-api").versionAsInProject(),
                mavenBundle("jakarta.jws", "jakarta.jws-api").versionAsInProject()
            );
        }
        return basicOptions;
    }
}
