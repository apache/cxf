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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.stream.XMLStreamReader;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
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
        CachedOutputStream cos = new CachedOutputStream();
        if (msg.getAttachments() == null) {
            rmmsg.setContentType((String)msg.get(Message.CONTENT_TYPE));
            IOUtils.copyAndCloseInput(msgContent, cos);
            cos.flush();
            rmmsg.setContent(cos);
        } else {
            MessageImpl msgImpl1 = new MessageImpl();
            msgImpl1.setContent(OutputStream.class, cos);
            msgImpl1.setAttachments(msg.getAttachments());
            msgImpl1.put(Message.CONTENT_TYPE, msg.get(Message.CONTENT_TYPE));
            msgImpl1.setContent(InputStream.class, msgContent);
            AttachmentSerializer serializer = new AttachmentSerializer(msgImpl1);
            serializer.setXop(false);
            serializer.writeProlog();
            // write soap root message into cached output stream
            IOUtils.copyAndCloseInput(msgContent, cos);
            cos.flush();
            serializer.writeAttachments();
            rmmsg.setContentType((String) msgImpl1.get(Message.CONTENT_TYPE));
            rmmsg.setContent(cos);
        }
    }

    public static void decodeRMContent(RMMessage rmmsg, Message msg) throws IOException {
        String contentType = rmmsg.getContentType();
        final CachedOutputStream cos = rmmsg.getContent();
        if ((null != contentType) && contentType.startsWith("multipart/related")) {
            final InputStream is = cos.getInputStream();
            msg.put(Message.CONTENT_TYPE, contentType);
            msg.setContent(InputStream.class, is);
            AttachmentDeserializer ad = new AttachmentDeserializer(msg);
            ad.initializeAttachments();
            // create new cos with soap envelope only
            CachedOutputStream cosSoap = new CachedOutputStream();
            IOUtils.copy(msg.getContent(InputStream.class), cosSoap);
            cosSoap.flush();
            msg.put(RMMessageConstants.SAVED_CONTENT, cosSoap);
            // REVISIT -- At the moment references must be hold for retransmission
            // and the final cleanup of the CachedOutputStream.
            msg.put(RMMessageConstants.ATTACHMENTS_CLOSEABLE, new Closeable() {

                @Override
                public void close() throws IOException {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                    try {
                        cos.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }

            });
        } else {
            msg.put(RMMessageConstants.SAVED_CONTENT, cos);
        }
    }

}
