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
package org.apache.cxf.aegis.type.encoded;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.cxf.aegis.Context;

/**
 * MarshalRegistry maintains tracks which object instances have been marshaled 
 * and which objects still need to be marshaled.
 * <p/>
 * Note: only the StructType register objects with this class.
 */
public class MarshalRegistry implements Iterable<Object> {
    /**
     * All objects instances with ids.  It is CRITICAL that is be an IdentityHashMap to assure we don't
     * exclude instances that are equivilant but different instances.
     */
    private final Map<Object, String> instances = new IdentityHashMap<Object, String>();

    /**
     * The objects not yet marshaled.  The is maintained as a map for debugging purposes. It is IMPORTANT
     * that this be a LinkedHashMap so we write the objects in the order they were discovered in the object
     * graphs (and writes them in numeric order).
     */
    private final Map<String, Object> notMarshalled = new LinkedHashMap<String, Object>();

    /**
     * The next id.
     */
    private int nextId;

    /**
     * Get the MarshalRegistry stored in the context, and if necessary create a new one.
     *
     * @param context the unmarshal context
     * @return the SoapRefRegistry; never null
     */
    public static MarshalRegistry get(Context context) {
        MarshalRegistry marshalRegistry = context.getProperty(MarshalRegistry.class);
        if (marshalRegistry == null) {
            marshalRegistry = new MarshalRegistry();
            context.setProperty(marshalRegistry);
        }
        return marshalRegistry;
    }


    public String getInstanceId(Object instance) {
        String id = instances.get(instance);
        if (id == null) {
            id = "" + nextId++;
            instances.put(instance, id);
            notMarshalled.put(id, instance);
        }
        return id;
    }

    /**
     * Returns an iterator over the object instances that have not been marshalled yet.  As each instance in
     * this iterator is written, it may contain references to additional objects that have not been written.
     * Those references objects will be added to the end of this iterator, so the "list" that is being
     * iterated over grows as the iteration preceeds.
     * <p/>
     * When an instance is returned from this iterator it is marked as marshalled.
     *
     * @return an iterator over the objects to be marshalled.
     */
    public Iterator<Object> iterator() {
        return new Iterator<Object>() {
            public boolean hasNext() {
                return !notMarshalled.isEmpty();
            }

            public Object next() {
                // remove the first entry in the notMarshalled map
                Iterator<Object> iterator = notMarshalled.values().iterator();
                Object instance = iterator.next();
                iterator.remove();

                // return the instance
                return instance;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}