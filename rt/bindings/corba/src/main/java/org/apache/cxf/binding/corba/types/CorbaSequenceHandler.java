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

public class CorbaSequenceHandler extends CorbaObjectHandler {

    private List<CorbaObjectHandler> elements = new ArrayList<CorbaObjectHandler>();
    private CorbaObjectHandler templateElement;
    private boolean hasRecursiveTypeElement;
    
    public CorbaSequenceHandler(QName seqName, QName seqIdlType, TypeCode seqTC, Object seqType) {
        super(seqName, seqIdlType, seqTC, seqType);
        hasRecursiveTypeElement = false;
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
    
    // These handle the case where we have an unbounded sequence and we need to 
    // construct Corba objects during the reading of an object.
    public CorbaObjectHandler getTemplateElement() {
        return templateElement;
    }

    public void setTemplateElement(CorbaObjectHandler el) {
        templateElement = el;
    }

    public void setElements(List<CorbaObjectHandler> els) {
        elements = els;
    }

    public void clear() {
        for (int i = 0; i < elements.size(); i++) {
            elements.get(i).clear();
        }
    }

    public boolean hasRecursiveTypeElement() {
        return hasRecursiveTypeElement;
    }

    public void hasRecursiveTypeElement(boolean value) {
        hasRecursiveTypeElement = value;
    }
}
