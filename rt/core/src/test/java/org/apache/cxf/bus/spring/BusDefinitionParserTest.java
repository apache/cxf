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

import javax.annotation.PostConstruct;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.CXFBusImpl;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.junit.Assert;
import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class BusDefinitionParserTest extends Assert {
    
    @Test
    public void testFeatures() {
        String cfgFile = "org/apache/cxf/bus/spring/bus.xml";
        Bus bus = new SpringBusFactory().createBus(cfgFile, true);
        
        List<Interceptor> in = bus.getInInterceptors();
        boolean found = false;
        for (Interceptor i : in) {
            if (i instanceof LoggingInInterceptor) {
                found = true;
            }
        }
        assertTrue("could not find logging interceptor.", found);
   
        Collection<AbstractFeature> features = ((CXFBusImpl)bus).getFeatures();
        TestFeature tf = null;
        for (AbstractFeature f : features) {
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
        ClassPathXmlApplicationContext context = null;
        try {
            context = new ClassPathXmlApplicationContext("org/apache/cxf/bus/spring/customerBus.xml");
            Bus cxf1 = (Bus)context.getBean("cxf1");
            
            assertTrue(cxf1.getOutInterceptors().size() == 1);
            assertTrue(cxf1.getInInterceptors().size() == 0);
            
            Bus cxf2 = (Bus)context.getBean("cxf2");
            assertTrue(cxf2.getInInterceptors().size() == 1);
            assertTrue(cxf2.getOutInterceptors().size() == 0);
        } finally {
            if (context != null) {
                context.close();
            }
        }
    }

    @Test
    public void testLazyInit() {
        String cfgFile = "org/apache/cxf/bus/spring/lazyInitBus.xml";
        Bus bus = new SpringBusFactory().createBus(cfgFile, true);

        List<Interceptor> in = bus.getInInterceptors();
        boolean found = false;
        for (Interceptor i : in) {
            if (i instanceof LoggingInInterceptor) {
                found = true;
            }
        }
        assertTrue("could not find logging interceptor.", found);
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
