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
package org.apache.cxf.transport.http.netty.server.blueprint;

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
import jakarta.xml.bind.Unmarshaller;
import org.apache.cxf.common.jaxb.JAXBContextCache;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.configuration.jsse.TLSServerParametersConfig;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.http.netty.server.NettyHttpServerEngine;
import org.apache.cxf.transport.http.netty.server.NettyHttpServerEngineFactory;
import org.apache.cxf.transport.http.netty.server.ThreadingParameters;
import org.apache.cxf.transports.http_netty_server.configuration.NettyHttpServerEngineConfigType;
import org.apache.cxf.transports.http_netty_server.configuration.NettyHttpServerEngineFactoryConfigType;
import org.apache.cxf.transports.http_netty_server.configuration.TLSServerParametersIdentifiedType;
import org.apache.cxf.transports.http_netty_server.configuration.ThreadingParametersIdentifiedType;
import org.apache.cxf.transports.http_netty_server.configuration.ThreadingParametersType;

public class NettyHttpServerEngineFactoryHolder {

    private static final Logger LOG = LogUtils.getL7dLogger(NettyHttpServerEngineFactoryHolder.class);

    private String parsedElement;
    private NettyHttpServerEngineFactory factory;

    private JAXBContext jaxbContext;
    private Set<Class<?>> jaxbClasses;

    public NettyHttpServerEngineFactoryHolder() {
    }

    public void init() {
        try {
            Element element = StaxUtils.read(new StringReader(parsedElement)).getDocumentElement();

            NettyHttpServerEngineFactoryConfigType config
                = (NettyHttpServerEngineFactoryConfigType) getJaxbObject(element,
                    NettyHttpServerEngineFactoryConfigType.class);

            factory = new NettyHttpServerEngineFactory();

            Map<String, ThreadingParameters> threadingParametersMap
                = new TreeMap<>();

            if (config.getIdentifiedThreadingParameters() != null) {
                for (ThreadingParametersIdentifiedType threads : config.getIdentifiedThreadingParameters()) {
                    ThreadingParameters rThreads = new ThreadingParameters();
                    String id = threads.getId();
                    rThreads.setThreadPoolSize(threads.getThreadingParameters().getThreadPoolSize());
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
                factory.setTlsServerParameters(sslMap);
            }
            //Engines

            List<NettyHttpServerEngine> engineList = new ArrayList<>();
            for (NettyHttpServerEngineConfigType engine : config.getEngine()) {
                NettyHttpServerEngine eng = new NettyHttpServerEngine();

                if (engine.getHost() != null && !StringUtils.isEmpty(engine.getHost())) {
                    eng.setHost(engine.getHost());
                }
                if (engine.getReadIdleTime() != null) {
                    eng.setReadIdleTime(engine.getReadIdleTime());
                }
                if (engine.getWriteIdleTime() != null) {
                    eng.setWriteIdleTime(engine.getWriteIdleTime());
                }
                if (engine.getMaxChunkContentSize() != null) {
                    eng.setMaxChunkContentSize(engine.getMaxChunkContentSize());
                }
                if (engine.getPort() != null) {
                    eng.setPort(engine.getPort());
                }
                if (engine.isSessionSupport() != null) {
                    eng.setSessionSupport(engine.isSessionSupport());
                }
                if (engine.getThreadingParameters() != null) {
                    ThreadingParametersType threads = engine.getThreadingParameters();
                    ThreadingParameters rThreads = new ThreadingParameters();
                    rThreads.setThreadPoolSize(threads.getThreadPoolSize());
                    eng.setThreadingParameters(rThreads);
                }

                //eng.setServer(engine.getTlsServerParameters());
                if (engine.getTlsServerParameters() != null) {
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

    protected Object getJaxbObject(Element parent, Class<?> c) {

        try {
            Unmarshaller umr = getContext(c).createUnmarshaller();
            JAXBElement<?> ele = (JAXBElement<?>) umr.unmarshal(parent);

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
