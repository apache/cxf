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

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.wss4j.common.crypto.WSProviderConfig;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;

public abstract class AbstractSamlOutInterceptor extends AbstractPhaseInterceptor<Message> {

    static {
        WSProviderConfig.init();
    }

    private boolean useDeflateEncoding = true;

    protected AbstractSamlOutInterceptor(String phase) {
        super(phase);
    }

    public void setUseDeflateEncoding(boolean deflate) {
        useDeflateEncoding = deflate;
    }

    protected SamlAssertionWrapper createAssertion(Message message) throws Fault {
        return SAMLUtils.createAssertion(message);

    }

    protected String encodeToken(String assertion) throws Base64Exception {
        byte[] tokenBytes = assertion.getBytes(StandardCharsets.UTF_8);

        if (useDeflateEncoding) {
            tokenBytes = new DeflateEncoderDecoder().deflateToken(tokenBytes);
        }
        StringWriter writer = new StringWriter();
        Base64Utility.encode(tokenBytes, 0, tokenBytes.length, writer);
        return writer.toString();
    }
}
