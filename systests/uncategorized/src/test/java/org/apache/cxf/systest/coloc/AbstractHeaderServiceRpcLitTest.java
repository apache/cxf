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
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceException;

import static junit.framework.Assert.assertEquals;

import org.apache.cxf.message.Message;
import org.apache.headers.coloc.types.FaultDetailT;
import org.apache.headers.coloc.types.HeaderInfo;
import org.apache.headers.coloc.types.InHeaderResponseT;
import org.apache.headers.coloc.types.InHeaderT;
import org.apache.headers.coloc.types.InoutHeaderResponseT;
import org.apache.headers.coloc.types.InoutHeaderT;
import org.apache.headers.coloc.types.OutHeaderResponseT;
import org.apache.headers.coloc.types.OutHeaderT;
import org.apache.headers.coloc.types.PingMeResponseT;
import org.apache.headers.coloc.types.PingMeT;
import org.apache.headers.rpc_lit.HeaderTester;
import org.apache.headers.rpc_lit.PingMeFault;

import org.junit.Before;
//import org.junit.Ignore;
import org.junit.Test;

/**
 * This class invokes the service described in /wsdl/header_rpc_lit.wsdl.
 * This WSDL contains operations with in-out parameters.
 * It sets up the a client in "testRouter()" to send requests to the
 * router which is listening on port 9001 (SOAP/HTTP).
 * The subclass defines where CXF configuration and the
 * target server (transport, etc).
 *
 */
public abstract class AbstractHeaderServiceRpcLitTest extends AbstractColocTest {
    static final QName SERVICE_NAME = new QName("http://apache.org/headers/rpc_lit",
                                                "SOAPHeaderService");
    static final QName PORT_NAME = new QName("http://apache.org/headers/rpc_lit", 
                                             "SoapPort");    
    static final String WSDL_LOCATION = "/wsdl/header_rpc_lit.wsdl";

    private HeaderTester port;
    
    @Before
    public void setUp() throws Exception {
        super.setUp();
        port = getPort(
                         getServiceQname(),
                         getPortQName(),
                         getWsdlLocation(),
                         HeaderTester.class);
    }

    @Test
    public void testTwoWayOperation() {
        for (int idx = 0; idx < 2; idx++) {
            verifyTwoWay(port);
        }
    }
    
    @Test
    public void testInHeaderParts() {
        for (int idx = 0; idx < 2; idx++) {
            verifyInHeaderParts(port);
        }
    }

    @Test
    public void testInOutHeaderParts() {
        for (int idx = 0; idx < 2; idx++) {
            verifyInOutHeaderParts(port);
        }
    }

    @Test
    public void testOutHeaderParts() {
        for (int idx = 0; idx < 2; idx++) {
            verifyOutHeaderParts(port);
        }
    }

    @Test
    public void testFault() {
        for (int idx = 0; idx < 2; idx++) {
            verifyFaults(port);
        }
    }
    
    @Test
    public void testAll() {
        for (int idx = 0; idx < 2; idx++) {
            verifyTwoWay(port);
            verifyInHeaderParts(port);
            verifyInOutHeaderParts(port);
            verifyOutHeaderParts(port);
            verifyFaults(port);
        }
    }

    public void verifyTwoWay(HeaderTester ht) {
        getLogger().debug("Client: calling pingMe");
        PingMeT in = new PingMeT();
        try {
            in.setFaultType("ABCD");
            PingMeResponseT ret = ht.pingMe(in);
            assertNotNull(ret);
        } catch (Exception ex) {
            fail("Should not throw any exception");
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
        assertNotSame(HeaderTesterUtil.INOUT_REQUEST_TYPE_OUT,
                      inoutHeader.getRequestType());
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

    public void verifyFaults(HeaderTester ht) {
        getLogger().debug("Client: calling pingMe user fault");
        PingMeT in = new PingMeT();
        
        try {
            in.setFaultType("USER");
            ht.pingMe(in);
            fail("Should throw a PingeMeFault exception");
        } catch (PingMeFault pmf) {
            FaultDetailT detail = pmf.getFaultInfo();
            assertNotNull(detail);
            assertEquals("Major Version should be 1", (short)1, detail.getMajor());
            assertEquals("Minor Version should be 2", (short)2, detail.getMinor());
            if (isFaultCodeCheckEnabled()) {
                verifyFaultCode(port);
            }
        }

        getLogger().debug("Client: calling pingMe Cxf System Fault");
        try {
            in.setFaultType("SYSTEM");
            ht.pingMe(in);
            fail("Should throw a CXF Fault exception");
        } catch (WebServiceException fault) {
            assertFalse("Wrong message: " + fault.getMessage(),
                        -1 == fault.getMessage().lastIndexOf(HeaderTesterUtil.EX_STRING));
            if (isFaultCodeCheckEnabled()) {
                verifyFaultCode(port);
            }
        } catch (PingMeFault pmf) {
            fail("Should not receive PingMefault");
        }

        getLogger().debug("Client: calling pingMe java runtime exception");
        try {
            in.setFaultType("RUNTIME");
            ht.pingMe(in);
            fail("Should throw a CXF Fault exception");
        } catch (WebServiceException fault) {
            assertFalse(-1 == fault.getMessage().lastIndexOf(HeaderTesterUtil.EX_STRING));
            if (isFaultCodeCheckEnabled()) {
                verifyFaultCode(port);
            }
        } catch (PingMeFault pmf) {
            fail("Should not receive PingMefault");
        }
    }

    protected void verifyFaultCode(HeaderTester proxy) {
        BindingProvider bp = (BindingProvider)proxy;
        java.util.Map<String, Object> respCtx = bp.getResponseContext();
        assertNotNull(respCtx);
        Integer val = (Integer)respCtx.get(Message.RESPONSE_CODE);
        assertNotNull(val);
        assertEquals("Message.RESPONSE_CODE should be 500", 500,  val.intValue());
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
    
    protected boolean isFaultCodeCheckEnabled() {
        return false;
    }
}
