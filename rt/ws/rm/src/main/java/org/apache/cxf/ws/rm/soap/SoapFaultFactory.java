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

package org.apache.cxf.ws.rm.soap;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapBinding;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.ws.rm.BindingFaultFactory;
import org.apache.cxf.ws.rm.Identifier;
import org.apache.cxf.ws.rm.RMConstants;
import org.apache.cxf.ws.rm.SequenceAcknowledgement;
import org.apache.cxf.ws.rm.SequenceFault;
import org.apache.cxf.ws.rm.SequenceType;



/**
 * 
 */
public class SoapFaultFactory implements BindingFaultFactory {

    private static final Logger LOG = LogUtils.getL7dLogger(SoapFaultFactory.class); 
    private static final String WS_RM_PACKAGE = 
        PackageUtils.getPackageName(SequenceType.class);
    
    private SoapVersion version;
    
    public SoapFaultFactory(Binding binding) {
        version = ((SoapBinding)binding).getSoapVersion();
    }
    
    public Fault createFault(SequenceFault sf) {
        Fault f = null;
        if (version == Soap11.getInstance()) {
            f = createSoap11Fault(sf);
            // so we can encode the SequenceFault as header   
            f.initCause(sf);
        } else {
            f = createSoap12Fault(sf);
        }
        return f;
    }
    
    Fault createSoap11Fault(SequenceFault sf) {
        SoapFault fault = new SoapFault(sf.getReason(),
            sf.isSender() ? version.getSender() : version.getReceiver());
        fault.setSubCode(sf.getSubCode());
        return fault;
    }
    
    Fault createSoap12Fault(SequenceFault sf) {
        SoapFault fault = (SoapFault)createSoap11Fault(sf);
        Object detail = sf.getDetail();
        if (null == detail) {
            return fault;
        }

        try {
            setDetail(fault, detail);
        } catch (Exception ex) {
            LogUtils.log(LOG, Level.SEVERE, "MARSHAL_FAULT_DETAIL_EXC", ex); 
            ex.printStackTrace();
        }
        return fault;
    }
    
    void setDetail(SoapFault fault, Object detail) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document doc = factory.newDocumentBuilder().newDocument();
        Element elem = null;
        
        JAXBContext ctx = JAXBContext.newInstance(
            WS_RM_PACKAGE,
            SequenceAcknowledgement.class.getClassLoader());
        Marshaller m = ctx.createMarshaller();
        if (RMConstants.getInvalidAcknowledgmentFaultCode().equals(fault.getSubCode())) {
            SequenceAcknowledgement ack = (SequenceAcknowledgement)detail;
            m.marshal(ack, doc);
        } else if (!RMConstants.getCreateSequenceRefusedFaultCode().equals(fault.getSubCode())) {
            Identifier id = (Identifier)detail;  
            m.marshal(id, doc);            
        }
        
        elem =  (Element)doc.getFirstChild();
        fault.setDetail(elem);
    }
    
    public String toString(Fault f) {
        SoapFault sf = (SoapFault)f;
        Message msg = new Message("SEQ_FAULT_MSG", LOG, 
            new Object[] {sf.getReason(), sf.getFaultCode(), sf.getSubCode()});
        return msg.toString();
    }
        

}
