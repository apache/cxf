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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.util.DOM2Writer;

public class SamlHeaderOutInterceptor extends AbstractSamlOutInterceptor {
    private static final Logger LOG =
        LogUtils.getL7dLogger(SamlHeaderOutInterceptor.class);

    public SamlHeaderOutInterceptor() {
        this(Phase.WRITE);
    }

    public SamlHeaderOutInterceptor(String phase) {
        super(phase);
    }

    public void handleMessage(Message message) throws Fault {
        try {
            SamlAssertionWrapper assertionWrapper = createAssertion(message);

            Document doc = DOMUtils.newDocument();
            Element assertionElement = assertionWrapper.toDOM(doc);
            String encodedToken = encodeToken(DOM2Writer.nodeToString(assertionElement));

            Map<String, List<String>> headers = getHeaders(message);

            StringBuilder builder = new StringBuilder();
            builder.append("SAML").append(' ').append(encodedToken);
            headers.put("Authorization",
                CastUtils.cast(Collections.singletonList(builder.toString()), String.class));

        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            ex.printStackTrace(new PrintWriter(sw));
            LOG.warning(sw.toString());
            throw new Fault(new RuntimeException(ex.getMessage() + ", stacktrace: " + sw.toString()));
        }

    }

    private Map<String, List<String>> getHeaders(Message message) {
        Map<String, List<String>> headers =
            CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));
        if (headers == null) {
            headers = new HashMap<>();
            message.put(Message.PROTOCOL_HEADERS, headers);
        }
        return headers;
    }


}
