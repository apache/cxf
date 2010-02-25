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
import java.util.concurrent.atomic.AtomicInteger;

public final class ModCountCopyOnWriteArrayList<T> extends CopyOnWriteArrayList<T> {
    AtomicInteger modCount = new AtomicInteger();
    
    public ModCountCopyOnWriteArrayList() {
        super();
    }
    public ModCountCopyOnWriteArrayList(Collection<? extends T> c) {
        super(c);
        if (c instanceof ModCountCopyOnWriteArrayList) {
            modCount.set(((ModCountCopyOnWriteArrayList)c).getModCount());
        }
    }
    
    public int getModCount() {
        return modCount.get();
    }
    
    public void setModCount(int i) {
        modCount.set(i);
    }
    
    @Override
    public void add(int index, T element) {
        super.add(index, element);
        modCount.incrementAndGet();
    }

    @Override
    public boolean add(T element) {
        if (super.add(element)) {
            modCount.incrementAndGet();
            return true;
        }
        return false;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        if (super.addAll(c)) {
            modCount.incrementAndGet();
            return true;
        }
        return false;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        if (super.addAll(index, c)) {
            modCount.incrementAndGet();
            return true;
        }
        return false;
    }

    @Override
    public int addAllAbsent(Collection<? extends T> c) {
        int i = super.addAllAbsent(c);
        if (i > 0) {
            modCount.incrementAndGet();
        }
        return i;
    }

    @Override
    public boolean addIfAbsent(T element) {
        if (super.addIfAbsent(element)) {
            modCount.incrementAndGet();
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        super.clear();
        modCount.incrementAndGet();
    }

    @Override
    public T remove(int index) {
        T t = super.remove(index);
        if (t != null) {
            modCount.incrementAndGet();
        }
        return t;
    }

    @Override
    public boolean remove(Object o) {
        if (super.remove(o)) {
            modCount.incrementAndGet();
            return true;
        }
        return false;
    }

    @Override
    public boolean removeAll(Collection c) {
        if (super.removeAll(c)) {
            modCount.incrementAndGet();
            return true;
        }
        return false;
    }

    @Override
    public boolean retainAll(Collection c) {
        if (super.retainAll(c)) {
            modCount.incrementAndGet();
            return true;
        }
        return false;
    }
    
    public int hashCode() {
        return super.hashCode() + modCount.get();
    }
    
    public boolean equals(Object o) {
        if (o instanceof ModCountCopyOnWriteArrayList) {
            return super.equals(o) && modCount.get() == ((ModCountCopyOnWriteArrayList)o).getModCount();
        }
        return false;
    }

}

