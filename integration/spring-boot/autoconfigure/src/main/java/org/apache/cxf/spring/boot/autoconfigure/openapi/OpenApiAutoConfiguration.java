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
package org.apache.cxf.spring.boot.autoconfigure.openapi;

import org.apache.cxf.jaxrs.openapi.OpenApiCustomizer;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.apache.cxf.spring.boot.autoconfigure.CxfAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configure the OpenApiCustomizer in case the Spring Boot application
 * uses OpenApiFeature. The actual injection happens after OpenApiFeature bean's
 * initialization phase.
 *
 */
@Configuration
@ConditionalOnClass(OpenApiFeature.class)
@AutoConfigureAfter(CxfAutoConfiguration.class)
public class OpenApiAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public OpenApiCustomizer openApiCustomizer() {
        final OpenApiCustomizer customizer = new OpenApiCustomizer();
        customizer.setDynamicBasePath(true);
        return customizer;
    }
    
    @Bean
    public OpenApiFeatureBeanPostProcessor openApiFeatureBeanPostProcessor() {
        return new OpenApiFeatureBeanPostProcessor(); 
    }
}
