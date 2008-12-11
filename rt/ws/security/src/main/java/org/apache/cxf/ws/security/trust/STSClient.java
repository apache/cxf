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

package org.apache.cxf.ws.security.trust;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.binding.BindingFactory;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.model.SoapOperationInfo;
import org.apache.cxf.configuration.Configurable;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.databinding.source.SourceDataBinding;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.ServiceImpl;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.security.policy.model.AlgorithmSuite;
import org.apache.cxf.ws.security.policy.model.Binding;
import org.apache.cxf.ws.security.policy.model.Trust10;
import org.apache.cxf.ws.security.policy.model.Trust13;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyComponent;
import org.apache.ws.security.util.Base64;
import org.apache.ws.security.util.WSSecurityUtil;

/**
 * 
 */
public class STSClient implements Configurable {
    
    Bus bus;
    String name = "default.sts-client";
    Client client;
    String location;
    Policy policy;
    String soapVersion = SoapBindingConstants.SOAP11_BINDING_ID;
    int keySize = 256;
    Trust10 trust10;
    Trust13 trust13;
    AlgorithmSuite algorithmSuite;
    
    Map<String, Object> ctx = new HashMap<String, Object>();
    
    public STSClient(Bus b) {
        bus = b;
    }

    public String getBeanName() {
        return name;
    }
    public void setBeanName(String s) {
        name = s;
    }
    public void setLocation(String location) {
        this.location = location;
    }
    public void setPolicy(Policy policy) {
        this.policy = policy;
        if (algorithmSuite == null) {
            Iterator i = policy.getAlternatives();
            while (i.hasNext() && algorithmSuite == null) {
                List<PolicyComponent> p = CastUtils.cast((List)i.next());
                for (PolicyComponent p2 : p) {
                    if (p2 instanceof Binding) {
                        algorithmSuite = ((Binding)p2).getAlgorithmSuite();
                    }
                }
            }
        }
    }
    public void setPolicy(Element policy) {
        setPolicy(bus.getExtension(PolicyBuilder.class).getPolicy(policy));
    }
    public void setSoap12() {
        soapVersion = SoapBindingConstants.SOAP12_BINDING_ID;
    }
    public void setSoap11() {
        soapVersion = SoapBindingConstants.SOAP11_BINDING_ID;
    }
    public void setSoap11(boolean b) {
        if (b) {
            setSoap11();
        } else {
            setSoap12();
        }
    }
    public void setTrust(Trust10 trust) {
        trust10 = trust;
    }
    public void setTrust(Trust13 trust) {
        trust13 = trust;        
    }
    public void setAlgorithmSuite(AlgorithmSuite ag) {
        algorithmSuite = ag;
    }
    
    public Map<String, Object> getRequestContext() {
        return ctx;
    }
    public void setProperties(Map<String, Object> p) {
        ctx.putAll(p);
    }
    public Map<String, Object> getProperties() {
        return ctx;
    }
    
    private void createClient() throws BusException, EndpointException {
        if (client != null) {
            return;
        }
        bus.getExtension(Configurer.class).configureBean(name, this);
        
        
        Service service = null;
        String ns = "http://schemas.xmlsoap.org/ws/2005/02/trust/wsdl";
        String typeNs = "http://schemas.xmlsoap.org/ws/2005/02/trust";
        ServiceInfo si = new ServiceInfo();
        
        QName iName = new QName(ns, "SecurityTokenService");
        si.setName(iName);
        InterfaceInfo ii = new InterfaceInfo(si, iName);
        OperationInfo oi = ii.addOperation(new QName(ns, "RequestSecurityToken"));
        MessageInfo mii = oi.createMessage(new QName(ns, "RequestSecurityTokenMsg"), 
                                           MessageInfo.Type.INPUT);
        oi.setInput("RequestSecurityTokenMsg", mii);
        MessagePartInfo mpi = mii.addMessagePart("request");
        mpi.setElementQName(new QName(typeNs, "RequestSecurityToken"));
        
        MessageInfo mio = oi.createMessage(new QName(ns, "RequestSecurityTokenResponseMsg"), 
                                           MessageInfo.Type.OUTPUT);
        oi.setOutput("RequestSecurityTokenResponseMsg", mio);
        mpi = mio.addMessagePart("response");
        mpi.setElementQName(new QName(typeNs, "RequestSecurityTokenResponse"));
        
        si.setInterface(ii);
        service = new ServiceImpl(si);
        
        BindingFactoryManager bfm = bus.getExtension(BindingFactoryManager.class);
        BindingFactory bindingFactory = bfm.getBindingFactory(soapVersion);
        BindingInfo bi = bindingFactory.createBindingInfo(service, 
                                                          soapVersion, null);
        si.addBinding(bi);
        ConduitInitiatorManager cim = bus.getExtension(ConduitInitiatorManager.class);
        ConduitInitiator ci = cim.getConduitInitiatorForUri(location);
        EndpointInfo ei = new EndpointInfo(si, ci.getTransportIds().get(0));
        ei.setBinding(bi);
        ei.setName(iName);
        ei.setAddress(location);
        si.addEndpoint(ei);
        ei.addExtensor(policy);
        
        BindingOperationInfo boi = bi.getOperation(oi);
        SoapOperationInfo soi = boi.getExtensor(SoapOperationInfo.class);
        if (soi == null) {
            soi = new SoapOperationInfo();
            boi.addExtensor(soi);
        }
        soi.setAction("http://schemas.xmlsoap.org/ws/2005/02/trust/RST/Issue");
        

        service.setDataBinding(new SourceDataBinding());
        Endpoint endpoint = new EndpointImpl(bus, service, ei);
        
        client = new ClientImpl(bus, endpoint);
        
    }

    public SecurityToken requestSecurityToken() throws Exception {
        return requestSecurityToken(null);
    }
    public SecurityToken requestSecurityToken(String appliesTo) throws Exception {
        createClient();
        client.getRequestContext().putAll(ctx);
        
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        String namespace = "http://schemas.xmlsoap.org/ws/2005/02/trust";
        writer.writeStartElement(namespace, "RequestSecurityToken");
        writer.writeStartElement(namespace, "RequestType");
        writer.writeCharacters("http://schemas.xmlsoap.org/ws/2005/02/trust/Issue");
        writer.writeEndElement();
        if (appliesTo != null) {
            //TODO: AppliesTo element? 
        }
        //TODO: Lifetime element?
        writer.writeStartElement(namespace, "KeyType");
        //TODO: Set the KeyType?
        writer.writeCharacters(namespace + "/SymmetricKey");
        writer.writeEndElement();
        writer.writeStartElement(namespace, "KeySize");
        writer.writeCharacters(Integer.toString(keySize));
        writer.writeEndElement();
        
        
        if ((trust10 != null && trust10.isRequireClientEntropy())
            || (trust13 != null && trust13.isRequireClientEntropy())) {
            writer.writeStartElement(namespace, "Entropy");
            writer.writeStartElement(namespace, "BinarySecret");
            writer.writeAttribute("Type", namespace + "/Nounce");
            byte[] requestorEntropy =
                WSSecurityUtil.generateNonce(algorithmSuite.getMaximumSymmetricKeyLength() / 8);
            writer.writeCharacters(Base64.encode(requestorEntropy));

            writer.writeEndElement();
            writer.writeEndElement();
            writer.writeStartElement(namespace, "ComputedKeyAlgorithm");
            writer.writeCharacters(namespace + "/CK/PSHA1");
            writer.writeEndElement();
        }
        writer.writeEndElement();
        
        client.invoke("RequestSecurityToken",
                      new DOMSource(writer.getDocument().getDocumentElement()));
        return null;
    }



   

}
