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

import org.apache.cxf.binding.corba.wsdl.CaseType;
import org.apache.cxf.binding.corba.wsdl.Enum;
import org.apache.cxf.binding.corba.wsdl.Enumerator;
import org.apache.cxf.binding.corba.wsdl.Union;
import org.apache.cxf.binding.corba.wsdl.Unionbranch;

import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;

public class CorbaUnionHandler extends CorbaObjectHandler {

    private CorbaObjectHandler discriminator;
    private CorbaObjectHandler value;
    private List<CorbaObjectHandler> cases = new ArrayList<CorbaObjectHandler>();
    private int defaultIndex;
    private List<String> labels = new ArrayList<String>();

    public CorbaUnionHandler(QName unionName, QName unionIdlType, TypeCode unionTC, Object unionType) {
        super(unionName, unionIdlType, unionTC, unionType);
        
        // Build a list of labels.  This will be used to generate a discriminator value for the 
        // default case (since we are not provided with one from the Stax stream of the Celtix object)
        Union union = (Union)unionType;
        List<Unionbranch> branches = union.getUnionbranch();
        int index = 0;
        for (Iterator<Unionbranch> branchesIter = branches.iterator(); branchesIter.hasNext();) {
            Unionbranch branch = branchesIter.next();
            List<CaseType> branchCases = branch.getCase();
            if (branchCases.size() == 0) {
                defaultIndex = index;
            } else {
                for (Iterator<CaseType> casesIter = branchCases.iterator(); casesIter.hasNext();) {
                    CaseType ct = casesIter.next();
                    labels.add(ct.getLabel());
                }
            }
            
            index++;
        }
    }
    
    public CorbaObjectHandler getDiscriminator() {
        return discriminator;
    }
    
    public String getDisciminatorValueData() {
        String result = null;
        // The discriminator is handled by either the enum handler or the primitive handler.
        if (discriminator.getTypeCodeKind().value() == TCKind._tk_enum) {
            CorbaEnumHandler enumHandler = (CorbaEnumHandler)discriminator;
            result = enumHandler.getValue();
        } else {
            CorbaPrimitiveHandler primitiveHandler = (CorbaPrimitiveHandler)discriminator;
            result = primitiveHandler.getDataFromValue();
        }
        return result;        
    }
    
    public void setDiscriminator(CorbaObjectHandler disc) {
        discriminator = disc;
    }
    
    public void setDiscriminatorValueFromData(String data) {
        // The discriminator is handled by either the enum handler or the primitive handler.
        if (discriminator.getTypeCodeKind().value() == TCKind._tk_enum) {
            CorbaEnumHandler enumHandler = (CorbaEnumHandler)discriminator;
            enumHandler.setValue(data);
        } else {
            CorbaPrimitiveHandler primitiveHandler = (CorbaPrimitiveHandler)discriminator;
            primitiveHandler.setValueFromData(data);
        }
    }
    
    public List<CorbaObjectHandler> getCases() {
        return cases;
    }
    
    public CorbaObjectHandler getBranchByName(String caseName) {
        for (Iterator<CorbaObjectHandler> caseIter = cases.iterator(); caseIter.hasNext();) {
            CorbaObjectHandler obj = caseIter.next();
            if (obj.getName().getLocalPart().equals(caseName)) {
                return obj;
            }
        }
        
        return null;
    }
    
    public void addCase(CorbaObjectHandler unionCase) {
        cases.add(unionCase);
    }
    
    public CorbaObjectHandler getValue() {
        return value;
    }
    
    public void setValue(String caseName, CorbaObjectHandler val) {
        value = val;
    }
    
    public int getDefaultIndex() {
        return defaultIndex;
    }
    
    public String createDefaultDiscriminatorLabel() {
        String label = null;
        // According to the CORBA specification, an enumeration discriminator can be one of the 
        // following types:
        //   - *integer* (short, long, ulong, either signed or unsigned)
        //   - boolean
        //   - character
        //   - enumeration
        // So when we need to create a default discriminator to accomodate for the lack of a 
        // discriminator from, these are the four cases we must check for.
        if (discriminator.getTypeCodeKind().value() == TCKind._tk_boolean) {
            // We can only have a default case with a boolean discriminator if we have
            // only one case, either TRUE or FALSE.  Therefore, we only need to check
            // against the first label, if there is one.
            if (labels.isEmpty()) {
                label = "false";
            } else {
                boolean boolValue = Boolean.parseBoolean(labels.get(0));
                label = String.valueOf(!boolValue);
            }
        } else if (discriminator.getTypeCodeKind().value() == TCKind._tk_char) {
            if (labels.isEmpty()) {
                label = String.valueOf('0');
            } else {
                char charValue = labels.get(0).charAt(0);
                while (labels.contains(String.valueOf(charValue))) {
                    charValue++;
                }
                label = String.valueOf(charValue);
            }
        } else if (discriminator.getTypeCodeKind().value() == TCKind._tk_enum) {
            // Get the list of possible enumerations in the enumerator and compare these to the
            // labels we obtained from the Union definition.  In order for the union/enum 
            // combination to be syntactically correct, there must be one enumeration not included
            // as a case for the default case to be valid.
            Enum enumType = (Enum)discriminator.getType();
            List<Enumerator> enumerators = enumType.getEnumerator();
            if (labels.isEmpty()) {
                // Any value will do since we only have a default case.
                label = enumerators.get(0).getValue();                  
            } else {
                String enumLabel = null;
                for (Iterator<Enumerator> enumIter = enumerators.iterator(); enumIter.hasNext();) {
                    enumLabel = ((Enumerator)enumIter.next()).getValue();
                    if (!labels.contains(enumLabel)) {
                        label = enumLabel;
                        break;
                    }
                }
            }
        } else if ((discriminator.getTypeCodeKind().value() == TCKind._tk_short)
                   || (discriminator.getTypeCodeKind().value() == TCKind._tk_ushort)) {
            if (labels.isEmpty()) {
                label = String.valueOf(Short.MAX_VALUE);
            }
            for (int i = Short.MAX_VALUE; i >= Short.MIN_VALUE; i--) {
                if (!labels.contains(String.valueOf(i))) {
                    label = String.valueOf(i);
                    break;
                }   
            }
        } else if ((discriminator.getTypeCodeKind().value() == TCKind._tk_long)
                   || (discriminator.getTypeCodeKind().value() == TCKind._tk_ulong)) {
            if (labels.isEmpty()) {
                label = String.valueOf(Integer.MAX_VALUE);
            }
            for (int i = Integer.MAX_VALUE; i >= Integer.MIN_VALUE; i--) {
                if (!labels.contains(String.valueOf(i))) {
                    label = String.valueOf(i);
                    break;
                }   
            }
        }
        return label;
    }

    public void clear() {
        if (discriminator != null) {
            discriminator.clear();          
        }
        if (value != null) {
            value.clear();
        }
    }  
}
