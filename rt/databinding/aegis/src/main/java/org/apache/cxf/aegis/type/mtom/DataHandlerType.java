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
package org.apache.cxf.aegis.type.mtom;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.activation.DataHandler;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.message.Attachment;

public class DataHandlerType extends AbstractXOPType {
    
    public DataHandlerType(boolean useXmimeContentType, String expectedContentTypes) {
        super(useXmimeContentType, expectedContentTypes);
    }

    @Override
    protected Object readAttachment(Attachment att, Context context) {
        return att.getDataHandler();
    }

    @Override
    protected Attachment createAttachment(Object object, String id) {
        DataHandler handler = (DataHandler)object;

        AttachmentImpl att = new AttachmentImpl(id, handler);
        att.setXOP(true);

        return att;
    }

    @Override
    protected String getContentType(Object object, Context context) {
        return ((DataHandler)object).getContentType();
    }

    @Override
    protected Object wrapBytes(byte[] bareBytes, String contentType) {
        // for the benefit of those who are working with string data, we have the following
        // trickery
        String charset = null;
        if (contentType != null
            && contentType.indexOf("text/") != -1
            && contentType.indexOf("charset") != -1) {
            charset = contentType.substring(contentType.indexOf("charset") + 8);
            if (charset.indexOf(";") != -1) {
                charset = charset.substring(0, charset.indexOf(";"));
            }
        }
        String normalizedEncoding = HttpHeaderHelper.mapCharset(charset, "UTF-8");
        try {
            String stringData = new String(bareBytes, normalizedEncoding);
            return new DataHandler(stringData, contentType);
        } catch (UnsupportedEncodingException e) {
            // this space intentionally left blank.
        }
        return new DataHandler(bareBytes, contentType);
    }
    
    @Override
    protected byte[] getBytes(Object object) {
        DataHandler handler = (DataHandler) object;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            InputStream stream = handler.getInputStream();
            IOUtils.copy(stream, baos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }
}
