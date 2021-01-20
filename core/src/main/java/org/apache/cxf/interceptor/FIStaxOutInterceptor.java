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

package org.apache.cxf.interceptor;


import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.sun.xml.fastinfoset.stax.StAXDocumentSerializer;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;


/**
 * Creates an XMLStreamReader from the InputStream on the Message.
 */
public class FIStaxOutInterceptor extends AbstractPhaseInterceptor<Message> {
    public static final String FI_ENABLED = "org.apache.cxf.fastinfoset.enabled";

    private static final Logger LOG = LogUtils.getL7dLogger(FIStaxOutInterceptor.class);
    private static final String OUTPUT_STREAM_HOLDER = FIStaxOutInterceptor.class.getName() + ".outputstream";
    private static final StaxOutEndingInterceptor ENDING = new StaxOutEndingInterceptor(OUTPUT_STREAM_HOLDER);

    boolean force;
    private Integer serializerAttributeValueMapMemoryLimit;
    private Integer serializerMinAttributeValueSize;
    private Integer serializerMaxAttributeValueSize;
    private Integer serializerCharacterContentChunkMapMemoryLimit;
    private Integer serializerMinCharacterContentChunkSize;
    private Integer serializerMaxCharacterContentChunkSize;

    public FIStaxOutInterceptor() {
        super(Phase.PRE_STREAM);
        addAfter(AttachmentOutInterceptor.class.getName());
        addBefore(StaxOutInterceptor.class.getName());
    }
    public FIStaxOutInterceptor(boolean f) {
        this();
        force = f;
    }

    @Override
    public void handleFault(Message message) {
        super.handleFault(message);
        OutputStream os = (OutputStream)message.get(OUTPUT_STREAM_HOLDER);
        if (os != null) {
            message.setContent(OutputStream.class, os);
        }
    }

    public void handleMessage(Message message) {
        OutputStream out = message.getContent(OutputStream.class);
        XMLStreamWriter writer = message.getContent(XMLStreamWriter.class);
        if (out == null || writer != null) {
            return;
        }

        boolean req = isRequestor(message);
        Object o = message.getContextualProperty(FI_ENABLED);
        if (!req) {
            if (message.getExchange().getInMessage() != null) {
                //check incoming accept header
                String s = (String)message.getExchange().getInMessage().get(Message.ACCEPT_CONTENT_TYPE);
                if (s != null && s.contains("fastinfoset")) {
                    o = Boolean.TRUE;
                }
            }
        } else {
            Map<String, List<String>> headers
                = CastUtils.cast((Map<?, ?>)message.get(Message.PROTOCOL_HEADERS));
            List<String> accepts = headers.get("Accept");
            if (accepts == null) {
                accepts = new ArrayList<>();
                headers.put("Accept", accepts);
            }
            String a = "application/fastinfoset";
            if (!accepts.isEmpty()) {
                a += ", " + accepts.get(0);
                accepts.set(0, a);
            } else {
                accepts.add(a);
            }
        }

        if (force
            || PropertyUtils.isTrue(o)) {
            XMLStreamWriter serializer = getOutput(out);
            message.setContent(XMLStreamWriter.class, serializer);

            message.removeContent(OutputStream.class);
            message.put(OUTPUT_STREAM_HOLDER, out);
            message.put(AbstractOutDatabindingInterceptor.DISABLE_OUTPUTSTREAM_OPTIMIZATION,
                  Boolean.TRUE);

            String s = (String)message.get(Message.CONTENT_TYPE);
            if (s != null && s.contains("application/soap+xml")) {
                s = s.replace("application/soap+xml", "application/soap+fastinfoset");
                message.put(Message.CONTENT_TYPE, s);
            } else {
                message.put(Message.CONTENT_TYPE, "application/fastinfoset");
            }

            try {
                serializer.writeStartDocument();
            } catch (XMLStreamException e) {
                throw new Fault(e);
            }
            message.getInterceptorChain().add(ENDING);
        }
    }

    private XMLStreamWriter getOutput(OutputStream out) {
        /*
        StAXDocumentSerializer serializer = (StAXDocumentSerializer)m.getExchange().getEndpoint()
            .remove(StAXDocumentSerializer.class.getName());
        if (serializer != null) {
            serializer.setOutputStream(out);
        } else {
            serializer = new StAXDocumentSerializer(out);
        }
        return serializer;
        */
        final StAXDocumentSerializer stAXDocumentSerializer = new StAXDocumentSerializer(out);
        if (serializerAttributeValueMapMemoryLimit != null && serializerAttributeValueMapMemoryLimit.intValue() > 0) {
            stAXDocumentSerializer.setAttributeValueMapMemoryLimit(serializerAttributeValueMapMemoryLimit.intValue());
        }
        if (serializerMinAttributeValueSize != null && serializerMinAttributeValueSize.intValue() > 0) {
            stAXDocumentSerializer.setMinAttributeValueSize(serializerMinAttributeValueSize.intValue());
        }
        if (serializerMaxAttributeValueSize != null && serializerMaxAttributeValueSize.intValue() > 0) {
            stAXDocumentSerializer.setMaxAttributeValueSize(serializerMaxAttributeValueSize.intValue());
        }
        if (serializerCharacterContentChunkMapMemoryLimit != null
                && serializerCharacterContentChunkMapMemoryLimit.intValue() > 0) {
            stAXDocumentSerializer
                    .setCharacterContentChunkMapMemoryLimit(serializerCharacterContentChunkMapMemoryLimit.intValue());
        }
        if (serializerMinCharacterContentChunkSize != null && serializerMinCharacterContentChunkSize.intValue() > 0) {
            stAXDocumentSerializer.setMinCharacterContentChunkSize(serializerMinCharacterContentChunkSize.intValue());
        }
        if (serializerMaxCharacterContentChunkSize != null && serializerMaxCharacterContentChunkSize.intValue() > 0) {
            stAXDocumentSerializer.setMaxCharacterContentChunkSize(serializerMaxCharacterContentChunkSize.intValue());
        }
        return stAXDocumentSerializer;
    }

    public Integer getSerializerMinAttributeValueSize() {
        return serializerMinAttributeValueSize;
    }

    /**
     * Sets the property <code>minAttributeValueSize</code> on FastInfoset StAX Serializers created and used
     * by this interceptor. The property controls the <b>minimum</b> size of attribute values to be indexed.
     *
     * @param serializerMinAttributeValueSize
     *         The <b>minimum</b> size for attribute values to be indexed,
     *         measured as a number of characters. The default is typically 0.
     */
    public void setSerializerMinAttributeValueSize(Integer serializerMinAttributeValueSize) {
        logSetter("serializerMinAttributeValueSize", serializerMinAttributeValueSize);
        this.serializerMinAttributeValueSize = serializerMinAttributeValueSize;
    }

    public Integer getSerializerMaxAttributeValueSize() {
        return serializerMaxAttributeValueSize;
    }

    /**
     * Sets the property <code>maxAttributeValueSize</code> on FastInfoset StAX Serializers created and used
     * by this interceptor. The property controls the <b>maximum</b> size of attribute values to be indexed.
     * Tests have shown that setting this property to lower values reduces CPU burden of processing, at the expense
     * of larger sizes of resultant encoded Fast Infoset data.
     *
     * @param serializerMaxAttributeValueSize
     *         The <b>maximum</b> size for attribute values to be indexed,
     *         measured as a number of characters. The default is typically 32.
     */
    public void setSerializerMaxAttributeValueSize(Integer serializerMaxAttributeValueSize) {
        logSetter("serializerMaxAttributeValueSize", serializerMaxAttributeValueSize);
        this.serializerMaxAttributeValueSize = serializerMaxAttributeValueSize;
    }

    public Integer getSerializerCharacterContentChunkMapMemoryLimit() {
        return serializerCharacterContentChunkMapMemoryLimit;
    }

    /**
     * Sets the property <code>characterContentChunkMapMemoryLimit</code> on FastInfoset StAX Serializers created and
     * used by this interceptor. The property controls character content chunk map size and can be used to control the
     * memory and (indirectly) CPU footprint of processing.
     *
     * @param serializerCharacterContentChunkMapMemoryLimit
     *         The value for the limit, measured as a number of Unicode characters.
     */
    public void setSerializerCharacterContentChunkMapMemoryLimit(
            Integer serializerCharacterContentChunkMapMemoryLimit) {
        logSetter("serializerCharacterContentChunkMapMemoryLimit", serializerCharacterContentChunkMapMemoryLimit);
        this.serializerCharacterContentChunkMapMemoryLimit = serializerCharacterContentChunkMapMemoryLimit;
    }

    public Integer getSerializerMinCharacterContentChunkSize() {
        return serializerMinCharacterContentChunkSize;
    }

    /**
     * Sets the property <code>minCharacterContentChunkSize</code> on FastInfoset StAX Serializers created and used
     * by this interceptor. The property controls the <b>minimum</b> size of character content chunks to be indexed.
     *
     * @param serializerMinCharacterContentChunkSize
     *         The <b>minimum</b> size for character content chunks to be indexed,
     *         measured as a number of characters. The default is typically 0.
     */
    public void setSerializerMinCharacterContentChunkSize(Integer serializerMinCharacterContentChunkSize) {
        logSetter("serializerMinCharacterContentChunkSize", serializerMinCharacterContentChunkSize);
        this.serializerMinCharacterContentChunkSize = serializerMinCharacterContentChunkSize;
    }

    public Integer getSerializerMaxCharacterContentChunkSize() {
        return serializerMaxCharacterContentChunkSize;
    }

    /**
     * Sets the property <code>maxCharacterContentChunkSize</code> on FastInfoset StAX Serializers created and used
     * by this interceptor. The property controls the <b>maximum</b> size of character content chunks to be indexed.
     * Tests have shown that setting this property to lower values reduces CPU burden of processing, at the expense
     * of larger sizes of resultant encoded Fast Infoset data.
     *
     * @param serializerMaxCharacterContentChunkSize
     *         The <b>maximum</b> size for character content chunks to be indexed,
     *         measured as a number of characters. The default is typically 32.
     */
    public void setSerializerMaxCharacterContentChunkSize(Integer serializerMaxCharacterContentChunkSize) {
        logSetter("serializerMaxCharacterContentChunkSize", serializerMaxCharacterContentChunkSize);
        this.serializerMaxCharacterContentChunkSize = serializerMaxCharacterContentChunkSize;
    }

    public Integer getSerializerAttributeValueMapMemoryLimit() {
        return serializerAttributeValueMapMemoryLimit;
    }

    /**
     * Sets the property <code>attributeValueMapMemoryLimit</code> on FastInfoset StAX Serializers created and used
     * by this interceptor. The property controls attribute value map size and can be used to control
     * the memory and (indirectly) CPU footprint of processing.
     *
     * @param serializerAttributeValueMapMemoryLimit
     *         The value for the limit, measured as a number of Unicode characters.
     */
    public void setSerializerAttributeValueMapMemoryLimit(Integer serializerAttributeValueMapMemoryLimit) {
        logSetter("serializerAttributeValueMapMemoryLimit", serializerAttributeValueMapMemoryLimit);
        this.serializerAttributeValueMapMemoryLimit = serializerAttributeValueMapMemoryLimit;
    }

    private void logSetter(String propertyName, Object propertyValue) {
        if (LOG.isLoggable(Level.CONFIG)) {
            LOG.config("Setting " + propertyName + " to " + propertyValue);
        }
    }
}
