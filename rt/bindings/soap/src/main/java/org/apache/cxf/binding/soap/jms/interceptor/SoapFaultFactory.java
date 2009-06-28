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

package org.apache.cxf.binding.soap.jms.interceptor;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapBinding;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;

/**
 * 
 */
public class SoapFaultFactory  {

    private static final Logger LOG = LogUtils.getL7dLogger(SoapFaultFactory.class); 
    
    private SoapVersion version;
    
    public SoapFaultFactory(Binding binding) {
        version = ((SoapBinding)binding).getSoapVersion();
    }
    
    public Fault createFault(JMSFault jmsFault) {
        Fault f = null;
        if (version == Soap11.getInstance()) {
            f = createSoap11Fault(jmsFault);
            // so we can encode the SequenceFault as header   
            f.initCause(jmsFault);
        } else {
            f = createSoap12Fault(jmsFault);
        }
        return f;
    }
    
    Fault createSoap11Fault(JMSFault jmsFault) {
        SoapFault fault = new SoapFault(jmsFault.getReason(),
            jmsFault.isSender() ? version.getSender() : version.getReceiver());
        fault.setSubCode(jmsFault.getSubCode());
        return fault;
    }
    
    Fault createSoap12Fault(JMSFault jmsFault) {
        SoapFault fault = (SoapFault)createSoap11Fault(jmsFault);
        Object detail = jmsFault.getDetail();
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
        
        elem =  (Element)doc.getFirstChild();
        fault.setDetail(elem);
    }
    
    public String toString(Fault f) {
        SoapFault sf = (SoapFault)f;
        return sf.toString();
    }
        

}
