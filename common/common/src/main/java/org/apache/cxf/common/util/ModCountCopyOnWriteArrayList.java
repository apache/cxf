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

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ModCountCopyOnWriteArrayList<T> extends CopyOnWriteArrayList<T> {
    int modCount;
    
    public ModCountCopyOnWriteArrayList() {
        super();
    }
    public ModCountCopyOnWriteArrayList(Collection<? extends T> c) {
        super();
        synchronized (c) {
            addAll(c);
            if (c instanceof ModCountCopyOnWriteArrayList) {
                modCount = ((ModCountCopyOnWriteArrayList)c).getModCount();
            }
        }
    }
    
    public synchronized int getModCount() {
        return modCount;
    }
    
    public synchronized void setModCount(int i) {
        modCount = i;
    }
    
    @Override
    public synchronized void add(int index, T element) {
        ++modCount;
        super.add(index, element);
    }

    @Override
    public synchronized boolean add(T element) {
        ++modCount;
        return super.add(element);
    }

    @Override
    public synchronized boolean addAll(Collection<? extends T> c) {
        ++modCount;
        return super.addAll(c);
    }

    @Override
    public synchronized boolean addAll(int index, Collection<? extends T> c) {
        ++modCount;
        return super.addAll(index, c);
    }

    @Override
    public synchronized int addAllAbsent(Collection<? extends T> c) {
        ++modCount;
        return super.addAllAbsent(c);
    }

    @Override
    public synchronized boolean addIfAbsent(T element) {
        ++modCount;
        return super.addIfAbsent(element);
    }

    @Override
    public synchronized void clear() {
        ++modCount;
        super.clear();
    }

    @Override
    public synchronized T remove(int index) {
        ++modCount;
        return super.remove(index);
    }

    @Override
    public synchronized boolean remove(Object o) {
        ++modCount;
        return super.remove(o);
    }

    @Override
    public synchronized boolean removeAll(Collection c) {
        ++modCount;
        return super.removeAll(c);
    }

    @Override
    public synchronized boolean retainAll(Collection c) {
        ++modCount;
        return super.retainAll(c);
    }
    
    public synchronized int hashCode() {
        return super.hashCode() + modCount;
    }
    
    public synchronized boolean equals(Object o) {
        if (o instanceof ModCountCopyOnWriteArrayList) {
            return super.equals(o) && modCount == ((ModCountCopyOnWriteArrayList)o).getModCount();
        }
        return false;
    }

}

