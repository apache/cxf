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

import org.mozilla.javascript.Scriptable;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class AbstractDOMProviderTest {

    private String epAddr = "http://cxf.apache.org/";

    private Scriptable scriptMock;

    @Before
    public void setUp() throws Exception {
        scriptMock = mock(Scriptable.class);
    }

    @Test
    public void testNoWsdlLocation() throws Exception {
        when(scriptMock.get("wsdlLocation", scriptMock))
            .thenReturn(Scriptable.NOT_FOUND);

        AbstractDOMProvider adp = new DOMMessageProvider(scriptMock, scriptMock,
                                                         null, false, false);
        try {
            adp.publish();
            fail("expected exception did not occur");
        } catch (AbstractDOMProvider.JSDOMProviderException ex) {
            assertEquals("wrong exception message",
                         AbstractDOMProvider.NO_WSDL_LOCATION, ex.getMessage());
        }
    }

    @Test
    public void testNoSvcName() throws Exception {
        when(scriptMock.get("wsdlLocation", scriptMock))
            .thenReturn("found");
        when(scriptMock.get("serviceName", scriptMock))
            .thenReturn(Scriptable.NOT_FOUND);

        AbstractDOMProvider adp = new DOMPayloadProvider(scriptMock, scriptMock,
                                                         null, false, false);
        try {
            adp.publish();
            fail("expected exception did not occur");
        } catch (AbstractDOMProvider.JSDOMProviderException ex) {
            assertEquals("wrong exception message",
                         AbstractDOMProvider.NO_SERVICE_NAME, ex.getMessage());
        }
    }

    @Test
    public void testNoPortName() throws Exception {
        when(scriptMock.get("wsdlLocation", scriptMock))
            .thenReturn("found");
        when(scriptMock.get("serviceName", scriptMock))
            .thenReturn("found");
        when(scriptMock.get("portName", scriptMock))
            .thenReturn(Scriptable.NOT_FOUND);

        AbstractDOMProvider adp = new DOMMessageProvider(scriptMock, scriptMock,
                                                         null, false, false);
        try {
            adp.publish();
            fail("expected exception did not occur");
        } catch (AbstractDOMProvider.JSDOMProviderException ex) {
            assertEquals("wrong exception message",
                         AbstractDOMProvider.NO_PORT_NAME, ex.getMessage());
        }
    }

    @Test
    public void testNoTgtNamespace() throws Exception {
        when(scriptMock.get("wsdlLocation", scriptMock))
            .thenReturn("found");
        when(scriptMock.get("serviceName", scriptMock))
            .thenReturn("found");
        when(scriptMock.get("portName", scriptMock))
            .thenReturn("found");
        when(scriptMock.get("targetNamespace", scriptMock))
            .thenReturn(Scriptable.NOT_FOUND);

        AbstractDOMProvider adp = new DOMMessageProvider(scriptMock, scriptMock,
                                                         null, false, false);
        try {
            adp.publish();
            fail("expected exception did not occur");
        } catch (AbstractDOMProvider.JSDOMProviderException ex) {
            assertEquals("wrong exception message",
                         AbstractDOMProvider.NO_TGT_NAMESPACE, ex.getMessage());
        }
    }

    @Test
    public void testNoAddr() throws Exception {
        when(scriptMock.get("wsdlLocation", scriptMock))
            .thenReturn("found");
        when(scriptMock.get("serviceName", scriptMock))
            .thenReturn("found");
        when(scriptMock.get("portName", scriptMock))
            .thenReturn("found");
        when(scriptMock.get("targetNamespace", scriptMock))
            .thenReturn("found");
        when(scriptMock.get("EndpointAddress", scriptMock))
            .thenReturn(Scriptable.NOT_FOUND);

        AbstractDOMProvider adp = new DOMPayloadProvider(scriptMock, scriptMock,
                                                         null, false, false);
        try {
            adp.publish();
            fail("expected exception did not occur");
        } catch (AbstractDOMProvider.JSDOMProviderException ex) {
            assertEquals("wrong exception message",
                         AbstractDOMProvider.NO_EP_ADDR, ex.getMessage());
        }
    }

    @Test
    public void testNoInvoke() throws Exception {
        when(scriptMock.get("wsdlLocation", scriptMock))
            .thenReturn("found");
        when(scriptMock.get("serviceName", scriptMock))
            .thenReturn("found");
        when(scriptMock.get("portName", scriptMock))
            .thenReturn("found");
        when(scriptMock.get("targetNamespace", scriptMock))
            .thenReturn("found");
        when(scriptMock.get("EndpointAddress", scriptMock))
            .thenReturn(epAddr);
        when(scriptMock.get("BindingType", scriptMock))
            .thenReturn(Scriptable.NOT_FOUND);
        when(scriptMock.get("invoke", scriptMock))
            .thenReturn(Scriptable.NOT_FOUND);

        AbstractDOMProvider adp = new DOMPayloadProvider(scriptMock, scriptMock,
                                                         null, false, false);
        try {
            adp.publish();
            fail("expected exception did not occur");
        } catch (AbstractDOMProvider.JSDOMProviderException ex) {
            assertEquals("wrong exception message",
                         AbstractDOMProvider.NO_INVOKE, ex.getMessage());
        }
    }

    @Test
    public void testIllegalInvoke() throws Exception {
        when(scriptMock.get("wsdlLocation", scriptMock))
            .thenReturn("found");
        when(scriptMock.get("serviceName", scriptMock))
            .thenReturn("found");
        when(scriptMock.get("portName", scriptMock))
            .thenReturn("found");
        when(scriptMock.get("targetNamespace", scriptMock))
            .thenReturn("found");
        when(scriptMock.get("BindingType", scriptMock))
            .thenReturn(Scriptable.NOT_FOUND);
        when(scriptMock.get("invoke", scriptMock))
            .thenReturn("string");

        AbstractDOMProvider adp = new DOMMessageProvider(scriptMock, scriptMock,
                                                         epAddr, true, true);
        try {
            adp.publish();
            fail("expected exception did not occur");
        } catch (AbstractDOMProvider.JSDOMProviderException ex) {
            assertEquals("wrong exception message",
                         AbstractDOMProvider.ILLEGAL_INVOKE_TYPE, ex.getMessage());
        }
    }
}