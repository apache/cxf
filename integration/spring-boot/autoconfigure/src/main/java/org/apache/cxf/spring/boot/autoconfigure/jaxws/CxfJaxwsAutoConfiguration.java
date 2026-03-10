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

package org.apache.cxf.spring.boot.autoconfigure.jaxws;

import jakarta.annotation.Resource;
import jakarta.xml.ws.WebServiceContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;
import org.springframework.context.annotation.Configuration;

//CHECKSTYLE:OFF

@Configuration
@ConditionalOnClass({ Resource.class, WebServiceContext.class })
public class CxfJaxwsAutoConfiguration {
    @Bean
    static BeanFactoryPostProcessor jaxwsBeanFactoryPostProcessor() {
        return new BeanFactoryPostProcessor() {
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                final var beans = beanFactory.getBeansOfType(CommonAnnotationBeanPostProcessor.class, true, false);
                // The {@code javax.xml.ws.WebServiceContext} interface should be ignored since it will 
                // be resolved by the CXF's JAX-WS runtime.
                beans.forEach((name, bean) -> bean.ignoreResourceType(WebServiceContext.class.getName()));
            }
        };
    }
}
