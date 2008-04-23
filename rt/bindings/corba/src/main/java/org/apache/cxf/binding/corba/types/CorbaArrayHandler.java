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
package org.apache.cxf.binding.corba.types;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.omg.CORBA.TypeCode;

public class CorbaArrayHandler extends CorbaObjectHandler {

    private List<CorbaObjectHandler> elements = new ArrayList<CorbaObjectHandler>();
    
    public CorbaArrayHandler(QName arrayName, QName arrayIdlType, TypeCode arrayTC, Object arrayType) {
        super(arrayName, arrayIdlType, arrayTC, arrayType);
    }
    
    public void addElement(CorbaObjectHandler el) {
        elements.add(el);
    }
    
    public int getNumberOfElements() {
        return elements.size();
    }
    
    public List<CorbaObjectHandler> getElements() {
        return elements;
    }
    
    public CorbaObjectHandler getElement(int index) {
        return elements.get(index);
    }

    public void clear() {
        for (int i = 0; i < elements.size(); i++) {
            elements.get(i).clear();
        }
    }  
}
