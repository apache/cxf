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

package org.apache.cxf.sdo;


import java.util.Collection;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import junit.framework.AssertionFailedError;

import org.junit.Test;


/**
 * 
 */
public abstract class AbstractHelloWorldTest extends AbstractSDOTest {

    
    @Test
    public void testBasicInvoke() throws Exception {
        Node response = invoke("TestService", "bean11.xml");
        addNamespace("ns1", "http://apache.org/hello_world_soap_http/types");
        assertValid("/s:Envelope/s:Body/ns1:greetMeResponse", response);
        assertValid("//ns1:greetMeResponse/ns1:responseType", response);
        assertValid("//ns1:greetMeResponse/ns1:responseType[text()='Hello World']", response);
    }
    
    @Test
    public void testStructure() throws Exception {
        Node response = invoke("TestService", "structure.xml");
        addNamespace("ns1", "http://apache.org/hello_world_soap_http/types");
        assertValid("/s:Envelope/s:Body/ns1:echoStructResponse", response);
        assertValid("//ns1:echoStructResponse/ns1:return", response);
        assertValid("//ns1:echoStructResponse/ns1:return/ns1:text[text()='Hello']", response);        
    }
    
    @Test
    public void testWSDL() throws Exception {
        Collection<Document> docs = getWSDLDocuments("TestService");
        for (Document doc : docs) {
            try {
                assertValid("/wsdl:definitions/wsdl:types/xsd:schema"
                            + "[@targetNamespace='http://apache.org/hello_world_soap_http/types']", 
                            doc);
                return;
            } catch (AssertionFailedError ex) {
                //ignore
            }
        }
        fail("Did not find schemas");
    }
}
