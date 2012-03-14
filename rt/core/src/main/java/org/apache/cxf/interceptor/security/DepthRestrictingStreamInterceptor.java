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

package org.apache.cxf.interceptor.security;


import java.io.InputStream;
import java.util.List;

import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.DepthRestrictingStreamReader;
import org.apache.cxf.staxutils.StaxUtils;


/**
 * Creates an XMLStreamReader from the InputStream on the Message.
 */
public class DepthRestrictingStreamInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String JSON_CONTENT_TYPE = "application/json";
    private int elementCountThreshold = 2000;
    private int innerElementLevelThreshold = 20;
    private int innerElementCountThreshold = 50;
    
    public DepthRestrictingStreamInterceptor() {
        this(Phase.POST_STREAM);
    }
    
    public DepthRestrictingStreamInterceptor(String phase) {
        super(phase);
    }
    
    public DepthRestrictingStreamInterceptor(String phase, List<String> after) {
        super(phase);
        if (after != null) {
            addAfter(after);
        }
    }
    
    public DepthRestrictingStreamInterceptor(String phase, List<String> before, List<String> after) {
        this(phase, after);
        if (before != null) {
            addBefore(before);
        }
    }
    
    public void handleMessage(Message message) {
        
        if (canBeIgnored(message)) {
            return;
        }
        
        XMLStreamReader reader = null;
        InputStream is = message.getContent(InputStream.class);
        if (is != null) {
            reader = StaxUtils.createXMLStreamReader(is);
            message.setContent(InputStream.class, null);
        } else {
            reader = message.getContent(XMLStreamReader.class);
        }
        if (reader == null) {
            return;
        }
        DepthRestrictingStreamReader dr = 
            new DepthRestrictingStreamReader(reader,
                                             elementCountThreshold, 
                                             innerElementLevelThreshold,
                                             innerElementCountThreshold);
        message.setContent(XMLStreamReader.class, dr);
    }

    // custom subclasses can further customize it
    protected boolean canBeIgnored(Message message) {
        String ct = (String)message.get(Message.CONTENT_TYPE);
        return ct != null && (FORM_CONTENT_TYPE.equals(ct) || JSON_CONTENT_TYPE.equals(ct));
    }
    
    /**
     * Sets the acceptable total number of elements in the XML payload 
     * @param elementCountThreshold
     */
    public void setElementCountThreshold(int elementCountThreshold) {
        this.elementCountThreshold = elementCountThreshold;
    }

    public int getElementCountThreshold() {
        return elementCountThreshold;
    }

    /**
     * Sets the acceptable total stack depth in the XML payload 
     * @param elementLevelThreshold
     */
    public void setInnerElementLevelThreshold(int elementLevelThreshold) {
        this.innerElementLevelThreshold = elementLevelThreshold;
    }

    public int getInnerElementLevelThreshold() {
        return innerElementLevelThreshold;
    }

    /**
     * Sets the acceptable total number of child elements for the current XML element 
     * @param innerElementCountThreshold
     */
    public void setInnerElementCountThreshold(int innerElementCountThreshold) {
        this.innerElementCountThreshold = innerElementCountThreshold;
    }

    public int getInnerElementCountThreshold() {
        return innerElementCountThreshold;
    }
    
}
