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
package org.apache.cxf.osgi.itests.soap;

import java.io.InputStream;
import java.util.Collections;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.apache.cxf.helpers.JavaUtils;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.osgi.itests.CXFOSGiTestSupport;
import org.apache.cxf.transport.jms.ConnectionFactoryFeature;
import org.osgi.framework.Constants;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.tinybundles.core.TinyBundles;

import static org.junit.Assert.assertEquals;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class JmsServiceTest extends CXFOSGiTestSupport {

    @Test
    public void testJmsEndpoint() throws Exception {
        Greeter greeter = greeterJms();
        String res = greeter.greetMe("Chris");
        assertEquals("Hi Chris", res);
    }

    private static Greeter greeterJms() {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(Greeter.class);
        factory.setAddress("jms:queue:greeter");
        ConnectionFactory connectionFactory = createConnectionFactory();
        factory.setFeatures(Collections.singletonList(new ConnectionFactoryFeature(connectionFactory)));
        return factory.create(Greeter.class);
    }

    private static ActiveMQConnectionFactory createConnectionFactory() {
        ActiveMQConnectionFactory connectionFactory
            = new ActiveMQConnectionFactory("vm://JmsServiceTest");
        connectionFactory.setUser("karaf");
        connectionFactory.setPassword("karaf");
        return connectionFactory;
    }

    @Configuration
    public Option[] config() {
        return OptionUtils.combine(
            cxfBaseConfig(),
            testUtils(),
            features(cxfUrl, "cxf-jaxws", "cxf-transports-jms"),
            features(maven().groupId("org.apache.activemq").artifactId("activemq-karaf").versionAsInProject()
                .type("xml").classifier("features-core"),
                "cxf-jackson", "activemq-client"),
            provision(serviceBundle())
        );
    }

    private static InputStream serviceBundle() {
        if (JavaUtils.isJava11Compatible()) {
            return TinyBundles.bundle()
                .add(JmsTestActivator.class)
                .add(Greeter.class)
                .add(GreeterImpl.class)
                .set(Constants.BUNDLE_ACTIVATOR, JmsTestActivator.class.getName())
                .set("Require-Capability", "osgi.ee;filter:=\"(&(osgi.ee=JavaSE)(version=11))\"")
                .build(TinyBundles.withBnd());
        } else {
            return TinyBundles.bundle()
                .add(JmsTestActivator.class)
                .add(Greeter.class)
                .add(GreeterImpl.class)
                .set(Constants.BUNDLE_ACTIVATOR, JmsTestActivator.class.getName())
                .build(TinyBundles.withBnd());
        }
    }

}
