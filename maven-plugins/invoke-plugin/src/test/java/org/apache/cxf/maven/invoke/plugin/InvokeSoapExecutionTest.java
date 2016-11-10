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

import javax.xml.transform.Transformer;

import org.w3c.dom.Document;

import org.easymock.EasyMockRule;
import org.easymock.Mock;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

@RunWith(PowerMockRunner.class)
@PrepareForTest(InvokeSoap.class)
public class InvokeSoapExecutionTest {

    @Rule
    public EasyMockRule easyMock = new EasyMockRule(this);

    @Mock
    private Document document;

    @Mock
    private Transformer transformer;

    @Test
    public void shouldExecuteOneTime() throws Exception {
        final InvokeSoap invokeSoap = PowerMock.createPartialMock(InvokeSoap.class,
                new String[] {"invokeService", "extractProperties", "shouldRepeat"}, transformer);

        expect(invokeSoap.invokeService()).andReturn(document);
        expect(invokeSoap.shouldRepeat(document)).andReturn(false);
        invokeSoap.extractProperties(document);
        expectLastCall().andVoid();

        PowerMock.replay(invokeSoap);

        invokeSoap.execute();

        PowerMock.verify(invokeSoap);
    }

    @Test
    public void shouldExecuteUntilRepeatConditionReturnsFalse() throws Exception {
        final InvokeSoap invokeSoap = PowerMock.createPartialMock(InvokeSoap.class,
                new String[] {"invokeService", "extractProperties", "shouldRepeat"}, transformer);

        expect(invokeSoap.invokeService()).andReturn(document).times(3);
        expect(invokeSoap.shouldRepeat(document)).andReturn(true).andReturn(true).andReturn(false);
        invokeSoap.extractProperties(document);
        expectLastCall().andVoid();

        PowerMock.replay(invokeSoap);

        invokeSoap.execute();

        PowerMock.verify(invokeSoap);
    }

}
