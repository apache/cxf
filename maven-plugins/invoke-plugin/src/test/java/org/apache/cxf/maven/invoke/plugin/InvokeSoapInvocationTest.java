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
package org.apache.cxf.maven.invoke.plugin;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.handler.MessageContext;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.apache.maven.plugin.MojoExecution;
import org.easymock.EasyMockRule;
import org.easymock.Mock;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.same;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.powermock.api.easymock.PowerMock.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest(InvokeSoap.class)
public class InvokeSoapInvocationTest {

    @Rule
    public EasyMockRule easyMock = new EasyMockRule(this);

    @Rule
    public TemporaryFolder workdir = new TemporaryFolder();

    @Mock
    private Dispatch<Source> dispatch;

    @Mock
    private MojoExecution mojoExecution;

    @Mock
    private Node node;

    @Mock
    private Service service;

    private final QName somePort = new QName("test:namespace", "test-port");

    @Mock
    private Transformer transformer;

    @Test
    public void shouldInvokeService() throws Exception {
        final InvokeSoap invokeSoap = PowerMock.createPartialMock(InvokeSoap.class,
                new String[] {"createService", "determinePort", "shouldRepeat"}, transformer);

        mockStatic(InvokeSoap.class);

        invokeSoap.mojoExecution = mojoExecution;
        invokeSoap.requestPath = workdir.newFolder();
        invokeSoap.request = new Node[] {node};
        invokeSoap.namespace = "uri:namespace";
        invokeSoap.operation = "operation";

        final DOMSource request = new DOMSource(node);
        final DOMSource response = new DOMSource(node);

        expect(InvokeSoap.createRequest(isA(Node.class))).andReturn(request);

        expect(invokeSoap.createService()).andReturn(service);

        expect(invokeSoap.determinePort(service)).andReturn(somePort);

        expect(mojoExecution.getExecutionId()).andReturn("test");

        expect(service.createDispatch(somePort, Source.class, Service.Mode.PAYLOAD)).andReturn(dispatch);

        final Map<String, Object> context = new HashMap<>();
        expect(dispatch.getRequestContext()).andReturn(context);

        expect(dispatch.invoke(isA(DOMSource.class))).andReturn(response);

        transformer.transform(same(request), isA(StreamResult.class));
        expectLastCall().andVoid();

        transformer.transform(same(response), isA(DOMResult.class));
        expectLastCall().andVoid();

        transformer.transform(isA(DOMSource.class), isA(StreamResult.class));
        expectLastCall().andVoid();

        PowerMock.replay(InvokeSoap.class);
        replay(invokeSoap, mojoExecution, service, dispatch, transformer);

        final Document result = invokeSoap.invokeService();

        assertNotNull("Should return result", result);

        assertThat("Operation should be configured in message context", context,
                hasEntry(MessageContext.WSDL_OPERATION, new QName("uri:namespace", "operation")));

        verify(invokeSoap, mojoExecution, service, dispatch, transformer);
    }

}
