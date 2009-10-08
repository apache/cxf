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

package org.apache.cxf.service.stax;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Node;

import org.apache.cxf.databinding.stax.StaxDataBinding;
import org.apache.cxf.databinding.stax.StaxDataBindingFeature;
import org.apache.cxf.databinding.stax.XMLStreamWriterCallback;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.staxutils.FragmentStreamReader;
import org.apache.cxf.test.AbstractCXFTest;
import org.apache.cxf.transport.local.LocalTransportFactory;
import org.junit.Test;

public class StaxDatabindingTest extends AbstractCXFTest {
    @Test
    public void testCallback() throws Exception {
        String address = "local://foo";
        
        ServerFactoryBean sf = new ServerFactoryBean();
        sf.setServiceBean(new CallbackService());
        sf.setAddress(address);
        sf.setDataBinding(new StaxDataBinding());
        sf.getFeatures().add(new StaxDataBindingFeature());
        sf.setBus(getBus());
        sf.create();
        
        Node res = invoke(address, LocalTransportFactory.TRANSPORT_ID, "req.xml");
        
        assertValid("//bleh", res);
    }
    
    @Test
    public void testCopy() throws Exception {
        String address = "local://foo";
        
        ServerFactoryBean sf = new ServerFactoryBean();
        sf.setServiceBean(new CopyService());
        sf.setAddress(address);
        sf.setDataBinding(new StaxDataBinding());
        sf.getFeatures().add(new StaxDataBindingFeature());

        sf.create();
        
        Node res = invoke(address, LocalTransportFactory.TRANSPORT_ID, "req.xml");
        
        //DOMUtils.writeXml(res, System.out);
        addNamespace("a", "http://stax.service.cxf.apache.org/");
        assertValid("//a:bleh", res);
    }
    
    public static class CallbackService {
        public XMLStreamWriterCallback invoke(final XMLStreamReader reader) {
            try {
                reader.nextTag();
            } catch (XMLStreamException e) {
                throw new Fault(e);
            }
            
            return new XMLStreamWriterCallback() {

                public void write(XMLStreamWriter writer) throws Fault, XMLStreamException {
                    writer.writeEmptyElement("bleh");
                }
                
            };
        }
    }
    
    public static class CopyService {
        public XMLStreamReader invoke(final XMLStreamReader reader) {
            return new FragmentStreamReader(reader);
        }
    }
}
