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

package org.apache.cxf.ws.addressing;

import java.io.InputStream;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.wsdl.EndpointReferenceUtils;
import org.junit.Assert;
import org.junit.Test;


public class VersionTransformerTest extends Assert {
    
    @Test
    public void testConvertToInternal() throws Exception {
        InputStream is = getClass().getResourceAsStream("resources/hello_world_soap_http_infoset.xml");
        Document doc = XMLUtils.parse(is);
        DOMSource erXML = new DOMSource(doc);
        EndpointReference endpointReference = readEndpointReference(erXML);
        
        EndpointReferenceType ert = VersionTransformer.convertToInternal(endpointReference);

        assertNotNull(ert);
        assertEquals("http://localhost:8080/test", ert.getAddress().getValue());
        assertEquals(new QName("http://apache.org/hello_world_soap_http", "SOAPService"), 
                               EndpointReferenceUtils.getServiceName(ert, null));
        
        // VersionTransformer.convertToInternal produces metadata extensions of type 
        // DOM Element hence we're testing the relevant EndpointReferenceUtils.setPortName
        // code path here
        
        List<Object> metadata = ert.getMetadata().getAny();
        assertEquals("Single metadata extension expected", 1, metadata.size());
        assertTrue("Metadata extension of type DOM Element expected", 
                   metadata.get(0) instanceof Element);
        assertNull("No portName has been set yet", EndpointReferenceUtils.getPortName(ert));
        EndpointReferenceUtils.setPortName(ert, "Greeter");
        assertEquals("No portName has been set", "Greeter", EndpointReferenceUtils.getPortName(ert));
    }
    
    private EndpointReference readEndpointReference(Source eprInfoset) {
        try {
            Unmarshaller unmarshaller = JAXBContext.newInstance(W3CEndpointReference.class)
                .createUnmarshaller();
            return (EndpointReference)unmarshaller.unmarshal(eprInfoset);
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        return null;
    }

}
