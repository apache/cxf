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

package org.apache.cxf.jaxws.handler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.LogicalHandler;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.ResourceInjector;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxws.javaee.ParamValueType;
import org.apache.cxf.jaxws.javaee.PortComponentHandlerType;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.resource.ResourceResolver;

public class HandlerChainBuilder {
    static final Logger LOG = LogUtils.getL7dLogger(HandlerChainBuilder.class);
    private static final ResourceBundle BUNDLE = LOG.getResourceBundle();

    private Bus bus;
    private boolean handlerInitEnabled = true;

    public HandlerChainBuilder(Bus aBus) {
        bus = aBus;
    }

    public HandlerChainBuilder() {
        this(null);
    }

    public List<Handler> buildHandlerChainFromConfiguration(PortComponentHandlerType hc) {
        if (null == hc) {
            return null;
        }
        return sortHandlers(buildHandlerChain(hc, getHandlerClassLoader()));
    }
    public List<Handler> buildHandlerChainFromConfiguration(List<PortComponentHandlerType> hc) {
        if (null == hc || hc.size() == 0) {
            return null;
        }
        List<Handler> handlers = new ArrayList<Handler>();
        for (PortComponentHandlerType pt : hc) {
            handlers.addAll(buildHandlerChain(pt, getHandlerClassLoader()));
        }
        return sortHandlers(handlers);
    }

    // methods used by Geronimo to allow configuring things themselves
    public void setHandlerInitEnabled(boolean b) {
        handlerInitEnabled = b;
    }

    public boolean isHandlerInitEnabled() {
        return handlerInitEnabled;
    }
    
    /**
     * sorts the handlers into correct order. All of the logical handlers first
     * followed by the protocol handlers
     * 
     * @param handlers
     * @return sorted list of handlers
     */
    public List<Handler> sortHandlers(List<Handler> handlers) {

        List<LogicalHandler> logicalHandlers = new ArrayList<LogicalHandler>();
        List<Handler> protocolHandlers = new ArrayList<Handler>();

        for (Handler handler : handlers) {
            if (handler instanceof LogicalHandler) {
                logicalHandlers.add((LogicalHandler)handler);
            } else {
                protocolHandlers.add(handler);
            }
        }

        List<Handler> sortedHandlers = new ArrayList<Handler>();
        sortedHandlers.addAll(logicalHandlers);
        sortedHandlers.addAll(protocolHandlers);
        return sortedHandlers;
    }

    protected ClassLoader getHandlerClassLoader() {
        return getClass().getClassLoader();
    }

    protected List<Handler> buildHandlerChain(PortComponentHandlerType ht, ClassLoader classLoader) {
        List<Handler> handlerChain = new ArrayList<Handler>();
        try {
            LOG.log(Level.FINE, "loading handler", trimString(ht.getHandlerName().getValue()));

            Class<? extends Handler> handlerClass = Class.forName(
                                                                  trimString(ht.getHandlerClass()
                                                                      .getValue()), true, classLoader)
                .asSubclass(Handler.class);

            Handler handler = handlerClass.newInstance();
            LOG.fine("adding handler to chain: " + handler);
            configureHandler(handler, ht);
            handlerChain.add(handler);
        } catch (Exception e) {
            throw new WebServiceException(BUNDLE.getString("HANDLER_INSTANTIATION_EXC"), e);
        }
        return handlerChain;
    }
    
    /**
     * Resolve handler chain configuration file associated with the given class
     * 
     * @param clz
     * @param filename
     * @return A URL object or null if no resource with this name is found
     */    
    protected URL resolveHandlerChainFile(Class clz, String filename) {
        URL handlerFile = clz.getResource(filename);
        if (handlerFile == null) {
            //the file location might be an absolute java.net.URL in externalForm.
            try {
                handlerFile = new URL(filename);
                //test if the URL can be opened
                handlerFile.openStream();
            } catch (Exception e) {
                //do nothing
            } 
        }
        return handlerFile;
    } 
    
    private void configureHandler(Handler handler, PortComponentHandlerType h) {
        if (!handlerInitEnabled) {
            return;
        }

        if (h.getInitParam().size() == 0) {
            return;
        }

        Map<String, String> params = new HashMap<String, String>();

        for (ParamValueType param : h.getInitParam()) {
            params.put(trimString(param.getParamName() == null ? null : param.getParamName().getValue()),
                       trimString(param.getParamValue() == null ? null : param.getParamValue().getValue()));
        }

        Method initMethod = getInitMethod(handler);
        if (initMethod != null) {
            initializeViaInitMethod(handler, params, initMethod);
        } else {
            initializeViaInjection(handler, params);
        }
    }

    private void initializeViaInjection(Handler handler, final Map<String, String> params) {
        if (bus != null) {
            ResourceManager resMgr = bus.getExtension(ResourceManager.class);
            List<ResourceResolver> resolvers = resMgr.getResourceResolvers();
            resolvers.add(new InitParamResourceResolver(params));
            ResourceInjector resInj = new ResourceInjector(resMgr, resolvers);
            resInj.inject(handler);
            resInj.construct(handler);
        }
    }

    private void initializeViaInitMethod(Handler handler, Map<String, String> params, Method init) {
        try {
            init.invoke(handler, params);
        } catch (InvocationTargetException ex) {
            Throwable t = ex.getCause() != null ? ex.getCause() : ex;
            LogUtils.log(LOG, Level.WARNING, "INIT_METHOD_THREW_EXCEPTION", t, handler.getClass());
        } catch (IllegalAccessException ex) {
            LOG.log(Level.SEVERE, "CANNOT_ACCESS_INIT", handler.getClass());
        }
    }

    private Method getInitMethod(Handler handler) {
        Method m = null;
        try {
            m = handler.getClass().getMethod("init", Map.class);
        } catch (NoSuchMethodException ex) {
            // empty
        }
        return m;
    }

    private String trimString(String str) {
        return str != null ? str.trim() : null;
    }
}
