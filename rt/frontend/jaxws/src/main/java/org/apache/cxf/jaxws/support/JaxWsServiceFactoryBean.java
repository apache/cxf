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

package org.apache.cxf.jaxws.support;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.wsdl.Operation;
import javax.xml.bind.annotation.XmlNsForm;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.namespace.QName;
import javax.xml.ws.Action;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.BindingType;
import javax.xml.ws.FaultAction;
import javax.xml.ws.Service;
import javax.xml.ws.WebFault;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.soap.Addressing;
import javax.xml.ws.soap.AddressingFeature;
import javax.xml.ws.soap.MTOM;
import javax.xml.ws.soap.MTOMFeature;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.XMLSchemaQNames;
import org.apache.cxf.databinding.source.SourceDataBinding;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.JAXWSMethodDispatcher;
import org.apache.cxf.jaxws.JAXWSProviderMethodDispatcher;
import org.apache.cxf.jaxws.WrapperClassGenerator;
import org.apache.cxf.jaxws.interceptors.WebFaultOutInterceptor;
import org.apache.cxf.service.factory.AbstractServiceConfiguration;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.wsdl11.WSDLServiceBuilder;

/**
 * Constructs a service model from JAX-WS service endpoint classes. Works
 * with both @@WebServiceProvider and @@WebService annotated classes.
 *
 * @see org.apache.cxf.jaxws.JaxWsServerFactoryBean
 */
public class JaxWsServiceFactoryBean extends ReflectionServiceFactoryBean {
    // used to tag property on service.
    public static final  String WS_FEATURES = "JAXWS-WS-FEATURES";
    private static final Logger LOG = LogUtils.getLogger(JaxWsServiceFactoryBean.class);
    
    private AbstractServiceConfiguration jaxWsConfiguration;

    private JaxWsImplementorInfo implInfo;

    private List<WebServiceFeature> setWsFeatures;
    private List<WebServiceFeature> wsFeatures;
    
    private boolean wrapperBeanGenerated;
    private Set<Class<?>> wrapperClasses;
    
    
    public JaxWsServiceFactoryBean() {
        getIgnoredClasses().add(Service.class.getName());
        
        //the JAXWS-RI doesn't qualify the schemas for the wrapper types
        //and thus won't work if we do.
        setQualifyWrapperSchema(false);
        initSchemaLocations();
    }

    public JaxWsServiceFactoryBean(JaxWsImplementorInfo implInfo) {
        this();
        this.implInfo = implInfo;
        initConfiguration(implInfo);
        this.serviceClass = implInfo.getEndpointClass();
        loadWSFeatureAnnotation(implInfo.getSEIClass(), implInfo.getImplementorClass());
    }
    
    @Override
    public void reset() {
        super.reset();
        wrapperBeanGenerated = false;
        wrapperClasses = null;
    }

    private void initSchemaLocations() {
        this.schemaLocationMapping.put(JAXWSAConstants.NS_WSA, 
                                       JAXWSAConstants.WSA_XSD);
    }

    private void loadWSFeatureAnnotation(Class<?> serviceClass, Class<?> implementorClass) {
        List<WebServiceFeature> features = new ArrayList<WebServiceFeature>();
        MTOM mtom = implInfo.getImplementorClass().getAnnotation(MTOM.class);        
        if (mtom == null && serviceClass != null) {
            mtom = serviceClass.getAnnotation(MTOM.class);
        }
        if (mtom != null) {
            features.add(new MTOMFeature(mtom.enabled(), mtom.threshold()));
        } else {
            //deprecated way to set mtom
            BindingType bt = implInfo.getImplementorClass().getAnnotation(BindingType.class);
            if (bt != null
                && (SOAPBinding.SOAP11HTTP_MTOM_BINDING.equals(bt.value())
                || SOAPBinding.SOAP12HTTP_MTOM_BINDING.equals(bt.value()))) {
                features.add(new MTOMFeature(true));                
            }
        }
        

        Addressing addressing = null;
        if (implementorClass != null) {
            addressing = implementorClass.getAnnotation(Addressing.class);
        }

        if (addressing == null && serviceClass != null) {
            addressing = serviceClass.getAnnotation(Addressing.class);
        }

        if (addressing != null) {
            features.add(new AddressingFeature(addressing.enabled(), addressing.required()));
        }

        if (features.size() > 0) {
            wsFeatures = features;
            if (setWsFeatures != null) {
                wsFeatures.addAll(setWsFeatures);
            }
        } else {
            wsFeatures = setWsFeatures;
        }
    }

    @Override
    public org.apache.cxf.service.Service create() {
        org.apache.cxf.service.Service s = super.create();
        
        s.put(ENDPOINT_CLASS, implInfo.getEndpointClass());
        
        return s;
    }


    @Override
    public void setServiceClass(Class<?> serviceClass) {
        if (serviceClass == null) {
            Message message = new Message("SERVICECLASS_MUST_BE_SET", LOG);
            throw new ServiceConstructionException(message);
        }
        setJaxWsImplementorInfo(new JaxWsImplementorInfo(serviceClass));
        super.setServiceClass(getJaxWsImplementorInfo().getEndpointClass());
    }
    @Override
    protected void checkServiceClassAnnotations(Class<?> sc) {
        //no need to check
    }

    @Override
    protected void initializeFaultInterceptors() {
        getService().getOutFaultInterceptors().add(new WebFaultOutInterceptor());
    }

    @Override
    public Endpoint createEndpoint(EndpointInfo ei) throws EndpointException {        
        return new JaxWsEndpointImpl(getBus(), getService(), ei, implInfo, wsFeatures, 
                                     this.getFeatures(), this.isFromWsdl());
    }

    @Override
    protected void initializeWSDLOperation(InterfaceInfo intf, OperationInfo o, Method method) {
        method = ((JaxWsServiceConfiguration)jaxWsConfiguration).getDeclaredMethod(method);
        o.setProperty(Method.class.getName(), method);
        o.setProperty(METHOD, method);
        initializeWrapping(o, method);

        // rpc out-message-part-info class mapping
        Operation op = (Operation)o.getProperty(WSDLServiceBuilder.WSDL_OPERATION);

        initializeClassInfo(o, method, op == null ? null
            : CastUtils.cast(op.getParameterOrdering(), String.class));

        bindOperation(o, method);
    }
    
    protected void bindOperation(OperationInfo op, Method method) {
        
        try {
            // Find the Async method which returns a Response
            Method responseMethod = method.getDeclaringClass().getDeclaredMethod(method.getName() + "Async",
                                                                                 method.getParameterTypes());

            // Find the Async method whic has a Future & AsyncResultHandler
            List<Class<?>> asyncHandlerParams = Arrays.asList(method.getParameterTypes());
            //copy it to may it non-readonly
            asyncHandlerParams = new ArrayList<Class<?>>(asyncHandlerParams);
            asyncHandlerParams.add(AsyncHandler.class);
            Method futureMethod = method.getDeclaringClass()
                .getDeclaredMethod(method.getName() + "Async",
                                   asyncHandlerParams.toArray(new Class<?>[asyncHandlerParams.size()]));

            getMethodDispatcher().bind(op, method, responseMethod, futureMethod);

        } catch (SecurityException e) {
            throw new ServiceConstructionException(e);
        } catch (NoSuchMethodException e) {
            getMethodDispatcher().bind(op, method);
        }
    }

    @Override
    protected void initializeWSDLOperations() {
        if (implInfo.isWebServiceProvider()) {
            initializeWSDLOperationsForProvider();
        } else {
            super.initializeWSDLOperations();
        }
    }

    protected void initializeWSDLOperationsForProvider() {
        Type[] genericInterfaces = getServiceClass().getGenericInterfaces();
        ParameterizedType pt = (ParameterizedType)genericInterfaces[0];
        Class c = (Class)pt.getActualTypeArguments()[0];

        if (getEndpointInfo() == null
            && isFromWsdl()) {
            //most likely, they specified a WSDL, but for some reason
            //did not give a valid ServiceName/PortName.  For provider,
            //we'll allow this since everything is bound directly to 
            //the invoke method, however, this CAN cause other problems
            //such as addresses in the wsdl not getting updated and such
            //so we'll WARN about it.....
            List<QName> enames = new ArrayList<QName>();
            for (ServiceInfo si : getService().getServiceInfos()) {
                for (EndpointInfo ep : si.getEndpoints()) {
                    enames.add(ep.getName());
                }
            }
            LOG.log(Level.WARNING, "COULD_NOT_FIND_ENDPOINT", 
                    new Object[] {getEndpointName(), enames});
        }
        
        try {
            Method invoke = getServiceClass().getMethod("invoke", c);
            QName catchAll = new QName("http://cxf.apache.org/jaxws/provider", "invoke");
            
            // Bind every operation to the invoke method.
            for (ServiceInfo si : getService().getServiceInfos()) {
                si.setProperty("soap.force.doclit.bare", Boolean.TRUE);
                for (BindingInfo bind : si.getBindings()) {
                    for (BindingOperationInfo bop : bind.getOperations()) {
                        OperationInfo o = bop.getOperationInfo();
                        if (o.getInput() != null) {
                            if (o.getInput().getMessageParts().isEmpty()) {
                                MessagePartInfo inf = o.getInput().addMessagePart(o.getName());
                                inf.setConcreteName(o.getName());
                                bop.getInput().setMessageParts(o.getInput().getMessageParts());
                            }
                            for (MessagePartInfo inf : o.getInput().getMessageParts()) {
                                inf.setTypeClass(c);
                                break;
                            }
                        }
                        if (o.getOutput() != null) {
                            if (o.getOutput().getMessageParts().isEmpty()) {
                                MessagePartInfo inf = o.getOutput().addMessagePart(o.getName());
                                inf.setConcreteName(new QName(o.getName().getNamespaceURI(),
                                                              o.getName().getLocalPart() + "Response"));
                                bop.getOutput().setMessageParts(o.getOutput().getMessageParts());
                            }
                            for (MessagePartInfo inf : o.getOutput().getMessageParts()) {
                                inf.setTypeClass(c);
                                break;
                            }
                        }
                        getMethodDispatcher().bind(o, invoke);
                    }
                    BindingOperationInfo bop = bind.getOperation(catchAll);
                    if (bop == null) {
                        OperationInfo op = bind.getInterface().getOperation(catchAll);
                        if (op == null) {
                            op = bind.getInterface().addOperation(catchAll);
                            String name = catchAll.getLocalPart();
                            MessageInfo mInfo = op.createMessage(new QName(catchAll.getNamespaceURI(),
                                                                           name + "Request"),
                                                                           MessageInfo.Type.INPUT);
                            op.setInput(catchAll.getLocalPart() + "Request", mInfo);
                            MessagePartInfo mpi = mInfo.addMessagePart("parameters");
                            mpi.setElement(true);
                            mpi.setTypeClass(c);
                            mpi.setTypeQName(XMLSchemaQNames.XSD_ANY);
                            
                            mInfo = op.createMessage(new QName(catchAll.getNamespaceURI(), 
                                                               name + "Response"),
                                                               MessageInfo.Type.OUTPUT);
                            op.setOutput(name + "Response", mInfo);
                            mpi = mInfo.addMessagePart("parameters");
                            mpi.setElement(true);
                            mpi.setTypeClass(c);
                            mpi.setTypeQName(XMLSchemaQNames.XSD_ANY);
                        
                            BindingOperationInfo bo = new BindingOperationInfo(bind, op);
                            op.setProperty("operation.is.synthetic", Boolean.TRUE);
                            bo.setProperty("operation.is.synthetic", Boolean.TRUE);
                            bind.addOperation(bo);
                        }
                    }
                }
            }
        } catch (SecurityException e) {
            throw new ServiceConstructionException(e);
        } catch (NoSuchMethodException e) {
            throw new ServiceConstructionException(e);
        }

        
    }

    void initializeWrapping(OperationInfo o, Method selected) {
        Class responseWrapper = getResponseWrapper(selected);
        if (responseWrapper != null) {
            o.getOutput().getMessageParts().get(0).setTypeClass(responseWrapper);
        }
        if (getResponseWrapperClassName(selected) != null) {
            o.getOutput().getMessageParts().get(0).setProperty("RESPONSE.WRAPPER.CLASSNAME",
                                                           getResponseWrapperClassName(selected));
        }
        Class<?> requestWrapper = getRequestWrapper(selected);
        if (requestWrapper != null) {
            o.getInput().getMessageParts().get(0).setTypeClass(requestWrapper);
        }
        if (getRequestWrapperClassName(selected) != null) {
            o.getInput().getMessageParts().get(0).setProperty("REQUEST.WRAPPER.CLASSNAME",
                                                           getRequestWrapperClassName(selected));
        }
    }


    @Override
    protected Class<?> getBeanClass(Class<?> exClass) {
        try {
            if (java.rmi.ServerException.class.isAssignableFrom(exClass)
                || java.rmi.RemoteException.class.isAssignableFrom(exClass)
                || "javax.xml.ws".equals(PackageUtils.getPackageName(exClass))) {
                return null;
            }

            Method getFaultInfo = exClass.getMethod("getFaultInfo", new Class[0]);

            return getFaultInfo.getReturnType();
        } catch (SecurityException e) {
            throw new ServiceConstructionException(e);
        } catch (NoSuchMethodException e) {
            //ignore for now
        }
        WebFault fault = exClass.getAnnotation(WebFault.class);
        if (fault != null && !StringUtils.isEmpty(fault.faultBean())) {
            try {
                return ClassLoaderUtils.loadClass(fault.faultBean(),
                                                   exClass);
            } catch (ClassNotFoundException e1) {
                //ignore
            }
        }
        
        return super.getBeanClass(exClass);
    }

    public void setJaxWsConfiguration(JaxWsServiceConfiguration jaxWsConfiguration) {
        this.jaxWsConfiguration = jaxWsConfiguration;
    }

    public JaxWsImplementorInfo getJaxWsImplementorInfo() {
        return implInfo;
    }

    public void setJaxWsImplementorInfo(JaxWsImplementorInfo jaxWsImplementorInfo) {
        this.implInfo = jaxWsImplementorInfo;

        initConfiguration(jaxWsImplementorInfo);
    }

    protected final void initConfiguration(JaxWsImplementorInfo ii) {
        if (ii.isWebServiceProvider()) {
            jaxWsConfiguration = new WebServiceProviderConfiguration();
            jaxWsConfiguration.setServiceFactory(this);
            getServiceConfigurations().add(0, jaxWsConfiguration);
            setWrapped(false);
            setDataBinding(new SourceDataBinding());
            setMethodDispatcher(new JAXWSProviderMethodDispatcher(implInfo));
        } else {
            jaxWsConfiguration = new JaxWsServiceConfiguration();
            jaxWsConfiguration.setServiceFactory(this);
            getServiceConfigurations().add(0, jaxWsConfiguration);
            
            Class<?> seiClass = ii.getEndpointClass();
            if (seiClass != null && seiClass.getPackage() != null) {
                XmlSchema schema = seiClass.getPackage().getAnnotation(XmlSchema.class);
                if (schema != null && XmlNsForm.QUALIFIED.equals(schema.elementFormDefault())) {
                    setQualifyWrapperSchema(true);
                }
            }
            setMethodDispatcher(new JAXWSMethodDispatcher(implInfo));
        }
        loadWSFeatureAnnotation(ii.getSEIClass(), ii.getImplementorClass());        
    }

    public List<WebServiceFeature> getWsFeatures() {
        return setWsFeatures;
    }

    public void setWsFeatures(List<WebServiceFeature> swsFeatures) {
        this.setWsFeatures = swsFeatures;
        if (wsFeatures == null) {
            wsFeatures = setWsFeatures;
        }
    }

    private FaultInfo getFaultInfo(final OperationInfo operation, final Class expClass) {
        for (FaultInfo fault : operation.getFaults()) {
            if (fault.getProperty(Class.class.getName()) == expClass
                || fault.getProperty(Class.class.getName()) == expClass) {
                return fault;
            }
        }
        return null;
    }
    private void buildWSAActions(OperationInfo operation, Method method) {
        //nothing
        if (method == null) {
            return;
        }

        Action action = method.getAnnotation(Action.class);
        if (action == null) {
            return;
        }
        MessageInfo input = operation.getInput();
        if (!StringUtils.isEmpty(action.input())) {
            input.addExtensionAttribute(JAXWSAConstants.WSAW_ACTION_QNAME, action.input());
        }
        
        MessageInfo output = operation.getOutput();
        if (output != null && !StringUtils.isEmpty(action.output())) {
            output.addExtensionAttribute(JAXWSAConstants.WSAW_ACTION_QNAME, action.output());
        }
        
        FaultAction[] faultActions = action.fault();
        if (faultActions != null 
            && faultActions.length > 0 
            && operation.getFaults() != null) {
            for (FaultAction faultAction : faultActions) {                
                FaultInfo faultInfo = getFaultInfo(operation, faultAction.className());
                faultInfo.addExtensionAttribute(JAXWSAConstants.WSAW_ACTION_QNAME, 
                                                faultAction.value());
                if (operation.isUnwrappedCapable()) {
                    faultInfo = getFaultInfo(operation.getUnwrappedOperation(), faultAction.className());
                    faultInfo.addExtensionAttribute(JAXWSAConstants.WSAW_ACTION_QNAME, 
                                                    faultAction.value());
                }
            }
        }        
    }
    
    @Override
    protected OperationInfo createOperation(ServiceInfo serviceInfo, InterfaceInfo intf, Method m) {
        OperationInfo op = super.createOperation(serviceInfo, intf, m);
        if (op.getUnwrappedOperation() != null) {
            op = op.getUnwrappedOperation();
        }
        buildWSAActions(op, m);
        return op;
    }
    
    @Override
    protected Set<Class<?>> getExtraClass() {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        if (!wrapperBeanGenerated) {
            wrapperClasses = generatedWrapperBeanClass();
        }
        if (wrapperClasses != null) {
            classes.addAll(wrapperClasses);
        }
        
        XmlSeeAlso xmlSeeAlsoAnno = getServiceClass().getAnnotation(XmlSeeAlso.class);
        
        if (xmlSeeAlsoAnno != null && xmlSeeAlsoAnno.value() != null) {
            for (int i = 0; i < xmlSeeAlsoAnno.value().length; i++) {
                classes.add(xmlSeeAlsoAnno.value()[i]);
            }
        }
        return classes;
    }
    
    private Set<Class<?>> generatedWrapperBeanClass() {
        if (getDataBinding() instanceof JAXBDataBinding) {
            ServiceInfo serviceInfo = getService().getServiceInfos().get(0);
            WrapperClassGenerator wrapperGen = new WrapperClassGenerator(this,
                                                                         serviceInfo.getInterface(),
                                                                         getQualifyWrapperSchema());
            return wrapperGen.generate();            
        }
        return Collections.emptySet();
    }

    @Override
    protected void buildServiceFromClass() {
        super.buildServiceFromClass();
        getService().put(WS_FEATURES, getWsFeatures()); 
    }
    
    protected void initializeParameter(MessagePartInfo part, Class rawClass, Type type) {
        if (implInfo.isWebServiceProvider()) {
            part.setTypeQName(XMLSchemaQNames.XSD_ANY);
            part.setTypeClass(rawClass);
            return;
        }
        super.initializeParameter(part, rawClass, type);
    }
}
