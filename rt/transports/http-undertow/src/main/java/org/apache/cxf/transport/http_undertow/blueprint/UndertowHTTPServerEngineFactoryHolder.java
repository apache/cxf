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
package org.apache.cxf.transport.http_undertow.blueprint;

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
import org.apache.cxf.common.jaxb.JAXBContextCache;
import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.configuration.jsse.TLSServerParametersConfig;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.http_undertow.CXFUndertowHttpHandler;
import org.apache.cxf.transport.http_undertow.ThreadingParameters;
import org.apache.cxf.transport.http_undertow.UndertowHTTPServerEngine;
import org.apache.cxf.transport.http_undertow.UndertowHTTPServerEngineFactory;
import org.apache.cxf.transports.http_undertow.configuration.TLSServerParametersIdentifiedType;
import org.apache.cxf.transports.http_undertow.configuration.ThreadingParametersIdentifiedType;
import org.apache.cxf.transports.http_undertow.configuration.ThreadingParametersType;
import org.apache.cxf.transports.http_undertow.configuration.UndertowHTTPServerEngineConfigType;
import org.apache.cxf.transports.http_undertow.configuration.UndertowHTTPServerEngineFactoryConfigType;


public class UndertowHTTPServerEngineFactoryHolder {

    private static final Logger LOG = LogUtils.getL7dLogger(UndertowHTTPServerEngineFactoryHolder.class);

    private String parsedElement;
    private UndertowHTTPServerEngineFactory factory;

    private Map<String, List<CXFUndertowHttpHandler>> handlersMap;


    private JAXBContext jaxbContext;
    private Set<Class<?>> jaxbClasses;

    public UndertowHTTPServerEngineFactoryHolder() {
    }

    public void init() {
        try {
            Element element = StaxUtils.read(new StringReader(parsedElement)).getDocumentElement();

            UndertowHTTPServerEngineFactoryConfigType config
                = getJaxbObject(element,
                UndertowHTTPServerEngineFactoryConfigType.class);

            factory = new UndertowHTTPServerEngineFactory();

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
                    if (threads.getThreadingParameters().getWorkerIOName() != null) {
                        rThreads.setWorkerIOName(threads.getThreadingParameters().getWorkerIOName());
                    }
                    if (threads.getThreadingParameters().getWorkerIOThreads() != null) {
                        rThreads.setWorkerIOThreads(threads.getThreadingParameters().getWorkerIOThreads());
                    }
                    
                    rThreads.setWorkerIOThreads(threads.getThreadingParameters().getWorkerIOThreads());
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

            List<UndertowHTTPServerEngine> engineList = new ArrayList<>();
            for (UndertowHTTPServerEngineConfigType engine : config.getEngine()) {
                UndertowHTTPServerEngine eng = new UndertowHTTPServerEngine();

                if (engine.getHandlers() != null && handlersMap != null) {
                    List<CXFUndertowHttpHandler> handlers = handlersMap.get(engine.getPort().toString());
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


                if (engine.getHost() != null && !StringUtils.isEmpty(engine.getHost())) {
                    eng.setHost(engine.getHost());
                }
                if (engine.getMaxIdleTime() != null) {
                    eng.setMaxIdleTime(engine.getMaxIdleTime());
                }
                if (engine.getPort() != null) {
                    eng.setPort(engine.getPort());
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
                    if (threads.getWorkerIOThreads() != null) {
                        rThreads.setWorkerIOThreads(threads.getWorkerIOThreads());
                    }
                    if (threads.getWorkerIOName() != null) {
                        rThreads.setWorkerIOName(threads.getWorkerIOName());
                    }
                    eng.setThreadingParameters(rThreads);
                }


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

    public void setHandlersMap(Map<String, List<CXFUndertowHttpHandler>> handlersMap) {
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
