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
package org.apache.cxf.aegis.type;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import javax.xml.namespace.QName;

public interface TypeCreator {
    /**
     * Get the mapped name of a method parameter.
     * 
     * @param m
     * @param index
     * @return
     */
    QName getElementName(Method m, int index);

    AegisType createType(Method m, int index);

    AegisType createType(PropertyDescriptor pd);
    
    AegisType createType(Type type);

    AegisType createType(Field f);

    TypeCreator getParent();
    
    void setParent(TypeCreator creator);
        
    void setTypeMapping(TypeMapping typeMapping);

    /** Retrieve the classInfo for a method. Needed to get parameters right. 
     * 
     * @param m Method object
     * @param index index in the parameter list
     * @return info
     */
    TypeClassInfo createClassInfo(Method m, int index); 

    /**
     * Create class info for a Type.
     * @param itemType
     * @return info
     */
    TypeClassInfo createBasicClassInfo(Type itemType);
    
    /**
     * Turn a TypeClassInfo into a type.
     * @param info
     * @return
     */
    AegisType createTypeForClass(TypeClassInfo info);
}
