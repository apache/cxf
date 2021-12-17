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
package org.apache.cxf.rs.security.jose.jaxrs;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.jose.common.JoseUtils;
import org.apache.cxf.rs.security.jose.jws.JwsDetachedSignature;

public class JwsDetachedSignatureProvider implements MessageBodyWriter<JwsDetachedSignature> {
    @Override
    public long getSize(JwsDetachedSignature arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> cls, Type t, Annotation[] anns, MediaType mt) {
        return true;
    }

    @Override
    public void writeTo(JwsDetachedSignature parts, Class<?> cls, Type t, Annotation[] anns, MediaType mt,
                        MultivaluedMap<String, Object> headers, OutputStream os)
                            throws IOException, WebApplicationException {
        JoseUtils.traceHeaders(parts.getHeaders());
        
        byte[] finalBytes = parts.getSignature().sign();
        
        if (!parts.isUseJwsJsonSignatureFormat()) {
            os.write(StringUtils.toBytesASCII(parts.getEncodedHeaders()));
            byte[] dotBytes = new byte[]{'.'};
            os.write(dotBytes);
            os.write(dotBytes);
            Base64UrlUtility.encodeAndStream(finalBytes, 0, finalBytes.length, os);
        } else {
            // use flattened JWS JSON format
            os.write(StringUtils.toBytesASCII("{"));
            String headersProp = "\"protected\":\"" + parts.getEncodedHeaders() + "\"";
            os.write(StringUtils.toBytesUTF8(headersProp));
            os.write(StringUtils.toBytesASCII(","));
            String sigProp = "\"signature\":\"" + Base64UrlUtility.encode(finalBytes) + "\"";
            os.write(StringUtils.toBytesUTF8(sigProp));
            os.write(StringUtils.toBytesASCII("}"));
        }
    }

}
