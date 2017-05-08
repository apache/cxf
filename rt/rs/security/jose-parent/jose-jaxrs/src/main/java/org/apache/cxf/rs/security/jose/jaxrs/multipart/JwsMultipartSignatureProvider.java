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
package org.apache.cxf.rs.security.jose.jaxrs.multipart;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.rs.security.jose.common.JoseUtils;
import org.apache.cxf.rs.security.jose.jws.JwsDetachedSignature;

public class JwsMultipartSignatureProvider implements MessageBodyWriter<JwsDetachedSignature> {
    private JsonMapObjectReaderWriter writer = new JsonMapObjectReaderWriter();
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
        byte[] headerBytes = StringUtils.toBytesUTF8(writer.toJson(parts.getHeaders()));
        Base64UrlUtility.encodeAndStream(headerBytes, 0, headerBytes.length, os);
        os.write(new byte[]{'.'});
        
        byte[] finalBytes = parts.getSignature().sign();
        os.write(new byte[]{'.'});
        Base64UrlUtility.encodeAndStream(finalBytes, 0, finalBytes.length, os);
    }

}
