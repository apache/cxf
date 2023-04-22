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
package org.apache.cxf.jaxrs.utils;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;



public class ExceptionUtilsTest {

    @Test
    public void testConvertFaultToResponseWAEWithResponse() {
        Message m = createMessage();
        WebApplicationException wae = new WebApplicationException(Response.ok("_fromWAE_", 
                                                                              MediaType.TEXT_PLAIN_TYPE).build());
        Response r = ExceptionUtils.convertFaultToResponse(wae, m);
        assertEquals(200, r.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, r.getMediaType());
        assertEquals("_fromWAE_", r.readEntity(String.class));
    }

    @Test
    public void testConvertFaultToResponseWAESubClassWithResponse() {
        Message m = createMessage();
        BadRequestException wae = new BadRequestException(Response.status(400)
                                                                  .type(MediaType.TEXT_HTML)
                                                                  .entity("<em>fromBRE</em>")
                                                                  .build());
        Response r = ExceptionUtils.convertFaultToResponse(wae, m);
        assertEquals(400, r.getStatus());
        assertEquals(MediaType.TEXT_HTML_TYPE, r.getMediaType());
        assertEquals("<em>fromBRE</em>", r.readEntity(String.class));
    }

    private Message createMessage() {
        Message m = new MessageImpl();
        Exchange e = new ExchangeImpl();
        m.setExchange(e);
        e.setInMessage(m);
        e.put("org.apache.cxf.jaxrs.provider.ServerProviderFactory", ServerProviderFactory.getInstance());
        return m;
    }
}