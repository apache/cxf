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

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapBinding;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.rm.RM10Constants;
import org.apache.cxf.ws.rm.RMContextUtils;
import org.apache.cxf.ws.rm.RMProperties;
import org.apache.cxf.ws.rm.SequenceFault;
import org.apache.cxf.ws.rm.v200702.Identifier;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;
import org.apache.cxf.ws.rm.v200702.SequenceFaultType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class SoapFaultFactoryTest {

    private SequenceFault sf;

    SequenceFault setupSequenceFault(boolean isSender, QName code, Object detail) {
        sf = mock(SequenceFault.class);
        when(sf.getReason()).thenReturn("reason");
        when(sf.isSender()).thenReturn(isSender);
        when(sf.getFaultCode()).thenReturn(code);
        if (null != detail) {
            when(sf.getDetail()).thenReturn(detail);
            SequenceFaultType sft = new SequenceFaultType();
            sft.setFaultCode(RM10Constants.UNKNOWN_SEQUENCE_FAULT_QNAME);
        }
        return sf;
    }

    @Test
    public void createSoap11Fault() {
        SoapBinding sb = mock(SoapBinding.class);
        when(sb.getSoapVersion()).thenReturn(Soap11.getInstance());
        setupSequenceFault(false, RM10Constants.SEQUENCE_TERMINATED_FAULT_QNAME, null);

        SoapFaultFactory factory = new SoapFaultFactory(sb);
        SoapFault fault = (SoapFault)factory.createFault(sf, createInboundMessage());
        assertEquals("reason", fault.getReason());
        assertEquals(Soap11.getInstance().getReceiver(), fault.getFaultCode());
        assertNull(fault.getSubCode());
        assertNull(fault.getDetail());
        assertSame(sf, fault.getCause());
    }

    @Test
    public void createSoap12Fault() {
        SoapBinding sb = mock(SoapBinding.class);
        when(sb.getSoapVersion()).thenReturn(Soap12.getInstance());
        Identifier id = new Identifier();
        id.setValue("sid");
        setupSequenceFault(true, RM10Constants.UNKNOWN_SEQUENCE_FAULT_QNAME, id);

        SoapFaultFactory factory = new SoapFaultFactory(sb);
        SoapFault fault = (SoapFault)factory.createFault(sf, createInboundMessage());
        assertEquals("reason", fault.getReason());
        assertEquals(Soap12.getInstance().getSender(), fault.getFaultCode());
        assertEquals(RM10Constants.UNKNOWN_SEQUENCE_FAULT_QNAME, fault.getSubCode());
        Element elem = fault.getDetail();
        assertEquals(RM10Constants.NAMESPACE_URI, elem.getNamespaceURI());
        assertEquals("Identifier", elem.getLocalName());
        assertNull(fault.getCause());
    }

    @Test
    public void createSoap12FaultWithIdentifierDetail() {
        SoapBinding sb = mock(SoapBinding.class);
        when(sb.getSoapVersion()).thenReturn(Soap12.getInstance());
        Identifier id = new Identifier();
        id.setValue("sid");
        setupSequenceFault(true, RM10Constants.UNKNOWN_SEQUENCE_FAULT_QNAME, id);

        SoapFaultFactory factory = new SoapFaultFactory(sb);
        SoapFault fault = (SoapFault)factory.createFault(sf, createInboundMessage());
        assertEquals("reason", fault.getReason());
        assertEquals(Soap12.getInstance().getSender(), fault.getFaultCode());
        assertEquals(RM10Constants.UNKNOWN_SEQUENCE_FAULT_QNAME, fault.getSubCode());
        Element elem = fault.getDetail();
        assertEquals(RM10Constants.NAMESPACE_URI, elem.getNamespaceURI());
        assertEquals("Identifier", elem.getLocalName());
    }

    @Test
    public void createSoap12FaultWithAcknowledgementDetail() {
        SoapBinding sb = mock(SoapBinding.class);
        when(sb.getSoapVersion()).thenReturn(Soap12.getInstance());
        SequenceAcknowledgement ack = new SequenceAcknowledgement();
        Identifier id = new Identifier();
        id.setValue("sid");
        ack.setIdentifier(id);
        SequenceAcknowledgement.AcknowledgementRange range =
            new SequenceAcknowledgement.AcknowledgementRange();
        range.setLower(Long.valueOf(1));
        range.setUpper(Long.valueOf(10));
        ack.getAcknowledgementRange().add(range);
        setupSequenceFault(true, RM10Constants.INVALID_ACKNOWLEDGMENT_FAULT_QNAME, ack);

        SoapFaultFactory factory = new SoapFaultFactory(sb);
        SoapFault fault = (SoapFault)factory.createFault(sf, createInboundMessage());
        assertEquals("reason", fault.getReason());
        assertEquals(Soap12.getInstance().getSender(), fault.getFaultCode());
        assertEquals(RM10Constants.INVALID_ACKNOWLEDGMENT_FAULT_QNAME, fault.getSubCode());
        Element elem = fault.getDetail();
        assertEquals(RM10Constants.NAMESPACE_URI, elem.getNamespaceURI());
        assertEquals("SequenceAcknowledgement", elem.getLocalName());
    }

    @Test
    public void createSoap12FaultWithoutDetail() {
        SoapBinding sb = mock(SoapBinding.class);
        when(sb.getSoapVersion()).thenReturn(Soap12.getInstance());
        setupSequenceFault(true, RM10Constants.CREATE_SEQUENCE_REFUSED_FAULT_QNAME, null);

        SoapFaultFactory factory = new SoapFaultFactory(sb);
        SoapFault fault = (SoapFault)factory.createFault(sf, createInboundMessage());
        assertEquals("reason", fault.getReason());
        assertEquals(Soap12.getInstance().getSender(), fault.getFaultCode());
        assertEquals(RM10Constants.CREATE_SEQUENCE_REFUSED_FAULT_QNAME, fault.getSubCode());
        assertNull(fault.getDetail());
    }

    @Test
    public void testToString() {
        SoapBinding sb = mock(SoapBinding.class);
        when(sb.getSoapVersion()).thenReturn(Soap11.getInstance());
        SoapFault fault = mock(SoapFault.class);
        when(fault.getReason()).thenReturn("r");
        when(fault.getFaultCode()).thenReturn(new QName("ns", "code"));
        when(fault.getSubCode()).thenReturn(new QName("ns", "subcode"));

        SoapFaultFactory factory = new SoapFaultFactory(sb);
        assertEquals("Reason: r, code: {ns}code, subCode: {ns}subcode",
                     factory.toString(fault));
    }

    private Message createInboundMessage() {
        Message message = new MessageImpl();
        RMProperties rmps = new RMProperties();
        rmps.exposeAs(RM10Constants.NAMESPACE_URI);
        RMContextUtils.storeRMProperties(message, rmps, false);
        AddressingProperties maps = new AddressingProperties();
        RMContextUtils.storeMAPs(maps, message, false, false);
        return message;
    }
}