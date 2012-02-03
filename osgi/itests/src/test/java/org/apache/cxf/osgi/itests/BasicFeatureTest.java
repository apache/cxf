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

import java.io.File;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openengsb.labs.paxexam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.logLevel;
import static org.openengsb.labs.paxexam.karaf.options.KarafDistributionOption.replaceConfigurationFile;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class BasicFeatureTest extends CXFOSGiTestSupport {
    //private static final String UNINSTALLED = "[uninstalled]";
    //private static final String INSTALLED = "[installed  ]";

    @Test
    public void testCXFFeaturesModule() throws InterruptedException {
        installCXF();
        Thread.sleep(DEFAULT_TIMEOUT);

        System.err.println(executeCommand("list"));


    }

    @After
    public void tearDown() {
        try {
            unInstallCXF();
        } catch (Exception ex) {
            //Ignore
        }
    }

    @Configuration
    public Option[] config() {
        File file = new File("target/karaf-base/apache-karaf-2.2.5/etc/jre.properties.cxf")
            .getAbsoluteFile();
        return new Option[]{
                cxfDistributionConfiguration(), 
                keepRuntimeFolder(),
                replaceConfigurationFile("etc/jre.properties", file),
                logLevel(LogLevelOption.LogLevel.INFO)};
    }
}
