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

package org.apache.cxf.ws.rm.soap;

import java.math.BigInteger;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapBinding;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.ws.rm.Identifier;
import org.apache.cxf.ws.rm.RMConstants;
import org.apache.cxf.ws.rm.SequenceAcknowledgement;
import org.apache.cxf.ws.rm.SequenceFault;
import org.apache.cxf.ws.rm.SequenceFaultType;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class SoapFaultFactoryTest extends Assert {

    private IMocksControl control;
    private SequenceFault sf;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl(); 
    }
    
    SequenceFault setupSequenceFault(boolean isSender, QName code, Object detail) {
        sf = control.createMock(SequenceFault.class);
        EasyMock.expect(sf.getReason()).andReturn("reason");
        EasyMock.expect(sf.isSender()).andReturn(isSender);
        EasyMock.expect(sf.getSubCode()).andReturn(code);
        if (null != detail) {
            EasyMock.expect(sf.getDetail()).andReturn(detail);
            SequenceFaultType sft = new SequenceFaultType();
            sft.setFaultCode(RMConstants.getUnknownSequenceFaultCode());
        }
        return sf;
    }
    
    @Test 
    public void createSoap11Fault() {
        SoapBinding sb = control.createMock(SoapBinding.class);
        EasyMock.expect(sb.getSoapVersion()).andReturn(Soap11.getInstance());        
        setupSequenceFault(false, RMConstants.getSequenceTerminatedFaultCode(), null);
        control.replay();
        SoapFaultFactory factory = new SoapFaultFactory(sb);
        SoapFault fault = (SoapFault)factory.createFault(sf);
        assertEquals("reason", fault.getReason());
        assertEquals(Soap11.getInstance().getReceiver(), fault.getFaultCode());
        assertEquals(RMConstants.getSequenceTerminatedFaultCode(), fault.getSubCode());
        assertNull(fault.getDetail());
        assertSame(sf, fault.getCause());
        control.verify();        
    }
    
    @Test 
    public void createSoap12Fault() {
        SoapBinding sb = control.createMock(SoapBinding.class);
        EasyMock.expect(sb.getSoapVersion()).andReturn(Soap12.getInstance());        
        Identifier id = new Identifier();
        id.setValue("sid");
        setupSequenceFault(true, RMConstants.getUnknownSequenceFaultCode(), id);        
        control.replay();
        SoapFaultFactory factory = new SoapFaultFactory(sb);
        SoapFault fault = (SoapFault)factory.createFault(sf);
        assertEquals("reason", fault.getReason());
        assertEquals(Soap12.getInstance().getSender(), fault.getFaultCode());
        assertEquals(RMConstants.getUnknownSequenceFaultCode(), fault.getSubCode());
        Element elem = fault.getDetail();
        assertEquals(RMConstants.getNamespace(), elem.getNamespaceURI());
        assertEquals("Identifier", elem.getLocalName());
        assertNull(fault.getCause());
        control.verify();        
    }
    
    @Test 
    public void createSoap12FaultWithIdentifierDetail() {
        SoapBinding sb = control.createMock(SoapBinding.class);
        EasyMock.expect(sb.getSoapVersion()).andReturn(Soap12.getInstance());        
        Identifier id = new Identifier();
        id.setValue("sid");
        setupSequenceFault(true, RMConstants.getUnknownSequenceFaultCode(), id);        
        control.replay();
        SoapFaultFactory factory = new SoapFaultFactory(sb);
        SoapFault fault = (SoapFault)factory.createFault(sf);
        assertEquals("reason", fault.getReason());
        assertEquals(Soap12.getInstance().getSender(), fault.getFaultCode());
        assertEquals(RMConstants.getUnknownSequenceFaultCode(), fault.getSubCode());
        Element elem = fault.getDetail();
        assertEquals(RMConstants.getNamespace(), elem.getNamespaceURI());
        assertEquals("Identifier", elem.getLocalName());
        control.verify();        
    }
    
    @Test 
    public void createSoap12FaultWithAcknowledgementDetail() {
        SoapBinding sb = control.createMock(SoapBinding.class);
        EasyMock.expect(sb.getSoapVersion()).andReturn(Soap12.getInstance());        
        SequenceAcknowledgement ack = new SequenceAcknowledgement();
        Identifier id = new Identifier();
        id.setValue("sid");
        ack.setIdentifier(id);
        SequenceAcknowledgement.AcknowledgementRange range = 
            new SequenceAcknowledgement.AcknowledgementRange();
        range.setLower(BigInteger.ONE);
        range.setUpper(BigInteger.TEN);
        ack.getAcknowledgementRange().add(range);   
        setupSequenceFault(true, RMConstants.getInvalidAcknowledgmentFaultCode(), ack);        
        control.replay();
        SoapFaultFactory factory = new SoapFaultFactory(sb);
        SoapFault fault = (SoapFault)factory.createFault(sf);
        assertEquals("reason", fault.getReason());
        assertEquals(Soap12.getInstance().getSender(), fault.getFaultCode());
        assertEquals(RMConstants.getInvalidAcknowledgmentFaultCode(), fault.getSubCode());
        Element elem = fault.getDetail();
        assertEquals(RMConstants.getNamespace(), elem.getNamespaceURI());
        assertEquals("SequenceAcknowledgement", elem.getLocalName());
        control.verify();        
    }
    
    @Test 
    public void createSoap12FaultWithoutDetail() {
        SoapBinding sb = control.createMock(SoapBinding.class);
        EasyMock.expect(sb.getSoapVersion()).andReturn(Soap12.getInstance());
        setupSequenceFault(true, RMConstants.getCreateSequenceRefusedFaultCode(), null);        
        control.replay();
        SoapFaultFactory factory = new SoapFaultFactory(sb);
        SoapFault fault = (SoapFault)factory.createFault(sf);
        assertEquals("reason", fault.getReason());
        assertEquals(Soap12.getInstance().getSender(), fault.getFaultCode());
        assertEquals(RMConstants.getCreateSequenceRefusedFaultCode(), fault.getSubCode());
        assertNull(fault.getDetail());

        control.verify();        
    }
    
    @Test
    public void testToString() {
        SoapBinding sb = control.createMock(SoapBinding.class);
        EasyMock.expect(sb.getSoapVersion()).andReturn(Soap11.getInstance());
        SoapFault fault = control.createMock(SoapFault.class);
        EasyMock.expect(fault.getReason()).andReturn("r");
        EasyMock.expect(fault.getFaultCode()).andReturn(new QName("ns", "code"));
        EasyMock.expect(fault.getSubCode()).andReturn(new QName("ns", "subcode"));
        control.replay();
        SoapFaultFactory factory = new SoapFaultFactory(sb);
        assertEquals("Reason: r, code: {ns}code, subCode: {ns}subcode",
                     factory.toString(fault));
        control.verify();
        
    }
}
