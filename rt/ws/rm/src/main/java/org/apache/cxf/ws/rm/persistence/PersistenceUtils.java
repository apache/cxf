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

package org.apache.cxf.ws.rm.persistence;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.attachment.AttachmentDeserializer;
import org.apache.cxf.attachment.AttachmentSerializer;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.LoadingByteArrayOutputStream;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.rm.RMMessageConstants;
import org.apache.cxf.ws.rm.RewindableInputStream;
import org.apache.cxf.ws.rm.v200702.SequenceAcknowledgement;

/**
 * 
 */
public final class PersistenceUtils {
    
    private static PersistenceUtils instance;
    private JAXBContext context;

    /**
     * Prevents instantiation.
     */
    private PersistenceUtils() {
    }
    
    public static PersistenceUtils getInstance() {
        if (null == instance) {
            instance = new PersistenceUtils();
        }
        return instance;
    }

    public SequenceAcknowledgement deserialiseAcknowledgment(InputStream is) {
        Object obj = null;
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(is);
        try {
            obj = getContext().createUnmarshaller().unmarshal(reader);
            if (obj instanceof JAXBElement<?>) {
                JAXBElement<?> el = (JAXBElement<?>)obj;
                obj = el.getValue();
            }
        } catch (JAXBException ex) {
            throw new RMStoreException(ex);
        } finally {
            try {
                StaxUtils.close(reader);
                is.close();
            } catch (Throwable t) {
                // ignore, just cleaning up
            }
        }
        return (SequenceAcknowledgement)obj;
    }

    public InputStream serialiseAcknowledgment(SequenceAcknowledgement ack) {
        LoadingByteArrayOutputStream bos = new LoadingByteArrayOutputStream();
        try {
            getContext().createMarshaller().marshal(ack, bos);
        } catch (JAXBException ex) {
            throw new RMStoreException(ex);
        }
        return bos.createInputStream();
    }

    private JAXBContext getContext() throws JAXBException {
        if (null == context) {
            context = JAXBContext.newInstance(PackageUtils
                .getPackageName(SequenceAcknowledgement.class), 
                getClass().getClassLoader()); 
        }
        return context;
    }

    public static void encodeRMContent(RMMessage rmmsg, Message msg, InputStream msgContent)
        throws IOException {
        if (msg.getAttachments() == null) {
            rmmsg.setContentType((String)msg.get(Message.CONTENT_TYPE));
            rmmsg.setContent(msgContent);
        } else {
            MessageImpl msgImpl1 = new MessageImpl();
            // using cached output stream to handle large files
            CachedOutputStream cos = new CachedOutputStream();
            msgImpl1.setContent(OutputStream.class, cos);
            msgImpl1.setAttachments(msg.getAttachments());
            msgImpl1.put(Message.CONTENT_TYPE, (String) msg.get(Message.CONTENT_TYPE));
            msgImpl1.setContent(InputStream.class, msgContent);
            AttachmentSerializer serializer = new AttachmentSerializer(msgImpl1);
            serializer.setXop(false);
            serializer.writeProlog();
            // write soap root message into cached output stream
            IOUtils.copyAndCloseInput(msgContent, cos);
            serializer.writeAttachments();
            rmmsg.setContentType((String) msgImpl1.get(Message.CONTENT_TYPE));

            //TODO will pass the cos instance to rmmessage in the future
            rmmsg.setContent(cos.getInputStream());
        }
    }

    public static void decodeRMContent(RMMessage rmmsg, Message msg) throws IOException {
        String contentType = rmmsg.getContentType();
        if ((null != contentType) && contentType.startsWith("multipart/related")) {
            msg.put(Message.CONTENT_TYPE, contentType);
            msg.setContent(InputStream.class, rmmsg.getContent());
            AttachmentDeserializer ad = new AttachmentDeserializer(msg);
            ad.initializeAttachments();
        } else {
            msg.setContent(InputStream.class, rmmsg.getContent());
        }
        InputStream is = RewindableInputStream.makeRewindable(msg.getContent(InputStream.class));
        msg.setContent(InputStream.class, is);
        msg.put(RMMessageConstants.SAVED_CONTENT, is);
    }
}
