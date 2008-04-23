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


import javax.xml.namespace.QName;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class AddressingConstantsImplTest extends Assert {
    private AddressingConstants constants;

    @Before
    public void setUp() {
        constants = new AddressingConstantsImpl();
    }

    @Test
    public void testGetNamespaceURI() throws Exception {
        assertEquals("unexpected constant",
                     "http://www.w3.org/2005/08/addressing",
                     constants.getNamespaceURI());
    }

    @Test
    public void testGetWSDLNamespaceURI() throws Exception {
        assertEquals("unexpected constant",
                     "http://www.w3.org/2006/05/addressing/wsdl",
                     constants.getWSDLNamespaceURI());
    }

    @Test
    public void testGetWSDLExtensibility() throws Exception {
        assertEquals("unexpected constant",
                     new QName("http://www.w3.org/2006/05/addressing/wsdl",
                               "UsingAddressing"),
                     constants.getWSDLExtensibilityQName());
    }

    @Test
    public void testGetWSDLActionQName() throws Exception {
        assertEquals("unexpected constant",
                     new QName("http://www.w3.org/2006/05/addressing/wsdl",
                               "Action"),
                     constants.getWSDLActionQName());
    }

    @Test
    public void testGetAnonymousURI() throws Exception {
        assertEquals("unexpected constant",
                     "http://www.w3.org/2005/08/addressing/anonymous",
                     constants.getAnonymousURI());
    }

    @Test
    public void testGetNoneURI() throws Exception {
        assertEquals("unexpected constant",
                     "http://www.w3.org/2005/08/addressing/none",
                     constants.getNoneURI());
    }

    @Test
    public void testGetFromQName() throws Exception {
        assertEquals("unexpected constant",
                     new QName("http://www.w3.org/2005/08/addressing",
                               "From"),
                     constants.getFromQName());
    }

    @Test
    public void testGetToQName() throws Exception {
        assertEquals("unexpected constant",
                     new QName("http://www.w3.org/2005/08/addressing",
                               "To"),
                     constants.getToQName());
    }

    @Test
    public void testGetReplyToQName() throws Exception {
        assertEquals("unexpected constant",
                     new QName("http://www.w3.org/2005/08/addressing",
                               "ReplyTo"),
                     constants.getReplyToQName());
    }

    @Test
    public void testGetFaultToQName() throws Exception {
        assertEquals("unexpected constant",
                     new QName("http://www.w3.org/2005/08/addressing",
                               "FaultTo"),
                     constants.getFaultToQName());
    }

    @Test
    public void testGetActionQName() throws Exception {
        assertEquals("unexpected constant",
                     new QName("http://www.w3.org/2005/08/addressing",
                               "Action"),
                     constants.getActionQName());
    }

    @Test
    public void testGetMessageIDQName() throws Exception {
        assertEquals("unexpected constant",
                     new QName("http://www.w3.org/2005/08/addressing",
                               "MessageID"),
                     constants.getMessageIDQName());
    }

    @Test
    public void testGetRelationshipReply() throws Exception {
        assertEquals("unexpected constant",
                     "http://www.w3.org/2005/08/addressing/reply",
                     constants.getRelationshipReply());
    }

    @Test
    public void testGetRelatesToQName() throws Exception {
        assertEquals("unexpected constant",
                     new QName("http://www.w3.org/2005/08/addressing",
                               "RelatesTo"),
                     constants.getRelatesToQName());
    }

    @Test
    public void testGetRelationshipTypeQName() throws Exception {
        assertEquals("unexpected constant",
                     new QName("http://www.w3.org/2005/08/addressing",
                               "RelationshipType"),
                     constants.getRelationshipTypeQName());
    }

    @Test
    public void testGetMetadataQName() throws Exception {
        assertEquals("unexpected constant",
                     new QName("http://www.w3.org/2005/08/addressing",
                               "Metadata"),
                     constants.getMetadataQName());
    }

    @Test
    public void testGetAddressQName() throws Exception {
        assertEquals("unexpected constant",
                     new QName("http://www.w3.org/2005/08/addressing",
                               "Address"),
                     constants.getAddressQName());
    }

    @Test
    public void testGetPackageName() throws Exception {
        assertEquals("unexpected constant",
                     "org.apache.cxf.ws.addressing",
                     constants.getPackageName());
    }

    @Test
    public void testGetIsReferenceParameterQName() throws Exception {
        assertEquals("unexpected constant",
                     new QName("http://www.w3.org/2005/08/addressing",
                               "IsReferenceParameter"),
                     constants.getIsReferenceParameterQName());
    }

    @Test
    public void testGetInvalidMapQName() throws Exception {
        assertEquals("unexpected constant",
                     new QName("http://www.w3.org/2005/08/addressing",
                               "InvalidMessageAddressingProperty"),
                     constants.getInvalidMapQName());
    }

    @Test
    public void testMapRequiredQName() throws Exception {
        assertEquals("unexpected constant",
                     new QName("http://www.w3.org/2005/08/addressing",
                               "MessageAddressingPropertyRequired"),
                     constants.getMapRequiredQName());
    }

    @Test
    public void testDestinationUnreachableQName() throws Exception {
        assertEquals("unexpected constant",
                     new QName("http://www.w3.org/2005/08/addressing",
                               "DestinationUnreachable"),
                     constants.getDestinationUnreachableQName());
    }

    @Test
    public void testActionNotSupportedQName() throws Exception {
        assertEquals("unexpected constant",
                     new QName("http://www.w3.org/2005/08/addressing",
                               "ActionNotSupported"),
                     constants.getActionNotSupportedQName());
    }

    @Test
    public void testEndpointUnavailableQName() throws Exception {
        assertEquals("unexpected constant",
                     new QName("http://www.w3.org/2005/08/addressing",
                               "EndpointUnavailable"),
                     constants.getEndpointUnavailableQName());
    }

    @Test
    public void testDefaultFaultAction() throws Exception {
        assertEquals("unexpected constant",
                     "http://www.w3.org/2005/08/addressing/fault",
                     constants.getDefaultFaultAction());
    }

    @Test
    public void testActionNotSupportedText() throws Exception {
        assertEquals("unexpected constant",
                     "Action {0} not supported",
                     constants.getActionNotSupportedText());
    }

    @Test
    public void testDestinationUnreachableText() throws Exception {
        assertEquals("unexpected constant",
                     "Destination {0} unreachable",
                     constants.getDestinationUnreachableText());
    }

    @Test
    public void testEndpointUnavailableText() throws Exception {
        assertEquals("unexpected constant",
                     "Endpoint {0} unavailable",
                     constants.getEndpointUnavailableText());
    }

    @Test
    public void testGetInvalidMapText() throws Exception {
        assertEquals("unexpected constant",
                     "Invalid Message Addressing Property {0}",
                     constants.getInvalidMapText());
    }


    @Test
    public void testMapRequiredText() throws Exception {
        assertEquals("unexpected constant",
                     "Message Addressing Property {0} required",
                     constants.getMapRequiredText());
    }

    @Test
    public void testDuplicateMessageIDText() throws Exception {
        assertEquals("unexpected constant",
                     "Duplicate Message ID {0}",
                     constants.getDuplicateMessageIDText());
    }
}
