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

package org.apache.cxf.jaxws.context;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import jakarta.activation.DataHandler;
import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.message.Attachment;

/**
 * This is a package local attachments wrapper class to treat the jaxws attachments
 * as CXF's attachments.
 */
class WrappedAttachments implements Set<Attachment> {
    private Map<String, DataHandler> attachments;
    private Map<String, Attachment> cache;

    WrappedAttachments(Map<String, DataHandler> attachments) {
        this.attachments = attachments;
        this.cache = new HashMap<>();
    }

    public int size() {
        return attachments.size();
    }

    public boolean isEmpty() {
        return attachments.isEmpty();
    }

    public boolean contains(Object o) {
        if (o instanceof Attachment) {
            return attachments.containsKey(((Attachment) o).getId());
        }
        return false;
    }

    public Iterator<Attachment> iterator() {
        return new WrappedAttachmentsIterator(attachments.entrySet().iterator());
    }

    public Object[] toArray() {
        return toArray(new Object[0]);
    }

    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        T[] copy = a.length == attachments.size()
            ? a : (T[])Array.newInstance(a.getClass().getComponentType(), attachments.size());
        int i = 0;
        for (Map.Entry<String, DataHandler> entry : attachments.entrySet()) {
            Attachment o = cache.get(entry.getKey());
            if (o == null) {
                o = new AttachmentImpl(entry.getKey(), entry.getValue());
                cache.put(entry.getKey(), o);
            }
            copy[i++] = (T)o;
        }
        return copy;
    }

    public boolean add(Attachment e) {
        if (!attachments.containsKey(e.getId())) {
            attachments.put(e.getId(), e.getDataHandler());
            cache.put(e.getId(), e);
            return true;
        }
        return false;
    }

    public boolean remove(Object o) {
        if (o instanceof Attachment) {
            cache.remove(((Attachment) o).getId());
            return attachments.remove(((Attachment) o).getId()) != null;
        }
        return false;
    }

    public boolean containsAll(Collection<?> c) {
        boolean b = true;
        for (Iterator<?> it = c.iterator(); it.hasNext();) {
            Object o = it.next();
            if (!(o instanceof Attachment && attachments.containsKey(((Attachment) o).getId()))) {
                b = false;
                break;
            }
        }
        return b;
    }

    public boolean addAll(Collection<? extends Attachment> c) {
        boolean b = false;
        for (Iterator<? extends Attachment> it = c.iterator(); it.hasNext();) {
            Attachment o = it.next();
            if (!attachments.containsKey(o.getId())) {
                b = true;
                attachments.put(o.getId(), o.getDataHandler());
                cache.put(o.getId(), o);
            }
        }
        return b;
    }

    public boolean retainAll(Collection<?> c) {
        boolean b = false;
        Set<String> ids = new HashSet<>();
        for (Iterator<?> it = c.iterator(); it.hasNext();) {
            Object o = it.next();
            if (o instanceof Attachment) {
                ids.add(((Attachment)o).getId());
            }
        }

        for (Iterator<String> it = attachments.keySet().iterator(); it.hasNext();) {
            String k = it.next();
            if (!ids.contains(k)) {
                b = true;
                it.remove();
                cache.remove(k);
            }
        }
        return b;
    }

    public boolean removeAll(Collection<?> c) {
        boolean b = false;
        for (Iterator<?> it = c.iterator(); it.hasNext();) {
            Object o = it.next();
            if (o instanceof Attachment && attachments.containsKey(((Attachment) o).getId())) {
                b = true;
                attachments.remove(((Attachment) o).getId());
                cache.remove(((Attachment) o).getId());
            }
        }
        return b;
    }

    public void clear() {
        attachments.clear();
        cache.clear();
    }

    Map<String, DataHandler> getAttachments() {
        return attachments;
    }

    class WrappedAttachmentsIterator implements Iterator<Attachment> {
        private Iterator<Map.Entry<String, DataHandler>> iterator;
        private String key;

        WrappedAttachmentsIterator(Iterator<Map.Entry<String, DataHandler>> iterator) {
            this.iterator = iterator;
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public Attachment next() {
            Map.Entry<String, DataHandler> e = iterator.next();
            key = e.getKey();
            Attachment o = cache.get(key);
            if (o == null) {
                o = new AttachmentImpl(key, e.getValue());
                cache.put(key, o);
            }
            return o;
        }

        public void remove() {
            iterator.remove();
            cache.remove(key);
        }
    }
}
