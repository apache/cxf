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
package org.apache.cxf.jaxws.spring;

import java.io.Closeable;

import org.apache.cxf.bus.spring.BusWiringBeanFactoryPostProcessor;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientFactoryBean;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.frontend.spring.ClientProxyFactoryBeanDefinitionParser;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class JaxWsProxyFactoryBeanDefinitionParser extends ClientProxyFactoryBeanDefinitionParser {

    public JaxWsProxyFactoryBeanDefinitionParser() {
        super();
        setBeanClass(JAXWSSpringClientProxyFactoryBean.class);
    }
    
    
    protected Class getRawFactoryClass() {
        return JaxWsProxyFactoryBean.class;
    }

    @Override
    protected Class getFactoryClass() {
        return JAXWSSpringClientProxyFactoryBean.class;
    }

    @Override
    protected String getSuffix() {
        return ".jaxws-client";
    }

    @NoJSR250Annotations
    public static class JAXWSSpringClientProxyFactoryBean extends JaxWsProxyFactoryBean
        implements ApplicationContextAware, FactoryBean, DisposableBean {

        private Object obj;

        public JAXWSSpringClientProxyFactoryBean() {
            super();
        }
        public JAXWSSpringClientProxyFactoryBean(ClientFactoryBean fact) {
            super(fact);
        }
        
        public void setApplicationContext(ApplicationContext ctx) throws BeansException {
            if (getBus() == null) {
                setBus(BusWiringBeanFactoryPostProcessor.addDefaultBus(ctx));
            }
        }
        public Object create() {
            configured = true;
            return super.create();
        }
        public synchronized Object getObject() throws Exception {
            if (obj == null) {
                obj = create();
            }
            return obj;
        }
        public Class getObjectType() {
            return this.getServiceClass();
        }
        public boolean isSingleton() {
            return true;
        }
        

        public void destroy() throws Exception {
            if (obj != null) {
                if (obj instanceof Closeable) {
                    ((Closeable)obj).close();
                } else {
                    Client c = ClientProxy.getClient(obj);
                    c.destroy();
                }
                obj = null;
            }
        }
    }
}
