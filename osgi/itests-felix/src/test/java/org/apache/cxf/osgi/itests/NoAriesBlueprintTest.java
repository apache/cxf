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


import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.ops4j.pax.exam.CoreOptions.junitBundles;
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
    public Option[] config() {
        String localRepo = System.getProperty("localRepository");
        if (localRepo == null) {
            localRepo = "";
        }

        return new Option[]{
                systemProperty("java.awt.headless").value("true"),
                when(!"".equals(localRepo))
                    .useOptions(systemProperty("org.ops4j.pax.url.mvn.localRepository").value(localRepo)),
                mvnBundle("org.apache.ws.xmlschema", "xmlschema-core"),
                mvnBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.wsdl4j"),
                mvnBundle("org.apache.cxf", "cxf-core"),
                mvnBundle("org.apache.cxf", "cxf-rt-wsdl"),
                mvnBundle("org.apache.cxf", "cxf-rt-databinding-jaxb"),
                mvnBundle("org.apache.cxf", "cxf-rt-bindings-xml"),
                mvnBundle("org.apache.cxf", "cxf-rt-bindings-soap"),
                mvnBundle("org.apache.cxf", "cxf-rt-frontend-simple"),
                mvnBundle("org.apache.geronimo.specs", "geronimo-servlet_3.0_spec"),
                mvnBundle("org.apache.cxf", "cxf-rt-transports-http"),
                mvnBundle("org.apache.cxf", "cxf-rt-frontend-jaxws"),
                junitBundles()
        };
    }
}
