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
import java.io.OutputStream;

import javax.activation.DataHandler;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.attachment.ByteDataSource;
import org.apache.cxf.message.Attachment;

/**
 * Byte arrays. Also handles MTOM.
 */
public class ByteArrayType extends AbstractXOPType {
    public ByteArrayType(boolean useXmimeBinaryType, String expectedContentTypes) {
        super(useXmimeBinaryType, expectedContentTypes);
        setTypeClass(byte[].class);
    }

    @Override
    protected Object readAttachment(Attachment att, Context context) throws IOException {
        DataHandler handler = att.getDataHandler();
        InputStream is = handler.getInputStream();

        // try
        // {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(is, out);
        is.close();
        return out.toByteArray();
    }

    public static void copy(InputStream input, OutputStream output) throws IOException {
        try {
            final byte[] buffer = new byte[8096];

            int n = input.read(buffer);
            while (-1 != n) {
                output.write(buffer, 0, n);
                n = input.read(buffer);
            }
        } finally {
            output.close();
            input.close();
        }
    }

    @Override
    protected Attachment createAttachment(Object object, String id) {
        byte[] data = (byte[])object;

        ByteDataSource source = new ByteDataSource(data);
        source.setContentType(getContentType(object, null));
        AttachmentImpl att = new AttachmentImpl(id, new DataHandler(source));
        att.setXOP(true);

        return att;
    }

    @Override
    protected String getContentType(Object object, Context context) {
        return "application/octet-stream";
    }

    @Override
    protected Object wrapBytes(byte[] bareBytes, String contentType) {
        return bareBytes;
    }

    @Override
    protected byte[] getBytes(Object object) {
        return (byte[])object;
    }
}
