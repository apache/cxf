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

package org.apache.cxf.binding.soap.jms.interceptor;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapBinding;
import org.apache.cxf.binding.soap.SoapFault;

import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 *
 */
public class SoapFaultFactoryTest {

    private IMocksControl control;
    private JMSFault jmsFault;

    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
    }

    JMSFault setupJMSFault(boolean isSender, QName code, Object detail, boolean isSoap12) {
        jmsFault = control.createMock(JMSFault.class);
        EasyMock.expect(jmsFault.getReason()).andReturn("reason");
        if (isSoap12) {
            EasyMock.expect(jmsFault.isSender()).andReturn(isSender);
        }
        EasyMock.expect(jmsFault.getSubCode()).andReturn(code);
        if (null != detail) {
            EasyMock.expect(jmsFault.getDetail()).andReturn(detail);
            JMSFaultType sft = new JMSFaultType();
            sft.setFaultCode(SoapJMSConstants.getContentTypeMismatchQName());
        }
        return jmsFault;
    }

    @Test
    public void createSoap11Fault() {
        SoapBinding sb = control.createMock(SoapBinding.class);
        EasyMock.expect(sb.getSoapVersion()).andReturn(Soap11.getInstance());
        setupJMSFault(true, SoapJMSConstants.getContentTypeMismatchQName(), null, false);
        control.replay();

        SoapFaultFactory factory = new SoapFaultFactory(sb);
        SoapFault fault = (SoapFault)factory.createFault(jmsFault);
        assertEquals("reason", fault.getReason());
        assertEquals(SoapJMSConstants.getContentTypeMismatchQName(), fault.getFaultCode());
        assertNull(fault.getDetail());
        assertSame(jmsFault, fault.getCause());
        control.verify();
    }

    @Test
    public void createSoap12Fault() {
        SoapBinding sb = control.createMock(SoapBinding.class);
        EasyMock.expect(sb.getSoapVersion()).andReturn(Soap12.getInstance());
        setupJMSFault(true, SoapJMSConstants.getMismatchedSoapActionQName(), null, true);
        control.replay();
        SoapFaultFactory factory = new SoapFaultFactory(sb);
        SoapFault fault = (SoapFault)factory.createFault(jmsFault);
        assertEquals("reason", fault.getReason());
        assertEquals(Soap12.getInstance().getSender(), fault.getFaultCode());
        assertEquals(SoapJMSConstants.getMismatchedSoapActionQName(), fault.getSubCode());
        assertNull(fault.getDetail());
        assertNull(fault.getCause());
        control.verify();
    }
}