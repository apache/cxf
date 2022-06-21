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

package org.apache.cxf.spring.boot.autoconfigure;

import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@ContextConfiguration(classes = {CxfPropertiesTest.Config.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class CxfPropertiesTest {

    @Configuration
    public static class Config {
        @Bean
        public MethodValidationPostProcessor methodValidationPostProcessor() {
            return new MethodValidationPostProcessor();
        }

        @Bean
        public CxfProperties cxfProperties() {
            return new CxfProperties();
        }
    }

    @Autowired
    private CxfProperties cxfproperties;

    @Test
    public void throwsViolationExceptionWhenIsNull() {
        doTestInvalidPath(null);
    }

    @Test
    public void throwsViolationExceptionWhenPathIsEmpty() {
        doTestInvalidPath("");
    }

    @Test
    public void throwsViolationExceptionWhenHasNoSlash() {
        doTestInvalidPath("invalid");
    }

    private void doTestInvalidPath(String value) {
        cxfproperties.setPath(value);
        try {
            cxfproperties.getPath();
            fail("ConstraintViolationException is expected");
        } catch (ConstraintViolationException e) {
            assertEquals(1, e.getConstraintViolations().size());
        }
    }

    @Test
    public void noViolationExceptionWhenPathValid() {
        cxfproperties.setPath("/valid");
        cxfproperties.getPath();
    }

}
