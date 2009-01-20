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

package org.apache.cxf.phase;

import java.util.Collection;
import java.util.Set;

import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.common.util.SortedArraySet;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;

public abstract class AbstractPhaseInterceptor<T extends Message> implements PhaseInterceptor<T> {
    private final String id;
    private final String phase;
    private final Set<String> before = new SortedArraySet<String>();
    private final Set<String> after = new SortedArraySet<String>();

    public AbstractPhaseInterceptor(String phase) {
        this(null, phase, false);
    }
    public AbstractPhaseInterceptor(String i, String p) {
        this(i, p, false);
    }
    public AbstractPhaseInterceptor(String phase, boolean uniqueId) {
        this(null, phase, uniqueId);
    }
    public AbstractPhaseInterceptor(String i, String p, boolean uniqueId) {
        if (i == null) {
            i = getClass().getName();
        }
        if (uniqueId) {
            i += System.identityHashCode(this);
        }
        id = i;
        phase = p;
    }
    
    public void setBefore(Collection<String> i) {
        before.clear();
        before.addAll(i);
    }
    public void setAfter(Collection<String> i) {
        after.clear();
        after.addAll(i);
    }
    public void addBefore(Collection<String> i) {
        before.addAll(i);
    }
    public void addAfter(Collection<String> i) {
        after.addAll(i);
    }
    
    public void addBefore(String i) {
        before.add(i);
    }

    public void addAfter(String i) {
        after.add(i);
    }


    public final Set<String> getAfter() {
        return after;
    }

    public final Set<String> getBefore() {
        return before;
    }

    public final String getId() {
        return id;
    }

    public final String getPhase() {
        return phase;
    }


    public void handleFault(T message) {
    }

    public boolean isGET(T message) {
        String method = (String)message.get(Message.HTTP_REQUEST_METHOD);
        return "GET".equals(method) && message.getContent(XMLStreamReader.class) == null;
    }
    
    protected boolean isRequestor(T message) {
        return MessageUtils.isRequestor(message);
    }  

}
