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

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.UUID;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.xml.bind.attachment.AttachmentMarshaller;
import javax.xml.namespace.QName;

import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.attachment.AttachmentUtil;
import org.apache.cxf.attachment.ByteDataSource;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;

public class JAXBAttachmentMarshaller extends AttachmentMarshaller {

    private int threshold = 5 * 1024;
    private Collection<Attachment> atts;
    private boolean isXop;
    private QName lastElementName;

    public JAXBAttachmentMarshaller(Collection<Attachment> attachments, Integer mtomThreshold) {
        super();
        if (mtomThreshold != null) {
            threshold = mtomThreshold.intValue();
        }
        atts = attachments;
        isXop = attachments != null;
    }
    
    public QName getLastMTOMElementName() {
        return lastElementName;
    }

    public String addMtomAttachment(byte[] data, int offset, int length, String mimeType, String elementNS,
                                    String elementLocalName) {
        
        if (!isXop) {
            return null;
        }        
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        if (length < threshold) {
            return null;
        }
        ByteDataSource source = new ByteDataSource(data, offset, length);
        source.setContentType(mimeType);
        DataHandler handler = new DataHandler(source);

        String id;
        try {
            id = AttachmentUtil.createContentID(elementNS);
        } catch (UnsupportedEncodingException e) {
            throw new Fault(e);
        }
        AttachmentImpl att = new AttachmentImpl(id, handler);
        att.setXOP(this.isXop);
        atts.add(att);

        lastElementName = new QName(elementNS, elementLocalName);
        return "cid:" + id;
    }

    public String addMtomAttachment(DataHandler handler, String elementNS, String elementLocalName) {

        if (!isXop) {
            return null;
        }        

        // The following is just wrong. Even if the DataHandler has a stream, we should still
        // apply the threshold.
        try {
            DataSource ds = handler.getDataSource();
            if (ds instanceof FileDataSource) {
                FileDataSource fds = (FileDataSource)ds;
                File file = fds.getFile();
                if (file.length() < threshold) {
                    return null;
                }
            } else if (ds.getClass().getName().endsWith("ObjectDataSource")) {
                Object o = handler.getContent();
                if (o instanceof String 
                    && ((String)o).length() < threshold) {
                    return null;
                } else if (o instanceof byte[] && ((byte[])o).length < threshold) {
                    return null;
                }
            }
        } catch (IOException e1) {
        //      ignore, just do the normal attachment thing
        }
        
        String id;
        try {
            id = AttachmentUtil.createContentID(elementNS);
        } catch (UnsupportedEncodingException e) {
            throw new Fault(e);
        }
        AttachmentImpl att = new AttachmentImpl(id, handler);
        att.setXOP(this.isXop);
        atts.add(att);
        lastElementName = new QName(elementNS, elementLocalName);

        return "cid:" + id;
    }

    @Override
    public String addSwaRefAttachment(DataHandler handler) {
        String id = UUID.randomUUID() + "@apache.org";
        AttachmentImpl att = new AttachmentImpl(id, handler);
        att.setXOP(false);
        atts.add(att);
        return id;
    }

    public void setXOPPackage(boolean xop) {
        this.isXop = xop;
    }

    @Override
    public boolean isXOPPackage() {
        return isXop;
    }
}
