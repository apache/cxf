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

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link CxfAutoConfiguration}.
 *
 * @author Vedran Pavic
 */
public class CxfAutoConfigurationTests {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private AnnotationConfigWebApplicationContext context;

    @After
    public void close() {
        if (this.context != null) {
            this.context.close();
        }
    }

    @Test
    public void defaultConfiguration() {
        load(CxfAutoConfiguration.class);
        assertThat(this.context.getBeansOfType(ServletRegistrationBean.class).size(),
                equalTo(1));
    }

    @Test
    public void customPathMustBeginWithASlash() {
        this.thrown.expect(BeanCreationException.class);
        this.thrown.expectMessage("Path must start with /");
        load(CxfAutoConfiguration.class, "spring.cxf.path=invalid");
    }

    @Test
    public void customPathWithTrailingSlash() {
        load(CxfAutoConfiguration.class, "spring.cxf.path=/valid/");
        assertThat(this.context.getBean(ServletRegistrationBean.class).getUrlMappings(),
                contains("/valid/*"));
    }

    @Test
    public void customPath() {
        load(CxfAutoConfiguration.class, "spring.cxf.path=/valid");
        assertThat(this.context.getBeansOfType(ServletRegistrationBean.class).size(),
                equalTo(1));
        assertThat(this.context.getBean(ServletRegistrationBean.class).getUrlMappings(),
                contains("/valid/*"));
    }

    @Test
    public void customLoadOnStartup() {
        load(CxfAutoConfiguration.class, "spring.cxf.servlet.load-on-startup=1");
        ServletRegistrationBean registrationBean = this.context
                .getBean(ServletRegistrationBean.class);
        Integer value = (Integer)ReflectionTestUtils.getField(registrationBean, "loadOnStartup");
        assertThat(value, equalTo(1));
    }

    @Test
    public void customInitParameters() {
        load(CxfAutoConfiguration.class, "spring.cxf.servlet.init.key1=value1",
                "spring.cxf.servlet.init.key2=value2");
        ServletRegistrationBean registrationBean = this.context
                .getBean(ServletRegistrationBean.class);
        assertThat(registrationBean.getInitParameters(), hasEntry("key1", "value1"));
        assertThat(registrationBean.getInitParameters(), hasEntry("key2", "value2"));
    }

    private void load(Class<?> config, String... environment) {
        AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
        ctx.setServletContext(new MockServletContext());
        EnvironmentTestUtils.addEnvironment(ctx, environment);
        ctx.register(config);
        ctx.refresh();
        this.context = ctx;
    }

}
