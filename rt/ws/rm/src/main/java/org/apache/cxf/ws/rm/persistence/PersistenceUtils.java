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

import javax.xml.stream.XMLStreamReader;

import jakarta.activation.MimeType;
import jakarta.activation.MimeTypeParseException;
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
import org.apache.cxf.message.MessageUtils;
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
            if (types != null && !types.isEmpty()) {
                // the charset is added again by the AttachmentSerializer when the message is written
                ct = removeCharset(types.get(0));
            }
        }
        if (ct == null) {
            ct = (String) msg.get(Message.CONTENT_TYPE);
        }
        if (isMultipart(ct)) {
            return getMultipartRootType(ct);
        }
        return ct;
    }

    /**
     * Content type of the root part of a multipart message of the given type, used when the multipart
     * type is the only known type, like when it was stored as the type of the root part. The start-info
     * parameter written by the AttachmentSerializer holds the SOAP content type with its parameters,
     * like the SOAP 1.2 action, and the type parameter is the one of the root part, application/xop+xml
     * for an XOP package.
     */
    private static String getMultipartRootType(String multipartType) {
        String rootType = null;
        try {
            MimeType mt = new MimeType(multipartType);
            String type = mt.getParameter("type");
            String startInfo = mt.getParameter("start-info");
            if (XOP_CONTENT_TYPE.equalsIgnoreCase(type)) {
                MimeType xop = new MimeType(XOP_CONTENT_TYPE);
                if (startInfo != null) {
                    xop.setParameter("type", removeCharset(startInfo));
                }
                rootType = xop.toString();
            } else {
                String soapType = startInfo != null ? startInfo : type;
                if (soapType != null && !isMultipart(soapType)) {
                    rootType = removeCharset(soapType);
                }
            }
        } catch (MimeTypeParseException e) {
            LOG.log(Level.FINE, "Invalid multipart Content-Type", e);
        }
        if (rootType == null) {
            LOG.log(Level.WARNING, "The Content-Type of the SOAP root message is unknown, using "
                + DEFAULT_ROOT_CONTENT_TYPE);
            return DEFAULT_ROOT_CONTENT_TYPE;
        }
        return rootType;
    }

    /**
     * Type of the SOAP message wrapped into an XOP root part of the given content type, or null if
     * the content type is not the one of an XOP root part.
     */
    private static String getXopWrappedType(String rootType) {
        if (rootType == null) {
            return null;
        }
        MimeType mt;
        try {
            mt = new MimeType(rootType);
        } catch (MimeTypeParseException e) {
            return null;
        }
        if (!XOP_CONTENT_TYPE.equalsIgnoreCase(mt.getBaseType())) {
            return null;
        }
        String type = mt.getParameter("type");
        return type != null ? removeCharset(type) : DEFAULT_ROOT_CONTENT_TYPE;
    }

    private static boolean isMultipart(String ct) {
        return ct != null && ct.regionMatches(true, 0, "multipart/", 0, "multipart/".length());
    }

    /** Removes the charset parameter, but not the one in the value of another parameter */
    private static String removeCharset(String ct) {
        try {
            MimeType mt = new MimeType(ct);
            mt.removeParameter("charset");
            return mt.toString();
        } catch (MimeTypeParseException e) {
            // only keep the type/subtype of an invalid content type
            int index = ct.indexOf(';');
            return (index == -1 ? ct : ct.substring(0, index)).trim();
        }
    }

    /**
     * Encodes the content of a message, which is stored as it is if the message has no attachments.
     */
    public static void encodeRMContent(RMMessage rmmsg, Message msg, CachedOutputStream msgContent)
        throws IOException {
        if (msg.getAttachments() == null || msg.getAttachments().isEmpty()) {
            rmmsg.setContentType(getRootContentType(msg));
            rmmsg.setContent(msgContent);
        } else {
            encodeRMContent(rmmsg, msg, msgContent.getInputStream());
        }
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
            // an outbound MTOM message is stored as an XOP package, so that it is resent as one
            boolean xop = MessageUtils.isOutbound(msg) && AttachmentUtil.isMtomEnabled(msg);
            String rootType = getRootContentType(msg);
            String xopWrappedType = xop ? getXopWrappedType(rootType) : null;
            // the AttachmentSerializer wraps the SOAP type into the XOP root part type
            msgImpl1.put(Message.CONTENT_TYPE, xopWrappedType != null ? xopWrappedType : rootType);
            // the encoding of the root part, otherwise the AttachmentSerializer declares UTF-8
            Object encoding = msg.get(Message.ENCODING);
            if (encoding != null) {
                msgImpl1.put(Message.ENCODING, encoding);
            }
            msgImpl1.setContent(InputStream.class, msgContent);
            AttachmentSerializer serializer = new AttachmentSerializer(msgImpl1);
            serializer.setXop(xop);
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
