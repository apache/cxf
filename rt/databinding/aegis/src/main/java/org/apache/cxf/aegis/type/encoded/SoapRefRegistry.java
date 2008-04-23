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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;

/**
 * SoapRefRegistry handles resolving all SOAP encoded references.  After each object is unmarshalled if the
 * xml contained a SOAP id attribute it is registered with that attribute.  As each object is unmarshalled if
 * any nested elements have a SOAP ref attribute it, the references is registered.  If there is a object
 * instance already registered with the referenced id, the SOAP reference is immediately set. Otherwise, the
 * reference is set when an object instance is registered with the id.  This allows for the objects to occur
 * in any order in the XML document.
 * <p/>
 * Note: only the StructType and TraillingBlocks register objects with this class.
 */
public class SoapRefRegistry {
    /**
     * The unmarshaled object instances by id.
     */
    private final SortedMap<String, Object> instances = new TreeMap<String, Object>();

    /**
     * The unresolved SOAP references by referenced id.
     */
    private final SortedMap<String, List<SoapRef>> unresolvedRefs = new TreeMap<String, List<SoapRef>>();

    /**
     * Get the SoapRefRegistry stored in the context, and if necessary create a new one.
     *
     * @param context the unmarshal context
     * @return the SoapRefRegistry; never null
     */
    public static SoapRefRegistry get(Context context) {
        SoapRefRegistry soapRefRegistry = context.getProperty(SoapRefRegistry.class);
        if (soapRefRegistry == null) {
            soapRefRegistry = new SoapRefRegistry();
            context.setProperty(soapRefRegistry);
        }
        return soapRefRegistry;
    }

    /**
     * Add an object instance to the registry.
     *
     * @param id the unique identifier of the instance
     * @param instance the instance
     * @throws DatabindingException if another object instance is already registered with the id
     */
    public void addInstance(String id, Object instance) {
        Object oldInstance = instances.put(id, instance);
        if (oldInstance != null) {
            throw new DatabindingException("Id " + id + " is already registered to instance " + instance);
        }
        List<SoapRef> list = unresolvedRefs.remove(id);
        if (list != null) {
            for (SoapRef soapRef : list) {
                soapRef.set(instance);
            }
        }
    }

    /**
     * Adds a reference to the specified id.  If an object is already registered with the specified id, the
     * SOAP reference will immedately be set.  Otherwise, the reference will be set when an object is
     * registered with the specified id.
     *
     * @param id the id of the referenced object instance
     * @param soapRef the reference to set
     */
    public void addRef(String id, SoapRef soapRef) {
        Object value = instances.get(id);
        if (value != null) {
            soapRef.set(value);
        } else {
            List<SoapRef> list = unresolvedRefs.get(id);
            if (list == null) {
                list = new ArrayList<SoapRef>();
                unresolvedRefs.put(id, list);
            }
            list.add(soapRef);
        }
    }

    /**
     * Gets the ids of the registered object instances.
     *
     * @return the ids of the registered object instances
     */
    public Set<String> getIds() {
        return Collections.unmodifiableSet(instances.keySet());
    }

    /**
     * Gets the unresolved SOAP references by referenced id.
     *
     * @return the unresolved SOAP references by referenced id
     */
    public SortedMap<String, List<SoapRef>> getUnresolvedRefs() {
        return unresolvedRefs;
    }
}
