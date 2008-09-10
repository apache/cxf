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

package org.apache.cxf.binding.corba.interceptors;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.WebFault;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.binding.corba.CorbaBindingException;
import org.apache.cxf.binding.corba.CorbaDestination;
import org.apache.cxf.binding.corba.CorbaMessage;
import org.apache.cxf.binding.corba.CorbaStreamable;
import org.apache.cxf.binding.corba.runtime.CorbaFaultStreamWriter;
import org.apache.cxf.binding.corba.types.CorbaObjectHandler;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.OperationType;
import org.apache.cxf.binding.corba.wsdl.RaisesType;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.service.model.ServiceModelUtil;
import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ServerRequest;
import org.omg.CORBA.SystemException;

public class CorbaStreamFaultOutInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOG = LogUtils.getL7dLogger(CorbaStreamFaultOutInterceptor.class);
    private ORB orb;

    public CorbaStreamFaultOutInterceptor() {
        super(Phase.MARSHAL);
    }

    public void handleMessage(Message msg) {
        CorbaMessage message = (CorbaMessage) msg;
        Exchange exchange = message.getExchange();
        CorbaDestination destination;
        Fault faultEx = null;
        if (message.getDestination() != null) {
            destination = (CorbaDestination)message.getDestination();
        } else {
            destination = (CorbaDestination)exchange.getDestination();
        }

        orb = (ORB) message.get(CorbaConstants.ORB);
        if (orb == null) {
            orb = (ORB) exchange.get(ORB.class); 
        }
        
        DataWriter<XMLStreamWriter> writer = getDataWriter(message);

        Throwable ex = message.getContent(Exception.class);
        // JCGS. If the cause is not available I can only continue if the exception 
        //       is a Fault instance and contains a detail object.
        if (ex.getCause() == null) {
            if ((ex instanceof Fault) && (((Fault)ex).getDetail() != null)) {
                faultEx = (Fault) ex;
            } else {
                throw new CorbaBindingException(ex);
            }
        } else {
            ex = ex.getCause();
        }
       
        if (ex instanceof InvocationTargetException) {
            ex = ex.getCause();
        }

        if (ex instanceof SystemException) {
            setSystemException(message, ex, destination);
            return;
        }
        
        String exClassName = null;
        if (faultEx == null) {
            //REVISIT, we should not have to depend on WebFault annotation
            //Try changing the fault name to the proper mangled java exception classname.
            WebFault fault = ex.getClass().getAnnotation(WebFault.class);
            if (fault == null) {
                throw new CorbaBindingException(ex);
            }
            exClassName = fault.name();
        }  else {
            //JCGS: exClassName to be set to the exception name
            Element faultElement = (Element) faultEx.getDetail().getFirstChild();
            exClassName = faultElement.getLocalName();
        }

        // Get information about the operation being invoked from the WSDL
        // definition.
        // We need this to marshal data correctly

        BindingInfo bInfo = destination.getBindingInfo();     
        
        String opName = message.getExchange().get(String.class);
                
        Iterator iter = bInfo.getOperations().iterator();

        BindingOperationInfo bopInfo = null;
        OperationType opType = null;           
        while (iter.hasNext()) {
            bopInfo = (BindingOperationInfo)iter.next();
            if (bopInfo.getName().getLocalPart().equals(opName)) {
                opType = bopInfo.getExtensor(OperationType.class);
                break;
            }
        }
        if (opType == null) {
            throw new CorbaBindingException("Unable to find binding operation for " + opName);
        }

        OperationInfo opInfo = bopInfo.getOperationInfo();
        
        if (faultEx != null) {
            MessagePartInfo partInfo = getFaultMessagePartInfo(opInfo, new QName("", exClassName));
            if (partInfo != null) {
                exClassName = partInfo.getTypeQName().getLocalPart();
            }
            
        }

        RaisesType exType = getRaisesType(opType, exClassName, ex);

        try {
            if (exType != null) {
                if (faultEx != null) {
                    setUserExceptionFromFaultDetail(message, 
                            faultEx.getDetail(), 
                            exType, opInfo, writer,
                            exchange.get(ServiceInfo.class));
                } else {
                    setUserException(message, ex, exType, opInfo, writer,
                                    exchange.get(ServiceInfo.class));
                }
            } else {
                throw new CorbaBindingException(ex);
            }
        } catch (Exception exp) {
            throw new CorbaBindingException(exp);
        }
    }

    protected RaisesType getRaisesType(OperationType opType, String exClassName, Throwable ex) {
        RaisesType result = null;
        List<RaisesType> exList = opType.getRaises();
        result = findRaisesType(exList, exClassName);

        if (result == null) {
            //REVISIT, need to find a better way to match the corba binding exception name with the wsdl one
            //if doc-literal, the part element name should be used, but for RPC, the message part name
            try {
                Method faultMethod = ex.getClass().getMethod("getFaultInfo");
                if (faultMethod != null) {
                    Class<?> faultClass = faultMethod.getReturnType();
                    XmlType exType = faultClass.getAnnotation(XmlType.class);
                    exClassName = exType.name();
                    result = findRaisesType(exList, exClassName);
                }
            } catch (Exception exp) {
                //Ignore it
            }
        }
        return result;
    }

    protected RaisesType findRaisesType(List<RaisesType> exList, String exClassName) {
        RaisesType result = null;
        for (Iterator<RaisesType> i = exList.iterator(); i.hasNext();) {
            RaisesType raises = i.next();
            if (raises.getException().getLocalPart().equals(exClassName)) {
                result = raises;
                break;
            }
        }
        return result;
    }

    protected void setSystemException(CorbaMessage message,
                                      Throwable ex,
                                      CorbaDestination dest) {
        SystemException sysEx = (SystemException)ex;
        message.setSystemException(sysEx);
        ServerRequest request  = message.getExchange().get(ServerRequest.class);
        Any exAny = dest.getOrbConfig().createSystemExceptionAny(orb, sysEx);
        request.set_exception(exAny);
    }

    protected void setUserException(CorbaMessage message,
                                    Throwable ex,
                                    RaisesType exType,
                                    OperationInfo opInfo,
                                    DataWriter<XMLStreamWriter> writer,
                                    ServiceInfo service)
        throws Exception {
        QName exIdlType = exType.getException();
        QName elName = new QName("", exIdlType.getLocalPart());
        MessagePartInfo faultPart = getFaultMessagePartInfo(opInfo, elName);
        if (faultPart == null) {
            throw new CorbaBindingException("Coulnd't find the message fault part : " + elName);
        }

        Method faultMethod = ex.getClass().getMethod("getFaultInfo");
        if (faultMethod == null) {
            return;
        }
        Object fault = faultMethod.invoke(ex);

        // This creates a default instance of the class representing the exception schema type if
        // one has not been created on the servant side which throws the UserException.
        if (fault == null) {
            Class faultClass = faultMethod.getReturnType();
            fault = faultClass.newInstance();
        }
        
        CorbaFaultStreamWriter faultWriter = new CorbaFaultStreamWriter(orb, exType, 
                message.getCorbaTypeMap(), service);
        writer.write(fault, faultPart, faultWriter);

        CorbaObjectHandler[] objs = faultWriter.getCorbaObjects();      
        CorbaStreamable streamable = message.createStreamableObject(objs[0], elName);
        message.setStreamableException(streamable);
    }

    protected void setUserExceptionFromFaultDetail(CorbaMessage message,
                                                   org.w3c.dom.Element faultDetail,
                                                   RaisesType exType,
                                                   OperationInfo opInfo,
                                                   DataWriter<XMLStreamWriter> writer,
                                                   ServiceInfo service)
        throws Exception {
        QName exIdlType = exType.getException();
        QName elName = new QName("", exIdlType.getLocalPart());
        MessagePartInfo faultPart = getFaultMessagePartInfo(opInfo, elName);
        
        // faultDetailt.getFirstChild() skips the "detail" element
        Object fault = extractPartsInfoFromDetail((Element) faultDetail.getFirstChild(), exType);

        
        CorbaFaultStreamWriter faultWriter = new CorbaFaultStreamWriter(orb, exType, 
                                                message.getCorbaTypeMap(), service);
        writer.write(fault, faultPart, faultWriter);

        CorbaObjectHandler[] objs = faultWriter.getCorbaObjects();      
        CorbaStreamable streamable = message.createStreamableObject(objs[0], elName);
        message.setStreamableException(streamable);
    }
    
    private Object extractPartsInfoFromDetail(Element faultDetail, RaisesType exType) {
        Document faultDoc = DOMUtils.createDocument();
        Element faultElement = faultDoc.createElement(exType.getException().getLocalPart());
        faultDoc.appendChild(faultElement);
        
        Node node = faultDetail.getFirstChild();
        while (node != null) {
            Node importedFaultData = faultDoc.importNode(node, true);
            faultElement.appendChild(importedFaultData);
            node = node.getNextSibling();     
        }
        
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Exception DOM: " + XMLUtils.toString(faultElement));
        }
        return faultDoc;
    }
    
    protected DataWriter<XMLStreamWriter> getDataWriter(CorbaMessage message) {
        Service serviceModel = ServiceModelUtil.getService(message.getExchange());

        DataWriter<XMLStreamWriter> dataWriter = 
            serviceModel.getDataBinding().createWriter(XMLStreamWriter.class);
        if (dataWriter == null) {
            throw new CorbaBindingException("Couldn't create data writer for outgoing fault message");
        }
        return dataWriter;
    }

    protected MessagePartInfo getFaultMessagePartInfo(OperationInfo opInfo, QName faultName) {
        Iterator<FaultInfo> faults = opInfo.getFaults().iterator();
        while (faults.hasNext()) {
            FaultInfo fault = faults.next();
            MessagePartInfo partInfo = fault.getMessageParts().get(0);
            if (partInfo.isElement()
                && partInfo.getElementQName().getLocalPart().equals(faultName.getLocalPart())) {
                return partInfo;
            } else if (partInfo.getTypeQName().getLocalPart().equals(faultName.getLocalPart())) {
                return partInfo;
            }
        }
        return null;
    }
    

}
