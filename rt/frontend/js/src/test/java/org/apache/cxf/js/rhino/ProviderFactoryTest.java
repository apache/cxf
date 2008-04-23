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

import java.io.File;

import javax.xml.ws.Service;


import org.easymock.classextension.EasyMock;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Scriptable;


public class ProviderFactoryTest extends Assert {

    private String epAddr = "http://cxf.apache.org/";

    private ProviderFactory ph;
    private AbstractDOMProvider dpMock;

    @Before
    public void setUp() throws Exception {
        dpMock = EasyMock.createMock(AbstractDOMProvider.class);
        ph = new ProviderFactory(epAddr) {
                protected AbstractDOMProvider createProvider(Service.Mode mode,
                                                             Scriptable scope,
                                                             Scriptable wspVar,
                                                             String epAddress,
                                                             boolean isBase,
                                                             boolean e4x)
                    throws Exception {
                    return dpMock;
                }
            };
    }

    @Test
    public void testMsgJSFile() throws Exception {
        dpMock.publish();
        dpMock.publish();
        EasyMock.replay(dpMock);
        File f = new File(getClass().getResource("msg.js").toURI().getPath());
        ph.createAndPublish(f);
        EasyMock.verify(dpMock);
    }

    @Test
    public void testBadJSFile() throws Exception {
        EasyMock.replay(dpMock);
        final String fname = "broken.js";
        File f = new File(getClass().getResource(fname).toURI().getPath());
        try {
            ph.createAndPublish(f);
            fail("expected exception did not occur");
        } catch (EvaluatorException ex) {
            assertTrue("wrong exception", ex.getMessage().startsWith("syntax error")
                                       || ex.getMessage().startsWith("erreur de syntaxe"));
        }
        EasyMock.verify(dpMock);
    }

    @Test
    public void testEmptyJSFile() throws Exception {
        EasyMock.replay(dpMock);
        final String fname = "empty.js";
        File f = new File(getClass().getResource(fname).toURI().getPath());
        try {
            ph.createAndPublish(f);
            fail("expected exception did not occur");
        } catch (Exception ex) {
            assertEquals("wrong exception message",
                         f.getPath() + ProviderFactory.NO_PROVIDER,
                         ex.getMessage());
        }
        EasyMock.verify(dpMock);
    }

    @Test
    public void testNoSuchJSFile() throws Exception {
        EasyMock.replay(dpMock);
        final String fname = "none.js";
        File f = new File(fname);
        try {
            ph.createAndPublish(f);
            fail("expected exception did not occur");
        } catch (Exception ex) {
            assertEquals("wrong exception message",
                         f.getPath() + ProviderFactory.NO_SUCH_FILE,
                         ex.getMessage());
        }
        EasyMock.verify(dpMock);
    }

    @Test
    public void testIllegalServiceMode() throws Exception {
        EasyMock.replay(dpMock);
        final String fname = "illegal1.js";
        File f = new File(getClass().getResource(fname).toURI().getPath());
        try {
            ph.createAndPublish(f);
            fail("expected exception did not occur");
        } catch (Exception ex) {
            assertEquals("wrong exception message",
                         f.getPath() + ProviderFactory.ILLEGAL_SVCMD_MODE + "bogus",
                         ex.getMessage());
        }
        EasyMock.verify(dpMock);
    }

    @Test
    public void testIllegalServiceModeType() throws Exception {
        EasyMock.replay(dpMock);
        final String fname = "illegal2.js";
        File f = new File(getClass().getResource(fname).toURI().getPath());
        try {
            ph.createAndPublish(f);
            fail("expected exception did not occur");
        } catch (Exception ex) {
            assertEquals("wrong exception message",
                         f.getPath() + ProviderFactory.ILLEGAL_SVCMD_TYPE,
                         ex.getMessage());
        }
        EasyMock.verify(dpMock);
    }

    @Test
    public void testProviderException() throws Exception {
        dpMock.publish();
        EasyMock.expectLastCall()
            .andThrow(new AbstractDOMProvider.JSDOMProviderException(AbstractDOMProvider.NO_EP_ADDR));
        EasyMock.replay(dpMock);
        File f = new File(getClass().getResource("msg.js").toURI().getPath());
        try {
            ph.createAndPublish(f);
            fail("expected exception did not occur");
        } catch (Exception ex) {
            assertEquals("wrong exception message",
                         f.getPath() + ": " + AbstractDOMProvider.NO_EP_ADDR,
                         ex.getMessage());
        }
        EasyMock.verify(dpMock);
    }
}
