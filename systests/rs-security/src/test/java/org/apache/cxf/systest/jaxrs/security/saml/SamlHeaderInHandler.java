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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.cxf.common.util.Base64Exception;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.message.Message;

public class SamlHeaderInHandler extends AbstractSamlInHandler {

    private static final String SAML_AUTH = "SAML";
    
    @Context
    private HttpHeaders headers;
    
    public Response handleRequest(Message message, ClassResourceInfo resourceClass) {
        
        List<String> values = headers.getRequestHeader(HttpHeaders.AUTHORIZATION);
        if (values == null || values.size() != 1 || !values.get(0).startsWith(SAML_AUTH)) {
            throwFault("Authorization header must be available and use SAML profile", null);    
        }
        
        String[] parts = values.get(0).split(" ");
        if (parts.length != 2) {
            throwFault("Authorization header is malformed", null);
        }
        
        try {
            validateToken(message, decodeAndInflateToken(parts[1])); 
        } catch (Base64Exception ex) {
            throwFault("Base64 decoding has failed", ex);
        } catch (DataFormatException ex) {
            throwFault("Encoded assertion can not be inflated", ex);
        }         
        return null;
    }

    
    private InputStream decodeAndInflateToken(String encodedToken) 
        throws DataFormatException, Base64Exception {
        byte[] deflatedToken = Base64Utility.decode(encodedToken);
        Inflater inflater = new Inflater();
        inflater.setInput(deflatedToken);
        
        byte[] input = new byte[deflatedToken.length * 2];
        
        int inflatedLen = 0;
        int inputLen = 0;
        byte[] inflatedToken = input;
        while (!inflater.finished()) {
            inputLen = inflater.inflate(input);
            if (!inflater.finished()) {
                inflatedToken = new byte[input.length + inflatedLen];
                System.arraycopy(input, 0, inflatedToken, inflatedLen, inputLen);
                inflatedLen += inputLen;
            }
        }
        InputStream is = new ByteArrayInputStream(input, 0, inputLen);
        if (inflatedToken != input) {
            is = new SequenceInputStream(new ByteArrayInputStream(inflatedToken, 0, inflatedLen),
                                         is);
        }
        return is;
    }
}
