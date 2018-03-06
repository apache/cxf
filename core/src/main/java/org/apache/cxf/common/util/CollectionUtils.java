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

package org.apache.cxf.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

public final class CollectionUtils {
    private CollectionUtils() {

    }

    public static <T> Collection<T> diff(Collection<T> c1, Collection<T> c2) {
        if (c1 == null || c1.isEmpty() || c2 == null || c2.size() == 0) {
            return c1;
        }
        Collection<T> difference = new ArrayList<>();
        for (T item : c1) {
            if (!c2.contains(item)) {
                difference.add(item);
            }
        }
        return difference;
    }

    public static <T> boolean isEmpty(Collection<T> c) {
        if (c == null || c.isEmpty()) {
            return true;
        }
        for (Iterator<T> iter = c.iterator(); iter.hasNext();) {
            if (iter.next() != null) {
                return false;
            }
        }
        return true;
    }

    public static <S, T> Dictionary<S, T> singletonDictionary(S s, T t) {
        return toDictionary(Collections.singletonMap(s, t));
    }
    public static <S, T> Dictionary<S, T> toDictionary(Map<S, T> map) {
        return new MapToDictionary<S, T>(map);
    }
    
    static class MapToDictionary<S, T> extends Dictionary<S, T> {
        /**
         * Map source.
         **/
        private Map<S, T> map;

        MapToDictionary(Map<S, T> map) {
            this.map = map;
        }


        public Enumeration<T> elements() {
            if (map == null) {
                return null;
            }
            return new IteratorToEnumeration<T>(map.values().iterator());
        }

        public T get(Object key) {
            if (map == null)  {
                return null;
            }
            return map.get(key);
        }

        public boolean isEmpty() {
            if (map == null) {
                return true;
            }
            return map.isEmpty();
        }

        public Enumeration<S> keys() {
            if (map == null) {
                return null;
            }
            return new IteratorToEnumeration<S>(map.keySet().iterator());
        }

        public T put(S key, T value) {
            throw new UnsupportedOperationException();
        }

        public T remove(Object key) {
            throw new UnsupportedOperationException();
        }

        public int size() {
            if (map == null) {
                return 0;
            }
            return map.size();
        }
        
        
        static class IteratorToEnumeration<X> implements Enumeration<X> {
            private final Iterator<X> iter;

            IteratorToEnumeration(Iterator<X> iter) {
                this.iter = iter;
            }

            public boolean hasMoreElements() {
                if (iter == null) {
                    return false;
                }
                return iter.hasNext();
            }

            public X nextElement() {
                if (iter == null) {
                    return null;
                }
                return iter.next();
            }
        }
    }
}
