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

package org.apache.cxf.rt.security.saml.xacml2;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.rt.security.saml.xacml.XACMLConstants;
import org.opensaml.xacml.ctx.AttributeType;
import org.opensaml.xacml.ctx.RequestType;
import org.opensaml.xacml.ctx.ResourceType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * Some unit tests to create a XACML Request via the XACMLRequestBuilder interface.
 */
public class XACMLRequestBuilderTest {

    static {
        org.apache.wss4j.common.saml.OpenSAMLUtil.initSamlEngine();
    }

    @org.junit.Test
    public void testXACMLRequestBuilder() throws Exception {
        // Mock up a request
        Principal principal = new Principal() {
            public String getName() {
                return "alice";
            }
        };

        String operation = "{http://www.example.org/contract/DoubleIt}DoubleIt";
        MessageImpl msg = new MessageImpl();
        msg.put(Message.WSDL_OPERATION, QName.valueOf(operation));
        String service = "{http://www.example.org/contract/DoubleIt}DoubleItService";
        msg.put(Message.WSDL_SERVICE, QName.valueOf(service));
        String resourceURL = "https://localhost:8080/doubleit";
        msg.put(Message.REQUEST_URI, resourceURL);

        XACMLRequestBuilder builder = new DefaultXACMLRequestBuilder();
        RequestType request =
            builder.createRequest(principal, Collections.singletonList("manager"), msg);
        assertNotNull(request);
    }


    @org.junit.Test
    public void testAction() throws Exception {
        // Mock up a request
        Principal principal = new Principal() {
            public String getName() {
                return "alice";
            }
        };

        String operation = "{http://www.example.org/contract/DoubleIt}DoubleIt";
        MessageImpl msg = new MessageImpl();
        msg.put(Message.WSDL_OPERATION, QName.valueOf(operation));
        String service = "{http://www.example.org/contract/DoubleIt}DoubleItService";
        msg.put(Message.WSDL_SERVICE, QName.valueOf(service));
        String resourceURL = "https://localhost:8080/doubleit";
        msg.put(Message.REQUEST_URI, resourceURL);

        DefaultXACMLRequestBuilder builder = new DefaultXACMLRequestBuilder();
        RequestType request =
            builder.createRequest(principal, Collections.singletonList("manager"), msg);
        assertNotNull(request);

        String action =
            request.getAction().getAttributes().get(0).getAttributeValues().get(0).getValue();
        assertEquals("execute", action);

        builder.setAction("write");
        request = builder.createRequest(principal, Collections.singletonList("manager"), msg);
        assertNotNull(request);

        action =
            request.getAction().getAttributes().get(0).getAttributeValues().get(0).getValue();
        assertEquals("write", action);
    }

    @org.junit.Test
    public void testEnvironment() throws Exception {
        // Mock up a request
        Principal principal = new Principal() {
            public String getName() {
                return "alice";
            }
        };

        String operation = "{http://www.example.org/contract/DoubleIt}DoubleIt";
        MessageImpl msg = new MessageImpl();
        msg.put(Message.WSDL_OPERATION, QName.valueOf(operation));
        String service = "{http://www.example.org/contract/DoubleIt}DoubleItService";
        msg.put(Message.WSDL_SERVICE, QName.valueOf(service));
        String resourceURL = "https://localhost:8080/doubleit";
        msg.put(Message.REQUEST_URL, resourceURL);

        XACMLRequestBuilder builder = new DefaultXACMLRequestBuilder();
        RequestType request =
            builder.createRequest(principal, Collections.singletonList("manager"), msg);
        assertNotNull(request);
        assertFalse(request.getEnvironment().getAttributes().isEmpty());

        ((DefaultXACMLRequestBuilder)builder).setSendDateTime(false);
        request = builder.createRequest(principal, Collections.singletonList("manager"), msg);
        assertNotNull(request);
        assertTrue(request.getEnvironment().getAttributes().isEmpty());
    }

    @org.junit.Test
    public void testSOAPResource() throws Exception {
        // Mock up a request
        Principal principal = new Principal() {
            public String getName() {
                return "alice";
            }
        };

        String operation = "{http://www.example.org/contract/DoubleIt}DoubleIt";
        MessageImpl msg = new MessageImpl();
        msg.put(Message.WSDL_OPERATION, QName.valueOf(operation));
        String service = "{http://www.example.org/contract/DoubleIt}DoubleItService";
        msg.put(Message.WSDL_SERVICE, QName.valueOf(service));
        String resourceURL = "https://localhost:8080/doubleit";
        msg.put(Message.REQUEST_URL, resourceURL);

        XACMLRequestBuilder builder = new DefaultXACMLRequestBuilder();
        RequestType request =
            builder.createRequest(principal, Collections.singletonList("manager"), msg);
        assertNotNull(request);

        List<ResourceType> resources = request.getResources();
        assertNotNull(resources);
        assertEquals(1, resources.size());

        ResourceType resource = resources.get(0);
        assertEquals(4, resource.getAttributes().size());

        boolean resourceIdSatisfied = false;
        boolean soapServiceSatisfied = false;
        boolean soapOperationSatisfied = false;
        boolean resourceURISatisfied = false;
        for (AttributeType attribute : resource.getAttributes()) {
            String attributeValue = attribute.getAttributeValues().get(0).getValue();
            if (XACMLConstants.RESOURCE_ID.equals(attribute.getAttributeId())
                && "{http://www.example.org/contract/DoubleIt}DoubleItService#DoubleIt".equals(
                    attributeValue)) {
                resourceIdSatisfied = true;
            } else if (XACMLConstants.RESOURCE_WSDL_SERVICE_ID.equals(attribute.getAttributeId())
                && service.equals(attributeValue)) {
                soapServiceSatisfied = true;
            } else if (XACMLConstants.RESOURCE_WSDL_OPERATION_ID.equals(attribute.getAttributeId())
                && operation.equals(attributeValue)) {
                soapOperationSatisfied = true;
            } else if (XACMLConstants.RESOURCE_WSDL_ENDPOINT.equals(attribute.getAttributeId())
                && resourceURL.equals(attributeValue)) {
                resourceURISatisfied = true;
            }
        }

        assertTrue(resourceIdSatisfied && soapServiceSatisfied && soapOperationSatisfied
                   && resourceURISatisfied);
    }

    @org.junit.Test
    public void testSOAPResourceDifferentNamespace() throws Exception {
        // Mock up a request
        Principal principal = new Principal() {
            public String getName() {
                return "alice";
            }
        };

        String operation = "{http://www.example.org/contract/DoubleIt}DoubleIt";
        MessageImpl msg = new MessageImpl();
        msg.put(Message.WSDL_OPERATION, QName.valueOf(operation));
        String service = "{http://www.example.org/contract/DoubleItService}DoubleItService";
        msg.put(Message.WSDL_SERVICE, QName.valueOf(service));
        String resourceURL = "https://localhost:8080/doubleit";
        msg.put(Message.REQUEST_URL, resourceURL);

        XACMLRequestBuilder builder = new DefaultXACMLRequestBuilder();
        RequestType request =
            builder.createRequest(principal, Collections.singletonList("manager"), msg);
        assertNotNull(request);

        List<ResourceType> resources = request.getResources();
        assertNotNull(resources);
        assertEquals(1, resources.size());

        ResourceType resource = resources.get(0);
        assertEquals(4, resource.getAttributes().size());

        boolean resourceIdSatisfied = false;
        boolean soapServiceSatisfied = false;
        boolean soapOperationSatisfied = false;
        boolean resourceURISatisfied = false;
        String expectedResourceId =
            service + "#" + operation;
        for (AttributeType attribute : resource.getAttributes()) {
            String attributeValue = attribute.getAttributeValues().get(0).getValue();
            if (XACMLConstants.RESOURCE_ID.equals(attribute.getAttributeId())
                && expectedResourceId.equals(attributeValue)) {
                resourceIdSatisfied = true;
            } else if (XACMLConstants.RESOURCE_WSDL_SERVICE_ID.equals(attribute.getAttributeId())
                && service.equals(attributeValue)) {
                soapServiceSatisfied = true;
            } else if (XACMLConstants.RESOURCE_WSDL_OPERATION_ID.equals(attribute.getAttributeId())
                && operation.equals(attributeValue)) {
                soapOperationSatisfied = true;
            } else if (XACMLConstants.RESOURCE_WSDL_ENDPOINT.equals(attribute.getAttributeId())
                && resourceURL.equals(attributeValue)) {
                resourceURISatisfied = true;
            }
        }

        assertTrue(resourceIdSatisfied && soapServiceSatisfied && soapOperationSatisfied
                   && resourceURISatisfied);
    }

    @org.junit.Test
    public void testRESTResource() throws Exception {
        // Mock up a request
        Principal principal = new Principal() {
            public String getName() {
                return "alice";
            }
        };

        MessageImpl msg = new MessageImpl();
        String resourceURL = "https://localhost:8080/doubleit";
        msg.put(Message.REQUEST_URL, resourceURL);

        XACMLRequestBuilder builder = new DefaultXACMLRequestBuilder();
        RequestType request =
            builder.createRequest(principal, Collections.singletonList("manager"), msg);
        assertNotNull(request);

        List<ResourceType> resources = request.getResources();
        assertNotNull(resources);
        assertEquals(1, resources.size());

        ResourceType resource = resources.get(0);
        assertEquals(1, resource.getAttributes().size());

        for (AttributeType attribute : resource.getAttributes()) {
            String attributeValue = attribute.getAttributeValues().get(0).getValue();
            assertEquals(attributeValue, resourceURL);
        }
    }

    @org.junit.Test
    public void testRESTResourceTruncatedURI() throws Exception {
        // Mock up a request
        Principal principal = new Principal() {
            public String getName() {
                return "alice";
            }
        };

        MessageImpl msg = new MessageImpl();
        String resourceURL = "https://localhost:8080/doubleit";
        msg.put(Message.REQUEST_URL, resourceURL);
        String resourceURI = "/doubleit";
        msg.put(Message.REQUEST_URI, resourceURI);

        XACMLRequestBuilder builder = new DefaultXACMLRequestBuilder();
        ((DefaultXACMLRequestBuilder)builder).setSendFullRequestURL(false);
        RequestType request =
            builder.createRequest(principal, Collections.singletonList("manager"), msg);
        assertNotNull(request);

        List<ResourceType> resources = request.getResources();
        assertNotNull(resources);
        assertEquals(1, resources.size());

        ResourceType resource = resources.get(0);
        assertEquals(1, resource.getAttributes().size());

        for (AttributeType attribute : resource.getAttributes()) {
            String attributeValue = attribute.getAttributeValues().get(0).getValue();
            assertEquals(attributeValue, resourceURI);
        }
    }
}