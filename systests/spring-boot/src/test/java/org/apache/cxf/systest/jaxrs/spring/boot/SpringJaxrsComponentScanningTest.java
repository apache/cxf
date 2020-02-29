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

package org.apache.cxf.systest.jaxrs.spring.boot;

import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.apache.cxf.jaxrs.spring.AbstractSpringComponentScanServer;
import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationFeature;
import org.apache.cxf.systest.jaxrs.resources.Library;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
        classes = SpringJaxrsComponentScanningTest.TestConfig.class)
@ActiveProfiles("jaxrs-component-scan")
public class SpringJaxrsComponentScanningTest {
    @Autowired
    private AbstractSpringComponentScanServer scanner;

    @Configuration
    @EnableAutoConfiguration
    @ComponentScan(basePackageClasses = Library.class)
    static class TestConfig {
    }

    @Test
    public void testCxfComponentScan() {
        // The component scanner only looks for CXF's @Provider annotations, 
        // not JAX-RS Features/@Provider.
        assertThat(scanner.getFeatures())
                .hasSize(2)
                .hasOnlyElementsOfTypes(OpenApiFeature.class, JAXRSBeanValidationFeature.class);

        assertThat(scanner.getOutInterceptors()).isEmpty();
        assertThat(scanner.getInInterceptors()).isEmpty();
        assertThat(scanner.getOutFaultInterceptors()).isEmpty();
        assertThat(scanner.getInFaultInterceptors()).isEmpty();
    }
}
