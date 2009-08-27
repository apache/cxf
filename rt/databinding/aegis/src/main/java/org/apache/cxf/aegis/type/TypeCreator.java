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

import javax.xml.namespace.QName;


/**
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 */
public interface TypeCreator {
    /**
     * Get the mapped name of a method parameter.
     * 
     * @param m
     * @param index
     * @return
     */
    QName getElementName(Method m, int index);

    Type createType(Method m, int index);

    Type createType(PropertyDescriptor pd);
    
    Type createType(java.lang.reflect.Type type);

    Type createType(Field f);

    Type createType(Class clazz);

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
     * Retrieve the class info for a class. Needed to get parameters right.
     * @param itemClass
     * @return info
     */
    TypeClassInfo createBasicClassInfo(Class<?> itemClass);
    
    /**
     * Turn a TypeClassInfo into a type.
     * @param info
     * @return
     */
    Type createTypeForClass(TypeClassInfo info);
}
