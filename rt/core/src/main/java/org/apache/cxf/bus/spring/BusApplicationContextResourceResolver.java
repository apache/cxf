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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.resource.ResourceResolver;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

@NoJSR250Annotations
public class BusApplicationContextResourceResolver 
    implements ResourceResolver, ApplicationContextAware {
    
    ApplicationContext context;
    
    public BusApplicationContextResourceResolver() {
    }
    public BusApplicationContextResourceResolver(ApplicationContext c) {
        context = c;
    }
    

    public InputStream getAsStream(String name) {
        Resource r = context.getResource(name);
        if (r != null && r.exists()) {
            try {
                return r.getInputStream();
            } catch (IOException e) {
                //ignore and return null
            }
        } 
        return null;
    }

    public <T> T resolve(String resourceName, Class<T> resourceType) {
        if (resourceName == null) {
            return null;
        }   
        try {
            return resourceType.cast(context.getBean(resourceName, resourceType));
        } catch (NoSuchBeanDefinitionException def) {
            //ignore
        }
        try {
            if (URL.class.isAssignableFrom(resourceType)) {
                Resource r = context.getResource(resourceName);
                if (r != null && r.exists()) {
                    return resourceType.cast(r.getURL());
                }
            }
        } catch (IOException e) {
            //ignore
        }
        return null;
    }


    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;        
    }

}
