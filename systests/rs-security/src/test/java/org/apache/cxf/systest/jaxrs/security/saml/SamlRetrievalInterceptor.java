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
package org.apache.cxf.systest.jaxrs.security.saml;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.rs.security.saml.SAMLConstants;
import org.apache.cxf.rs.security.saml.SamlFormOutInterceptor;
import org.apache.cxf.rs.security.saml.SamlHeaderOutInterceptor;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.SAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.engine.WSSConfig;

/**
 * An Interceptor to "retrieve" a SAML Token, i.e. create one and set it on the message
 * context so that the Saml*Interceptors can write it out in a particular way.
 */
public class SamlRetrievalInterceptor extends AbstractPhaseInterceptor<Message> {

    static {
        WSSConfig.init();
    }

    protected SamlRetrievalInterceptor() {
        super(Phase.WRITE);
        addBefore(SamlFormOutInterceptor.class.getName());
        addBefore(SamlHeaderOutInterceptor.class.getName());
    }

    @Override
    public void handleMessage(Message message) throws Fault {

        // Create a SAML Token
        SAMLCallback samlCallback = new SAMLCallback();
        SAMLUtil.doSAMLCallback(new SamlCallbackHandler(), samlCallback);

        try {
            SamlAssertionWrapper assertion = new SamlAssertionWrapper(samlCallback);
            Document doc = DOMUtils.createDocument();
            Element token = assertion.toDOM(doc);
            message.put(SAMLConstants.SAML_TOKEN_ELEMENT, token);
        } catch (WSSecurityException ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            throw new Fault(new RuntimeException(ex.getMessage() + ", stacktrace: " + sw.toString()));
        }

    }
}
