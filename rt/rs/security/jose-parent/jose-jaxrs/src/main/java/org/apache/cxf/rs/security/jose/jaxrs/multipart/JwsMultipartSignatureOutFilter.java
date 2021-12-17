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

import java.io.OutputStream;
import java.util.List;

import jakarta.activation.DataHandler;
import org.apache.cxf.attachment.ByteDataSource;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartOutputFilter;
import org.apache.cxf.rs.security.jose.jws.JwsException;
import org.apache.cxf.rs.security.jose.jws.JwsOutputStream;
import org.apache.cxf.rs.security.jose.jws.JwsSignature;

public class JwsMultipartSignatureOutFilter implements MultipartOutputFilter {

    private JwsSignature sig;
    public JwsMultipartSignatureOutFilter(JwsSignature sig) {
        this.sig = sig;
    }
    @Override
    public void filter(List<Attachment> parts) {
        for (int i = 0; i < parts.size() - 1; i++) {
            Attachment dataPart = parts.get(i);
            DataHandler handler = dataPart.getDataHandler();
            dataPart.setDataHandler(new JwsSignatureDataHandler(handler));
        }
    }

    private class JwsSignatureDataHandler extends DataHandler {
        private DataHandler handler;
        JwsSignatureDataHandler(DataHandler handler) {
            super(new ByteDataSource("1".getBytes()));
            this.handler = handler;
        }
        @Override
        public String getContentType() {
            return handler.getContentType();
        }
        @Override
        public void writeTo(OutputStream os) {
            JwsOutputStream jwsOutStream = new JwsOutputStream(os, sig, false);
            try {
                handler.writeTo(jwsOutStream);
                jwsOutStream.flush();
            } catch (Exception ex) {
                throw new JwsException(JwsException.Error.INVALID_SIGNATURE);
            }
        }
    }
}
