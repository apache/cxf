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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamReader;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import org.apache.cxf.attachment.AttachmentDeserializer;
import org.apache.cxf.attachment.AttachmentSerializer;
import org.apache.cxf.attachment.AttachmentUtil;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.helpers.CastUtils;
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

    private static final Logger LOG = LogUtils.getLogger(PersistenceUtils.class);

    private static final String DEFAULT_ROOT_CONTENT_TYPE = "text/xml";

    private static final String XOP_CONTENT_TYPE = "application/xop+xml";

    private static final Pattern CHARSET_PARAM =
        Pattern.compile(";\\s*charset\\s*=\\s*(\"[^\"]*\"|[^;\\s]*)", Pattern.CASE_INSENSITIVE);

    /** The type parameter of an XOP content type, whose quoted value may contain escaped quotes */
    private static final Pattern TYPE_PARAM =
        Pattern.compile(";\\s*type\\s*=\\s*(?:\"((?:[^\"\\\\]|\\\\.)*)\"|([^;\\s]+))",
                        Pattern.CASE_INSENSITIVE);

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

    /**
     * Content type of the SOAP root message (without the multipart wrapper), or null if unknown.
     * Captured outbound messages provide it via SAVED_CONTENT_TYPE; for inbound multipart
     * messages it is taken from the headers of the root part. The result is never a multipart type,
     * as the multipart wrapper is recreated when the message is written, so a multipart type would
     * end up nested into another multipart message.
     */
    private static String getRootContentType(Message msg) {
        String ct = (String)msg.get(RMMessageConstants.SAVED_CONTENT_TYPE);
        if (ct == null) {
            Map<String, List<String>> ih =
                CastUtils.cast((Map<?, ?>)msg.get(AttachmentDeserializer.ATTACHMENT_PART_HEADERS));
            List<String> types = ih != null ? ih.get("Content-Type") : null;
            ct = types != null && !types.isEmpty() ? String.join("; ", types) : null;
            if (ct != null) {
                // the charset is added again by the AttachmentSerializer when the message is written
                ct = CHARSET_PARAM.matcher(ct).replaceAll("").trim();
                if (ct.endsWith(";")) {
                    ct = ct.substring(0, ct.length() - 1).trim();
                }
            }
        }
        if (ct == null) {
            ct = (String) msg.get(Message.CONTENT_TYPE);
        }
        if (ct != null && ct.regionMatches(true, 0, "multipart/", 0, "multipart/".length())) {
            LOG.log(Level.WARNING, "The Content-Type of the SOAP root message is unknown, using "
                + DEFAULT_ROOT_CONTENT_TYPE);
            return DEFAULT_ROOT_CONTENT_TYPE;
        }
        return ct;
    }

    /**
     * Type of the SOAP message wrapped into an XOP root part of the given content type, or null if
     * the content type is not the one of an XOP root part.
     */
    private static String getXopWrappedType(String rootType) {
        if (rootType == null
            || !rootType.regionMatches(true, 0, XOP_CONTENT_TYPE, 0, XOP_CONTENT_TYPE.length())) {
            return null;
        }
        Matcher m = TYPE_PARAM.matcher(rootType);
        if (!m.find()) {
            return DEFAULT_ROOT_CONTENT_TYPE;
        }
        return m.group(1) != null ? m.group(1).replaceAll("\\\\(.)", "$1") : m.group(2);
    }

    public static void encodeRMContent(RMMessage rmmsg, Message msg, InputStream msgContent)
        throws IOException {
        CachedOutputStream cos = new CachedOutputStream();
        if (msg.getAttachments() == null || msg.getAttachments().isEmpty()) {
            rmmsg.setContentType(getRootContentType(msg));
            IOUtils.copyAndCloseInput(msgContent, cos);
            cos.flush();
            rmmsg.setContent(cos);
        } else {
            MessageImpl msgImpl1 = new MessageImpl();
            msgImpl1.setContent(OutputStream.class, cos);
            msgImpl1.setAttachments(msg.getAttachments());
            msgImpl1.put(Message.CONTENT_TYPE, getRootContentType(msg));
            // the encoding of the root part, otherwise the AttachmentSerializer declares UTF-8
            Object encoding = msg.get(Message.ENCODING);
            if (encoding != null) {
                msgImpl1.put(Message.ENCODING, encoding);
            }
            msgImpl1.setContent(InputStream.class, msgContent);
            AttachmentSerializer serializer = new AttachmentSerializer(msgImpl1);
            // a captured outbound MTOM message is stored as an XOP package, so that it is resent as one
            serializer.setXop(msg.get(RMMessageConstants.SAVED_CONTENT_TYPE) != null
                && AttachmentUtil.isMtomEnabled(msg));
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

            // restore the SOAP root content type, the multipart type is recreated when the message is sent
            String rootType = getRootContentType(msg);
            String xopWrappedType = getXopWrappedType(rootType);
            if (xopWrappedType != null) {
                // the message was an XOP package, so the multipart is recreated as one
                rootType = xopWrappedType;
                msg.put(Message.MTOM_ENABLED, Boolean.TRUE);
            }
            msg.put(RMMessageConstants.SAVED_CONTENT_TYPE, rootType);
            msg.put(Message.CONTENT_TYPE, rootType);

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
            msg.put(Message.CONTENT_TYPE, contentType);
            msg.put(RMMessageConstants.SAVED_CONTENT_TYPE, contentType);
        }
    }

}
