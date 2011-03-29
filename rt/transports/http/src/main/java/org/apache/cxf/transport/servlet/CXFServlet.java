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
package org.apache.cxf.transport.servlet;

import java.io.InputStream;

import javax.servlet.ServletConfig;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.InputStreamResource;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class CXFServlet extends CXFNonSpringServlet {

    public CXFServlet() {
    }

    @Override
    protected void loadBus(ServletConfig sc) {
        ApplicationContext wac = WebApplicationContextUtils.
            getWebApplicationContext(sc.getServletContext());
        String configLocation = sc.getInitParameter("config-location");
        if (wac == null && (configLocation != null)) {
            wac = createSpringContext(sc, configLocation);
        }
        if (wac != null) {
            setBus(wac.getBean("cxf", Bus.class));
        } else {
            setBus(BusFactory.newInstance().createBus());
        }
    }

    /**
     * Try to create a spring application context from the config location.
     * Will first try to resolve the location using the servlet context.
     * If that does not work then the location is given as is to spring
     * 
     * @param sc
     * @param configLocation
     * @return
     */
    private ApplicationContext createSpringContext(ServletConfig sc, String configLocation) {
        InputStream is = sc.getServletContext().getResourceAsStream(configLocation);
        if (is != null) {
            GenericApplicationContext ctx = new GenericApplicationContext();
            XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(ctx);
            reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
            reader.loadBeanDefinitions(new InputStreamResource(is, configLocation));
            ctx.refresh();
            return ctx;
        } else {
            return new ClassPathXmlApplicationContext(configLocation);
        }
    }

}
