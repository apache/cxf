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

package org.apache.cxf.aegis.type.basic;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;

/**
 * 
 */
public class CharacterAsStringType extends AegisType {
    
    public static final QName CHARACTER_AS_STRING_TYPE_QNAME 
        = new QName("http://cxf.apache.org/aegisTypes", "char");
    
    private IntType intType;
    
    public CharacterAsStringType() {
        intType = new IntType();
    }

    /** {@inheritDoc}*/
    @Override
    public Object readObject(MessageReader reader, Context context) throws DatabindingException {
        Integer readInteger = (Integer)intType.readObject(reader, context);
        return new Character((char)readInteger.intValue());
    }

    /** {@inheritDoc}*/
    @Override
    public void writeObject(Object object, MessageWriter writer, Context context) 
        throws DatabindingException {
        Character charObject = (Character) object;
        intType.writeObject(Integer.valueOf(charObject.charValue()), writer, context);
    }

    @Override
    public boolean usesUtilityTypes() {
        return true;
    }

}
