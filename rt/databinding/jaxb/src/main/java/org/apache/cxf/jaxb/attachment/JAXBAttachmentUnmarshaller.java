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

package org.apache.cxf.jaxb.attachment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.xml.bind.attachment.AttachmentUnmarshaller;

import org.apache.cxf.attachment.AttachmentUtil;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;

public class JAXBAttachmentUnmarshaller extends AttachmentUnmarshaller {
    private static final Logger LOG = LogUtils.getL7dLogger(JAXBAttachmentUnmarshaller.class);

    private Collection<Attachment> attachments;
    
    public JAXBAttachmentUnmarshaller(Collection<Attachment> attachments) {
        super();
        this.attachments = attachments;
    }

    @Override
    public DataHandler getAttachmentAsDataHandler(String contentId) {
        return new DataHandler(AttachmentUtil.getAttachmentDataSource(contentId, attachments));
    }

    @Override
    public byte[] getAttachmentAsByteArray(String contentId) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            InputStream is = AttachmentUtil.getAttachmentDataSource(contentId, attachments)
                                           .getInputStream();
            IOUtils.copy(is, bos);
            is.close();
            bos.close();
        } catch (IOException e) {
            throw new Fault(new org.apache.cxf.common.i18n.Message("ATTACHMENT_READ_ERROR", LOG), e);
        }
        return bos.toByteArray();
    }

    @Override
    public boolean isXOPPackage() {
        return attachments != null;
    }

}
