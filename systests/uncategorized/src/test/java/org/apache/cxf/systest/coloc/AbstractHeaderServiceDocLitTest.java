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

package org.apache.cxf.systest.coloc;

import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import static junit.framework.Assert.assertEquals;

import org.apache.headers.coloc.types.HeaderInfo;
import org.apache.headers.coloc.types.InHeaderResponseT;
import org.apache.headers.coloc.types.InHeaderT;
import org.apache.headers.coloc.types.InoutHeaderResponseT;
import org.apache.headers.coloc.types.InoutHeaderT;
import org.apache.headers.coloc.types.OutHeaderResponseT;
import org.apache.headers.coloc.types.OutHeaderT;
import org.apache.headers.doc_lit.HeaderTester;

import org.junit.Before;
//import org.junit.Ignore;
import org.junit.Test;

/**
 * This class invokes the service described in /wsdl/header_doc_lit.wsdl.
 * This WSDL contains operations with in-out parameters.
 * It sets up the a client in "testColoc()" to send requests to the
 * colco server which is listening on port 9001 (SOAP/HTTP).
 * The subclass defines where CXF configuration and the
 * target server (transport, etc).
 *
 */
public abstract class AbstractHeaderServiceDocLitTest extends AbstractColocTest {
    static final QName SERVICE_NAME = new QName("http://apache.org/headers/doc_lit", "SOAPHeaderService");
    static final QName PORT_NAME = new QName("http://apache.org/headers/doc_lit", "SoapPort9000");
    static final String WSDL_LOCATION = "/wsdl/header_doc_lit.wsdl";

    private HeaderTester service;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        service = getPort(
                         getServiceQname(),
                         getPortQName(),
                         getWsdlLocation(),
                         HeaderTester.class);
    }

    /**
     * The client exercise the service's method here.
     *
     */
    @Test
    public void testInHeaderParts() {
        for (int idx = 0; idx < 2; idx++) {
            verifyInHeaderParts(service);
        }
    }

    @Test
    public void testInOutHeaderParts() {
        for (int idx = 0; idx < 2; idx++) {
            verifyInOutHeaderParts(service);
        }
    }

    @Test
    public void testOutHeaderParts() {
        for (int idx = 0; idx < 2; idx++) {
            verifyOutHeaderParts(service);
        }
    }

    @Test
    public void testAll() {
        for (int idx = 0; idx < 2; idx++) {
            verifyInHeaderParts(service);
            verifyInOutHeaderParts(service);
            verifyOutHeaderParts(service);
        }
    }

    protected void verifyInHeaderParts(HeaderTester ht) {
        getLogger().debug("Client: calling inHeader");
        InHeaderT inHeader = new InHeaderT();
        inHeader.setRequestType(HeaderTesterUtil.IN_REQUEST_TYPE);

        HeaderInfo headerInfo = new HeaderInfo();
        headerInfo.setMessage(HeaderTesterUtil.IN_MESSAGE);
        headerInfo.setOriginator(HeaderTesterUtil.IN_ORIGINATOR);

        InHeaderResponseT inHeaderResponse = ht.inHeader(inHeader, headerInfo);
        assertEquals(HeaderTesterUtil.OUT_RESPONSE_TYPE, inHeaderResponse.getResponseType());
    }

    protected void verifyInOutHeaderParts(HeaderTester ht) {
        getLogger().debug("Client: calling inoutHeader");
        InoutHeaderT inoutHeader = new InoutHeaderT();
        inoutHeader.setRequestType(HeaderTesterUtil.INOUT_REQUEST_TYPE_IN);

        HeaderInfo headerInfo = new HeaderInfo();
        headerInfo.setMessage(HeaderTesterUtil.INOUT_MESSAGE_IN);
        headerInfo.setOriginator(HeaderTesterUtil.INOUT_ORIGINATOR_IN);

        Holder<HeaderInfo> holder  = new Holder<HeaderInfo>();
        holder.value = headerInfo;
        InoutHeaderResponseT inoutHeaderResponse = ht.inoutHeader(inoutHeader, holder);

        assertEquals(HeaderTesterUtil.INOUT_REQUEST_TYPE_OUT,
                     inoutHeaderResponse.getResponseType());
        assertNotSame(HeaderTesterUtil.INOUT_REQUEST_TYPE_OUT, inoutHeader.getRequestType());
        assertEquals(HeaderTesterUtil.INOUT_MESSAGE_OUT, holder.value.getMessage());
        assertEquals(HeaderTesterUtil.INOUT_ORIGINATOR_OUT, holder.value.getOriginator());

    }

    protected void verifyOutHeaderParts(HeaderTester ht) {
        getLogger().debug("Client: calling outHeader");
        OutHeaderT outHeader = new OutHeaderT();
        outHeader.setRequestType(HeaderTesterUtil.OUT_REQUEST_TYPE);

        OutHeaderResponseT theResponse = new OutHeaderResponseT();
        theResponse.setResponseType("bogus");
        Holder<OutHeaderResponseT> respHolder = new Holder<OutHeaderResponseT>();
        respHolder.value = theResponse;

        Holder<HeaderInfo> holder  = new Holder<HeaderInfo>();
        HeaderInfo headerInfo = new HeaderInfo();
        headerInfo.setMessage(HeaderTesterUtil.OUT_MESSAGE_IN);
        headerInfo.setOriginator(HeaderTesterUtil.OUT_ORIGINATOR_IN);
        holder.value = headerInfo;

        ht.outHeader(outHeader, respHolder, holder);
        assertEquals(HeaderTesterUtil.OUT_MESSAGE_OUT, holder.value.getMessage());
        assertEquals(HeaderTesterUtil.OUT_ORIGINATOR_OUT, holder.value.getOriginator());
        assertEquals(HeaderTesterUtil.OUT_RESPONSE_TYPE, respHolder.value.getResponseType());
    }

    protected String getWsdlLocation() {
        return WSDL_LOCATION;
    }

    protected QName getServiceQname() {
        return SERVICE_NAME;
    }

    protected QName getPortQName() {
        return PORT_NAME;
    }

    protected HeaderTester getService() {
        return service;
    }

}
