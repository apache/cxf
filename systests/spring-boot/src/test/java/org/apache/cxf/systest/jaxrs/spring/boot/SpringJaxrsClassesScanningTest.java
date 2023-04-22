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

import org.apache.cxf.jaxrs.spring.AbstractJaxrsClassesScanServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
        classes = SpringJaxrsClassesScanningTest.TestConfig.class)
@ActiveProfiles("jaxrs-classes-scan")
public class SpringJaxrsClassesScanningTest {
    @Autowired
    private AbstractJaxrsClassesScanServer scanner;

    @EnableAutoConfiguration
    static class TestConfig {
    }

    @Test
    public void testCxfClassesScan() {
        // No features are registered since class scanner only looks for JAX-RS's 
        // @Provider annotations, not JAX-RS/CXF Features.
        assertThat(scanner.getFeatures()).isEmpty();
        assertThat(scanner.getOutInterceptors()).isEmpty();
        assertThat(scanner.getInInterceptors()).isEmpty();
        assertThat(scanner.getOutFaultInterceptors()).isEmpty();
        assertThat(scanner.getInFaultInterceptors()).isEmpty();
    }
}
