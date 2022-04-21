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
import org.apache.cxf.annotations.DataBinding;
import org.apache.cxf.annotations.EndpointProperties;
import org.apache.cxf.annotations.EndpointProperty;
import org.apache.cxf.annotations.FactoryType;
import org.apache.cxf.annotations.FastInfoset;
import org.apache.cxf.annotations.GZIP;
import org.apache.cxf.annotations.SchemaValidation;
import org.apache.cxf.annotations.WSDLDocumentation;
import org.apache.cxf.annotations.WSDLDocumentation.Placement;
import org.apache.cxf.annotations.WSDLDocumentationCollection;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.FIStaxInInterceptor;
import org.apache.cxf.interceptor.FIStaxOutInterceptor;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.message.Message;
import org.apache.cxf.resource.ResourceManager;
import org.apache.cxf.service.invoker.Factory;
import org.apache.cxf.service.invoker.FactoryInvoker;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.invoker.PerRequestFactory;
import org.apache.cxf.service.invoker.PooledFactory;
import org.apache.cxf.service.invoker.SessionFactory;
import org.apache.cxf.service.invoker.SingletonFactory;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.transport.common.gzip.GZIPFeature;

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
            InterfaceInfo ii = (InterfaceInfo)args[0];
            Class<?> cls = (Class<?>)args[1];
            WSDLDocumentation doc = cls.getAnnotation(WSDLDocumentation.class);
            if (doc != null) {
                addDocumentation(ii, WSDLDocumentation.Placement.PORT_TYPE, doc);
            }
            WSDLDocumentationCollection col = cls.getAnnotation(WSDLDocumentationCollection.class);
            if (col != null) {
                addDocumentation(ii, WSDLDocumentation.Placement.PORT_TYPE, col.value());
            }
            setDataBinding(factory, cls.getAnnotation(DataBinding.class));
            break;
        }
        case ENDPOINT_SELECTED: {
            Class<?> implCls = args.length > 3 ? (Class<?>)args[3] : null;
            Class<?> cls = (Class<?>)args[2];
            Endpoint ep = (Endpoint)args[1];
            Bus bus = factory.getBus();
            // To avoid the NPE
            if (cls == null) {
                return;
            }
            addSchemaValidationSupport(ep, cls.getAnnotation(SchemaValidation.class));
            addFastInfosetSupport(ep, cls.getAnnotation(FastInfoset.class));
            addGZipSupport(ep, bus, cls.getAnnotation(GZIP.class));
            addEndpointProperties(ep, bus, cls.getAnnotation(EndpointProperty.class));
            EndpointProperties props = cls.getAnnotation(EndpointProperties.class);
            if (props != null) {
                addEndpointProperties(ep, bus, props.value());
            }
            // To avoid the NPE
            if (implCls == null || implCls == cls) {
                return;
            }
            WSDLDocumentation doc = implCls.getAnnotation(WSDLDocumentation.class);
            if (doc != null) {
                addDocumentation(ep, WSDLDocumentation.Placement.SERVICE, doc);
            }
            WSDLDocumentationCollection col = implCls.getAnnotation(WSDLDocumentationCollection.class);
            if (col != null) {
                addDocumentation(ep, WSDLDocumentation.Placement.SERVICE, col.value());
            }
            InterfaceInfo i = ep.getEndpointInfo().getInterface();
            List<WSDLDocumentation> docs = CastUtils.cast((List<?>)i.removeProperty(EXTRA_DOCUMENTATION));
            if (docs != null) {
                addDocumentation(ep,
                                 WSDLDocumentation.Placement.SERVICE,
                                 docs.toArray(new WSDLDocumentation[0]));
            }
            addBindingOperationDocs(ep);
            for (Method method : implCls.getMethods()) {
                if (method.getAnnotation(SchemaValidation.class) != null) {
                    try {
                        Method interfaceMethod = cls.getMethod(method.getName(), method.getParameterTypes());
                        for (BindingOperationInfo bopInfo : ep.getBinding().getBindingInfo().getOperations()) {
                            if (interfaceMethod.equals(bopInfo.getOperationInfo()
                                .getProperty("operation.method"))) {
                                addSchemaValidationSupport(bopInfo.getOperationInfo(),
                                                           method.getAnnotation(SchemaValidation.class));
                            }
                        }
                    } catch (Exception e) {
                        // ignore this
                    }
                }
            }
            break;
        }
        case SERVER_CREATED: {
            Class<?> cls = (Class<?>)args[2];
            if (cls == null) {
                return;
            }
            Server server = (Server)args[0];
            Bus bus = factory.getBus();
            addGZipSupport(server.getEndpoint(), bus, cls.getAnnotation(GZIP.class));
            addSchemaValidationSupport(server.getEndpoint(), cls.getAnnotation(SchemaValidation.class));
            addFastInfosetSupport(server.getEndpoint(), cls.getAnnotation(FastInfoset.class));
            addEndpointProperties(server.getEndpoint(), bus, cls.getAnnotation(EndpointProperty.class));
            EndpointProperties props = cls.getAnnotation(EndpointProperties.class);
            if (props != null) {
                addEndpointProperties(server.getEndpoint(), bus, props.value());
            }
            setScope(server, cls);
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
            SchemaValidation methodValidation = m.getAnnotation(SchemaValidation.class);
            if (methodValidation != null) {
                addSchemaValidationSupport(inf, methodValidation);
            }
            break;
        }
        default:
            //do nothing
        }
    }

    private void setScope(Server server, Class<?> cls) {
        FactoryType scope = cls.getAnnotation(FactoryType.class);
        if (scope != null) {
            Invoker i = server.getEndpoint().getService().getInvoker();
            if (i instanceof FactoryInvoker) {
                Factory f;
                if (scope.factoryClass() == FactoryType.DEFAULT.class) {
                    switch (scope.value()) {
                    case Session:
                        if (scope.args().length > 0) {
                            f = new SessionFactory(cls, Boolean.parseBoolean(scope.args()[0]));
                        } else {
                            f = new SessionFactory(cls);
                        }
                        break;
                    case PerRequest:
                        f = new PerRequestFactory(cls);
                        break;
                    case Pooled:
                        f = new PooledFactory(cls, Integer.parseInt(scope.args()[0]));
                        break;
                    default:
                        f = new SingletonFactory(cls);
                        break;
                    }
                } else {
                    try {
                        f = scope.factoryClass().getConstructor(Class.class, String[].class)
                            .newInstance(cls, scope.args());
                    } catch (Throwable t) {
                        throw new ServiceConstructionException(t);
                    }
                }
                ((FactoryInvoker)i).setFactory(f);
            }

        }
    }

    private void addEndpointProperties(Endpoint ep, Bus bus, EndpointProperty ... annotations) {
        for (EndpointProperty prop : annotations) {
            if (prop == null) {
                continue;
            }
            String ref = prop.ref();
            Class<?> cls = prop.beanClass();
            final Object obj;
            String[] s = prop.value();
            if (!StringUtils.isEmpty(ref)) {
                obj = bus.getExtension(ConfiguredBeanLocator.class).getBeanOfType(ref, cls);
            } else if (s.length == 0 && cls != Object.class) {
                obj = createObject(cls, ep, bus);
            } else if (s.length == 1) {
                obj = s[0];
            } else {
                obj = s;
            }
            ep.getEndpointInfo().setProperty(prop.key(), obj);
        }

    }

    private Object createObject(Class<?> cls, Endpoint ep, Bus bus) {
        try {
            try {
                return cls.getConstructor(Endpoint.class, Bus.class).newInstance(ep, bus);
            } catch (NoSuchMethodException e) {
                try {
                    return cls.getConstructor(Endpoint.class).newInstance(ep);
                } catch (NoSuchMethodException e2) {
                    return cls.getConstructor().newInstance();
                }
            }
        } catch (Exception ex) {
            throw new ServiceConstructionException(ex);
        }
    }

    private void setDataBinding(AbstractServiceFactoryBean factory,
                                DataBinding annotation) {
        if (annotation != null && factory.getDataBinding(false) == null) {
            try {
                if (!StringUtils.isEmpty(annotation.ref())) {
                    factory.setDataBinding(factory.getBus().getExtension(ResourceManager.class)
                        .resolveResource(annotation.ref(), annotation.value()));
                }

                factory.setDataBinding(annotation.value().getConstructor().newInstance());
            } catch (Exception e) {
                //REVISIT - log a warning
            }
        }
    }

    private void addGZipSupport(Endpoint ep, Bus bus, GZIP annotation) {
        if (annotation != null) {
            try {
                GZIPFeature feature = new GZIPFeature();
                feature.setThreshold(annotation.threshold());
                feature.setForce(annotation.force());
                feature.initialize(ep, bus);
            } catch (Exception e) {
                //ignore - just assume it's an unsupported/unknown annotation
            }
        }
    }

    /**
     * @param endpoint
     * @param annotation
     */
    private void addSchemaValidationSupport(Endpoint endpoint, SchemaValidation annotation) {
        if (annotation != null) {
            endpoint.getEndpointInfo().setProperty(Message.SCHEMA_VALIDATION_TYPE, annotation.type());
        }
    }

    private void addFastInfosetSupport(InterceptorProvider provider, FastInfoset annotation) {
        if (annotation != null) {
            FIStaxInInterceptor in = new FIStaxInInterceptor();

            FIStaxOutInterceptor out = new FIStaxOutInterceptor(annotation.force());
            out.setSerializerAttributeValueMapMemoryLimit(annotation.serializerAttributeValueMapMemoryLimit());
            out.setSerializerMinAttributeValueSize(annotation.serializerMinAttributeValueSize());
            out.setSerializerMaxAttributeValueSize(annotation.serializerMaxAttributeValueSize());
            out.setSerializerCharacterContentChunkMapMemoryLimit(
                    annotation.serializerCharacterContentChunkMapMemoryLimit());
            out.setSerializerMinCharacterContentChunkSize(annotation.serializerMinCharacterContentChunkSize());
            out.setSerializerMaxCharacterContentChunkSize(annotation.serializerMaxCharacterContentChunkSize());

            provider.getInInterceptors().add(in);
            provider.getInFaultInterceptors().add(in);
            provider.getOutInterceptors().add(out);
            provider.getOutFaultInterceptors().add(out);
        }
    }

    private void addBindingOperationDocs(Endpoint ep) {
        for (BindingOperationInfo binfo : ep.getBinding()
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

    private void addSchemaValidationSupport(OperationInfo inf, SchemaValidation annotation) {
        if (annotation != null) {
            inf.setProperty(Message.SCHEMA_VALIDATION_TYPE, annotation.type());
        }
    }

    private void addDocumentation(OperationInfo inf, Placement defPlace, WSDLDocumentation ... values) {
        List<WSDLDocumentation> later = new ArrayList<>();
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
                break;
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
        List<WSDLDocumentation> later = new ArrayList<>();
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
                break;
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
    private void addDocumentation(Endpoint ep,
                                  WSDLDocumentation.Placement defPlace,
                                  WSDLDocumentation ... values) {
        for (WSDLDocumentation doc : values) {
            WSDLDocumentation.Placement p = doc.placement();
            if (p == WSDLDocumentation.Placement.DEFAULT) {
                p = defPlace;
            }
            switch (p) {
            case PORT_TYPE:
                ep.getEndpointInfo().getService()
                    .getInterface().setDocumentation(doc.value());
                break;
            case TOP:
                ep.getEndpointInfo().getService().setTopLevelDoc(doc.value());
                break;
            case SERVICE:
                ep.getEndpointInfo().getService().setDocumentation(doc.value());
                break;
            case SERVICE_PORT:
                ep.getEndpointInfo().setDocumentation(doc.value());
                break;
            case BINDING:
                ep.getEndpointInfo().getBinding().setDocumentation(doc.value());
                break;
            default:
                //nothing?
            }
        }
    }

}
