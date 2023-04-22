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

package org.apache.cxf.bus.spring;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.PostConstruct;
import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;



public class BusDefinitionParserTest {

    @Test
    @SuppressWarnings("deprecation")
    public void testFeatures() {
        String cfgFile = "org/apache/cxf/bus/spring/bus.xml";
        Bus bus = new SpringBusFactory().createBus(cfgFile, true);

        List<Interceptor<? extends Message>> in = bus.getInInterceptors();
        assertTrue("could not find logging interceptor.",
                in.stream().anyMatch(i -> i.getClass() == org.apache.cxf.interceptor.LoggingInInterceptor.class));

        Collection<Feature> features = bus.getFeatures();
        TestFeature tf = null;
        for (Feature f : features) {
            if (f instanceof TestFeature) {
                tf = (TestFeature)f;
                break;
            }
        }

        assertNotNull(tf);
        assertTrue("test feature  has not been initialised", tf.initialised);
        assertNotNull("test feature has not been injected", tf.testBean);
        assertTrue("bean injected into test feature has not been initialised", tf.testBean.initialised);
    }

    @Test
    public void testBusConfigure() {
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "org/apache/cxf/bus/spring/customerBus.xml")) {
            Bus cxf1 = (Bus)context.getBean("cxf1");

            assertEquals(1, cxf1.getOutInterceptors().size());
            assertTrue(cxf1.getInInterceptors().isEmpty());

            Bus cxf2 = (Bus)context.getBean("cxf2");
            assertEquals(1, cxf2.getInInterceptors().size());
            assertTrue(cxf2.getOutInterceptors().isEmpty());
        }
    }

    @Test
    public void testBusConfigureCreateBus() {
        final AtomicBoolean b = new AtomicBoolean();
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                "org/apache/cxf/bus/spring/customerBus2.xml")) {
            Bus cxf1 = (Bus)context.getBean("cxf1");

            assertEquals(1, cxf1.getOutInterceptors().size());
            assertTrue(cxf1.getInInterceptors().isEmpty());

            Bus cxf2 = (Bus)context.getBean("cxf2");

            assertEquals(1, cxf2.getInInterceptors().size());
            assertTrue(cxf2.getOutInterceptors().isEmpty());

            cxf2.getExtension(BusLifeCycleManager.class)
                .registerLifeCycleListener(new BusLifeCycleListener() {
                    public void initComplete() {
                    }

                    public void preShutdown() {
                    }

                    public void postShutdown() {
                        b.set(true);
                    }

                });
        }
        assertTrue("postShutdown not called", b.get());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testLazyInit() {
        String cfgFile = "org/apache/cxf/bus/spring/lazyInitBus.xml";
        Bus bus = new SpringBusFactory().createBus(cfgFile, true);

        List<Interceptor<? extends Message>> in = bus.getInInterceptors();
        assertTrue("could not find logging interceptor.",
                in.stream().anyMatch(i -> i.getClass() == org.apache.cxf.interceptor.LoggingInInterceptor.class));
    }

    static class TestBean {

        boolean initialised;

        @PostConstruct
        public void initialise() {
            initialised = true;
        }
    }

    static class TestFeature extends AbstractFeature {

        boolean initialised;
        TestBean testBean;

        @PostConstruct
        public void initialise() {
            initialised = true;
        }

        public void setTestBean(TestBean tb) {
            testBean = tb;
        }
    }

}