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
package org.apache.cxf.aegis.type.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.basic.ArrayType;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;

public class CollectionType extends ArrayType {
    private AegisType componentType;

    public CollectionType(AegisType componentType) {
        super();

        this.componentType = componentType;
    }

    @Override
    public Object readObject(MessageReader reader, Context context) throws DatabindingException {
        try {
            return readCollection(reader, null, context);
        } catch (IllegalArgumentException e) {
            throw new DatabindingException("Illegal argument.", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Collection<Object> createCollection() {
        Collection values = null;
        
        /*
         * getTypeClass returns the type of the object. These 'if's asked if the proposed
         * type can be assigned to the object, not the other way around. Thus List before
         * Vector and Set before SortedSet.
         */
        
        Class userTypeClass = getTypeClass();
        if (userTypeClass.isAssignableFrom(List.class)) {
            values = new ArrayList();
        } else if (userTypeClass.isAssignableFrom(LinkedList.class)) {
            values = new LinkedList();
        } else if (userTypeClass.isAssignableFrom(Set.class)) {
            values = new HashSet();
        } else if (userTypeClass.isAssignableFrom(SortedSet.class)) {
            values = new TreeSet();
        } else if (userTypeClass.isAssignableFrom(Vector.class)) {
            values = new Vector();
        } else if (userTypeClass.isAssignableFrom(Stack.class)) {
            values = new Stack();
        } else if (userTypeClass.isInterface()) {
            values = new ArrayList();
        } else {
            try {
                values = (Collection<Object>)userTypeClass.newInstance();
            } catch (Exception e) {
                throw new DatabindingException("Could not create map implementation: "
                                               + userTypeClass.getName(), e);
            }
        }

        return values;
    }

    @Override
    public void writeObject(Object object,
                            MessageWriter writer,
                            Context context) throws DatabindingException {
        if (object == null) {
            return;
        }

        try {
            Collection list = (Collection)object;

            AegisType type = getComponentType();

            if (type == null) {
                throw new DatabindingException("Couldn't find component type for Collection.");
            }

            for (Iterator itr = list.iterator(); itr.hasNext();) {
                String ns = null;
                if (type.isAbstract()) {
                    ns = getSchemaType().getNamespaceURI();
                } else {
                    ns = type.getSchemaType().getNamespaceURI();
                }

                writeValue(itr.next(), writer, context, type, type.getSchemaType().getLocalPart(), ns);
            }
        } catch (IllegalArgumentException e) {
            throw new DatabindingException("Illegal argument.", e);
        }
    }

    @Override
    public AegisType getComponentType() {
        return componentType;
    }
}
