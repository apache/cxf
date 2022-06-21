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
package org.apache.cxf.transport.http_jetty.blueprint;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.w3c.dom.Element;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.jaxb.JAXBContextCache;
import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.configuration.jsse.TLSServerParametersConfig;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngine;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngineFactory;
import org.apache.cxf.transport.http_jetty.ThreadingParameters;
import org.apache.cxf.transports.http_jetty.configuration.JettyHTTPServerEngineConfigType;
import org.apache.cxf.transports.http_jetty.configuration.JettyHTTPServerEngineFactoryConfigType;
import org.apache.cxf.transports.http_jetty.configuration.TLSServerParametersIdentifiedType;
import org.apache.cxf.transports.http_jetty.configuration.ThreadingParametersIdentifiedType;
import org.apache.cxf.transports.http_jetty.configuration.ThreadingParametersType;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;

public class JettyHTTPServerEngineFactoryHolder {

    private static final Logger LOG = LogUtils.getL7dLogger(JettyHTTPServerEngineFactoryHolder.class);

    private String parsedElement;
    private JettyHTTPServerEngineFactory factory;

    private Map<String, Connector> connectorMap;

    private Map<String, List<Handler>> handlersMap;

    private JAXBContext jaxbContext;
    private Set<Class<?>> jaxbClasses;

    public JettyHTTPServerEngineFactoryHolder() {
    }

    public void init() {
        try {
            Element element = StaxUtils.read(new StringReader(parsedElement)).getDocumentElement();

            JettyHTTPServerEngineFactoryConfigType config
                = getJaxbObject(element,
                JettyHTTPServerEngineFactoryConfigType.class);

            Bus defaultBus = BusFactory.getDefaultBus();
            factory = new JettyHTTPServerEngineFactory(defaultBus);

            Map<String, ThreadingParameters> threadingParametersMap
                = new TreeMap<>();

            if (config.getIdentifiedThreadingParameters() != null) {
                for (ThreadingParametersIdentifiedType threads : config.getIdentifiedThreadingParameters()) {
                    ThreadingParameters rThreads = new ThreadingParameters();
                    String id = threads.getId();
                    if (threads.getThreadingParameters().getMaxThreads() != null) {
                        rThreads.setMaxThreads(threads.getThreadingParameters().getMaxThreads());
                    }
                    if (threads.getThreadingParameters().getMinThreads() != null) {
                        rThreads.setMinThreads(threads.getThreadingParameters().getMinThreads());
                    }
                    rThreads.setThreadNamePrefix(threads.getThreadingParameters().getThreadNamePrefix());
                    threadingParametersMap.put(id, rThreads);
                }

                factory.setThreadingParametersMap(threadingParametersMap);
            }

            //SSL
            Map<String, TLSServerParameters> sslMap = new TreeMap<>();
            if (config.getIdentifiedTLSServerParameters() != null) {

                for (TLSServerParametersIdentifiedType t : config.getIdentifiedTLSServerParameters()) {
                    try {
                        TLSServerParameters parameter
                            = new TLSServerParametersConfig(t.getTlsServerParameters());
                        sslMap.put(t.getId(), parameter);
                    } catch (Exception e) {
                        throw new RuntimeException("Could not configure TLS for id " + t.getId(), e);
                    }
                }
                factory.setTlsServerParametersMap(sslMap);
            }
            //Engines

            List<JettyHTTPServerEngine> engineList = new ArrayList<>();
            for (JettyHTTPServerEngineConfigType engine : config.getEngine()) {
                JettyHTTPServerEngine eng = new JettyHTTPServerEngine(
                        factory.getMBeanContainer(), engine.getHost(), engine.getPort());
                if (engine.getConnector() != null && connectorMap != null) {
                    // we need to setup the Connector from the connectorMap
                    Connector connector = connectorMap.get(engine.getPort().toString());
                    if (connector != null) {
                        eng.setConnector(connector);
                    } else {
                        throw new RuntimeException("Could not find the connector instance for engine with port"
                            + engine.getPort().toString());
                    }
                }
                if (engine.getHandlers() != null && handlersMap != null) {
                    List<Handler> handlers = handlersMap.get(engine.getPort().toString());
                    if (handlers != null) {
                        eng.setHandlers(handlers);
                    } else {
                        throw new RuntimeException("Could not find the handlers instance for engine with port"
                            + engine.getPort().toString());
                    }
                }

                if (engine.isContinuationsEnabled() != null) {
                    eng.setContinuationsEnabled(engine.isContinuationsEnabled());
                }
                if (engine.isSendServerVersion() != null) {
                    eng.setSendServerVersion(engine.isSendServerVersion());
                }
                if (engine.getHost() != null && !StringUtils.isEmpty(engine.getHost())) {
                    eng.setHost(engine.getHost());
                }
                if (engine.getMaxIdleTime() != null) {
                    eng.setMaxIdleTime(engine.getMaxIdleTime());
                }
                if (engine.getPort() != null) {
                    eng.setPort(engine.getPort());
                }
                if (engine.isReuseAddress() != null) {
                    eng.setReuseAddress(engine.isReuseAddress());
                }
                if (engine.isSessionSupport() != null) {
                    eng.setSessionSupport(engine.isSessionSupport());
                }
                if (engine.getSessionTimeout() != null) {
                    eng.setSessionTimeout(engine.getSessionTimeout().intValue());
                }
                if (engine.getThreadingParameters() != null) {
                    ThreadingParametersType threads = engine.getThreadingParameters();
                    ThreadingParameters rThreads = new ThreadingParameters();
                    if (threads.getMaxThreads() != null) {
                        rThreads.setMaxThreads(threads.getMaxThreads());
                    }
                    if (threads.getMinThreads() != null) {
                        rThreads.setMinThreads(threads.getMinThreads());
                    }

                    eng.setThreadingParameters(rThreads);
                }

                //eng.setServer(engine.getTlsServerParameters());
                if (engine.getTlsServerParameters() != null
                    && (engine.getTlsServerParameters().getKeyManagers() != null
                    || engine.getTlsServerParameters().getTrustManagers() != null)) {
                    try {
                        TLSServerParameters parameter =
                            new TLSServerParametersConfig(engine.getTlsServerParameters());
                        eng.setTlsServerParameters(parameter);
                    } catch (Exception e) {
                        throw new RuntimeException("Could not configure TLS for engine on  "
                            + eng.getHost() + ":" + eng.getPort(), e);
                    }
                }
                eng.finalizeConfig();

                engineList.add(eng);
            }
            factory.setEnginesList(engineList);
            //Unravel this completely.

            factory.initComplete();
        } catch (Exception e) {
            throw new RuntimeException("Could not process configuration.", e);
        }
    }

    public void destroy() {
        // need to release the reference of the jaxb Classes
        factory.postShutdown();
        jaxbClasses.clear();
        jaxbContext = null;
    }

    public String getParsedElement() {
        return parsedElement;
    }

    public void setParsedElement(String parsedElement) {
        this.parsedElement = parsedElement;
    }

    public void setConnectorMap(Map<String, Connector> connectorMap) {
        this.connectorMap = connectorMap;
    }

    public void setHandlersMap(Map<String, List<Handler>> handlersMap) {
        this.handlersMap = handlersMap;
    }

    protected <T> T getJaxbObject(Element parent, Class<T> c) {

        try {
            JAXBElement<T> ele = JAXBUtils.unmarshall(getContext(c), parent, c);
            return ele.getValue();
        } catch (JAXBException e) {
            LOG.warning("Unable to parse property due to " + e);
            return null;
        }
    }

    protected synchronized JAXBContext getContext(Class<?> cls) {
        if (jaxbContext == null || jaxbClasses == null || !jaxbClasses.contains(cls)) {
            try {
                Set<Class<?>> tmp = new HashSet<>();
                if (jaxbClasses != null) {
                    tmp.addAll(jaxbClasses);
                }
                JAXBContextCache.addPackage(tmp, PackageUtils.getPackageName(cls),
                                            cls == null ? getClass().getClassLoader() : cls.getClassLoader());
                if (cls != null) {
                    boolean hasOf = false;
                    for (Class<?> c : tmp) {
                        if (c.getPackage() == cls.getPackage() && "ObjectFactory".equals(c.getSimpleName())) {
                            hasOf = true;
                        }
                    }
                    if (!hasOf) {
                        tmp.add(cls);
                    }
                }
                JAXBContextCache.scanPackages(tmp);
                JAXBContextCache.CachedContextAndSchemas ccs
                    = JAXBContextCache.getCachedContextAndSchemas(tmp, null, null, null, false);
                jaxbClasses = ccs.getClasses();
                jaxbContext = ccs.getContext();
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        }
        return jaxbContext;
    }
}
