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

import javax.activation.DataHandler;
import javax.activation.DataSource;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.message.Attachment;

public class DataSourceType extends AbstractXOPType {
    public DataSourceType(boolean useXmimeBinaryType, String expectedContentTypes) {
        super(useXmimeBinaryType, expectedContentTypes);
    }

    @Override
    protected Object readAttachment(Attachment att, Context context) {
        return att.getDataHandler().getDataSource();
    }

    @Override
    protected Attachment createAttachment(Object object, String id) {
        DataSource source = (DataSource)object;

        DataHandler handler = new DataHandler(source);
        AttachmentImpl att = new AttachmentImpl(id, handler);
        att.setXOP(true);
        return att;
    }

    @Override
    protected String getContentType(Object object, Context context) {
        return ((DataSource)object).getContentType();
    }

    @Override
    protected Object wrapBytes(byte[] bareBytes, String contentType) {
        return new DataHandler(bareBytes, contentType).getDataSource();
    }

    @Override
    protected byte[] getBytes(Object object) {
        DataSource dataSource = (DataSource) object;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            InputStream stream = dataSource.getInputStream();
            IOUtils.copy(stream, baos);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return baos.toByteArray();
    }
}
