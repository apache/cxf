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
package org.apache.cxf.binding.corba.types;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.corba.CorbaBindingException;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TCKind;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

public class CorbaAnyEventProducerTest {

    private ORB orb;

    @Before
    public void setUp() throws Exception {
        java.util.Properties props = System.getProperties();
        props.put("yoko.orb.id", "CXF-CORBA-Binding");
        orb = ORB.init(new String[0], props);
    }

    @After
    public void tearDown() throws Exception {
        if (orb != null) {
            try {
                orb.destroy();
            } catch (Exception ex) {
                // ignore
            }
        }
    }

    @Test
    public void testNestedAny() {
        CorbaAnyEventProducer producer = new CorbaAnyEventProducer(createAnyHandler(nestedAny(10)), null, orb);
        assertNotNull(producer.getAttributes());
    }

    @Test
    public void testNestedAnyTooDeep() {
        // each nested Any is read when the producer for the Any containing it is created, so the depth
        // must be limited across them rather than recursing once per level
        CorbaAnyHandler handler = createAnyHandler(nestedAny(150));
        assertThrows(CorbaBindingException.class, () -> new CorbaAnyEventProducer(handler, null, orb));
    }

    private CorbaAnyHandler createAnyHandler(Any value) {
        CorbaAnyHandler handler = new CorbaAnyHandler(new QName("any"), CorbaConstants.NT_CORBA_ANY,
                                                      orb.get_primitive_tc(TCKind.tk_any), null);
        handler.setValue(value);
        return handler;
    }

    private Any nestedAny(int depth) {
        Any any = orb.create_any();
        any.insert_long(42);
        for (int i = 1; i < depth; i++) {
            Any outer = orb.create_any();
            outer.insert_any(any);
            any = outer;
        }
        return any;
    }
}
