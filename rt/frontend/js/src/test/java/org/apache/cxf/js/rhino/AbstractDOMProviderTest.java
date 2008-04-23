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


package org.apache.cxf.js.rhino;


import org.easymock.classextension.EasyMock;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Scriptable;


public class AbstractDOMProviderTest extends Assert {

    private String epAddr = "http://cxf.apache.org/";

    private Scriptable scriptMock;

    @Before
    public void setUp() throws Exception {
        scriptMock = EasyMock.createMock(Scriptable.class);
    }

    @Test
    public void testNoWsdlLocation() throws Exception {
        EasyMock.expect(scriptMock.get("wsdlLocation", scriptMock))
            .andReturn(Scriptable.NOT_FOUND);
        EasyMock.replay(scriptMock);
        AbstractDOMProvider adp = new DOMMessageProvider(scriptMock, scriptMock,
                                                         null, false, false);
        try {
            adp.publish();
            fail("expected exception did not occur");
        } catch (AbstractDOMProvider.JSDOMProviderException ex) {
            assertEquals("wrong exception message",
                         AbstractDOMProvider.NO_WSDL_LOCATION, ex.getMessage());
        }
        EasyMock.verify(scriptMock);
    }

    @Test
    public void testNoSvcName() throws Exception {
        EasyMock.expect(scriptMock.get("wsdlLocation", scriptMock))
            .andReturn("found");
        EasyMock.expect(scriptMock.get("serviceName", scriptMock))
            .andReturn(Scriptable.NOT_FOUND);
        EasyMock.replay(scriptMock);
        AbstractDOMProvider adp = new DOMPayloadProvider(scriptMock, scriptMock,
                                                         null, false, false);
        try {
            adp.publish();
            fail("expected exception did not occur");
        } catch (AbstractDOMProvider.JSDOMProviderException ex) {
            assertEquals("wrong exception message",
                         AbstractDOMProvider.NO_SERVICE_NAME, ex.getMessage());
        }
        EasyMock.verify(scriptMock);
    }

    @Test
    public void testNoPortName() throws Exception {
        EasyMock.expect(scriptMock.get("wsdlLocation", scriptMock))
            .andReturn("found");
        EasyMock.expect(scriptMock.get("serviceName", scriptMock))
            .andReturn("found");
        EasyMock.expect(scriptMock.get("portName", scriptMock))
            .andReturn(Scriptable.NOT_FOUND);
        EasyMock.replay(scriptMock);
        AbstractDOMProvider adp = new DOMMessageProvider(scriptMock, scriptMock,
                                                         null, false, false);
        try {
            adp.publish();
            fail("expected exception did not occur");
        } catch (AbstractDOMProvider.JSDOMProviderException ex) {
            assertEquals("wrong exception message",
                         AbstractDOMProvider.NO_PORT_NAME, ex.getMessage());
        }
        EasyMock.verify(scriptMock);
    }

    @Test
    public void testNoTgtNamespace() throws Exception {
        EasyMock.expect(scriptMock.get("wsdlLocation", scriptMock))
            .andReturn("found");
        EasyMock.expect(scriptMock.get("serviceName", scriptMock))
            .andReturn("found");
        EasyMock.expect(scriptMock.get("portName", scriptMock))
            .andReturn("found");
        EasyMock.expect(scriptMock.get("targetNamespace", scriptMock))
            .andReturn(Scriptable.NOT_FOUND);
        EasyMock.replay(scriptMock);
        AbstractDOMProvider adp = new DOMMessageProvider(scriptMock, scriptMock,
                                                         null, false, false);
        try {
            adp.publish();
            fail("expected exception did not occur");
        } catch (AbstractDOMProvider.JSDOMProviderException ex) {
            assertEquals("wrong exception message",
                         AbstractDOMProvider.NO_TGT_NAMESPACE, ex.getMessage());
        }
        EasyMock.verify(scriptMock);
    }

    @Test
    public void testNoAddr() throws Exception {
        EasyMock.expect(scriptMock.get("wsdlLocation", scriptMock))
            .andReturn("found");
        EasyMock.expect(scriptMock.get("serviceName", scriptMock))
            .andReturn("found");
        EasyMock.expect(scriptMock.get("portName", scriptMock))
            .andReturn("found");
        EasyMock.expect(scriptMock.get("targetNamespace", scriptMock))
            .andReturn("found");
        EasyMock.expect(scriptMock.get("EndpointAddress", scriptMock))
            .andReturn(Scriptable.NOT_FOUND);
        EasyMock.replay(scriptMock);
        AbstractDOMProvider adp = new DOMPayloadProvider(scriptMock, scriptMock,
                                                         null, false, false);
        try {
            adp.publish();
            fail("expected exception did not occur");
        } catch (AbstractDOMProvider.JSDOMProviderException ex) {
            assertEquals("wrong exception message",
                         AbstractDOMProvider.NO_EP_ADDR, ex.getMessage());
        }
        EasyMock.verify(scriptMock);
    }

    @Test
    public void testNoInvoke() throws Exception {
        EasyMock.expect(scriptMock.get("wsdlLocation", scriptMock))
            .andReturn("found");
        EasyMock.expect(scriptMock.get("serviceName", scriptMock))
            .andReturn("found");
        EasyMock.expect(scriptMock.get("portName", scriptMock))
            .andReturn("found");
        EasyMock.expect(scriptMock.get("targetNamespace", scriptMock))
            .andReturn("found");
        EasyMock.expect(scriptMock.get("EndpointAddress", scriptMock))
            .andReturn(epAddr);
        EasyMock.expect(scriptMock.get("BindingType", scriptMock))
            .andReturn(Scriptable.NOT_FOUND);
        EasyMock.expect(scriptMock.get("invoke", scriptMock))
            .andReturn(Scriptable.NOT_FOUND);
        EasyMock.replay(scriptMock);
        AbstractDOMProvider adp = new DOMPayloadProvider(scriptMock, scriptMock,
                                                         null, false, false);
        try {
            adp.publish();
            fail("expected exception did not occur");
        } catch (AbstractDOMProvider.JSDOMProviderException ex) {
            assertEquals("wrong exception message",
                         AbstractDOMProvider.NO_INVOKE, ex.getMessage());
        }
        EasyMock.verify(scriptMock);
    }

    @Test
    public void testIllegalInvoke() throws Exception {
        EasyMock.expect(scriptMock.get("wsdlLocation", scriptMock))
            .andReturn("found");
        EasyMock.expect(scriptMock.get("serviceName", scriptMock))
            .andReturn("found");
        EasyMock.expect(scriptMock.get("portName", scriptMock))
            .andReturn("found");
        EasyMock.expect(scriptMock.get("targetNamespace", scriptMock))
            .andReturn("found");
        EasyMock.expect(scriptMock.get("BindingType", scriptMock))
            .andReturn(Scriptable.NOT_FOUND);
        EasyMock.expect(scriptMock.get("invoke", scriptMock))
            .andReturn("string");
        EasyMock.replay(scriptMock);
        AbstractDOMProvider adp = new DOMMessageProvider(scriptMock, scriptMock,
                                                         epAddr, true, true);
        try {
            adp.publish();
            fail("expected exception did not occur");
        } catch (AbstractDOMProvider.JSDOMProviderException ex) {
            assertEquals("wrong exception message",
                         AbstractDOMProvider.ILLEGAL_INVOKE_TYPE, ex.getMessage());
        }
        EasyMock.verify(scriptMock);
    }
}
