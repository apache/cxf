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
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.omg.CORBA.TypeCode;

public class CorbaStructHandler extends CorbaObjectHandler {

    List<CorbaObjectHandler> members = new ArrayList<CorbaObjectHandler>();
    
    public CorbaStructHandler(QName structName, QName structIdlType, TypeCode structTC, Object structType) {
        super(structName, structIdlType, structTC, structType);
    }
    
    public void addMember(CorbaObjectHandler member) {
        members.add(member);
    }
    
    public List<CorbaObjectHandler> getMembers() {
        return members;
    }
    
    public CorbaObjectHandler getMember(int index) {
        return members.get(index);
    }
    
    public CorbaObjectHandler getMemberByName(String name) {
        CorbaObjectHandler member = null;
        
        for (Iterator<CorbaObjectHandler> iterator = members.iterator(); iterator.hasNext();) {
            CorbaObjectHandler current = iterator.next();
            if (current.getName().getLocalPart().equals(name)) {
                member = current;
                break;
            }
        }
        
        return member;
    }

    public void clear() {
        for (int i = 0; i < members.size(); i++) {
            members.get(i).clear();
        }
    }  
}
