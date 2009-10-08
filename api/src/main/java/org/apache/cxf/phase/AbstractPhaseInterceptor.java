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

/**
 * Provides a starting point implementation for a interceptors that 
 * participate in phased message processing. Developers should extend from 
 * this class when implementing custom interceptors.
 * Developers need to provide an implementation for handleMessage() and 
 * can overide the handleFault() implementation. They should not overide 
 * the other methods.
 */
public abstract class AbstractPhaseInterceptor<T extends Message> implements PhaseInterceptor<T> {
    private final String id;
    private final String phase;
    private final Set<String> before = new SortedArraySet<String>();
    private final Set<String> after = new SortedArraySet<String>();

    /**
     * Instantiates the interceptor to live in a specified phase. The 
     * interceptor's id will be set to the name of the implementing class.
     *
     * @param phase the interceptor's phase
     */
    public AbstractPhaseInterceptor(String phase) {
        this(null, phase, false);
    }

    /**
     * Instantiates the interceptor with a specified id.
     *
     * @param i the interceptor's id
     * @param p the interceptor's phase
     */
    public AbstractPhaseInterceptor(String i, String p) {
        this(i, p, false);
    }

    /**
     * Instantiates the interceptor and specifies if it gets a system 
     * determined unique id. If <code>uniqueId</code> is set to true the 
     * interceptor's id will be determined by the runtime. If 
     * <code>uniqueId</code> is set to false, the implementing class' name 
     * is used as the id.
     *
     * @param p the interceptor's phase
     * @param uniqueId
     */
    public AbstractPhaseInterceptor(String phase, boolean uniqueId) {
        this(null, phase, uniqueId);
    }

    /**
     * Instantiates the interceptor with a specified id or with a system 
     * determined unique id. The specified id will be used unless 
     * <code>uniqueId</code> is set to true.
     *
     * @param i the interceptor's id
     * @param p the interceptor's phase
     * @param uniqueId
     */
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
    
    /**
     * Specifies that the current interceptor needs to be added to the 
     * interceptor chain before the specified collection of interceptors. 
     * This method replaces any existing list with the provided list.
     * 
     * @param i a collection of interceptor ids
     */
    public void setBefore(Collection<String> i) {
        before.clear();
        before.addAll(i);
    }

    /**
     * Specifies that the current interceptor needs to be added to the 
     * interceptor chain after the specified collection of interceptors.
     * This method replaces any existing list with the provided list.
     * 
     * @param i a collection of interceptor ids
     */
    public void setAfter(Collection<String> i) {
        after.clear();
        after.addAll(i);
    }

    /**
     * Specifies that the current interceptor needs to be added to the 
     * interceptor chain before the specified collection of interceptors.
     * 
     * @param i a collection of interceptor ids
     */
    public void addBefore(Collection<String> i) {
        before.addAll(i);
    }

    /**
     * Specifies that the current interceptor needs to be added to the 
     * interceptor chain after the specified collection of interceptors.
     * 
     * @param i a collection of interceptor ids
     */
    public void addAfter(Collection<String> i) {
        after.addAll(i);
    }
    
    /**
     * Specifies that the current interceptor needs to be added to the 
     * interceptor chain before the specified interceptor.
     * 
     * @param i an interceptor id
     */
    public void addBefore(String i) {
        before.add(i);
    }

    /**
     * Specifies that the current interceptor needs to be added to the 
     * interceptor chain after the specified interceptor.
     * 
     * @param i an interceptor id
     */
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

}
