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

import org.hamcrest.Matcher;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
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
        load(CxfAutoConfiguration.class, "cxf.path=invalid");
    }

    @Test
    public void customPathWithTrailingSlash() {
        load(CxfAutoConfiguration.class, "cxf.path=/valid/");
        assertThat(this.context.getBean(ServletRegistrationBean.class).getUrlMappings(),
                (Matcher) hasItem("/valid/*"));
    }

    @Test
    public void customPath() {
        load(CxfAutoConfiguration.class, "cxf.path=/valid");
        assertThat(this.context.getBeansOfType(ServletRegistrationBean.class).size(),
                equalTo(1));
        assertThat(this.context.getBean(ServletRegistrationBean.class).getUrlMappings(),
                (Matcher) hasItem("/valid/*"));
    }

    @Test
    public void customLoadOnStartup() {
        load(CxfAutoConfiguration.class, "cxf.servlet.load-on-startup=1");
        ServletRegistrationBean registrationBean = this.context
                .getBean(ServletRegistrationBean.class);
        assertThat(ReflectionTestUtils.getField(registrationBean, "loadOnStartup"),
                equalTo(1));
    }

    @Test
    public void customInitParameters() {
        load(CxfAutoConfiguration.class, "cxf.servlet.init.key1=value1",
                "spring.cxf.servlet.init.key2=value2");
        ServletRegistrationBean registrationBean = this.context
                .getBean(ServletRegistrationBean.class);
        assertThat(registrationBean.getInitParameters(), (Matcher) hasEntry("key1", "value1"));
        assertThat(registrationBean.getInitParameters(), (Matcher) hasEntry("key2", "value2"));
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
