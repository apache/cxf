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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.junit.Test;

import static org.apache.cxf.ws.addressing.ContextUtils.getAttributedURI;
import static org.apache.cxf.ws.addressing.ContextUtils.getMAPProperty;
import static org.apache.cxf.ws.addressing.ContextUtils.getRelatesTo;
import static org.apache.cxf.ws.addressing.ContextUtils.hasEmptyAction;
import static org.apache.cxf.ws.addressing.ContextUtils.isAnonymousAddress;
import static org.apache.cxf.ws.addressing.ContextUtils.isFault;
import static org.apache.cxf.ws.addressing.ContextUtils.isGenericAddress;
import static org.apache.cxf.ws.addressing.ContextUtils.isNoneAddress;
import static org.apache.cxf.ws.addressing.ContextUtils.isOutbound;
import static org.apache.cxf.ws.addressing.ContextUtils.isRequestor;
import static org.apache.cxf.ws.addressing.ContextUtils.retrieveMAPs;
import static org.apache.cxf.ws.addressing.ContextUtils.storeMAPs;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.ADDRESSING_PROPERTIES_INBOUND;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.ADDRESSING_PROPERTIES_OUTBOUND;
import static org.apache.cxf.ws.addressing.JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

public class ContextUtilsTest {

    @Test
    public void testIsOutbound() {
        assertThat(isOutbound(null), is(false));

        MessageImpl message = new MessageImpl();
        assertThat(isOutbound(message), is(false));

        message.setExchange(new ExchangeImpl());
        assertThat(isOutbound(message), is(false));

        message.getExchange().setOutMessage(message);
        assertThat(isOutbound(message), is(true));

        message.getExchange().setOutMessage(null);
        message.getExchange().setOutFaultMessage(message);
        assertThat(isOutbound(message), is(true));
    }

    @Test
    public void testIsFault() {
        assertThat(isFault(null), is(false));

        MessageImpl message = new MessageImpl();
        assertThat(isFault(message), is(false));

        message.setExchange(new ExchangeImpl());
        assertThat(isFault(message), is(false));

        message.getExchange().setInFaultMessage(message);
        assertThat(isFault(message), is(true));

        message.getExchange().setInFaultMessage(null);
        message.getExchange().setOutFaultMessage(message);
        assertThat(isFault(message), is(true));
    }

    @Test
    public void testIsRequestor() {
        MessageImpl message = new MessageImpl();
        assertThat(isRequestor(message), is(false));

        message.put(Message.REQUESTOR_ROLE, false);
        assertThat(isRequestor(message), is(false));

        message.put(Message.REQUESTOR_ROLE, true);
        assertThat(isRequestor(message), is(true));
    }

    @Test
    public void testGetMAPProperty() {
        assertThat(getMAPProperty(true, true, false), is(CLIENT_ADDRESSING_PROPERTIES));
        assertThat(getMAPProperty(true, true, true), is(CLIENT_ADDRESSING_PROPERTIES));
        assertThat(getMAPProperty(true, false, true), is(ADDRESSING_PROPERTIES_OUTBOUND));
        assertThat(getMAPProperty(true, false, false), is(ADDRESSING_PROPERTIES_INBOUND));
    }

    @Test
    public void testStoreMAPs() {
        AddressingProperties maps = new AddressingProperties();
        MessageImpl message = new MessageImpl();

        storeMAPs(maps, message, true);
        assertThat(message.get(ADDRESSING_PROPERTIES_OUTBOUND), equalTo(maps));

        storeMAPs(maps, message, false);
        assertThat(message.get(ADDRESSING_PROPERTIES_INBOUND), equalTo(maps));
    }

    @Test
    public void testRetrieveMAPs() {
        AddressingProperties maps = new AddressingProperties();
        MessageImpl message = new MessageImpl();

        storeMAPs(maps, message, true);
        assertThat(retrieveMAPs(message, false, true), equalTo(maps));

        storeMAPs(maps, message, false);
        assertThat(retrieveMAPs(message, false, false), equalTo(maps));
    }

    @Test
    public void testGetAttributedURI() {
        assertThat(getAttributedURI(null).getValue(), is((String) null));

        String value = "test";
        assertThat(getAttributedURI(value).getValue(), is(value));
    }

    @Test
    public void testGetRelatesTo() {
        assertThat(getRelatesTo(null).getValue(), is((String) null));

        String value = "test";
        assertThat(getRelatesTo(value).getValue(), is(value));
    }

    @Test
    public void testIsGenericAddress() {
        assertThat(isGenericAddress(null), is(true));

        EndpointReferenceType ref = new EndpointReferenceType();
        ref.setAddress(null);
        assertThat(isGenericAddress(ref), is(true));

        ref.setAddress(new AttributedURIType());
        assertThat(isGenericAddress(ref), is(false));

        Random random = new Random();

        ref.getAddress().setValue(Names.WSA_ANONYMOUS_ADDRESS + random.nextInt());
        assertThat(isGenericAddress(ref), is(true));

        ref.getAddress().setValue(Names.WSA_NONE_ADDRESS + random.nextInt());
        assertThat(isGenericAddress(ref), is(true));
    }

    @Test
    public void testIsAnonymousAddress() {
        assertThat(isAnonymousAddress(null), is(true));

        EndpointReferenceType ref = new EndpointReferenceType();
        ref.setAddress(null);
        assertThat(isAnonymousAddress(ref), is(true));

        ref.setAddress(new AttributedURIType());
        assertThat(isAnonymousAddress(ref), is(false));

        Random random = new Random();

        ref.getAddress().setValue(Names.WSA_ANONYMOUS_ADDRESS + random.nextInt());
        assertThat(isAnonymousAddress(ref), is(true));
    }

    @Test
    public void testIsNoneAddress() {
        assertThat(isNoneAddress(null), is(false));

        EndpointReferenceType ref = new EndpointReferenceType();
        assertThat(isNoneAddress(ref), is(false));

        ref.setAddress(new AttributedURIType());
        assertThat(isNoneAddress(ref), is(false));

        ref.getAddress().setValue(Names.WSA_NONE_ADDRESS);
        assertThat(isNoneAddress(ref), is(true));
    }

    @Test
    public void testHasEmptyAddress() {
        AddressingProperties maps = new AddressingProperties();

        assertThat(hasEmptyAction(maps), is(true));

        maps.setAction(new AttributedURIType());
        maps.getAction().setValue("");
        assertThat(hasEmptyAction(maps), is(false));
        assertThat(maps.getAction(), nullValue());

        maps.setAction(new AttributedURIType());
        maps.getAction().setValue("test");
        assertThat(hasEmptyAction(maps), is(false));
        assertThat(maps.getAction(), notNullValue());
    }

    @Test
    public void formatMessages() {
        assertThat(
                ContextUtils.formatDecoupledReplyToNotPermittedMessage("http://localhost:1234/decoupled"),
                is("Decoupled WS-Addressing ReplyTo (http://localhost:1234/decoupled) is not permitted by this server."
                        + " Enable with system property org.apache.cxf.ws.addressing.decoupled.enabled=true,"
                        + " or configure permitted URI schemes with"
                        + " org.apache.cxf.ws.addressing.decoupled.allowedSchemes"));
        assertThat(
                ContextUtils.formatDecoupledReplyToSchemeNotPermittedMessage("http://localhost:1234/decoupled"),
                is("Decoupled WS-Addressing ReplyTo (http://localhost:1234/decoupled) is not permitted by this server:"
                        + " URI scheme is not allowed."
                        + " Configure permitted schemes with system property"
                        + " org.apache.cxf.ws.addressing.decoupled.allowedSchemes"));
    }

    @Test
    public void logMessages() {
        assertLogMessage(log -> ContextUtils.logDisallowedDecoupledDestinationScheme(
                log,
                Level.WARNING,
                "http://localhost:1234/decoupled"),
                "Rejected pre-approved decoupled destination with disallowed scheme: http://localhost:1234/decoupled."
                        + " Configure permitted URI schemes with system property "
                        + ContextUtils.ALLOWED_DECOUPLED_DEST_SCHEMES_PROPERTY);
        assertLogMessage(
                log -> ContextUtils.logRejectedDecoupledDestination(
                        log,
                        Level.WARNING,
                        "http://localhost:1234/decoupled"),
                "Rejected wsa:ReplyTo/FaultTo decoupled destination: http://localhost:1234/decoupled."
                        + " Decoupled WS-Addressing is disabled by default;"
                        + " enable with system property org.apache.cxf.ws.addressing.decoupled.enabled=true,"
                        + " or configure permitted URI schemes with"
                        + " org.apache.cxf.ws.addressing.decoupled.allowedSchemes");
        assertLogMessage(
                log -> ContextUtils.logDecoupledFaultToSchemeNotAllowed(
                        log,
                        Level.WARNING,
                        "http://localhost:1234/decoupled"),
                "Decoupled pre-approved FaultTo (http://localhost:1234/decoupled) is not permitted:"
                        + " URI scheme is not allowed. Fault will be delivered to ReplyTo instead."
                        + " Configure permitted schemes with org.apache.cxf.ws.addressing.decoupled.allowedSchemes");
        assertLogMessage(
                log -> ContextUtils.logDecoupledFaultToNotAllowed(
                        log,
                        Level.WARNING,
                        "http://localhost:1234/decoupled"),
                "Fault will be delivered to ReplyTo instead. Configure permitted schemes with"
                        + " org.apache.cxf.ws.addressing.decoupled.enabled Decoupled WS-Addressing FaultTo"
                        + " (http://localhost:1234/decoupled) is not permitted; fault will be delivered to ReplyTo"
                        + " instead. Enable with system property org.apache.cxf.ws.addressing.decoupled.enabled=true,"
                        + " or configure permitted URI schemes with"
                        + " org.apache.cxf.ws.addressing.decoupled.allowedSchemes");
    }

    static void assertLogMessage(Consumer<Logger> logMethod, String expected) {
        Logger log = Logger.getLogger("test" + logMethod.toString());
        log.setLevel(Level.FINEST);
        log.setUseParentHandlers(false);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamHandler handler = new StreamHandler(out, new SimpleFormatter());
        handler.setLevel(Level.FINEST);
        log.addHandler(handler);
        logMethod.accept(log);
        handler.flush();
        String logOutput = out.toString(StandardCharsets.UTF_8);
        assertThat(
                logOutput,
                org.hamcrest.Matchers.containsString("WARNING: " + expected));
        log.removeHandler(handler);
    }

}
