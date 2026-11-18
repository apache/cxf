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

import java.io.File;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.corba.CorbaBindingException;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TCKind;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class CorbaObjectReferenceListenerTest {

    private static final QName ADDRESS = new QName("http://www.w3.org/2005/08/addressing", "Address");

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
    public void testIorAddress() {
        // parsing a corbaloc URL doesn't contact the host, it just builds the reference
        String ior = orb.object_to_string(orb.string_to_object("corbaloc::127.0.0.1:1/Test"));

        CorbaObjectReferenceHandler handler = createHandler();
        CorbaObjectReferenceListener listener = new CorbaObjectReferenceListener(handler, orb);
        listener.processStartElement(ADDRESS);
        listener.processCharacters(ior);
        assertNotNull(handler.getReference());
    }

    @Test
    public void testFileAddressRejected() throws Exception {
        File file = File.createTempFile("objref", ".ref");
        try {
            Files.write(file.toPath(),
                orb.object_to_string(orb.string_to_object("corbaloc::127.0.0.1:1/Test"))
                    .getBytes(StandardCharsets.UTF_8));

            CorbaObjectReferenceHandler handler = createHandler();
            CorbaObjectReferenceListener listener = new CorbaObjectReferenceListener(handler, orb);
            listener.processStartElement(ADDRESS);
            assertThrows(CorbaBindingException.class,
                () -> listener.processCharacters("file:" + file.getAbsolutePath()));
            assertNull(handler.getReference());
        } finally {
            file.delete();
        }
    }

    @Test
    public void testCorbalocAddress() {
        // a corbaloc address only describes the object, nothing is contacted when it is resolved
        CorbaObjectReferenceHandler handler = createHandler();
        CorbaObjectReferenceListener listener = new CorbaObjectReferenceListener(handler, orb);
        listener.processStartElement(ADDRESS);
        listener.processCharacters("corbaloc::127.0.0.1:1/Test");
        assertNotNull(handler.getReference());
    }

    @Test(timeout = 30000)
    public void testCorbanameAddressRejected() throws Exception {
        // resolving a corbaname address would contact the naming service it names, so check that nothing is
        try (ServerSocket server = new ServerSocket(0)) {
            CorbaObjectReferenceHandler handler = createHandler();
            CorbaObjectReferenceListener listener = new CorbaObjectReferenceListener(handler, orb);
            listener.processStartElement(ADDRESS);
            assertThrows(CorbaBindingException.class,
                () -> listener.processCharacters("corbaname::127.0.0.1:" + server.getLocalPort() + "#Test"));
            assertNull(handler.getReference());

            server.setSoTimeout(500);
            assertThrows(SocketTimeoutException.class, () -> server.accept().close());
        }
    }

    @Test
    public void testAddressWithoutSchemeRejected() {
        CorbaObjectReferenceHandler handler = createHandler();
        CorbaObjectReferenceListener listener = new CorbaObjectReferenceListener(handler, orb);
        listener.processStartElement(ADDRESS);
        assertThrows(CorbaBindingException.class, () -> listener.processCharacters("no-scheme"));
        assertNull(handler.getReference());
    }

    private CorbaObjectReferenceHandler createHandler() {
        QName idlType = new QName(CorbaConstants.NU_WSDL_CORBA, "object", CorbaConstants.NP_WSDL_CORBA);
        return new CorbaObjectReferenceHandler(new QName("ref"), idlType,
                                               orb.get_primitive_tc(TCKind.tk_objref), null);
    }
}
