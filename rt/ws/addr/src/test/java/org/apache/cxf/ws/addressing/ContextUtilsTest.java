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

package org.apache.cxf.ws.addressing;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import junit.framework.Assert;

import org.apache.cxf.service.model.Extensible;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class ContextUtilsTest extends Assert {
    private static final QName WSA_ACTION_QNAME = 
        new QName(JAXWSAConstants.NS_WSA, Names.WSAW_ACTION_NAME);
    private static final QName OLD_WSDL_WSA_ACTION_QNAME = 
        new QName(Names.WSA_NAMESPACE_WSDL_NAME_OLD, Names.WSAW_ACTION_NAME);
    
    private IMocksControl control;
        

    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
    }

    @Test
    public void testGetActionFromExtensible() {
        Map<QName, Object> attributes = new HashMap<QName, Object>();
        Extensible ext = control.createMock(Extensible.class);
        EasyMock.expect(ext.getExtensionAttributes()).andReturn(attributes).anyTimes();
        attributes.put(WSA_ACTION_QNAME, "urn:foo:test:2");
        EasyMock.expect(ext.getExtensionAttribute(JAXWSAConstants.WSAW_ACTION_QNAME)).
            andReturn("urn:foo:test:1");
        control.replay();
        
        String action = ContextUtils.getAction(ext);
        assertEquals("urn:foo:test:1", action);
        
        control.reset();
        attributes.clear();
        EasyMock.expect(ext.getExtensionAttributes()).andReturn(attributes).anyTimes();
        EasyMock.expect(ext.getExtensionAttribute(JAXWSAConstants.WSAW_ACTION_QNAME)).
            andReturn(null);
        attributes.put(WSA_ACTION_QNAME, "urn:foo:test:2");
        control.replay();
        
        action = ContextUtils.getAction(ext);
        assertEquals("urn:foo:test:2", action);
        
        control.reset();
        attributes.clear();
        EasyMock.expect(ext.getExtensionAttributes()).andReturn(attributes).anyTimes();
        EasyMock.expect(ext.getExtensionAttribute(JAXWSAConstants.WSAW_ACTION_QNAME)).
            andReturn(null);
        attributes.put(OLD_WSDL_WSA_ACTION_QNAME, "urn:foo:test:3");
        control.replay();
        
        action = ContextUtils.getAction(ext);
        assertEquals("urn:foo:test:3", action);
        
        control.reset();
        attributes.clear();
        EasyMock.expect(ext.getExtensionAttributes()).andReturn(attributes).anyTimes();
        EasyMock.expect(ext.getExtensionAttribute(JAXWSAConstants.WSAW_ACTION_QNAME)).
            andReturn(null);
        control.replay();
        
        action = ContextUtils.getAction(ext);
        assertEquals(null, action);
    }
}
