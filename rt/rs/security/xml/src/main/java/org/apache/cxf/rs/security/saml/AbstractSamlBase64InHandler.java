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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.zip.DataFormatException;

import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.message.Message;

public abstract class AbstractSamlBase64InHandler extends AbstractSamlInHandler {

    private boolean useDeflateEncoding = true;

    public void setUseDeflateEncoding(boolean deflate) {
        useDeflateEncoding = deflate;
    }
    public boolean useDeflateEncoding() {
        return useDeflateEncoding;
    }

    protected void handleToken(Message message, String assertion) {
        // the assumption here is that saml:Assertion is directly available, however, it
        // may be contained inside saml:Response or saml:ArtifactResponse/saml:Response
        if (assertion == null) {
            throwFault("SAML assertion is not available", null);
        }

        try {
            byte[] deflatedToken = Base64Utility.decode(assertion);
            InputStream is = useDeflateEncoding()
                ? new DeflateEncoderDecoder().inflateToken(deflatedToken)
                : new ByteArrayInputStream(deflatedToken);
            validateToken(message, is);
        } catch (Base64Exception ex) {
            throwFault("Base64 decoding has failed", ex);
        } catch (DataFormatException ex) {
            throwFault("Encoded assertion can not be inflated", ex);
        }
    }
}
