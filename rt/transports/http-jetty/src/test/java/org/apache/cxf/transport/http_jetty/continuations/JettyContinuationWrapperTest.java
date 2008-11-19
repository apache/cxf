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

package org.apache.cxf.transport.http_jetty.continuations;

import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.continuations.ContinuationInfo;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.easymock.classextension.EasyMock;
import org.junit.Assert;
import org.junit.Test;
import org.mortbay.util.ajax.Continuation;

public class JettyContinuationWrapperTest extends Assert {

    @Test
    public void testContinuationInterface() {
        Message m = new MessageImpl();
        ContinuationInfo ci = new ContinuationInfo(m);
        Object userObject = new Object();
        
               
        Continuation c = EasyMock.createMock(Continuation.class);
        c.isNew();
        EasyMock.expectLastCall().andReturn(true);
        c.isPending();
        EasyMock.expectLastCall().andReturn(true);
        c.isResumed();
        EasyMock.expectLastCall().andReturn(true);
        c.getObject();
        EasyMock.expectLastCall().andReturn(ci).times(3);
        
        c.setObject(ci);
        EasyMock.expectLastCall();
        c.reset();
        EasyMock.expectLastCall();
        c.resume();
        EasyMock.expectLastCall();
        c.suspend(100);
        EasyMock.expectLastCall().andReturn(true);
        EasyMock.replay(c);
        
        HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
        request.getAttribute("org.mortbay.jetty.ajax.Continuation");
        EasyMock.expectLastCall().andReturn(c);
        EasyMock.replay(request);
        
        JettyContinuationWrapper cw = new JettyContinuationWrapper(request, m);
        cw.isNew();
        cw.isPending();
        cw.isResumed();
        assertSame(ci, cw.getObject());
        cw.setObject(userObject);
        cw.reset();
        cw.resume();
        cw.suspend(100);
        EasyMock.verify(c);
        assertSame(userObject, ci.getUserObject());
    }
    
}
