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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.helpers.CastUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.context.support.XmlWebApplicationContext;

public class CXFServlet extends CXFNonSpringServlet
    implements ApplicationListener<ContextRefreshedEvent> {
    private static final long serialVersionUID = -5922443981969455305L;
    private static final String BUS_PARAMETER = "bus";

    private boolean busCreated;
    private XmlWebApplicationContext createdContext;

    public CXFServlet() {
    }

    @Override
    protected void loadBus(ServletConfig servletConfig) {
        ApplicationContext wac = WebApplicationContextUtils.
            getWebApplicationContext(servletConfig.getServletContext());

        if (wac instanceof AbstractApplicationContext) {
            addListener((AbstractApplicationContext)wac);
        }

        String configLocation = servletConfig.getInitParameter("config-location");
        if (configLocation == null) {
            try (InputStream is = servletConfig.getServletContext().getResourceAsStream("/WEB-INF/cxf-servlet.xml")) {
                if (is != null && is.available() > 0) {
                    configLocation = "/WEB-INF/cxf-servlet.xml";
                }
            } catch (Exception ex) {
                //ignore
            }
        }
        if (configLocation != null) {
            wac = createSpringContext(wac, servletConfig, configLocation);
        }
        if (wac != null) {
            String busParam = servletConfig.getInitParameter(BUS_PARAMETER);
            String busName = busParam == null ? "cxf" : busParam.trim();

            setBus(wac.getBean(busName, Bus.class));
        } else {
            busCreated = true;
            setBus(BusFactory.newInstance().createBus());
        }
    }

    protected void addListener(AbstractApplicationContext wac) {
        /**
         * The change in the way application listeners are maintained during the context refresh
         * since Spring Framework 5.1.5 (https://github.com/spring-projects/spring-framework/issues/22325). The
         * CXF adds listener **after** the context has been refreshed, not much control we have over it, but
         * it does matter now: the listeners registered after the context refresh disappear when
         * context is refreshed. The ugly hack here, to stay in the loop, is to add CXF servlet
         * to "earlyApplicationListeners" set, only than it will be kept between refreshes.
         */
        try {
            final Field f = ReflectionUtils.findField(wac.getClass(), "earlyApplicationListeners");

            if (f != null) {
                Collection<Object> c = CastUtils.cast((Collection<?>)ReflectionUtil.setAccessible(f).get(wac));
                if (c != null) {
                    c.add(this);
                }
            }
        } catch (SecurityException | IllegalAccessException e) {
            //ignore.
        }

        try {
            //spring 2 vs spring 3 return type is different
            Method m = wac.getClass().getMethod("getApplicationListeners");
            Collection<Object> c = CastUtils.cast((Collection<?>)ReflectionUtil
                                                      .setAccessible(m).invoke(wac));
            c.add(this);
        } catch (Throwable t) {
            //ignore.
        }
    }

    /**
     * Try to create a spring application context from the config location.
     * Will first try to resolve the location using the servlet context.
     * If that does not work then the location is given as is to spring
     *
     * @param ctx
     * @param servletConfig
     * @param location
     * @return
     */
    private ApplicationContext createSpringContext(ApplicationContext ctx,
                                                   ServletConfig servletConfig,
                                                   String location) {
        XmlWebApplicationContext ctx2 = new XmlWebApplicationContext();
        createdContext = ctx2;

        ctx2.setServletConfig(servletConfig);
        Resource r = ctx2.getResource(location);
        try {
            InputStream in = r.getInputStream();
            in.close();
        } catch (IOException e) {
            //ignore
            r = ctx2.getResource("classpath:" + location);
            try {
                r.getInputStream().close();
            } catch (IOException e2) {
                //ignore
                r = null;
            }
        }
        try {
            if (r != null) {
                location = r.getURL().toExternalForm();
            }
        } catch (IOException e) {
            //ignore
        }
        if (ctx != null) {
            ctx2.setParent(ctx);
            String[] names = ctx.getBeanNamesForType(Bus.class);
            if (names == null || names.length == 0) {
                ctx2.setConfigLocations(new String[] {"classpath:/META-INF/cxf/cxf.xml",
                                                      location});
            } else {
                ctx2.setConfigLocations(new String[] {location});
            }
        } else {
            ctx2.setConfigLocations(new String[] {"classpath:/META-INF/cxf/cxf.xml",
                                                  location});
            createdContext = ctx2;
        }
        ctx2.refresh();
        return ctx2;
    }
    public void destroyBus() {
        if (busCreated) {
            //if we created the Bus, we need to destroy it.  Otherwise, spring will handleit.
            getBus().shutdown(true);
            setBus(null);
        }
        if (createdContext != null) {
            createdContext.close();
        }
    }

    public void onApplicationEvent(ContextRefreshedEvent event) {
        destroy();
        setBus(null);
        try {
            init(getServletConfig());
        } catch (ServletException e) {
            throw new RuntimeException("Unable to reinitialize the CXFServlet", e);
        }
    }

}
