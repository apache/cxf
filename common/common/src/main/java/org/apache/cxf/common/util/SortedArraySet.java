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

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicReference;


/**
 * This class implements most of the <tt>Set</tt> interface, backed by a 
 * sorted Array.  This makes iterators very fast, lookups are log(n), but
 * adds are fairly expensive.
 * 
 * This class is also threadsafe, but without synchronizations.   Lookups
 * and iterators will iterate over the state of the Set when the iterator
 * was created.
 *
 * If no data is stored in the Set, it uses very little memory.  The backing
 * array is created on demand.
 * 
 * This class is primarly useful for stuff that will be setup at startup, but
 * then iterated over MANY times during runtime.
 *   
 * @param <T>
 */
public final class SortedArraySet<T> implements SortedSet<T> {
    final AtomicReference<T[]> data = new AtomicReference<T[]>();

    public void clear() {
        data.set(null);
    }

    public boolean isEmpty() {
        T[] tmp = data.get();
        return tmp == null || tmp.length == 0;
    }

    public Iterator<T> iterator() {
        return new SASIterator<T>(data.get());
    }

    public int size() {
        T[] tmp = data.get();
        return tmp == null ? 0 : tmp.length;
    }
    
    @SuppressWarnings("unchecked")
    private T[] newArray(int size) {
        return (T[])new Object[size];
    }
    
    public boolean add(T o) {
        if (!contains(o)) {
            T[] tmp = data.get();
            T[] tmp2;
            if (tmp == null) {
                tmp2 = newArray(1);
                tmp2[0] = o;
            } else {
                tmp2 = newArray(tmp.length + 1);
                System.arraycopy(tmp, 0, tmp2, 0, tmp.length);
                tmp2[tmp2.length - 1] = o;
                Arrays.sort(tmp2);
            }
            
            if (!data.compareAndSet(tmp, tmp2)) {
                return add(o);
            }
            return true;
        }
        return false;
    }
    public boolean addAll(Collection<? extends T> c) {
        boolean val = false;
        for (T t : c) {
            val |= add(t);
        }
        return val;
    }
    public boolean containsAll(Collection<?> c) {
        boolean val = false;
        for (Object t : c) {
            val |= contains(t);
        }
        return val;
    }

    public boolean contains(Object o) {
        T[] tmp = data.get();
        if (tmp == null) {
            return false;
        }
        return Arrays.binarySearch(tmp, o) >= 0;
    }

    public boolean removeAll(Collection<?> c) {
        boolean val = false;
        for (Object t : c) {
            val |= remove(t);
        }
        return val;
    }
    public boolean retainAll(Collection<?> c) {
        boolean val = false;
        for (T t : this) {
            if (!c.contains(t)) {
                val |= remove(t);
            }
        }
        return val;
    }

    public boolean remove(Object o) {
        T[] tmp = data.get();
        
        if (tmp == null) {
            return false;
        }
        int idx = Arrays.binarySearch(tmp, o);
        if (idx != -1) {
            if (tmp.length == 1 
                && !data.compareAndSet(tmp, null)) {
                return remove(o);
            }
            T[] tmp2 = newArray(tmp.length - 1);
            System.arraycopy(tmp, 0, tmp2, 0, idx);
            System.arraycopy(tmp, idx + 1, tmp2, idx, tmp.length - 1 - idx);
            if (!data.compareAndSet(tmp, tmp2)) {
                return remove(o);
            }
            return true;
        }
        return false;
    }


    public Object[] toArray() {
        T[] tmp = data.get();
        if (tmp == null) {
            return new Object[0];
        }
        T[] tmp2 = newArray(tmp.length);
        System.arraycopy(tmp, 0, tmp2, 0, tmp.length);
        return tmp2;
    }

    @SuppressWarnings("unchecked")
    public <X> X[] toArray(X[] a) {
        T[] tmp = data.get();
        if (tmp == null && a.length != 0) {
            return (X[])java.lang.reflect.Array.
                newInstance(a.getClass().getComponentType(), 0);
        }
        
        if (a.length < tmp.length) {
            a = (X[])java.lang.reflect.Array.
                newInstance(a.getClass().getComponentType(), tmp.length);
        }
        System.arraycopy(tmp, 0, a, 0, tmp.length);
        if (a.length > tmp.length) {
            a[tmp.length] = null;
        }
        return a;
    }

    @SuppressWarnings("unchecked")
    public boolean equals(Object o) {
        SortedArraySet<T> as = (SortedArraySet<T>)o;
        return Arrays.equals(data.get(), as.data.get());
    }
    public String toString() {
        return Arrays.toString(data.get());
    }
    public int hashCode() {
        return Arrays.hashCode(data.get()); 
    }
    

    private class SASIterator<X> implements Iterator<X> {
        final X[] data;
        int idx;
        
        public SASIterator(X[] d) {
            data = d;
        }
        
        public boolean hasNext() {
            return data != null && idx != data.length;
        }

        public X next() {
            if (data == null || idx == data.length) {
                throw new NoSuchElementException();
            }
            return data[idx++];
        }

        public void remove() {
            if (idx > 0) {
                SortedArraySet.this.remove(data[idx - 1]);
            }
        }
    }


    public Comparator<? super T> comparator() {
        return null;
    }

    public T first() {
        T[] tmp = data.get();
        if (tmp == null || tmp.length == 0) {
            return null;
        }
        return tmp[0];
    }
    
    public T last() {
        T[] tmp = data.get();
        if (tmp == null || tmp.length == 0) {
            return null;
        }
        return tmp[tmp.length - 1];
    }

    public SortedSet<T> headSet(T toElement) {
        throw new UnsupportedOperationException();
    }

    public SortedSet<T> subSet(T fromElement, T toElement) {
        throw new UnsupportedOperationException();
    }

    public SortedSet<T> tailSet(T fromElement) {
        throw new UnsupportedOperationException();
    }

}
