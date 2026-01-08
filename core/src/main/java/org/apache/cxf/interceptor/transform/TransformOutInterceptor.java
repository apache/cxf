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

package org.apache.cxf.interceptor.transform;


import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.interceptor.StaxOutEndingInterceptor;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.staxutils.transform.TransformUtils;

import static org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor.DISABLE_OUTPUTSTREAM_OPTIMIZATION;

/**
 * Creates an XMLStreamReader from the InputStream on the Message.
 */
public class TransformOutInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final String OUTPUT_STREAM_HOLDER =
        TransformOutInterceptor.class.getName() + ".outputstream";
    private static final String TRANSFORM_SKIP = "transform.skip";
    private static final StaxOutEndingInterceptor ENDING = new StaxOutEndingInterceptor(OUTPUT_STREAM_HOLDER);

    private Map<String, String> outElementsMap;
    private Map<String, String> outAppendMap;
    private List<String> outDropElements;
    private Map<String, String> outAttributesMap;
    private boolean attributesToElements;
    private boolean skipOnFault;
    private String contextPropertyName;
    private String defaultNamespace;

    public TransformOutInterceptor() {
        this(Phase.PRE_STREAM);
    }

    public TransformOutInterceptor(String phase) {
        super(phase);
        addBefore(StaxOutInterceptor.class.getName());
        addAfter("org.apache.cxf.ext.logging.LoggingOutInterceptor");
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
        if (!isHttpVerbSupported(message)) {
            return;
        }

        if (contextPropertyName != null
            && !MessageUtils.getContextualBoolean(message.getExchange().getInMessage(),
                                               contextPropertyName,
                                               false)) {
            return;
        }

        if (skipOnFault && null != message.getContent(Exception.class)
            || MessageUtils.getContextualBoolean(message, TRANSFORM_SKIP, false)) {
            return;
        }

        XMLStreamWriter writer = message.getContent(XMLStreamWriter.class);
        OutputStream out = message.getContent(OutputStream.class);

        XMLStreamWriter transformWriter = createTransformWriterIfNeeded(writer, out);
        if (transformWriter != null) {
            message.setContent(XMLStreamWriter.class, transformWriter);
            if (message.getContextualProperty(DISABLE_OUTPUTSTREAM_OPTIMIZATION) == null) {
                message.put(DISABLE_OUTPUTSTREAM_OPTIMIZATION, Boolean.TRUE);
            }
            if (MessageUtils.isRequestor(message)) {
                message.removeContent(OutputStream.class);
                message.put(OUTPUT_STREAM_HOLDER, out);
                message.getInterceptorChain().add(ENDING);
            }
        }
    }

    private String getEncoding(Message message) {
        Exchange ex = message.getExchange();
        String encoding = (String)message.get(Message.ENCODING);
        if (encoding == null && ex.getInMessage() != null) {
            encoding = (String) ex.getInMessage().get(Message.ENCODING);
            message.put(Message.ENCODING, encoding);
        }

        if (encoding == null) {
            encoding = StandardCharsets.UTF_8.name();
            message.put(Message.ENCODING, encoding);
        }
        return encoding;
    }

    protected XMLStreamWriter createTransformWriterIfNeeded(XMLStreamWriter writer, OutputStream os) {
        Message m = PhaseInterceptorChain.getCurrentMessage();
        String encoding = getEncoding(m);
        return TransformUtils.createTransformWriterIfNeeded(writer, os,
                                                      outElementsMap,
                                                      outDropElements,
                                                      outAppendMap,
                                                      outAttributesMap,
                                                      attributesToElements,
                                                      defaultNamespace,
                                                      encoding);
    }

    public void setOutTransformElements(Map<String, String> outElements) {
        this.outElementsMap = outElements;
    }

    public void setOutAppendElements(Map<String, String> map) {
        this.outAppendMap = map;
    }

    public void setOutDropElements(List<String> dropElementsSet) {
        this.outDropElements = dropElementsSet;
    }

    public void setOutTransformAttributes(Map<String, String> outAttributes) {
        this.outAttributesMap = outAttributes;
    }

    public void setAttributesToElements(boolean value) {
        this.attributesToElements = value;
    }

    public void setSkipOnFault(boolean value) {
        this.skipOnFault = value;
    }

    protected boolean isHttpVerbSupported(Message message) {
        return !(isRequestor(message) && isGET(message));
    }

    public void setContextPropertyName(String propertyName) {
        contextPropertyName = propertyName;
    }

    public void setDefaultNamespace(String defaultNamespace) {
        this.defaultNamespace = defaultNamespace;
    }

}
