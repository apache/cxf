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

package org.apache.cxf.bus.spring;

import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.configuration.Configurer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.xml.NamespaceHandlerResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;

public class SpringBusFactory extends BusFactory {

    private static final Logger LOG = LogUtils.getL7dLogger(SpringBusFactory.class);

    private final ApplicationContext context;
    private NamespaceHandlerResolver resolver;

    public SpringBusFactory() {
        this.context = null;
    }

    public SpringBusFactory(ApplicationContext context) {
        this.context = context;
        this.resolver = tryFindNamespaceHandler(context);
    }
    public SpringBusFactory(NamespaceHandlerResolver r) {
        context = null;
        this.resolver = r;
    }

    private static NamespaceHandlerResolver tryFindNamespaceHandler(ApplicationContext ctx) {
        try {
            SpringBeanLocator sbl = new SpringBeanLocator(ctx);
            List<NamespaceHandlerResolver> r = sbl.getOSGiServices(NamespaceHandlerResolver.class);
            if (r != null && !r.isEmpty()) {
                return r.get(0);
            }
        } catch (Exception t) {
            LOG.log(Level.FINEST, "Could not find NamespaceHandlerResolver",  t);
        }
        return null;
    }

    public ApplicationContext getApplicationContext() {
        return context;
    }
    public void setNamespaceHandlerResolver(NamespaceHandlerResolver r) {
        resolver = r;
    }

    public Bus createBus() {
        return createBus((String)null);
    }

    private boolean defaultBusNotExists() {
        if (null != context) {
            return !context.containsBean(Bus.DEFAULT_BUS_ID);
        }
        return true;
    }

    public Bus createBus(String cfgFile) {
        return createBus(cfgFile, defaultBusNotExists());
    }

    public Bus createBus(String[] cfgFiles) {
        return createBus(cfgFiles, defaultBusNotExists());
    }

    protected Bus finishCreatingBus(ConfigurableApplicationContext bac) {
        final Bus bus = (Bus)bac.getBean(Bus.DEFAULT_BUS_ID);

        bus.setExtension(bac, ApplicationContext.class);
        if (bac instanceof BusApplicationContext) {
            bus.setExtension((BusApplicationContext)bac, BusApplicationContext.class);
        }
        possiblySetDefaultBus(bus);

        initializeBus(bus);

        registerApplicationContextLifeCycleListener(bus, bac);

        if (bus instanceof SpringBus && defaultBusNotExists()) {
            ((SpringBus)bus).setCloseContext(true);
        }
        return bus;
    }

    public Bus createBus(String cfgFile, boolean includeDefaults) {
        if (cfgFile == null) {
            return createBus((String[])null, includeDefaults);
        }
        return createBus(new String[] {cfgFile}, includeDefaults);
    }

    public Bus createBus(String[] cfgFiles, boolean includeDefaults) {
        try {
            String userCfgFile
                = SystemPropertyAction.getPropertyOrNull(Configurer.USER_CFG_FILE_PROPERTY_NAME);
            String sysCfgFileUrl
                = SystemPropertyAction.getPropertyOrNull(Configurer.USER_CFG_FILE_PROPERTY_URL);
            final Resource r = BusApplicationContext.findResource(Configurer.DEFAULT_USER_CFG_FILE);

            boolean exists = true;
            if (r != null) {
                exists = AccessController
                    .doPrivileged(new PrivilegedAction<Boolean>() {
                        public Boolean run() {
                            return r.exists();
                        }
                    });
            }
            if (context == null && userCfgFile == null && cfgFiles == null && sysCfgFileUrl == null
                && (r == null || !exists) && includeDefaults) {
                return new org.apache.cxf.bus.CXFBusFactory().createBus();
            }
            return finishCreatingBus(createApplicationContext(cfgFiles, includeDefaults));
        } catch (BeansException ex) {
            LogUtils.log(LOG, Level.WARNING, "APP_CONTEXT_CREATION_FAILED_MSG", ex, (Object[])null);
            throw new RuntimeException(ex);
        }
    }

    protected ConfigurableApplicationContext createApplicationContext(String[] cfgFiles, boolean includeDefaults) {
        try {
            return new BusApplicationContext(cfgFiles, includeDefaults, context, resolver);
        } catch (BeansException ex) {
            LogUtils.log(LOG, Level.WARNING, "INITIAL_APP_CONTEXT_CREATION_FAILED_MSG", ex, (Object[])null);
            ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
            if (contextLoader != BusApplicationContext.class.getClassLoader()) {
                Thread.currentThread().setContextClassLoader(
                    BusApplicationContext.class.getClassLoader());
                try {
                    return new BusApplicationContext(cfgFiles, includeDefaults, context);
                } finally {
                    Thread.currentThread().setContextClassLoader(contextLoader);
                }
            }
            throw ex;
        }
    }

    public Bus createBus(URL url) {
        return createBus(url, defaultBusNotExists());
    }
    public Bus createBus(URL[] urls) {
        return createBus(urls, defaultBusNotExists());
    }

    public Bus createBus(URL url, boolean includeDefaults) {
        if (url == null) {
            return createBus((URL[])null, includeDefaults);
        }
        return createBus(new URL[] {url}, includeDefaults);
    }

    public Bus createBus(URL[] urls, boolean includeDefaults) {
        try {
            return finishCreatingBus(createAppContext(urls, includeDefaults));
        } catch (BeansException ex) {
            LogUtils.log(LOG, Level.WARNING, "APP_CONTEXT_CREATION_FAILED_MSG", ex, (Object[])null);
            throw new RuntimeException(ex);
        }
    }

    protected ConfigurableApplicationContext createAppContext(URL[] urls, boolean includeDefaults) {
        return new BusApplicationContext(urls, includeDefaults, context, resolver);
    }

    void registerApplicationContextLifeCycleListener(Bus bus, ConfigurableApplicationContext bac) {
        BusLifeCycleManager lm = bus.getExtension(BusLifeCycleManager.class);
        if (null != lm) {
            lm.registerLifeCycleListener(new BusApplicationContextLifeCycleListener(bac));
        }
    }

    static class BusApplicationContextLifeCycleListener implements BusLifeCycleListener {
        private ConfigurableApplicationContext bac;

        BusApplicationContextLifeCycleListener(ConfigurableApplicationContext b) {
            bac = b;
        }

        public void initComplete() {
        }

        public void preShutdown() {
        }

        public void postShutdown() {
            bac.close();
        }

    }
}
