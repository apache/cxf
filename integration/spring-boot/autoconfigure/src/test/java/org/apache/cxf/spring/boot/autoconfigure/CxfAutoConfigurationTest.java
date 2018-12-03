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

import java.util.Map;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerImpl;
import org.apache.cxf.spring.boot.jaxrs.CustomJaxRSServer;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link CxfAutoConfiguration}.
 *
 * @author Vedran Pavic
 */
public class CxfAutoConfigurationTest {

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
        ServletRegistrationBean registrationBean = this.context.getBean(ServletRegistrationBean.class);
        Matcher<java.lang.Iterable<? super String>> v = hasItem("/valid/*");
        assertThat(registrationBean.getUrlMappings(), v);
    }

    @Test
    public void customPath() {
        load(CxfAutoConfiguration.class, "cxf.path=/valid");
        assertThat(this.context.getBeansOfType(ServletRegistrationBean.class).size(),
                equalTo(1));
        ServletRegistrationBean registrationBean = this.context.getBean(ServletRegistrationBean.class);
        Matcher<java.lang.Iterable<? super String>> v = hasItem("/valid/*");
        assertThat(registrationBean.getUrlMappings(), v);
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
                "cxf.servlet.init.key2=value2");
        ServletRegistrationBean registrationBean = this.context
                .getBean(ServletRegistrationBean.class);
        assertThat(registrationBean.getInitParameters(), hasEntry("key1", "value1"));
        assertThat(registrationBean.getInitParameters(), hasEntry("key2", "value2"));
    }
    
    @Test
    public void customizedJaxRsServer() {
        load(new Class<?>[] {CxfAutoConfiguration.class, CustomJaxRSServer.class}, 
                "cxf.jaxrs.classes-scan=true", 
                "cxf.jaxrs.classes-scan-packages=org.apache.cxf.spring.boot.jaxrs");
        Map<String, Server> beans = 
                this.context.getBeansOfType(Server.class);
        assertThat(beans.size(),
                equalTo(1));
        
        Object serverInstance = beans.values().iterator().next();
        assertFalse(serverInstance instanceof ServerImpl);
    }
    
    @Test
    public void defaultJaxRsServer() {
        load(CxfAutoConfiguration.class, 
                "cxf.jaxrs.classes-scan=true", 
                "cxf.jaxrs.classes-scan-packages=org.apache.cxf.spring.boot.jaxrs");
        Map<String, Server> beans = 
                this.context.getBeansOfType(Server.class);
        assertThat(beans.size(),
                equalTo(1));
        
        Object serverInstance = beans.values().iterator().next();
        assertTrue(serverInstance instanceof ServerImpl);
    }

    private void load(Class<?> config, String... environment) {
        load(new Class<?>[] {config}, environment);
    }

    private void load(Class<?>[] configs, String... environment) {
        AnnotationConfigWebApplicationContext ctx = new AnnotationConfigWebApplicationContext();
        ctx.setServletContext(new MockServletContext());
        EnvironmentTestUtils.addEnvironment(ctx, environment);
        ctx.register(configs);
        ctx.refresh();
        this.context = ctx;
    }
}
