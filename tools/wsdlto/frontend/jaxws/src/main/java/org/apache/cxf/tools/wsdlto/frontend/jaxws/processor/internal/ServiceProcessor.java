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

package org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.http.HTTPBinding;
import javax.wsdl.extensions.mime.MIMEContent;
import javax.wsdl.extensions.mime.MIMEMultipartRelated;
import javax.wsdl.extensions.mime.MIMEPart;
import javax.wsdl.extensions.soap.SOAPHeader;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.common.extensions.soap.SoapBinding;
import org.apache.cxf.tools.common.extensions.soap.SoapBody;
import org.apache.cxf.tools.common.extensions.soap.SoapHeader;
import org.apache.cxf.tools.common.extensions.soap.SoapOperation;
import org.apache.cxf.tools.common.model.JAnnotation;
import org.apache.cxf.tools.common.model.JAnnotationElement;
import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.common.model.JavaMethod;
import org.apache.cxf.tools.common.model.JavaModel;
import org.apache.cxf.tools.common.model.JavaParameter;
import org.apache.cxf.tools.common.model.JavaPort;
import org.apache.cxf.tools.common.model.JavaServiceClass;
import org.apache.cxf.tools.common.model.JavaType;
import org.apache.cxf.tools.util.ClassCollector;
import org.apache.cxf.tools.util.NameUtil;
import org.apache.cxf.tools.util.SOAPBindingUtil;
import org.apache.cxf.tools.wsdlto.frontend.jaxws.customization.JAXWSBinding;

public class ServiceProcessor extends AbstractProcessor {

    private String soapOPAction = "SOAPACTION";

    private String soapOPStyle = "STYLE";

    private BindingType bindingType;

    private final int inHEADER = 1;

    private final int outHEADER = 2;

    private final int resultHeader = 3;

    private final int noHEADER = 0;

    private Object bindingObj;
    private ServiceInfo service;

    private JAXWSBinding jaxwsBinding = new JAXWSBinding();

    public ServiceProcessor(ToolContext penv) {
        super(penv);
    }

    public void process(ServiceInfo si) throws ToolException {
        jaxwsBinding = new JAXWSBinding();
        this.service = si;
        if (si.getName() == null) {
            processBindings(context.get(JavaModel.class));
        } else {
            processService(context.get(JavaModel.class));
        }

    }
    private String mapName(String packageName, String name) {
        while (isNameCollision(packageName, name)) {
            name = name + "_Service";
        }
        ClassCollector collector = context.get(ClassCollector.class);
        if (collector.isReserved(packageName, name)) {
            int count = 0;
            String checkName = name;
            while (collector.isReserved(packageName, checkName)) {
                checkName = name + (++count);
            }
            name = checkName;
        }
        return name;
    }

    private boolean isNameCollision(String packageName, String className) {
        if (context.optionSet(ToolConstants.CFG_GEN_OVERWRITE)) {
            return false;
        }
        ClassCollector collector = context.get(ClassCollector.class);
        return collector.containTypesClass(packageName, className)
               || collector.containSeiClass(packageName, className)
               || collector.containExceptionClass(packageName, className);
    }

    private void processService(JavaModel model) throws ToolException {
        JavaServiceClass sclz = (JavaServiceClass)service.getProperty("JavaServiceClass");
        if (sclz != null) {
            return;
        }
        
        for (JavaServiceClass sc : model.getServiceClasses().values()) {
            if (sc.getServiceName().equals(service.getName().getLocalPart())) {
                sclz = sc;
            }
        }
        if (sclz == null) {
            sclz = new JavaServiceClass(model);
            service.setProperty("JavaServiceClass", sclz);
            String name = NameUtil.mangleNameToClassName(service.getName().getLocalPart());
            String namespace = service.getName().getNamespaceURI();
            String packageName = ProcessorUtil.parsePackageName(namespace, context.mapPackageName(namespace));
    
    
            //customizing
            JAXWSBinding serviceBinding = null;
            if (service.getDescription() != null) {
                serviceBinding = service.getDescription().getExtensor(JAXWSBinding.class);
            }
            JAXWSBinding serviceBinding2 = service.getExtensor(JAXWSBinding.class);
    
            //TODO : Handle service customized class
            if (serviceBinding != null) {
                if (serviceBinding.getPackage() != null) {
                    jaxwsBinding.setPackage(serviceBinding.getPackage());
                }
    
                if (serviceBinding.isEnableAsyncMapping()) {
                    jaxwsBinding.setEnableAsyncMapping(true);
                }
    
                if (serviceBinding.isEnableMime()) {
                    jaxwsBinding.setEnableMime(true);
                }
                jaxwsBinding.setEnableWrapperStyle(serviceBinding.isEnableWrapperStyle());
    
                if (serviceBinding.getJaxwsClass() != null
                    && serviceBinding.getJaxwsClass().getClassName() != null) {
                    name = serviceBinding.getJaxwsClass().getClassName();  
                    if (name.contains(".")) {
                        jaxwsBinding.setPackage(name.substring(0, name.lastIndexOf('.')));
                        name = name.substring(name.lastIndexOf('.') + 1);
                    }
                    
                    sclz.setClassJavaDoc(serviceBinding.getJaxwsClass().getComments());          
                }
                sclz.setPackageJavaDoc(serviceBinding.getPackageJavaDoc());
            }
            if (serviceBinding2 != null) {
                if (serviceBinding2.getPackage() != null) {
                    jaxwsBinding.setPackage(serviceBinding2.getPackage());
                }
    
                if (serviceBinding2.isEnableAsyncMapping()) {
                    jaxwsBinding.setEnableAsyncMapping(true);
                }
    
                if (serviceBinding2.isEnableMime()) {
                    jaxwsBinding.setEnableMime(true);
                }
    
                if (serviceBinding2.isEnableWrapperStyle()) {
                    jaxwsBinding.setEnableWrapperStyle(true);
                }
                if (serviceBinding2.getJaxwsClass() != null
                    && serviceBinding2.getJaxwsClass().getClassName() != null) {
                    name = serviceBinding2.getJaxwsClass().getClassName();                
                    if (name.contains(".")) {
                        jaxwsBinding.setPackage(name.substring(0, name.lastIndexOf('.')));
                        name = name.substring(name.lastIndexOf('.') + 1);
                    }
                }
                if (serviceBinding2.getJaxwsClass().getComments() != null) {
                    jaxwsBinding.setClassJavaDoc(serviceBinding2.getJaxwsClass().getComments());
                }
                if (!serviceBinding2.getPackageJavaDoc().equals("")) {
                    sclz.setPackageJavaDoc(serviceBinding2.getPackageJavaDoc());
                }
            }
            
            sclz.setServiceName(service.getName().getLocalPart());
            sclz.setNamespace(namespace);
    
            if (jaxwsBinding.getPackage() != null) {
                packageName = jaxwsBinding.getPackage();
            }
            sclz.setPackageName(packageName);
            name = mapName(packageName, name);
            sclz.setName(name);
            ClassCollector collector = context.get(ClassCollector.class);
            String checkName = name;
            int count = 0;
            while (collector.containServiceClass(packageName, checkName)) {
                checkName = name + (++count);
            }        
            name = checkName;
            sclz.setName(name);
            collector.addServiceClassName(packageName, name, packageName + "." + name);
    
            Element handler = (Element)context.get(ToolConstants.HANDLER_CHAIN);
            sclz.setHandlerChains(handler);
        }

        Collection<EndpointInfo> ports = service.getEndpoints();

        for (EndpointInfo port : ports) {
            JavaPort javaport = processPort(model, service, port);
            sclz.addPort(javaport);
        }
        
        sclz.setClassJavaDoc(jaxwsBinding.getClassJavaDoc());
        model.addServiceClass(sclz.getName(), sclz);
    }

    private JavaPort processPort(JavaModel model, ServiceInfo si, EndpointInfo port) throws ToolException {
        BindingInfo binding = port.getBinding();
        String portType = binding.getInterface().getName().getLocalPart();
        JavaInterface intf = PortTypeProcessor.getInterface(context, si, binding.getInterface());
        JavaPort jport = new JavaPort(NameUtil.mangleNameToClassName(port.getName().getLocalPart()));
        jport.setPackageName(intf.getPackageName());
        
        jport.setPortName(port.getName().getLocalPart());
        jport.setBindingAdress(port.getAddress());
        jport.setBindingName(binding.getName().getLocalPart());


        jport.setPortType(portType);


        jport.setInterfaceClass(intf.getName());
        bindingType = getBindingType(binding);

        if (bindingType == null) {
            org.apache.cxf.common.i18n.Message msg =
                new org.apache.cxf.common.i18n.Message("BINDING_SPECIFY_ONE_PROTOCOL",
                                                       LOG,
                                                       binding.getName());
            throw new ToolException(msg);
        }

        if (isSoapBinding()) {
            SoapBinding spbd = SOAPBindingUtil.getProxy(SoapBinding.class, this.bindingObj);
            jport.setStyle(SOAPBindingUtil.getSoapStyle(spbd.getStyle()));
            jport.setTransURI(spbd.getTransportURI());
        }

        Collection<BindingOperationInfo> operations = binding.getOperations();
        for (BindingOperationInfo bop : operations) {
            processOperation(model, bop, binding);
        }
        return jport;
    }
    private void processBindings(JavaModel model) {
        for (BindingInfo binding : service.getBindings()) {
            bindingType = getBindingType(binding);

            if (bindingType == null) {
                org.apache.cxf.common.i18n.Message msg =
                    new org.apache.cxf.common.i18n.Message("BINDING_SPECIFY_ONE_PROTOCOL",
                                                           LOG,
                                                           binding.getName());
                throw new ToolException(msg);
            }

            Collection<BindingOperationInfo> operations = binding.getOperations();
            for (BindingOperationInfo bop : operations) {
                processOperation(model, bop, binding);
            }
        }
    }


    private void processOperation(JavaModel model, BindingOperationInfo bop, BindingInfo binding)
        throws ToolException {
        boolean enableOpMime = false;
        JAXWSBinding bind = binding.getExtensor(JAXWSBinding.class);

        if (bind != null && bind.isEnableMime()) {
            enableOpMime = true;
        }

        JAXWSBinding bopBinding = bop.getExtensor(JAXWSBinding.class);

        if (bopBinding != null && bopBinding.isEnableMime()) {
            enableOpMime = true;
            if (bopBinding.getJaxwsParas() != null) {
                jaxwsBinding.setJaxwsParas(bopBinding.getJaxwsParas());
            }
        }
        JavaInterface jf = null;
        for (JavaInterface jf2 : model.getInterfaces().values()) {
            if (binding.getInterface().getName().getLocalPart()
                    .equals(jf2.getWebServiceName())) {
                jf = jf2;
            }
        }
        if (isSoapBinding()) {
            SoapBinding soapBinding = (SoapBinding)bindingObj;
            if (SOAPBindingUtil.getSoapStyle(soapBinding.getStyle()) == null) {
                jf.setSOAPStyle(javax.jws.soap.SOAPBinding.Style.DOCUMENT);
            } else {
                jf.setSOAPStyle(SOAPBindingUtil.getSoapStyle(soapBinding.getStyle()));
            }
        } else {
            // REVISIT: fix for xml binding
            jf.setSOAPStyle(javax.jws.soap.SOAPBinding.Style.DOCUMENT);
        }

        Object[] methods = jf.getMethods().toArray();
        for (int i = 0; i < methods.length; i++) {
            JavaMethod jm = (JavaMethod)methods[i];
            if (jm.getOperationName() != null && jm.getOperationName().equals(bop.getName().getLocalPart())) {
                if (isSoapBinding()) {
                    // TODO: add customize here
                    //doCustomizeOperation(jf, jm, bop);
                    Map prop = getSoapOperationProp(bop);
                    String soapAction = prop.get(soapOPAction) == null ? "" : (String)prop.get(soapOPAction);
                    String soapStyle = prop.get(soapOPStyle) == null ? "" : (String)prop.get(soapOPStyle);
                    jm.setSoapAction(soapAction);

                    if (SOAPBindingUtil.getSoapStyle(soapStyle) == null && this.bindingObj == null) {
                        org.apache.cxf.common.i18n.Message msg =
                            new  org.apache.cxf.common.i18n.Message("BINDING_STYLE_NOT_DEFINED",
                                                                         LOG);
                        throw new ToolException(msg);
                    }
                    if (SOAPBindingUtil.getSoapStyle(soapStyle) == null) {
                        jm.setSoapStyle(jf.getSOAPStyle());
                    } else {
                        jm.setSoapStyle(SOAPBindingUtil.getSoapStyle(soapStyle));
                    }
                } else {
                    // REVISIT: fix for xml binding
                    jm.setSoapStyle(jf.getSOAPStyle());
                }

                if (jm.getSoapStyle().equals(javax.jws.soap.SOAPBinding.Style.RPC)) {
                    jm.getAnnotationMap().remove("SOAPBinding");
                }

                OperationProcessor processor = new OperationProcessor(context);

                int headerType = isNonWrappable(bop);

                OperationInfo opinfo = bop.getOperationInfo();

                JAXWSBinding opBinding = (JAXWSBinding)opinfo.getExtensor(JAXWSBinding.class);
                JAXWSBinding infBinding = (JAXWSBinding)opinfo.getInterface().getExtensor(JAXWSBinding.class);
                boolean enableMime = enableOpMime;
                boolean enableWrapperStyle = true;
                
                if (infBinding != null && infBinding.isSetEnableWrapperStyle()) {
                    enableWrapperStyle = infBinding.isEnableWrapperStyle();
                }
                if (infBinding != null && infBinding.isSetEnableMime()) {
                    enableMime = infBinding.isEnableMime();
                }
                if (opBinding != null && opBinding.isSetEnableWrapperStyle()) {
                    enableWrapperStyle = opBinding.isEnableWrapperStyle();
                }
                if (opBinding != null && opBinding.isSetEnableMime()) {
                    enableMime = opBinding.isEnableMime();
                }
                if (jaxwsBinding.isEnableMime() || enableMime) {
                    jm.setMimeEnable(true);
                }
                if ((jm.isWrapperStyle() && headerType > this.noHEADER)
                    || !jaxwsBinding.isEnableWrapperStyle()
                    || (jm.enableMime() && jm.isWrapperStyle())
                    || !enableWrapperStyle) {
                    // changed wrapper style

                    jm.setWrapperStyle(false);
                    processor.processMethod(jm, bop.getOperationInfo());
                    jm.getAnnotationMap().remove("ResponseWrapper");
                    jm.getAnnotationMap().remove("RequestWrapper");

                } else {
                    processor.processMethod(jm, bop.getOperationInfo());
                }

                if (headerType == this.resultHeader) {
                    JAnnotation resultAnno = jm.getAnnotationMap().get("WebResult");
                    if (resultAnno != null) {
                        resultAnno.addElement(new JAnnotationElement("header", true, true));
                    }
                }
                processParameter(jm, bop);
            }
        }
    }

    private void setParameterAsHeader(JavaParameter parameter) {
        parameter.setHeader(true);
        JAnnotation parameterAnnotation = parameter.getAnnotation("WebParam");
        parameterAnnotation.addElement(new JAnnotationElement("header", true, true));
        parameterAnnotation.addElement(new JAnnotationElement("name", 
                                                                     parameter.getQName().getLocalPart()));
        parameterAnnotation.addElement(new JAnnotationElement("partName", parameter.getPartName()));
        parameterAnnotation.addElement(new JAnnotationElement("targetNamespace", 
                                                                     parameter.getTargetNamespace()));
    }

    private void processParameter(JavaMethod jm, BindingOperationInfo operation) throws ToolException {

        // process input

        List<ExtensibilityElement> inbindings = null;
        if (operation.getInput() != null) {
            inbindings = operation.getInput().getExtensors(ExtensibilityElement.class);
            if (inbindings == null) {
                inbindings = new ArrayList<ExtensibilityElement>();
            }
        }
        String use = null;
        for (ExtensibilityElement ext : inbindings) {
            if (SOAPBindingUtil.isSOAPBody(ext)) {
                SoapBody soapBody = SOAPBindingUtil.getSoapBody(ext);
                use = soapBody.getUse();
            } else if (SOAPBindingUtil.isSOAPHeader(ext)) {
                processSoapHeader(jm, operation, ext);
            }
            if (ext instanceof MIMEMultipartRelated && jm.enableMime()) {
                processMultipart(jm, operation, (MIMEMultipartRelated)ext, JavaType.Style.IN);
            }
        }

        // process output
        if (operation.getOutput() != null) {
            List<ExtensibilityElement> outbindings =
                operation.getOutput().getExtensors(ExtensibilityElement.class);
            if (outbindings == null) {
                outbindings = new ArrayList<ExtensibilityElement>();
            }
            for (ExtensibilityElement ext : outbindings) {
                if (SOAPBindingUtil.isSOAPHeader(ext)) {
                    SoapHeader soapHeader = SOAPBindingUtil.getSoapHeader(ext);
                    boolean found = false;
                    for (JavaParameter parameter : jm.getParameters()) {
                        if (soapHeader.getPart().equals(parameter.getPartName())) {
                            setParameterAsHeader(parameter);
                            found = true;
                        }
                    }
                    if (jm.getReturn().getName() != null 
                        && jm.getReturn().getName().equals(soapHeader.getPart())) {
                        found = true;
                    }
                    if (Boolean.valueOf((String)context.get(ToolConstants.CFG_EXTRA_SOAPHEADER))
                        && !found) {
                        // Header can't be found in java method parameters, in
                        // different message
                        // other than messages used in porttype operation
                        ParameterProcessor processor = new ParameterProcessor(context);
                        MessagePartInfo exPart = service.getMessage(soapHeader.getMessage())
                            .getMessagePart(new QName(soapHeader.getMessage().getNamespaceURI(),
                                                      soapHeader.getPart()));
                        JavaParameter jp = processor.addParameterFromBinding(jm, exPart, JavaType.Style.OUT);
                        setParameterAsHeader(jp);
                    }
                }
                if (ext instanceof MIMEMultipartRelated && jm.enableMime()) {
                    processMultipart(jm, operation, (MIMEMultipartRelated)ext, JavaType.Style.OUT);
                }
            }
        }

        jm.setSoapUse(SOAPBindingUtil.getSoapUse(use));
        if (javax.jws.soap.SOAPBinding.Style.RPC == jm.getSoapStyle()
            && javax.jws.soap.SOAPBinding.Use.ENCODED == jm.getSoapUse()) {
            System.err.println("** Unsupported RPC-Encoded Style Use **");
        }
        if (javax.jws.soap.SOAPBinding.Style.RPC == jm.getSoapStyle()
            && javax.jws.soap.SOAPBinding.Use.LITERAL == jm.getSoapUse()) {
            return;
        }
        if (javax.jws.soap.SOAPBinding.Style.DOCUMENT == jm.getSoapStyle()
            && javax.jws.soap.SOAPBinding.Use.LITERAL == jm.getSoapUse()) {
            return;
        }
    }

    private void processSoapHeader(JavaMethod jm, BindingOperationInfo operation, ExtensibilityElement ext) {
        SoapHeader soapHeader = SOAPBindingUtil.getSoapHeader(ext);
        for (JavaParameter parameter : jm.getParameters()) {
            if (soapHeader.getPart().equals(parameter.getPartName())) {
                setParameterAsHeader(parameter);
                break;
            }
        }
    }

    private static String getJavaTypeForMimeType(MIMEPart mPart) {
        if (mPart.getExtensibilityElements().size() > 1) {
            return "javax.activation.DataHandler";
        } else {
            ExtensibilityElement extElement = (ExtensibilityElement)mPart.getExtensibilityElements().get(0);
            if (extElement instanceof MIMEContent) {
                MIMEContent mimeContent = (MIMEContent)extElement;
                if ("image/jpeg".equals(mimeContent.getType()) || "image/gif".equals(mimeContent.getType())) {
                    return "java.awt.Image";
                } else if ("text/xml".equals(mimeContent.getType())
                           || "application/xml".equals(mimeContent.getType())) {
                    return "javax.xml.transform.Source";
                }  else {
                    return "javax.activation.DataHandler";
                }
            }
        }
        return "javax.activation.DataHandler";
    }

    public void processMultipart(JavaMethod jm, BindingOperationInfo operation,
                                 MIMEMultipartRelated ext, JavaType.Style style) throws ToolException {
        List mimeParts = ext.getMIMEParts();
        Iterator itParts = mimeParts.iterator();
        while (itParts.hasNext()) {
            MIMEPart mPart = (MIMEPart)itParts.next();
            Iterator extns = mPart.getExtensibilityElements().iterator();
            while (extns.hasNext()) {
                ExtensibilityElement extElement = (ExtensibilityElement)extns.next();
                if (extElement instanceof MIMEContent) {
                    MIMEContent mimeContent = (MIMEContent)extElement;
                    String mimeJavaType = getJavaTypeForMimeType(mPart);
                    if (JavaType.Style.IN.equals(style)) {
                        String paramName = ProcessorUtil.mangleNameToVariableName(mimeContent.getPart());
                        JavaParameter jp = jm.getParameter(paramName);
                        if (jp == null) {
                            Message message = new Message("MIMEPART_CANNOT_MAP", LOG, mimeContent.getPart());
                            throw new ToolException(message);
                        }
                        if (!jp.getClassName().equals(mimeJavaType)) {
                            // jp.setType(mimeJavaType);
                            jp.setClassName(mimeJavaType);
                        }
                    } else if (JavaType.Style.OUT.equals(style)) {
                        JavaType jp = null;
                        if (!"void".equals(jm.getReturn().getType())
                            && mimeContent.getPart().equals(jm.getReturn().getName())) {
                            jp = jm.getReturn();
                            jp.setClassName(mimeJavaType);
                        }



                        if (jp == null) {
                            for (JavaParameter para : jm.getParameters()) {
                                if (mimeContent.getPart().equals(para.getPartName())) {
                                    jp = para;
                                }
                            }
                            if (jp != null) {
                                ((JavaParameter)jp).setClassName(mimeJavaType);
                            }

                        }


                        if (jp == null) {
                            Message message = new Message("MIMEPART_CANNOT_MAP", LOG, mimeContent
                                .getPart());
                            throw new ToolException(message);
                        }
                    }
                } else if (extElement instanceof SOAPHeader) {
                    processSoapHeader(jm, operation, extElement);
                }
            }
        }
    }

    private Map getSoapOperationProp(BindingOperationInfo bop) {
        Map<String, Object> soapOPProp = new HashMap<String, Object>();
        if (bop.getExtensor(ExtensibilityElement.class) != null) {
            for (ExtensibilityElement ext : bop.getExtensors(ExtensibilityElement.class)) {
                if (SOAPBindingUtil.isSOAPOperation(ext)) {
                    SoapOperation soapOP = SOAPBindingUtil.getSoapOperation(ext);
                    soapOPProp.put(this.soapOPAction, soapOP.getSoapActionURI());
                    soapOPProp.put(this.soapOPStyle, soapOP.getStyle());
                }
            }
        } else {
            for (ExtensibilityElement ext :  bop.getBinding().getExtensors(ExtensibilityElement.class)) {
                if (SOAPBindingUtil.isSOAPOperation(ext)) {
                    SoapOperation soapOP = SOAPBindingUtil.getSoapOperation(ext);
                    soapOPProp.put(this.soapOPAction, soapOP.getSoapActionURI());
                    soapOPProp.put(this.soapOPStyle, soapOP.getStyle());
                }
            }


        }
        return soapOPProp;
    }

    private BindingType getBindingType(BindingInfo binding) {
        for (ExtensibilityElement ext : binding.getExtensors(ExtensibilityElement.class)) {
            if (SOAPBindingUtil.isSOAPBinding(ext)) {
                bindingObj = SOAPBindingUtil.getSoapBinding(ext);
                return BindingType.SOAPBinding;
            }
            if (ext instanceof HTTPBinding) {
                bindingObj = (HTTPBinding)ext;
                return BindingType.HTTPBinding;
            }
        }
        return BindingType.XMLBinding;
    }

    private int isNonWrappable(BindingOperationInfo bop) {
        QName operationName = bop.getName();
        MessageInfo bodyMessage = null;
        QName headerMessage = null;
        SoapHeader header = null;
        boolean containParts = false;
        boolean isSameMessage = false;
        boolean isNonWrappable = false;
        boolean allPartsHeader = false;
        int result = this.noHEADER;

        // begin process input
        if (bop.getInput() != null
            && bop.getInput().getExtensors(ExtensibilityElement.class) != null) {
            List<ExtensibilityElement> extensors = bop.getInput().getExtensors(ExtensibilityElement.class);
            if (extensors != null) {
                for (ExtensibilityElement ext : extensors) {
                    if (SOAPBindingUtil.isSOAPBody(ext)) {
                        bodyMessage = getMessage(operationName, true);
                    }
                    if (SOAPBindingUtil.isSOAPHeader(ext)) {
                        header = SOAPBindingUtil.getSoapHeader(ext);
                        headerMessage = header.getMessage();
                        if (header.getPart().length() > 0) {
                            containParts = true;
                        }
                    }
                }
            }

            if (headerMessage != null && bodyMessage != null
                && headerMessage.getNamespaceURI().equalsIgnoreCase(bodyMessage.getName().getNamespaceURI())
                && headerMessage.getLocalPart().equalsIgnoreCase(bodyMessage.getName().getLocalPart())) {
                isSameMessage = true;
            }

            isNonWrappable = isSameMessage && containParts;
            // if is nonwrapple then return
            if (isNonWrappable) {
                result = this.inHEADER;
            }
        }
        isSameMessage = false;
        containParts = false;

        // process output
        if (bop.getOutput() != null && bop.getOutput().getExtensors(ExtensibilityElement.class) != null) {
            List<ExtensibilityElement> extensors = bop.getOutput().getExtensors(ExtensibilityElement.class);
            if (extensors != null) {
                for (ExtensibilityElement ext : extensors) {
                    if (SOAPBindingUtil.isSOAPBody(ext)) {
                        bodyMessage = getMessage(operationName, false);
                    }
                    if (SOAPBindingUtil.isSOAPHeader(ext)) {
                        header = SOAPBindingUtil.getSoapHeader(ext);
                        headerMessage = header.getMessage();
                        if (header.getPart().length() > 0) {
                            containParts = true;
                        }
                    }
                }
            }
            if (headerMessage != null && bodyMessage != null
                && headerMessage.getNamespaceURI().equalsIgnoreCase(bodyMessage.getName().getNamespaceURI())
                && headerMessage.getLocalPart().equalsIgnoreCase(bodyMessage.getName().getLocalPart())) {
                isSameMessage = true;
                if (bodyMessage.getMessageParts().size() == 1) {
                    allPartsHeader = true;
                }

            }
            isNonWrappable = isSameMessage && containParts;
            if (isNonWrappable && allPartsHeader) {
                result = this.resultHeader;
            }
            if (isNonWrappable && !allPartsHeader) {
                result = this.outHEADER;
            }
        }

        return result;
    }

    private MessageInfo getMessage(QName operationName, boolean isIn) {
        for (OperationInfo operation : service.getInterface().getOperations()) {
            if (operationName.equals(operation.getName()) && isIn) {
                return operation.getInput();
            } else {
                return operation.getOutput();
            }
        }
        return null;
    }

    public enum BindingType {
        HTTPBinding, SOAPBinding, XMLBinding
    }

    private boolean isSoapBinding() {
        return bindingType != null && "SOAPBinding".equals(bindingType.name());

    }
}
