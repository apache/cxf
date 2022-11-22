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

package org.apache.cxf.message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.service.model.MessagePartInfo;

public class MessageContentsList extends ArrayList<Object> {

    /**
     * Indicates that the element of the underlying list is absent.
     * This is necessary for the elements to keep their original
     * indexes within this list when some preceding elements
     * are not populated or deleted.
     * 
     * @see {@link #get(MessagePartInfo)}, {@link #remove(MessagePartInfo)}
     */
    public static final Object REMOVED_MARKER = new Object();
    private static final long serialVersionUID = -5780720048950696258L;

    public MessageContentsList() {
        super(6);
    }
    public MessageContentsList(Object ... values) {
        super(Arrays.asList(values));
    }
    public MessageContentsList(List<?> values) {
        super(values);
    }

    public static MessageContentsList getContentsList(Message msg) {
        List<Object> o = CastUtils.cast(msg.getContent(List.class));
        if (o == null) {
            return null;
        }
        if (!(o instanceof MessageContentsList)) {
            MessageContentsList l2 = new MessageContentsList(o);
            msg.setContent(List.class, l2);
            return l2;
        }
        return (MessageContentsList)o;
    }

    @Override
    public Object set(int idx, Object value) {
        ensureSize(idx);
        return super.set(idx, value);
    }

    private void ensureSize(int idx) {
        while (idx >= size()) {
            add(REMOVED_MARKER);
        }
    }

    public Object put(MessagePartInfo key, Object value) {
        ensureSize(key.getIndex());
        return super.set(key.getIndex(), value);
    }

    public boolean hasValue(MessagePartInfo key) {
        if (key.getIndex() >= size()) {
            return false;
        }
        return super.get(key.getIndex()) != REMOVED_MARKER;
    }

    /**
     * @param key the key whose associated element is to be returned.
     * @return the element to which the index property of the specified key
     * is mapped, or {@code null} if mapped element is marked as removed.
     */
    public Object get(MessagePartInfo key) {
        Object o = super.get(key.getIndex());
        return o == REMOVED_MARKER ? null : o;
    }

    /**
     * Marks corresponding element as removed, indicating absent value,
     * so subsequent {@code get(MessagePartInfo)} for the same key return null.
     * @param key the key whose associated element is to be marked as removed.
     */
    public void remove(MessagePartInfo key) {
        put(key, REMOVED_MARKER);
    }

    /**
     * Allocates a new array containing the elements of the underlying list.
     *
     * @return an array containing all the elements of this list which are not
     * marked as removed and {@code null} instead of those elements which are
     * marked as removed, producing the same sequence of the elements as when
     * sequentially iterating through underlying list using {@code get(int)}.
     *                                                                              
     * @see {@link #get(MessagePartInfo)}
     */
    @Override
    public Object[] toArray() {
        final int size = size();
        Object[] array = new Object[size];
        for (int idx = 0; idx < size; ++idx) {
            Object o = super.get(idx);
            if (o != REMOVED_MARKER) {
                array[idx] = o;
            }
        }
        return array;
    }

}
