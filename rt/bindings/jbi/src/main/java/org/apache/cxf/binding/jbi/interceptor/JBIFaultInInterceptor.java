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

package org.apache.cxf.binding.jbi.interceptor;

import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Element;



import org.apache.cxf.binding.jbi.JBIFault;
import org.apache.cxf.binding.jbi.JBIMessage;
import org.apache.cxf.common.i18n.BundleUtils;


import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.FragmentStreamReader;
import org.apache.cxf.staxutils.StaxUtils;


public class JBIFaultInInterceptor extends AbstractPhaseInterceptor<JBIMessage> {
    
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(JBIFaultInInterceptor.class);
    public JBIFaultInInterceptor() {
        super(Phase.UNMARSHAL);
        addBefore("*");
    }
    
    public void handleMessage(JBIMessage message) throws Fault {
        XMLStreamReader xsr = message.getContent(XMLStreamReader.class);
        DepthXMLStreamReader reader = new DepthXMLStreamReader(xsr);

        try {            
            
            if (!StaxUtils.toNextElement(reader)) {
                throw new Fault(new org.apache.cxf.common.i18n.Message("ILLEGAL_JBIFAULT_FORMAT", BUNDLE));
            }
            Fault fault = new JBIFault(new org.apache.cxf.common.i18n.Message(JBIFault.JBI_FAULT_STRING, 
                                                                              (ResourceBundle) null));
            
            if (StaxUtils.toNextElement(reader)) {
                // handling detail
                Element detail = StaxUtils.read(new FragmentStreamReader(reader)).getDocumentElement();
                fault.setDetail(detail);
            }
            message.setContent(Exception.class, fault);
        } catch (XMLStreamException xse) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("STAX_READ_EXC", BUNDLE));
        }

    }
}
