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
package org.apache.cxf.jaxrs.client.spring;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.common.util.ClasspathScanner;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class JaxRsProxyClientConfiguration extends AbstractJaxRsClientConfiguration {
    @Value("${cxf.jaxrs.client.classes-scan-packages:}")
    private String scanPackages;

    @Bean
    protected Client jaxRsProxyClient() {
        return super.createClient();
    }

    protected Class<?> getServiceClass() {
        return null;
    }

    protected void setJaxrsResources(JAXRSClientFactoryBean factory) {
        Class<?> serviceClass = getServiceClass();
        if (serviceClass != null) {
            factory.setServiceClass(serviceClass);
        } else if (!StringUtils.isEmpty(scanPackages)) {
            try {
                final Map< Class< ? extends Annotation >, Collection< Class< ? > > > classes =
                    ClasspathScanner.findClasses(scanPackages, Path.class, Provider.class);
                factory.setServiceClass(
                    JAXRSClientFactoryBeanDefinitionParser.getServiceClass(classes.get(Path.class)));
                factory.setProviders(
                    JAXRSClientFactoryBeanDefinitionParser.getProviders(context, classes.get(Provider.class)));
            } catch (Exception ex) {
                throw new ServiceConstructionException(ex);
            }
        }
    }


}
