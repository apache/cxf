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
import java.util.Iterator;

public final class CollectionUtils {
    private CollectionUtils() {
        
    }
    
    public static <T> Collection<T> diff(Collection<T> c1, Collection<T> c2) {
        if (c1 == null || c1.size() == 0 || c2 == null || c2.size() == 0) {
            return c1;
        }
        Collection<T> difference = new ArrayList<T>();
        for (T item : c1) {
            if (!c2.contains(item)) {
                difference.add(item);
            }
        }
        return difference;
    }
    
    public static <T> boolean isEmpty(Collection<T> c) {
        if (c == null || c.size() == 0) {
            return true;
        }
        for (Iterator<T> iter = c.iterator(); iter.hasNext();) {
            if (iter.next() != null) {
                return false;
            }
        }
        return true;
    }
    
}
