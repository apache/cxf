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

import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jakarta.jws.HandlerChain;
import jakarta.jws.WebService;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.handler.Handler;
import org.apache.cxf.Bus;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.staxutils.StaxUtils;

@SuppressWarnings("rawtypes")
public class AnnotationHandlerChainBuilder extends HandlerChainBuilder {
    private static final Logger LOG = LogUtils.getL7dLogger(AnnotationHandlerChainBuilder.class);
    private static final ResourceBundle BUNDLE = LOG.getResourceBundle();

    public AnnotationHandlerChainBuilder() {
    }

    public AnnotationHandlerChainBuilder(Bus bus) {
        super(bus);
    }

    /**
     * @param clz
     * @param existingHandlers
     * @return
     */
    public List<Handler> buildHandlerChainFromClass(Class<?> clz, List<Handler> existingHandlers,
                                                    QName portQName, QName serviceQName, String bindingID) {
        LOG.fine("building handler chain");
        
        HandlerChainAnnotation hcAnn = findHandlerChainAnnotation(clz, true);
        final List<Handler> chain;
        if (hcAnn == null) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("no HandlerChain annotation on " + clz);
            }
            chain = new ArrayList<>();
        } else {
            hcAnn.validate();

            try {

                URL handlerFileURL = resolveHandlerChainFile(clz, hcAnn.getFileName());
                if (handlerFileURL == null) {
                    throw new WebServiceException(new Message("HANDLER_CFG_FILE_NOT_FOUND_EXC", BUNDLE, hcAnn
                        .getFileName()).toString());
                }

                Document doc = StaxUtils.read(handlerFileURL.openStream());
                Element el = doc.getDocumentElement();
                boolean isJavaEENamespace = JavaeeHandlerChainBuilder.JAVAEE_NS.equals(el.getNamespaceURI());
                boolean isJakartaEENamespace = JakartaeeHandlerChainBuilder.JAKARTAEE_NS.equals(el.getNamespaceURI());
                if (!isJavaEENamespace && !isJakartaEENamespace) {
                    throw new WebServiceException(
                        BundleUtils.getFormattedString(BUNDLE,
                                                       "NOT_VALID_NAMESPACE",
                                                       el.getNamespaceURI()));
                }
                
                final ClassLoader classLoader = getClassLoader(clz);
                final DelegatingHandlerChainBuilder delegate = ht -> buildHandlerChain(ht, classLoader);
                if (isJavaEENamespace) {
                    chain = new JavaeeHandlerChainBuilder(BUNDLE, handlerFileURL, delegate)
                            .build(el, portQName, serviceQName, bindingID);
                } else {
                    chain = new JakartaeeHandlerChainBuilder(BUNDLE, handlerFileURL, delegate)
                            .build(el, portQName, serviceQName, bindingID);
                }
            } catch (WebServiceException e) {
                throw e;
            } catch (Exception e) {
                throw new WebServiceException(BUNDLE.getString("CHAIN_NOT_SPECIFIED_EXC"), e);
            }
        }
        assert chain != null;
        if (existingHandlers != null) {
            chain.addAll(existingHandlers);
        }
        return sortHandlers(chain);
    }

    private static ClassLoader getClassLoader(final Class<?> clazz) {
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                public ClassLoader run() {
                    return clazz.getClassLoader();
                }
            });
        }
        return clazz.getClassLoader();
    }

    public List<Handler> buildHandlerChainFromClass(Class<?> clz, QName portQName, QName serviceQName,
                                                    String bindingID) {
        return buildHandlerChainFromClass(clz, null, portQName, serviceQName, bindingID);
    }

    protected URL resolveHandlerChainAnnotationFile(Class<?> clazz, String name) {
        return clazz.getResource(name);
    }

    private HandlerChainAnnotation findHandlerChainAnnotation(Class<?> clz, boolean searchSEI) {
        if (clz == null) {
            return null;
        }
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Checking for HandlerChain annotation on " + clz.getName());
        }
        HandlerChainAnnotation hcAnn = null;
        HandlerChain ann = clz.getAnnotation(HandlerChain.class);
        if (ann == null) {
            if (searchSEI) {
                /* HandlerChain annotation can be specified on the SEI
                 * but the implementation bean might not implement the SEI.
                 */
                WebService ws = clz.getAnnotation(WebService.class);
                if (ws != null && !StringUtils.isEmpty(ws.endpointInterface())) {
                    String seiClassName = ws.endpointInterface().trim();
                    final Class<?> seiClass;
                    try {
                        seiClass = ClassLoaderUtils.loadClass(seiClassName, clz);
                    } catch (ClassNotFoundException e) {
                        throw new WebServiceException(BUNDLE.getString("SEI_LOAD_FAILURE_EXC"), e);
                    }

                    // check SEI class and its interfaces for HandlerChain annotation
                    hcAnn = findHandlerChainAnnotation(seiClass, false);
                }
            }
            if (hcAnn == null) {
                // check interfaces for HandlerChain annotation
                for (Class<?> iface : clz.getInterfaces()) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Checking for HandlerChain annotation on " + iface.getName());
                    }
                    ann = iface.getAnnotation(HandlerChain.class);
                    if (ann != null) {
                        hcAnn = new HandlerChainAnnotation(ann, iface);
                        break;
                    }
                }
                if (hcAnn == null) {
                    hcAnn = findHandlerChainAnnotation(clz.getSuperclass(), false);
                }
            }
        } else {
            hcAnn = new HandlerChainAnnotation(ann, clz);
        }

        return hcAnn;
    }

    static class HandlerChainAnnotation {
        private final Class<?> declaringClass;
        private final HandlerChain ann;

        HandlerChainAnnotation(HandlerChain hc, Class<?> clz) {
            ann = hc;
            declaringClass = clz;
        }

        public Class<?> getDeclaringClass() {
            return declaringClass;
        }

        public String getFileName() {
            return ann.file();
        }

        public void validate() {
            if (null == ann.file() || "".equals(ann.file())) {
                throw new WebServiceException(BUNDLE.getString("ANNOTATION_WITHOUT_URL_EXC"));
            }
        }

        public String toString() {
            return "[" + declaringClass + "," + ann + "]";
        }
    }
}
