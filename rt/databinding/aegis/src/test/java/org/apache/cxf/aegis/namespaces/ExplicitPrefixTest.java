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

package org.apache.cxf.aegis.namespaces;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.wsdl.Definition;
import javax.wsdl.factory.WSDLFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.aegis.namespaces.impl.NameServiceImpl;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.service.Service;
import org.junit.Test;

/**
 * Test of the ability of the user to control prefixes by specifying them in a
 * map.
 */
public class ExplicitPrefixTest extends AbstractAegisTest {
    
    private static final String AEGIS_TEST_NAMESPACE_PREFIX_XYZZY = "xyzzy";
    private static final String URN_AEGIS_NAMESPACE_TEST = "urn:aegis:namespace:test";

    static class ServiceAndMapping {

        private TypeMapping typeMapping;
        private Service service;
        private Server server;
        
        /**
         * *
         * 
         * @return Returns the server.
         */
        public Server getServer() {
            return server;
        }
        /**
         * @param server The server to set.
         */
        public void setServer(Server server) {
            this.server = server;
        }
        /**
         * *
         * 
         * @return Returns the typeMapping.
         */
        public TypeMapping getTypeMapping() {
            return typeMapping;
        }
        /**
         * @param typeMapping The typeMapping to set.
         */
        public void setTypeMapping(TypeMapping typeMapping) {
            this.typeMapping = typeMapping;
        }
        /**
         * *
         * 
         * @return Returns the service.
         */
        public Service getService() {
            return service;
        }
        /**
         * @param service The service to set.
         */
        public void setService(Service service) {
            this.service = service;
        }
    }
        
    private ServiceAndMapping setupService(Class<?> seiClass, Map<String, String> namespaces) {
        AegisDatabinding db = new AegisDatabinding();
        db.setNamespaceMap(namespaces);
        Server s = createService(seiClass, null, db);
        ServiceAndMapping serviceAndMapping = new ServiceAndMapping();
        serviceAndMapping.setServer(s);
        serviceAndMapping.setService(s.getEndpoint().getService());
        serviceAndMapping.setTypeMapping((TypeMapping)
                                         serviceAndMapping.getService().get(TypeMapping.class.getName()));
        return serviceAndMapping;
    }
    
    /**
     * The W3C dom is not helpful in looking at declarations. We could convert
     * to JDOM, but this is enough to get the job done.
     * 
     * @param node
     * @return
     */
    private Map<String, String>
    getNodeNamespaceDeclarations(Node node) {
        Map<String, String> result = new HashMap<String, String>();
        NamedNodeMap attributes = node.getAttributes();
        for (int x = 0; x < attributes.getLength(); x++) {
            Attr attr = (Attr)attributes.item(x);
            if (attr.getName().startsWith("xmlns:")) {
                String[] ns = attr.getName().split(":");
                result.put(ns[1], attr.getValue());
            }
        }
        return result;
    }
    
    /**
     * This substitutes for using the commons-collection BiDiMap.
     * @param nsmap
     * @param namespace
     * @return
     */
    private String lookupPrefix(Map<String, String> nsmap, String namespace) {
        for (Map.Entry<String, String> nspair : nsmap.entrySet()) {
            if (namespace.equals(nspair.getValue())) {
                return nspair.getKey();
            }
        }
        return null;
    }
    
    @Test
    public void testOnePrefix() throws Exception {
        Map<String, String> mappings = new HashMap<String, String>();
        mappings.put(URN_AEGIS_NAMESPACE_TEST, AEGIS_TEST_NAMESPACE_PREFIX_XYZZY);
        ServiceAndMapping serviceAndMapping = setupService(NameServiceImpl.class, mappings);
        Definition def = getWSDLDefinition("NameServiceImpl");
        StringWriter wsdlSink = new StringWriter();
        WSDLFactory.newInstance().newWSDLWriter().writeWSDL(def, wsdlSink);
        org.w3c.dom.Document wsdlDoc = getWSDLDocument("NameServiceImpl");
        Element rootElement = wsdlDoc.getDocumentElement();
        addNamespace(AEGIS_TEST_NAMESPACE_PREFIX_XYZZY, URN_AEGIS_NAMESPACE_TEST);
        assertXPathEquals("//namespace::xyzzy", URN_AEGIS_NAMESPACE_TEST, rootElement);
        Element nameSchema = (Element)
            assertValid("//xsd:schema[@targetNamespace='urn:aegis:namespace:test']", rootElement).item(0);
        Map<String, String> namePrefixes = getNodeNamespaceDeclarations(nameSchema);
        // there should be no TNS prefix, since the TNS namespace is explicitly
        // xyzzy.
        assertFalse(namePrefixes.containsKey("tns"));
        Element serviceSchema = (Element)
            assertValid("//xsd:schema[@targetNamespace='http://impl.namespaces.aegis.cxf.apache.org']", 
                        rootElement).item(0);
        Map<String, String> servicePrefixes = getNodeNamespaceDeclarations(serviceSchema);
        String testPrefix = lookupPrefix(servicePrefixes, URN_AEGIS_NAMESPACE_TEST);
        assertEquals(AEGIS_TEST_NAMESPACE_PREFIX_XYZZY, testPrefix);        

        serviceAndMapping.getServer().stop();
    }
}
