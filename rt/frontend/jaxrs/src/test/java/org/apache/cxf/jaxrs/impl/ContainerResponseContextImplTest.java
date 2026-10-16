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
package org.apache.cxf.jaxrs.impl;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status.Family;
import jakarta.ws.rs.core.Response.StatusType;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ContainerResponseContextImplTest {

    @Test
    public void testSetStatusInfoKeepsReasonPhrase() {
        ContainerResponseContextImpl ctx = createContext();
        ctx.setStatusInfo(new StatusType() {
            public int getStatusCode() {
                return 499;
            }
            public Family getFamily() {
                return Family.CLIENT_ERROR;
            }
            public String getReasonPhrase() {
                return "Custom Reason";
            }
        });

        assertEquals(499, ctx.getStatus());
        assertEquals(499, ctx.getStatusInfo().getStatusCode());
        assertEquals("Custom Reason", ctx.getStatusInfo().getReasonPhrase());
    }

    @Test
    public void testSetStatusUsesDefaultReasonPhrase() {
        ContainerResponseContextImpl ctx = createContext();
        ctx.setStatus(404);

        assertEquals(404, ctx.getStatusInfo().getStatusCode());
        assertEquals(Response.Status.NOT_FOUND.getReasonPhrase(), ctx.getStatusInfo().getReasonPhrase());
    }

    private static ContainerResponseContextImpl createContext() {
        Message m = new MessageImpl();
        Exchange e = new ExchangeImpl();
        m.setExchange(e);
        e.setOutMessage(m);
        ResponseImpl r = new ResponseImpl(200);
        return new ContainerResponseContextImpl(r, m, null, null);
    }
}
