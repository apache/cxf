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

package org.apache.cxf.ws.rm;

import java.util.List;

import javax.xml.datatype.Duration;

import org.apache.cxf.jaxb.DatatypeFactory;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.ws.addressing.AddressingPropertiesImpl;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.rm.manager.AcksPolicyType;
import org.apache.cxf.ws.rm.manager.DestinationPolicyType;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class ServantTest extends Assert {
    private static final String SERVICE_URL = "http://localhost:9000/SoapContext/GreeterPort";
    private static final String DECOUPLED_URL = "http://localhost:9990/decoupled_endpoint";

    private static final org.apache.cxf.ws.rm.manager.ObjectFactory RMMANGER_FACTORY = 
        new org.apache.cxf.ws.rm.manager.ObjectFactory();
    private static final Duration DURATION_SHORT = DatatypeFactory.createDuration("PT5S");
    private static final Duration DURATION_VERY_SHORT = DatatypeFactory.createDuration("PT2S");
    private static final Duration DURATION_DEFAULT = DatatypeFactory.createDuration("P0Y0M0DT0H0M0.0S");
    
    private IMocksControl control;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
    }

    @After
    public void tearDown() {
        control.verify();
    }
    
    
    @Test
    public void testCreateSequence() throws SequenceFault {
        RMEndpoint rme = control.createMock(RMEndpoint.class);
        RMManager manager = new RMManager();
        Destination destination = new Destination(rme);
        SequenceIdentifierGenerator generator = manager.new DefaultSequenceIdentifierGenerator();
        manager.setIdGenerator(generator);

        EasyMock.expect(rme.getDestination()).andReturn(destination).anyTimes();
        EasyMock.expect(rme.getManager()).andReturn(manager).anyTimes();

        control.replay();

        Servant servant = new Servant(rme);
        
        verifyCreateSequenceDefault(servant, manager);

        verifyCreateSequenceExpiresSetAtDestination(servant, manager);
        
        verifyCreateSequenceExpiresSetAtSource(servant, manager);
        
        verifyCreateSequenceExpiresSetAtBoth(servant, manager);
        
    }
    
    private void verifyCreateSequenceDefault(Servant servant, RMManager manager) throws SequenceFault {
        DestinationPolicyType dp = RMMANGER_FACTORY.createDestinationPolicyType();
        AcksPolicyType ap = RMMANGER_FACTORY.createAcksPolicyType();
        dp.setAcksPolicy(ap);
        
        manager.setDestinationPolicy(dp);
        
        Expires expires = RMUtils.getWSRMFactory().createExpires();
        expires.setValue(DatatypeFactory.createDuration("P0Y0M0DT0H0M0.0S"));
        Message message = createTestCreateSequenceMessage(expires, null);

        CreateSequenceResponseType csr = servant.createSequence(message);
        
        Expires expires2 = csr.getExpires();
        
        assertNotNull(expires2);
        assertEquals(DatatypeFactory.PT0S, expires2.getValue());
    }

    private void verifyCreateSequenceExpiresSetAtDestination(Servant servant, RMManager manager) 
        throws SequenceFault {
        DestinationPolicyType dp = RMMANGER_FACTORY.createDestinationPolicyType();
        AcksPolicyType ap = RMMANGER_FACTORY.createAcksPolicyType();
        dp.setAcksPolicy(ap);
        dp.setSequenceExpiration(DURATION_SHORT);
        manager.setDestinationPolicy(dp);
        
        Expires expires = RMUtils.getWSRMFactory().createExpires();
        expires.setValue(DURATION_DEFAULT);
        Message message = createTestCreateSequenceMessage(expires, null);

        CreateSequenceResponseType csr = servant.createSequence(message);
        
        Expires expires2 = csr.getExpires();
        
        assertNotNull(expires2);
        assertEquals(DURATION_SHORT, expires2.getValue());
    }

    private void verifyCreateSequenceExpiresSetAtSource(Servant servant, RMManager manager) 
        throws SequenceFault {
        DestinationPolicyType dp = RMMANGER_FACTORY.createDestinationPolicyType();
        AcksPolicyType ap = RMMANGER_FACTORY.createAcksPolicyType();
        dp.setAcksPolicy(ap);
        manager.setDestinationPolicy(dp);
        
        Expires expires = RMUtils.getWSRMFactory().createExpires();
        expires.setValue(DURATION_SHORT);
    
        Message message = createTestCreateSequenceMessage(expires, null);        

        CreateSequenceResponseType csr = servant.createSequence(message);
        
        Expires expires2 = csr.getExpires();
        
        assertNotNull(expires2);
        assertEquals(DURATION_SHORT, expires2.getValue());
    }

    private void verifyCreateSequenceExpiresSetAtBoth(Servant servant, RMManager manager) 
        throws SequenceFault {
        DestinationPolicyType dp = RMMANGER_FACTORY.createDestinationPolicyType();
        AcksPolicyType ap = RMMANGER_FACTORY.createAcksPolicyType();
        dp.setAcksPolicy(ap);
        dp.setSequenceExpiration(DURATION_SHORT);
        manager.setDestinationPolicy(dp);
        
        Expires expires = RMUtils.getWSRMFactory().createExpires();
        expires.setValue(DURATION_VERY_SHORT);
        
        Message message = createTestCreateSequenceMessage(expires, null);        
        
        CreateSequenceResponseType csr = servant.createSequence(message);
        
        Expires expires2 = csr.getExpires();
        
        assertNotNull(expires2);
        assertEquals(DURATION_VERY_SHORT, expires2.getValue());
    }

    private static Message createTestCreateSequenceMessage(Expires expires, OfferType offer) {
        Message message = new MessageImpl();
        Exchange exchange = new ExchangeImpl();
        exchange.setInMessage(message);
//        exchange.setOutMessage(new MessageImpl());

        message.put(Message.REQUESTOR_ROLE, Boolean.FALSE);
        
        AddressingPropertiesImpl maps = new AddressingPropertiesImpl();
        String msgId = "urn:uuid:12345-" + Math.random();
        AttributedURIType id = ContextUtils.getAttributedURI(msgId);
        maps.setMessageID(id);

        maps.setAction(ContextUtils.getAttributedURI(RMConstants.getCreateSequenceAction()));
        maps.setTo(ContextUtils.getAttributedURI(SERVICE_URL));

        maps.setReplyTo(RMUtils.createReference(DECOUPLED_URL));
        
        message.put(JAXWSAConstants.SERVER_ADDRESSING_PROPERTIES_INBOUND, maps);
        
        CreateSequenceType cs = RMUtils.getWSRMFactory().createCreateSequenceType();
        cs.setAcksTo(RMUtils.createReference2004(DECOUPLED_URL));

        cs.setExpires(expires);
        cs.setOffer(offer);
        
        MessageContentsList contents = new MessageContentsList();
        contents.add(cs);
        message.setContent(List.class, contents);
        
        return message;
    }
}
