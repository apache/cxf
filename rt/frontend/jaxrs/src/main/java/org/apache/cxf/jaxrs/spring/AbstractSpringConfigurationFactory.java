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
package org.apache.cxf.jaxrs.spring;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.AbstractBasicInterceptorProvider;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public abstract class AbstractSpringConfigurationFactory
    extends AbstractBasicInterceptorProvider implements ApplicationContextAware {

    protected ApplicationContext applicationContext;
    @Value("${cxf.jaxrs.server.address:}")
    private String jaxrsServerAddress;
    @Value("${cxf.jaxrs.extensions:}")
    private String jaxrsExtensions;

    protected Server createJaxRsServer() {

        JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean();
        factory.setAddress(getAddress());
        factory.setTransportId(getTransportId());
        factory.setBus(getBus());

        setJaxrsResources(factory);

        factory.setInInterceptors(getInInterceptors());
        factory.setOutInterceptors(getOutInterceptors());
        factory.setOutFaultInterceptors(getOutFaultInterceptors());
        factory.setFeatures(getFeatures());
        if (!StringUtils.isEmpty(jaxrsExtensions)) {
            factory.setExtensionMappings(CastUtils.cast((Map<?, ?>)parseMapSequence(jaxrsExtensions)));
        }
        finalizeFactorySetup(factory);
        return factory.create();
    }
    
    protected Bus getBus() {
        return applicationContext.getBean(SpringBus.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext ac) throws BeansException {
        applicationContext = ac;
    }

    protected abstract void setJaxrsResources(JAXRSServerFactoryBean factory);

    protected List<Object> getJaxrsProviders() {
        return Collections.emptyList();
    }

    public List<Feature> getFeatures() {
        return Collections.emptyList();
    }

    protected String getAddress() {
        if (!StringUtils.isEmpty(jaxrsServerAddress)) {
            return jaxrsServerAddress;
        }
        return "/";
    }

    protected String getTransportId() {
        return "http://cxf.apache.org/transports/http";
    }

    protected void finalizeFactorySetup(JAXRSServerFactoryBean factory) {
        // complete
    }
    protected static Map<String, String> parseMapSequence(String sequence) {
        if (sequence != null) {
            sequence = sequence.trim();
            Map<String, String> map = new HashMap<>();
            String[] pairs = sequence.split(",");
            for (String pair : pairs) {
                String thePair = pair.trim();
                if (thePair.length() == 0) {
                    continue;
                }
                String[] value = thePair.split("=");
                if (value.length == 2) {
                    map.put(value[0].trim(), value[1].trim());
                } else {
                    map.put(thePair, "");
                }
            }
            return map;
        }
        return Collections.emptyMap();
    }
}
