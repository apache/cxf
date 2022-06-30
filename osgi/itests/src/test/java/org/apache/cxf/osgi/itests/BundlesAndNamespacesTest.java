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
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class BundlesAndNamespacesTest extends CXFOSGiTestSupport {

    @Test
    public void test() throws Exception {
        assertBundleStarted("org.apache.cxf.cxf-core");
        assertBlueprintNamespacePublished("http://cxf.apache.org/blueprint/core", 1000L);
        assertBlueprintNamespacePublished("http://cxf.apache.org/configuration/beans", 1000L);
        assertBlueprintNamespacePublished("http://cxf.apache.org/configuration/parameterized-types", 1000L);
        assertBlueprintNamespacePublished("http://cxf.apache.org/configuration/security", 1000L);
        assertBlueprintNamespacePublished("http://schemas.xmlsoap.org/wsdl/", 1000L);

        assertBundleStarted("org.apache.cxf.cxf-rt-frontend-jaxws");
        assertBlueprintNamespacePublished("http://cxf.apache.org/blueprint/jaxws", 1000L);
        assertBlueprintNamespacePublished("http://cxf.apache.org/blueprint/simple", 1000L);
    }

    @Configuration
    public Option[] config() {
        return OptionUtils.combine(
            cxfBaseConfig(),
            testUtils(),
            features(cxfUrl, "aries-blueprint", "cxf-core", "cxf-jaxws"),
            logLevel(LogLevel.INFO)
        );
    }
}
