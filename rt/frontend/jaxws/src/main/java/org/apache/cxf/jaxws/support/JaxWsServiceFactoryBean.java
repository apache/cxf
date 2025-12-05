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
import javax.xml.namespace.QName;

import jakarta.jws.WebMethod;
import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.ws.Action;
import jakarta.xml.ws.AsyncHandler;
import jakarta.xml.ws.BindingType;
import jakarta.xml.ws.FaultAction;
import jakarta.xml.ws.Provider;
import jakarta.xml.ws.RespectBinding;
import jakarta.xml.ws.RespectBindingFeature;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebFault;
import jakarta.xml.ws.WebServiceFeature;
import jakarta.xml.ws.soap.Addressing;
import jakarta.xml.ws.soap.AddressingFeature;
import jakarta.xml.ws.soap.MTOM;
import jakarta.xml.ws.soap.MTOMFeature;
import jakarta.xml.ws.soap.SOAPBinding;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.databinding.source.SourceDataBinding;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxws.JAXWSMethodDispatcher;
import org.apache.cxf.jaxws.JAXWSMethodInvoker;
import org.apache.cxf.jaxws.JAXWSProviderMethodDispatcher;
import org.apache.cxf.jaxws.interceptors.WebFaultOutInterceptor;
import org.apache.cxf.jaxws.spi.WrapperClassCreator;
import org.apache.cxf.service.factory.FactoryBeanListener;
import org.apache.cxf.service.factory.FactoryBeanListener.Event;
import org.apache.cxf.service.factory.ServiceConstructionException;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.invoker.SingletonFactory;
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
import org.apache.cxf.wsdl.service.factory.AbstractServiceConfiguration;
import org.apache.cxf.wsdl.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.wsdl11.WSDLServiceBuilder;
import org.apache.ws.commons.schema.constants.Constants;

/**
 * Constructs a service model from JAX-WS service endpoint classes. Works
 * with both @@WebServiceProvider and @@WebService annotated classes.
 *
 * @see org.apache.cxf.jaxws.JaxWsServerFactoryBean
 */
@NoJSR250Annotations
public class JaxWsServiceFactoryBean extends ReflectionServiceFactoryBean {
    // used to tag property on service.
    public static final  String WS_FEATURES = "JAXWS-WS-FEATURES";
    private static final Logger LOG = LogUtils.getLogger(JaxWsServiceFactoryBean.class);

    private AbstractServiceConfiguration jaxWsConfiguration;

    private JaxWsImplementorInfo implInfo;

    private List<WebServiceFeature> setWsFeatures;
    private List<WebServiceFeature> wsFeatures;

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
        this.serviceType = implInfo.getSEIType();
        loadWSFeatureAnnotation(implInfo.getSEIClass(), implInfo.getImplementorClass());
    }

    @Override
    public void reset() {
        super.reset();
        wrapperClasses = null;
    }

    private void initSchemaLocations() {
        this.schemaLocationMapping.put(JAXWSAConstants.NS_WSA,
                                       JAXWSAConstants.WSA_XSD);
    }

    private void loadWSFeatureAnnotation(Class<?> serviceClass, Class<?> implementorClass) {
        List<WebServiceFeature> features = new ArrayList<>();
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
            features.add(new AddressingFeature(addressing.enabled(),
                                               addressing.required(),
                                               addressing.responses()));
        }

        RespectBinding respectBinding = implInfo.getImplementorClass().getAnnotation(
            RespectBinding.class);
        if (respectBinding == null && serviceClass != null) {
            respectBinding = serviceClass.getAnnotation(RespectBinding.class);
        }
        if (respectBinding != null) {
            features.add(new RespectBindingFeature(respectBinding.enabled()));
        }

        if (!features.isEmpty()) {
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

        if (s.getDataBinding() != null) {
            setMTOMFeatures(s.getDataBinding());
        }
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
        super.setServiceType(getJaxWsImplementorInfo().getSEIType());
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
        Endpoint ep = new JaxWsEndpointImpl(getBus(), getService(), ei, implInfo, wsFeatures,
                                     this.getFeatures(), this.isFromWsdl());
        sendEvent(FactoryBeanListener.Event.ENDPOINT_CREATED, ei, ep);
        return ep;
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

        sendEvent(Event.INTERFACE_OPERATION_BOUND, o, method);
    }

    protected void bindOperation(OperationInfo op, Method method) {

        try {
            // Find the Async method which returns a Response
            Method responseMethod = ReflectionUtil.getDeclaredMethod(method.getDeclaringClass(),
                                                                     method.getName() + "Async",
                                                                     method.getParameterTypes());

            // Find the Async method whic has a Future & AsyncResultHandler
            List<Class<?>> asyncHandlerParams = Arrays.asList(method.getParameterTypes());
            //copy it to may it non-readonly
            asyncHandlerParams = new ArrayList<>(asyncHandlerParams);
            asyncHandlerParams.add(AsyncHandler.class);
            Method futureMethod = ReflectionUtil
                .getDeclaredMethod(method.getDeclaringClass(), method.getName() + "Async",
                                   asyncHandlerParams.toArray(new Class<?>[0]));

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
        Class<?> c = getProviderParameterType(getServiceClass());
        if (c == null) {
            throw new ServiceConstructionException(new Message("INVALID_PROVIDER_EXC", LOG));
        }

        if (getEndpointInfo() == null
            && isFromWsdl()) {
            //most likely, they specified a WSDL, but for some reason
            //did not give a valid ServiceName/PortName.  For provider,
            //we'll allow this since everything is bound directly to
            //the invoke method, however, this CAN cause other problems
            //such as addresses in the wsdl not getting updated and such
            //so we'll WARN about it.....
            List<QName> enames = new ArrayList<>();
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
                if (!isFromWsdl()) {
                    for (EndpointInfo ei : si.getEndpoints()) {
                        ei.setProperty("soap.no.validate.parts", Boolean.TRUE);
                    }
                }
                for (BindingInfo bind : si.getBindings()) {
                    for (BindingOperationInfo bop : bind.getOperations()) {
                        OperationInfo o = bop.getOperationInfo();
                        if (bop.isUnwrappedCapable()) {
                            //force to bare, no unwrapping
                            bop.getOperationInfo().setUnwrappedOperation(null);
                            bop.setUnwrappedOperation(null);
                        }
                        if (o.getInput() != null) {
                            final List<MessagePartInfo> messageParts;
                            if (o.getInput().getMessagePartsNumber() == 0) {
                                MessagePartInfo inf = o.getInput().addMessagePart(o.getName());
                                inf.setConcreteName(o.getName());
                                messageParts = o.getInput().getMessageParts();
                                bop.getInput().setMessageParts(messageParts);
                            } else {
                                messageParts = o.getInput().getMessageParts();
                            }
                            for (MessagePartInfo inf : messageParts) {
                                inf.setTypeClass(c);
                                break;
                            }
                        }
                        if (o.getOutput() != null) {
                            final List<MessagePartInfo> messageParts;
                            if (o.getOutput().getMessagePartsNumber() == 0) {
                                MessagePartInfo inf = o.getOutput().addMessagePart(o.getName());
                                inf.setConcreteName(new QName(o.getName().getNamespaceURI(),
                                                              o.getName().getLocalPart() + "Response"));
                                messageParts = o.getOutput().getMessageParts();
                                bop.getOutput().setMessageParts(messageParts);
                            } else {
                                messageParts = o.getOutput().getMessageParts();
                            }
                            for (MessagePartInfo inf : messageParts) {
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
                            mpi.setTypeQName(Constants.XSD_ANYTYPE);

                            mInfo = op.createMessage(new QName(catchAll.getNamespaceURI(),
                                                               name + "Response"),
                                                               MessageInfo.Type.OUTPUT);
                            op.setOutput(name + "Response", mInfo);
                            mpi = mInfo.addMessagePart("parameters");
                            mpi.setElement(true);
                            mpi.setTypeClass(c);
                            mpi.setTypeQName(Constants.XSD_ANYTYPE);

                            BindingOperationInfo bo = new BindingOperationInfo(bind, op);
                            op.setProperty("operation.is.synthetic", Boolean.TRUE);
                            bo.setProperty("operation.is.synthetic", Boolean.TRUE);
                            bind.addOperation(bo);
                        }
                    }
                }
            }
        } catch (SecurityException | NoSuchMethodException e) {
            throw new ServiceConstructionException(e);
        }


    }

    protected Class<?> getProviderParameterType(Class<?> cls) {
        if (cls == null) {
            return null;
        }
        Type[] genericInterfaces = cls.getGenericInterfaces();
        for (Type type : genericInterfaces) {
            if (type instanceof ParameterizedType) {
                Class<?> rawCls = (Class<?>)((ParameterizedType)type).getRawType();
                if (Provider.class == rawCls) {
                    return (Class<?>)((ParameterizedType)type).getActualTypeArguments()[0];
                }
            } else if (type instanceof Class && Provider.class.isAssignableFrom((Class<?>)type)) {
                return getProviderParameterType((Class<?>)type);
            }
        }
        return getProviderParameterType(cls.getSuperclass());
    }

    void initializeWrapping(OperationInfo o, Method selected) {
        Class<?> responseWrapper = getResponseWrapper(selected);
        if (responseWrapper != null) {
            o.getOutput().getFirstMessagePart().setTypeClass(responseWrapper);
        }
        if (getResponseWrapperClassName(selected) != null) {
            o.getOutput().getFirstMessagePart().setProperty("RESPONSE.WRAPPER.CLASSNAME",
                                                           getResponseWrapperClassName(selected));
        }
        Class<?> requestWrapper = getRequestWrapper(selected);
        if (requestWrapper != null) {
            o.getInput().getFirstMessagePart().setTypeClass(requestWrapper);
        }
        if (getRequestWrapperClassName(selected) != null) {
            o.getInput().getFirstMessagePart().setProperty("REQUEST.WRAPPER.CLASSNAME",
                                                           getRequestWrapperClassName(selected));
        }
    }


    @Override
    protected Class<?> getBeanClass(Class<?> exClass) {
        try {
            if (java.rmi.ServerException.class.isAssignableFrom(exClass)
                || java.rmi.RemoteException.class.isAssignableFrom(exClass)
                || "jakarta.xml.ws".equals(PackageUtils.getPackageName(exClass))) {
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
                if (schema != null && XmlNsForm.QUALIFIED == schema.elementFormDefault()) {
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

    private FaultInfo getFaultInfo(final OperationInfo operation, final Class<?> expClass) {
        for (FaultInfo fault : operation.getFaults()) {
            if (fault.getProperty(Class.class.getName()) == expClass) {
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
        Addressing addressing = method.getDeclaringClass().getAnnotation(Addressing.class);
        if (action == null && addressing == null) {
            return;
        }
        WebMethod wm = method.getAnnotation(WebMethod.class);
        String inputAction = "";
        if (action != null) {
            inputAction = action.input();
        }
        if (wm != null && StringUtils.isEmpty(inputAction)) {
            inputAction = wm.action();
        }
        if (StringUtils.isEmpty(inputAction)) {
            inputAction = computeAction(operation, "Request");
        }
        if (action == null && addressing != null) {
            operation.getInput().addExtensionAttribute(JAXWSAConstants.WSAM_ACTION_QNAME,
                                                       inputAction);
            operation.getInput().addExtensionAttribute(JAXWSAConstants.WSAW_ACTION_QNAME,
                                                       inputAction);
            if (operation.getOutput() != null) {
                operation.getOutput().addExtensionAttribute(JAXWSAConstants.WSAM_ACTION_QNAME,
                                                            computeAction(operation, "Response"));
                operation.getOutput().addExtensionAttribute(JAXWSAConstants.WSAW_ACTION_QNAME,
                                                            computeAction(operation, "Response"));
            }

        } else {
            MessageInfo input = operation.getInput();
            input.addExtensionAttribute(JAXWSAConstants.WSAM_ACTION_QNAME, inputAction);
            if (!StringUtils.isEmpty(action.input())) {
                input.addExtensionAttribute(JAXWSAConstants.WSAW_ACTION_QNAME, inputAction);
            }

            MessageInfo output = operation.getOutput();
            if (output != null && !StringUtils.isEmpty(action.output())) {
                output.addExtensionAttribute(JAXWSAConstants.WSAW_ACTION_QNAME, action.output());
                output.addExtensionAttribute(JAXWSAConstants.WSAM_ACTION_QNAME, action.output());
            } else if (output != null) {
                output.addExtensionAttribute(JAXWSAConstants.WSAM_ACTION_QNAME, computeAction(operation,
                                                                                              "Response"));
            }

            FaultAction[] faultActions = action.fault();
            if (faultActions != null && faultActions.length > 0 && operation.getFaults() != null) {
                for (FaultAction faultAction : faultActions) {
                    FaultInfo faultInfo = getFaultInfo(operation, faultAction.className());
                    if (faultInfo != null && !StringUtils.isEmpty(faultAction.value())) {
                        faultInfo.addExtensionAttribute(JAXWSAConstants.WSAW_ACTION_QNAME, faultAction
                            .value());
                        faultInfo.addExtensionAttribute(JAXWSAConstants.WSAM_ACTION_QNAME, faultAction
                            .value());
                    }
                    if (operation.isUnwrappedCapable()) {
                        faultInfo = getFaultInfo(operation.getUnwrappedOperation(), faultAction.className());
                        if (faultInfo != null && !StringUtils.isEmpty(faultAction.value())) {
                            faultInfo.addExtensionAttribute(JAXWSAConstants.WSAW_ACTION_QNAME, faultAction
                                .value());
                            faultInfo.addExtensionAttribute(JAXWSAConstants.WSAM_ACTION_QNAME, faultAction
                                .value());
                        }
                    }
                }
            }
        }
        for (FaultInfo fi : operation.getFaults()) {
            if (fi.getExtensionAttribute(JAXWSAConstants.WSAM_ACTION_QNAME) == null) {
                String f = "/Fault/" + fi.getName().getLocalPart();
                fi.addExtensionAttribute(JAXWSAConstants.WSAM_ACTION_QNAME,
                                         computeAction(operation, f));
                if (operation.isUnwrappedCapable()) {
                    fi = operation.getUnwrappedOperation().getFault(fi.getName());
                    fi.addExtensionAttribute(JAXWSAConstants.WSAM_ACTION_QNAME,
                                             computeAction(operation, f));
                }
            }
        }
    }

    private String computeAction(OperationInfo op, String postFix) {
        StringBuilder s = new StringBuilder(op.getName().getNamespaceURI());
        if (s.charAt(s.length() - 1) != '/') {
            s.append('/');
        }
        s.append(op.getInterface().getName().getLocalPart())
            .append('/').append(op.getName().getLocalPart()).append(postFix);
        return s.toString();
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
        Set<Class<?>> classes = new HashSet<>();
        wrapperClasses = generatedWrapperBeanClass();
        if (wrapperClasses != null) {
            classes.addAll(wrapperClasses);
        }

        XmlSeeAlso xmlSeeAlsoAnno = getServiceClass().getAnnotation(XmlSeeAlso.class);

        if (xmlSeeAlsoAnno != null && xmlSeeAlsoAnno.value() != null) {
            for (int i = 0; i < xmlSeeAlsoAnno.value().length; i++) {
                Class<?> value = xmlSeeAlsoAnno.value()[i];
                if (value == null) {
                    LOG.log(Level.WARNING, "XMLSEEALSO_NULL_CLASS",
                            new Object[] {getServiceClass().getName(), i});
                } else {
                    classes.add(value);
                }

            }
        }
        return classes;
    }

    private Set<Class<?>> generatedWrapperBeanClass() {
        DataBinding b = getDataBinding();
        if (b.getClass().getName().endsWith("JAXBDataBinding")
            && schemaLocations == null) {
            ServiceInfo serviceInfo = getService().getServiceInfos().get(0);
            WrapperClassCreator wrapperGen = getBus().getExtension(WrapperClassCreator.class);
            return wrapperGen.generate(this,
                    serviceInfo.getInterface(),
                    getQualifyWrapperSchema());
        }
        return Collections.emptySet();
    }

    private void setMTOMFeatures(DataBinding databinding) {
        if (this.wsFeatures != null) {
            for (WebServiceFeature wsf : this.wsFeatures) {
                if (wsf instanceof MTOMFeature) {
                    databinding.setMtomEnabled(true);
                    MTOMFeature f = (MTOMFeature) wsf;
                    if (f.getThreshold() > 0) {
                        databinding.setMtomThreshold(((MTOMFeature)wsf).getThreshold());
                    }
                }
            }
        }
    }

    @Override
    protected void buildServiceFromClass() {
        super.buildServiceFromClass();
        getService().put(WS_FEATURES, getWsFeatures());
    }

    protected void initializeParameter(MessagePartInfo part, Class<?> rawClass, Type type) {
        if (implInfo.isWebServiceProvider()) {
            part.setTypeQName(Constants.XSD_ANYTYPE);
            part.setTypeClass(rawClass);
            return;
        }
        super.initializeParameter(part, rawClass, type);
    }

    @Override
    protected Invoker createInvoker() {
        Class<?> cls = getServiceClass();
        if (cls.isInterface()) {
            return null;
        }
        return new JAXWSMethodInvoker(new SingletonFactory(getServiceClass()));
    }
}
