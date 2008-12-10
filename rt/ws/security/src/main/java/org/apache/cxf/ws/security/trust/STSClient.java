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

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.binding.BindingFactory;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.binding.soap.model.SoapOperationInfo;
import org.apache.cxf.databinding.source.SourceDataBinding;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.helpers.DOMUtils;
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
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.ws.security.policy.model.Trust10;
import org.apache.cxf.ws.security.policy.model.Trust13;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.neethi.Policy;

/**
 * 
 */
public class STSClient {
    Bus bus;
    Client client;
    String location;
    Policy policy;
    String soapVersion = SoapBindingConstants.SOAP11_BINDING_ID;
    
    Map<String, Object> ctx = new HashMap<String, Object>();
    
    public STSClient(Bus b) {
        bus = b;
    }

    public void setLocation(String location) {
        this.location = location;
    }
    public void setPolicy(Policy policy) {
        this.policy = policy;
    }
    public void setSoap12() {
        soapVersion = SoapBindingConstants.SOAP12_BINDING_ID;
    }
    public void setSoap11() {
        soapVersion = SoapBindingConstants.SOAP11_BINDING_ID;
    }
    public void setTrust(Trust10 trust) {
        
    }
    public void setTrust(Trust13 trust) {
        
    }
    private void createClient() throws BusException, EndpointException {
        if (client != null) {
            return;
        }
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

    public Map<String, Object> getRequestContext() throws Exception {
        return ctx;
    }
    public SecurityToken requestSecurityToken() throws Exception {
        createClient();
        client.getRequestContext().putAll(ctx);
        
        //TODO: create the DOM based on the Trust10/Trust13 tokens
        String rqst = "<t:RequestSecurityToken " 
            + "xmlns:u='http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd' "
            + "xmlns:t='http://schemas.xmlsoap.org/ws/2005/02/trust'>\n"
            + "<t:RequestType>"
            + "http://schemas.xmlsoap.org/ws/2005/02/trust/Issue"
            + "</t:RequestType>\n"
            + "<t:Entropy>\n"
            + "<t:BinarySecret u:Id='uuid-4acf589c-0076-4a83-8b66-5f29341514b7-3'"
            + " Type='http://schemas.xmlsoap.org/ws/2005/02/trust/Nonce'>"
            + "Uv38QLxDQM9gLoDZ6OwYDiFk094nmwu3Wmay7EdKmhw=</t:BinarySecret>\n"
            + "</t:Entropy>\n"
            + "<t:KeyType>http://schemas.xmlsoap.org/ws/2005/02/trust/SymmetricKey</t:KeyType>\n"
            + "<t:KeySize>256</t:KeySize>\n"
            + "<t:ComputedKeyAlgorithm>\n"
            + "http://schemas.xmlsoap.org/ws/2005/02/trust/CK/PSHA1"
            + "</t:ComputedKeyAlgorithm>\n"
            + "</t:RequestSecurityToken>\n";

        
        client.invoke("RequestSecurityToken",
                      new DOMSource(DOMUtils.readXml(new StringReader(rqst)).getDocumentElement()));
        return null;
    }

   

}
