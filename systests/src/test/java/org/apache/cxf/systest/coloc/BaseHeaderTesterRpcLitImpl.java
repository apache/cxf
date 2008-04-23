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

import java.util.ResourceBundle;

import javax.xml.ws.Holder;

import static junit.framework.Assert.assertEquals;

import org.apache.commons.logging.Log;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.interceptor.Fault;
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

public class BaseHeaderTesterRpcLitImpl implements HeaderTester {
    private Log logger;
    
    public InHeaderResponseT inHeader(InHeaderT in, HeaderInfo header) {
        getLogger().debug("Server: inHeader called");
        assertEquals(HeaderTesterUtil.IN_REQUEST_TYPE, in.getRequestType());
        assertEquals(HeaderTesterUtil.IN_MESSAGE, header.getMessage());
        assertEquals(HeaderTesterUtil.IN_ORIGINATOR, header.getOriginator());

        InHeaderResponseT inHeaderResponse = new InHeaderResponseT();
        inHeaderResponse.setResponseType(HeaderTesterUtil.OUT_RESPONSE_TYPE);
        return inHeaderResponse;
    }
    
    public InoutHeaderResponseT inoutHeader(InoutHeaderT in, Holder<HeaderInfo> header) {
        getLogger().debug("Server: inoutHeader called");
        assertEquals(HeaderTesterUtil.INOUT_REQUEST_TYPE_IN, in.getRequestType());
        assertEquals(HeaderTesterUtil.INOUT_MESSAGE_IN, header.value.getMessage());
        assertEquals(HeaderTesterUtil.INOUT_ORIGINATOR_IN, header.value.getOriginator());
        
        HeaderInfo out = new HeaderInfo();
        out.setMessage(HeaderTesterUtil.INOUT_MESSAGE_OUT);
        out.setOriginator(HeaderTesterUtil.INOUT_ORIGINATOR_OUT);
        header.value = out;
        
        InoutHeaderResponseT inoutHeaderResponse = new InoutHeaderResponseT();
        inoutHeaderResponse.setResponseType(HeaderTesterUtil.INOUT_REQUEST_TYPE_OUT);
        return inoutHeaderResponse;
    }
    
    public void outHeader(OutHeaderT in, Holder<OutHeaderResponseT> out, Holder<HeaderInfo> header) {
        getLogger().debug("Server: outHeader called");
        assertEquals(HeaderTesterUtil.OUT_REQUEST_TYPE, in.getRequestType());
        
        HeaderInfo outHeader = new HeaderInfo();
        outHeader.setMessage(HeaderTesterUtil.OUT_MESSAGE_OUT);
        outHeader.setOriginator(HeaderTesterUtil.OUT_ORIGINATOR_OUT);
        header.value = outHeader;
        
        OutHeaderResponseT resp = new OutHeaderResponseT();
        resp.setResponseType(HeaderTesterUtil.OUT_RESPONSE_TYPE);
        out.value = resp;
    }
    
    public PingMeResponseT pingMe(PingMeT in) throws PingMeFault {
        String msgType = in.getFaultType();
        getLogger().debug("Server: in pingMe:" + msgType);
        if ("USER".equals(msgType)) {

            FaultDetailT detail = new FaultDetailT();
            detail.setMajor((short)1);
            detail.setMinor((short)2);
            throw new PingMeFault("USER FAULT TEST", detail);
        } else if ("SYSTEM".equals(msgType)) {
            throw new Fault(new Message(HeaderTesterUtil.EX_STRING,
                                        (ResourceBundle)null,
                                        new Object[]{"FAULT TEST"}));
        } else if ("RUNTIME".equals(msgType)) {
            throw new IllegalArgumentException(HeaderTesterUtil.EX_STRING);
        } 
        
        return new PingMeResponseT();
    }
    
    public void init(Log log) {
        logger = log;
    }
    
    protected Log getLogger() {
        return logger;
    }    
}