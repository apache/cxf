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

import java.util.Collection;
import java.util.UUID;

import javax.xml.namespace.QName;

import jakarta.activation.DataHandler;
import jakarta.xml.bind.attachment.AttachmentMarshaller;
import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.attachment.AttachmentUtil;
import org.apache.cxf.message.Attachment;

public class JAXBAttachmentMarshaller extends AttachmentMarshaller {

    private int threshold = 5 * 1024;
    private Collection<Attachment> atts;
    private boolean isXop;
    private QName lastElementName;

    public JAXBAttachmentMarshaller(Collection<Attachment> attachments, 
                                    Integer mtomThreshold,
                                    boolean mtomEnabled) {
        super();
        if (mtomThreshold != null) {
            threshold = mtomThreshold.intValue();
        }
        atts = attachments;
        isXop = mtomEnabled && (attachments != null);
    }

    public QName getLastMTOMElementName() {
        return lastElementName;
    }

    public String addMtomAttachment(byte[] data, int offset, int length, String mimeType, String elementNS,
                                    String elementLocalName) {

        Attachment att = AttachmentUtil.createMtomAttachment(
                             isXop, mimeType, elementNS, data, offset, length, threshold);
        if (att != null) {
            atts.add(att);
            lastElementName = new QName(elementNS, elementLocalName);
            return "cid:" + att.getId();
        }
        return null;
    }

    public String addMtomAttachment(DataHandler handler, String elementNS, String elementLocalName) {

        Attachment att = AttachmentUtil.createMtomAttachmentFromDH(isXop, handler, elementNS, threshold);
        if (att != null) {
            atts.add(att);
            lastElementName = new QName(elementNS, elementLocalName);
            return "cid:" + att.getId();
        }
        return null;
    }

    @Override
    public String addSwaRefAttachment(DataHandler handler) {
        String id = UUID.randomUUID() + "@apache.org";
        AttachmentImpl att = new AttachmentImpl(id, handler);
        att.setXOP(false);
        atts.add(att);
        return "cid:" + id;
    }

    public void setXOPPackage(boolean xop) {
        this.isXop = xop;
    }

    @Override
    public boolean isXOPPackage() {
        return isXop;
    }
}
