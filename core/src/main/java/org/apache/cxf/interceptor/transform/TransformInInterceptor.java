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


import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.transform.TransformUtils;


/**
 * Creates an XMLStreamReader from the InputStream on the Message.
 */
public class TransformInInterceptor extends AbstractPhaseInterceptor<Message> {

    private List<String> inDropElements;
    private Map<String, String> inElementsMap;
    private Map<String, String> inAppendMap;
    private Map<String, String> inAttributesMap;
    private boolean blockOriginalReader = true;
    private String contextPropertyName;

    public TransformInInterceptor() {
        this(Phase.POST_STREAM);
        addBefore(StaxInInterceptor.class.getName());
    }

    public TransformInInterceptor(String phase) {
        super(phase);
    }

    public TransformInInterceptor(String phase, List<String> after) {
        super(phase);
        if (after != null) {
            addAfter(after);
        }
    }

    public TransformInInterceptor(String phase, List<String> before, List<String> after) {
        this(phase, after);
        if (before != null) {
            addBefore(before);
        }
    }

    public void handleMessage(Message message) {
        if (contextPropertyName != null
            && !MessageUtils.getContextualBoolean(message, contextPropertyName, false)) {
            return;
        }
        XMLStreamReader reader = message.getContent(XMLStreamReader.class);
        InputStream is = message.getContent(InputStream.class);

        XMLStreamReader transformReader = createTransformReaderIfNeeded(reader, is);
        if (transformReader != null) {
            message.setContent(XMLStreamReader.class, transformReader);
        }

    }

    protected XMLStreamReader createTransformReaderIfNeeded(XMLStreamReader reader, InputStream is) {
        return TransformUtils.createTransformReaderIfNeeded(reader, is,
                                                            inDropElements,
                                                            inElementsMap,
                                                            inAppendMap,
                                                            inAttributesMap,
                                                            blockOriginalReader);
    }

    public void setInAppendElements(Map<String, String> inElements) {
        this.inAppendMap = inElements;
    }

    public void setInDropElements(List<String> dropElementsSet) {
        this.inDropElements = dropElementsSet;
    }

    public void setInTransformElements(Map<String, String> inElements) {
        this.inElementsMap = inElements;
    }

    public void setInTransformAttributes(Map<String, String> inAttributes) {
        this.inAttributesMap = inAttributes;
    }

    public void setBlockOriginalReader(boolean blockOriginalReader) {
        this.blockOriginalReader = blockOriginalReader;
    }

    public void setContextPropertyName(String propertyName) {
        contextPropertyName = propertyName;
    }
}
