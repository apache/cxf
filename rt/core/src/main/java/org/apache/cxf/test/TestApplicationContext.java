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

package org.apache.cxf.test;

import org.apache.cxf.bus.spring.BusEntityResolver;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.xml.BeansDtdResolver;
import org.springframework.beans.factory.xml.PluggableSchemaResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * 
 */
public class TestApplicationContext extends ClassPathXmlApplicationContext {

    public TestApplicationContext(String resource) throws BeansException {
        super(resource);
    }

    public TestApplicationContext(String[] resources) throws BeansException {
        super(resources);
    }

    @Override
    protected void initBeanDefinitionReader(XmlBeanDefinitionReader reader) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        reader.setEntityResolver(new BusEntityResolver(new BeansDtdResolver(),
            new PluggableSchemaResolver(cl)));
    }
   
}
