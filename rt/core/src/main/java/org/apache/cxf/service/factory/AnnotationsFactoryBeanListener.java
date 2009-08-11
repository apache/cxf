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

package org.apache.cxf.service.factory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.annotations.FastInfoset;
import org.apache.cxf.annotations.GZIP;
import org.apache.cxf.annotations.SchemaValidation;
import org.apache.cxf.annotations.WSDLDocumentation;
import org.apache.cxf.annotations.WSDLDocumentation.Placement;
import org.apache.cxf.annotations.WSDLDocumentationCollection;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.FIStaxInInterceptor;
import org.apache.cxf.interceptor.FIStaxOutInterceptor;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.OperationInfo;

/**
 * 
 */
public class AnnotationsFactoryBeanListener implements FactoryBeanListener {
    
    private static final String EXTRA_DOCUMENTATION 
        = AnnotationsFactoryBeanListener.class.getName() + ".EXTRA_DOCS"; 

    /** {@inheritDoc}*/
    public void handleEvent(Event ev, AbstractServiceFactoryBean factory, Object... args) {
        switch (ev) {
        case INTERFACE_CREATED: {
            Class<?> cls = (Class<?>)args[1];
            WSDLDocumentation doc = cls.getAnnotation(WSDLDocumentation.class);
            if (doc != null) {
                addDocumentation((InterfaceInfo)args[0], WSDLDocumentation.Placement.PORT_TYPE, doc);
            }
            WSDLDocumentationCollection col = cls.getAnnotation(WSDLDocumentationCollection.class);
            if (col != null) {
                addDocumentation((InterfaceInfo)args[0], WSDLDocumentation.Placement.PORT_TYPE, col.value());
            }
            break;
        }
        case ENDPOINT_SELECTED: {
            Class<?> cls = (Class<?>)args[2];
            Endpoint ep = (Endpoint)args[1];
            addSchemaValidationSupport(ep, cls.getAnnotation(SchemaValidation.class));
            addFastInfosetSupport(ep, cls.getAnnotation(FastInfoset.class));
            addGZipSupport(ep, factory.getBus(), cls.getAnnotation(GZIP.class));
            break; 
        }
        case SERVER_CREATED: {
            Class<?> cls = (Class<?>)args[2];
            Server server = (Server)args[0];
            addGZipSupport(server.getEndpoint(), factory.getBus(), cls.getAnnotation(GZIP.class));
            addSchemaValidationSupport(server.getEndpoint(), cls.getAnnotation(SchemaValidation.class));
            addFastInfosetSupport(server.getEndpoint(), cls.getAnnotation(FastInfoset.class));
            WSDLDocumentation doc = cls.getAnnotation(WSDLDocumentation.class);
            if (doc != null) {
                addDocumentation(server, WSDLDocumentation.Placement.SERVICE, doc);
            }
            WSDLDocumentationCollection col = cls.getAnnotation(WSDLDocumentationCollection.class);
            if (col != null) {
                addDocumentation(server, WSDLDocumentation.Placement.SERVICE, col.value());
            }
            InterfaceInfo i = server.getEndpoint().getEndpointInfo().getInterface();
            List<WSDLDocumentation> docs = CastUtils.cast((List<?>)i.removeProperty(EXTRA_DOCUMENTATION));
            if (docs != null) {
                addDocumentation(server, 
                                 WSDLDocumentation.Placement.SERVICE,
                                 docs.toArray(new WSDLDocumentation[docs.size()]));
            }
            addBindingOperationDocs(server);
            break;
        }
        case INTERFACE_OPERATION_BOUND: {
            OperationInfo inf = (OperationInfo)args[0];
            Method m = (Method)args[1];
            WSDLDocumentation doc = m.getAnnotation(WSDLDocumentation.class);
            if (doc != null) {
                addDocumentation(inf, WSDLDocumentation.Placement.PORT_TYPE_OPERATION, doc);
            }
            WSDLDocumentationCollection col = m.getAnnotation(WSDLDocumentationCollection.class);
            if (col != null) {
                addDocumentation(inf, WSDLDocumentation.Placement.PORT_TYPE_OPERATION, col.value());
            }
            break;
        }
        default:
            //do nothing
        }
    }

    private void addGZipSupport(Endpoint ep, Bus bus, GZIP annotation) {
        if (annotation != null) {
            try {
                Class<?> cls = ClassLoaderUtils
                    .loadClass("org.apache.cxf.transport.http.gzip.GZIPFeature",
                               this.getClass());
                
                AbstractFeature feature = (AbstractFeature)cls.newInstance();
                cls.getMethod("setThreshold", new Class[] {Integer.TYPE})
                    .invoke(feature, annotation.threshold());
                feature.initialize(ep, bus);
            } catch (Exception e) {
                //ignore - just assume it's an unsupported/unknown annotation
            }
        }
    }

    private void addSchemaValidationSupport(Endpoint endpoint, SchemaValidation annotation) {
        if (annotation != null) {
            endpoint.put(Message.SCHEMA_VALIDATION_ENABLED, annotation.enabled());
        }
    }

    private void addFastInfosetSupport(InterceptorProvider provider, FastInfoset annotation) {
        if (annotation != null) {
            FIStaxInInterceptor in = new FIStaxInInterceptor();
            FIStaxOutInterceptor out = new FIStaxOutInterceptor(annotation.force());
            provider.getInInterceptors().add(in);
            provider.getInFaultInterceptors().add(in);
            provider.getOutInterceptors().add(out);
            provider.getOutFaultInterceptors().add(out);
        }
    }

    private void addBindingOperationDocs(Server server) {
        for (BindingOperationInfo binfo : server.getEndpoint().getBinding()
                .getBindingInfo().getOperations()) {
            List<WSDLDocumentation> later = CastUtils.cast((List<?>)binfo.getOperationInfo()
                                                               .getProperty(EXTRA_DOCUMENTATION));
            if (later != null) {
                for (WSDLDocumentation doc : later) {
                    switch (doc.placement()) {
                    case BINDING_OPERATION:
                        binfo.setDocumentation(doc.value());
                        break;
                    case BINDING_OPERATION_INPUT:
                        binfo.getInput().setDocumentation(doc.value());
                        break;
                    case BINDING_OPERATION_OUTPUT:
                        binfo.getOutput().setDocumentation(doc.value());
                        break;
                    case BINDING_OPERATION_FAULT: {
                        for (BindingFaultInfo f : binfo.getFaults()) {
                            if (doc.faultClass().equals(f.getFaultInfo()
                                                            .getProperty(Class.class.getName()))) {
                                f.setDocumentation(doc.value());
                            }
                        }
                        break;
                    }
                    default:
                        //nothing
                    }
                }
            }
        }
    }

    private void addDocumentation(OperationInfo inf, Placement defPlace, WSDLDocumentation ... values) {
        List<WSDLDocumentation> later = new ArrayList<WSDLDocumentation>();
        for (WSDLDocumentation doc : values) {
            WSDLDocumentation.Placement p = doc.placement();
            if (p == WSDLDocumentation.Placement.DEFAULT) {
                p = defPlace;
            }
            switch (p) {
            case PORT_TYPE_OPERATION:
                inf.setDocumentation(doc.value());
                break;
            case PORT_TYPE_OPERATION_INPUT:
                inf.getInput().setDocumentation(doc.value());
                break;
            case PORT_TYPE_OPERATION_OUTPUT:
                inf.getOutput().setDocumentation(doc.value());
                break;
            case FAULT_MESSAGE: 
            case PORT_TYPE_OPERATION_FAULT: {
                for (FaultInfo f : inf.getFaults()) {
                    if (doc.faultClass().equals(f.getProperty(Class.class.getName()))) {
                        if (p == Placement.FAULT_MESSAGE) {
                            f.setMessageDocumentation(doc.value());
                        } else {
                            f.setDocumentation(doc.value());
                        }
                    }
                }
                break;
            }
            case INPUT_MESSAGE:
                inf.getInput().setMessageDocumentation(doc.value());
                break;
            case OUTPUT_MESSAGE:
                inf.getOutput().setMessageDocumentation(doc.value());
                break;
            default:
                later.add(doc);
            }
        }
        if (!later.isEmpty()) {
            List<WSDLDocumentation> stuff = CastUtils.cast((List<?>)inf
                                                               .getProperty(EXTRA_DOCUMENTATION));
            if (stuff != null) {
                stuff.addAll(later);
            } else {
                inf.setProperty(EXTRA_DOCUMENTATION, later);
            }
        }
    }

    private void addDocumentation(InterfaceInfo interfaceInfo, 
                                  WSDLDocumentation.Placement defPlace,
                                  WSDLDocumentation ... values) {
        List<WSDLDocumentation> later = new ArrayList<WSDLDocumentation>();
        for (WSDLDocumentation doc : values) {
            WSDLDocumentation.Placement p = doc.placement();
            if (p == WSDLDocumentation.Placement.DEFAULT) {
                p = defPlace;
            }
            switch (p) {
            case PORT_TYPE:
                interfaceInfo.setDocumentation(doc.value());
                break;
            case SERVICE:
                interfaceInfo.getService().setDocumentation(doc.value());
                break;
            case TOP:
                interfaceInfo.getService().setTopLevelDoc(doc.value());
                break;
            default:
                later.add(doc);
            }
        }
        if (!later.isEmpty()) {
            List<WSDLDocumentation> stuff = CastUtils.cast((List<?>)interfaceInfo
                                                               .getProperty(EXTRA_DOCUMENTATION));
            if (stuff != null) {
                stuff.addAll(later);
            } else {
                interfaceInfo.setProperty(EXTRA_DOCUMENTATION, later);
            }
        }
    }
    private void addDocumentation(Server server, 
                                  WSDLDocumentation.Placement defPlace,
                                  WSDLDocumentation ... values) {
        for (WSDLDocumentation doc : values) {
            WSDLDocumentation.Placement p = doc.placement();
            if (p == WSDLDocumentation.Placement.DEFAULT) {
                p = defPlace;
            }
            switch (p) {
            case PORT_TYPE:
                server.getEndpoint().getEndpointInfo().getService()
                    .getInterface().setDocumentation(doc.value());
                break;
            case TOP:
                server.getEndpoint().getEndpointInfo().getService().setTopLevelDoc(doc.value());
                break;
            case SERVICE:
                server.getEndpoint().getEndpointInfo().getService().setDocumentation(doc.value());
                break;
            case SERVICE_PORT:
                server.getEndpoint().getEndpointInfo().setDocumentation(doc.value());
                break;
            case BINDING:
                server.getEndpoint().getEndpointInfo().getBinding().setDocumentation(doc.value());
                break;
            default:
                //nothing?
            }
        }
    }

}
