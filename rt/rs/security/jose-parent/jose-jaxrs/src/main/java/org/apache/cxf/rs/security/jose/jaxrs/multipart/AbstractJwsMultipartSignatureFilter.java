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

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.json.basic.JsonMapObjectReaderWriter;
import org.apache.cxf.jaxrs.utils.multipart.AttachmentUtils;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jws.JwsDetachedSignature;
import org.apache.cxf.rs.security.jose.jws.JwsHeaders;
import org.apache.cxf.rs.security.jose.jws.JwsSignature;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;

public abstract class AbstractJwsMultipartSignatureFilter {
    private JsonMapObjectReaderWriter writer = new JsonMapObjectReaderWriter();
    
    private JwsSignatureProvider sigProvider;
    private boolean useJwsJsonSignatureFormat;

    public void setSignatureProvider(JwsSignatureProvider signatureProvider) {
        this.sigProvider = signatureProvider;
    }
    
    protected List<Object> getAttachmentParts(Object rootEntity) {
        final List<Object> parts;
        
        if (rootEntity instanceof MultipartBody) {
            parts = CastUtils.cast(((MultipartBody)rootEntity).getAllAttachments());
        } else {
            if (rootEntity instanceof List) {
                List<Object> entityList = CastUtils.cast((List<?>)rootEntity);
                parts = new ArrayList<>(entityList);
            } else {
                parts = new ArrayList<>(2);
                parts.add(rootEntity);
            }
        }
        
        JwsHeaders headers = new JwsHeaders();
        headers.setPayloadEncodingStatus(false);
        JwsSignatureProvider theSigProvider = sigProvider != null ? sigProvider
            : JwsUtils.loadSignatureProvider(headers, true);
        JwsSignature jwsSignature = theSigProvider.createJwsSignature(headers);
        
        String base64UrlEncodedHeaders = Base64UrlUtility.encode(writer.toJson(headers));
        byte[] headerBytesWithDot = StringUtils.toBytesASCII(base64UrlEncodedHeaders + '.');
        jwsSignature.update(headerBytesWithDot, 0, headerBytesWithDot.length);
        AttachmentUtils.addMultipartOutFilter(new JwsMultipartSignatureOutFilter(jwsSignature));
        
        
        JwsDetachedSignature jws = new JwsDetachedSignature(headers, 
                                                            base64UrlEncodedHeaders,
                                                            jwsSignature,
                                                            useJwsJsonSignatureFormat);
        
        Attachment jwsPart = new Attachment("signature", JoseConstants.MEDIA_TYPE_JOSE, jws);
        parts.add(jwsPart);
        return parts;
    }

    public void setUseJwsJsonSignatureFormat(boolean useJwsJsonSignatureFormat) {
        this.useJwsJsonSignatureFormat = useJwsJsonSignatureFormat;
    }
}
