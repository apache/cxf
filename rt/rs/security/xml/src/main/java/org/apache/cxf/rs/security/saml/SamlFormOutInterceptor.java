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
package org.apache.cxf.rs.security.saml;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.Phase;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.util.DOM2Writer;

public class SamlFormOutInterceptor extends AbstractSamlOutInterceptor {
    private static final Logger LOG =
        LogUtils.getL7dLogger(SamlFormOutInterceptor.class);
    private static final String SAML_ELEMENT = "SAMLToken";

    public SamlFormOutInterceptor() {
        this(Phase.WRITE);
    }

    public SamlFormOutInterceptor(String phase) {
        super(phase);
    }

    public void handleMessage(Message message) throws Fault {
        Form form = getRequestForm(message);
        if (form == null) {
            return;
        }

        try {
            SamlAssertionWrapper assertionWrapper = SAMLUtils.createAssertion(message);

            Document doc = DOMUtils.newDocument();
            Element assertionElement = assertionWrapper.toDOM(doc);
            String encodedToken = encodeToken(DOM2Writer.nodeToString(assertionElement));

            updateForm(form, encodedToken);
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            LOG.warning(sw.toString());
            throw new Fault(new RuntimeException(ex.getMessage() + ", stacktrace: " + sw.toString()));
        }

    }

    protected void updateForm(Form form, String encodedToken) {
        form.param(SAML_ELEMENT, encodedToken);
    }

    @SuppressWarnings("unchecked")
    protected Form getRequestForm(Message message) {
        Object ct = message.get(Message.CONTENT_TYPE);
        if (ct == null || !MediaType.APPLICATION_FORM_URLENCODED.equalsIgnoreCase(ct.toString())) {
            return null;
        }
        MessageContentsList objs = MessageContentsList.getContentsList(message);
        if (objs != null && objs.size() == 1) {
            Object obj = objs.get(0);
            if (obj instanceof Form) {
                return (Form)obj;
            } else if (obj instanceof MultivaluedMap) {
                return new Form((MultivaluedMap<String, String>)obj);
            }
        }
        return null;
    }
}
