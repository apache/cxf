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

package org.apache.cxf.transport.jms;

import javax.jms.Destination;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.easymock.classextension.EasyMock;
import org.junit.Assert;
import org.junit.Test;

public class PooledSessionTest extends Assert {

    @Test
    public void testPooledSession() throws Exception {
            
        Session sess =  EasyMock.createMock(Session.class);
        Destination dest = EasyMock.createMock(Destination.class);
        MessageProducer mproducer = EasyMock.createMock(MessageProducer.class);
        MessageConsumer mconsumer = EasyMock.createMock(MessageConsumer.class);
       
        PooledSession ps = new PooledSession(sess, dest, mproducer, mconsumer);
       
        assertTrue(ps.session().equals(sess));
        assertTrue(ps.destination().equals(dest));
        assertTrue(ps.consumer().equals(mconsumer));
        assertTrue(ps.producer().equals(mproducer));    
         
        MessageConsumer mcons = EasyMock.createMock(MessageConsumer.class);
        assertFalse(mconsumer.equals(mcons));
         
        ps.consumer(mcons);
         
        assertTrue(ps.consumer().equals(mcons));
         
        Destination mdest = EasyMock.createMock(Destination.class);
        assertFalse(dest.equals(mdest));
        
        ps.destination(mdest);
        assertTrue(mdest.equals(ps.destination()));
    }    
}
