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

import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.binding.corba.CorbaBindingException;
import org.apache.cxf.binding.corba.CorbaMessage;
import org.apache.cxf.binding.corba.CorbaStreamable;
import org.apache.cxf.binding.corba.runtime.CorbaStreamReader;
import org.apache.cxf.binding.corba.types.CorbaHandlerUtils;
import org.apache.cxf.binding.corba.types.CorbaTypeEventProducer;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.ClientFaultConverter;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.service.model.ServiceModelUtil;
import org.omg.CORBA.SystemException;

public class CorbaStreamFaultInInterceptor extends AbstractPhaseInterceptor<Message> {
        
    private static final Logger LOG = LogUtils.getL7dLogger(CorbaStreamFaultInInterceptor.class);  
    

    public CorbaStreamFaultInInterceptor() {
        super(Phase.UNMARSHAL);
        addAfter(ClientFaultConverter.class.getName());
    }    

    public void handleMessage(Message msg) {
        CorbaMessage message = (CorbaMessage)msg;
        
        
        try {
            
            SystemException sysEx = message.getSystemException();
            if (sysEx != null) {
                // TODO: Do we need anything else to handle system exceptions here...i.e. do
                // we want to add a wrapper around this so that we can add some more information?
                message.setContent(Exception.class, sysEx);
                return;
            }

            CorbaStreamable exStreamable = message.getStreamableException();
            if (exStreamable != null) {
                DataReader<XMLStreamReader> reader = getDataReader(message);

                BindingOperationInfo bopInfo = message.getExchange().get(BindingOperationInfo.class);
                OperationInfo opInfo = bopInfo.getOperationInfo();
                
                ServiceInfo service = message.getExchange().get(ServiceInfo.class);
                
                org.omg.CORBA.ORB orb = (org.omg.CORBA.ORB) message.get(CorbaConstants.ORB);
                if (orb == null) {
                    orb = (org.omg.CORBA.ORB) message.getExchange().get(org.omg.CORBA.ORB.class); 
                }
                QName elName = new QName("", exStreamable.getName());
                FaultInfo fault = getFaultInfo(opInfo, elName);
                
                CorbaTypeEventProducer faultEventProducer =
                    CorbaHandlerUtils.getTypeEventProducer(exStreamable.getObject(),
                                                           service,
                                                           orb);
                CorbaStreamReader streamReader = new CorbaStreamReader(faultEventProducer);

                Object e = reader.read(fault.getMessageParts().get(0), streamReader);
                if (!(e instanceof Exception)) {
                    Class<?> exClass = fault.getProperty(Class.class.getName(), Class.class);
                    if (exClass != null) {
                        Class<?> beanClass = e.getClass();
                        Constructor<?> constructor =
                            exClass.getConstructor(new Class[]{String.class, beanClass});
                        
                        String repId = (message.getStreamableException()._type().id() != null) 
                            ? message.getStreamableException()._type().id()
                                : "";
                        e = constructor.newInstance(new Object[]{repId, e});
                    } else {
                        // Get the Fault                         
                        Fault faultEx = (Fault) message.getContent(Exception.class);
                        if (e instanceof Document) {
                            createFaultDetail((Document)e, fault, faultEx);
                        }
                        e = faultEx;
                    }
                }
                message.setContent(Exception.class, (Exception) e);
            }
        } catch (java.lang.Exception ex) {
            LOG.log(Level.SEVERE, "CORBA unmarshalFault exception", ex);
            throw new CorbaBindingException("CORBA unmarshalFault exception", ex);
        }

    }

    private void createFaultDetail(Document faultData, FaultInfo faultInfo, Fault faultEx) {
        MessagePartInfo partInfo =  faultInfo.getMessageParts().get(0);
        QName partInfoName = partInfo.getElementQName();
        Document faultDoc = DOMUtils.createDocument();
        Element faultElement = faultDoc.createElement("detail");
        faultDoc.appendChild(faultElement);
        Element partElement = 
            faultDoc.createElementNS(partInfoName.getNamespaceURI(), partInfoName.getLocalPart());
        
        Element faultDataElement = (Element) faultData.getFirstChild();
        Node node = faultDataElement.getFirstChild();
        while (node != null) {
            Node importedFaultData = faultDoc.importNode(node, true);
            partElement.appendChild(importedFaultData);
            node = node.getNextSibling();
        }
        faultElement.appendChild(partElement);
        faultEx.setDetail(faultElement);
    }

    
    protected FaultInfo getFaultInfo(OperationInfo opInfo, QName faultName) {
        Iterator<FaultInfo> faults = opInfo.getFaults().iterator();
        while (faults.hasNext()) {
            FaultInfo fault = faults.next();
            MessagePartInfo partInfo = fault.getMessageParts().get(0);
            if (partInfo.isElement()
                && partInfo.getElementQName().getLocalPart().equals(faultName.getLocalPart())) {
                return fault;
            } else if (partInfo.getTypeQName().getLocalPart().equals(faultName.getLocalPart())) {
                return fault;
            }
        }
        return null;
    }

    protected DataReader<XMLStreamReader> getDataReader(CorbaMessage message) {
        Service serviceModel = ServiceModelUtil.getService(message.getExchange());
        DataReader<XMLStreamReader> dataReader = 
            serviceModel.getDataBinding().createReader(XMLStreamReader.class);
        if (dataReader == null) {
            throw new CorbaBindingException("Couldn't create data reader for incoming fault message");
        }
        return dataReader;
    }
}
