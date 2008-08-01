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

import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.corba.CorbaTypeMap;
import org.apache.cxf.binding.corba.wsdl.CaseType;
import org.apache.cxf.binding.corba.wsdl.Union;
import org.apache.cxf.binding.corba.wsdl.Unionbranch;
import org.apache.cxf.service.model.ServiceInfo;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TCKind;

public class CorbaUnionListener extends AbstractCorbaTypeListener {

    private CorbaTypeMap typeMap;
    private ServiceInfo serviceInfo;
    private ORB orb;
    private List<Unionbranch> branches;
    private CorbaTypeListener currentTypeListener;
    private Union unionType;

    public CorbaUnionListener(CorbaObjectHandler handler,
            CorbaTypeMap tm, ORB oorb,
            ServiceInfo sInfo) {
        super(handler);
        typeMap = tm;
        serviceInfo = sInfo;
        orb = oorb;
        unionType = (Union) handler.getType();
        branches = unionType.getUnionbranch();  
    }

    public void processStartElement(QName name) {
        if (currentTypeListener == null) {
            for (Unionbranch branch : branches) {               
                CorbaObjectHandler content;
                QName unionName = null;
                String branchName = branch.getName();
                if (branch.getName().equals(name.getLocalPart())) {
                    unionName = name;
                } else if (branches.size() == 1) {
                    unionName = handler.getName();
                }
                if (unionName != null) {
                    CorbaObjectHandler discObj =
                        CorbaHandlerUtils.createTypeHandler(orb,
                                                            new QName("discriminator"),
                                                            unionType.getDiscriminator(),
                                                            typeMap);
                    ((CorbaUnionHandler)handler).setDiscriminator(discObj);
                    String descriminatorValue = determineDescriminatorValue(branch);
                    ((CorbaUnionHandler)handler).setDiscriminatorValueFromData(descriminatorValue);

                    currentTypeListener =
                        CorbaHandlerUtils.getTypeListener(unionName,
                                                          branch.getIdltype(),
                                                          typeMap,
                                                          orb,
                                                          serviceInfo);
                    currentTypeListener.setNamespaceContext(ctx);
                    content = currentTypeListener.getCorbaObject();
                    ((CorbaUnionHandler)handler).setValue(branchName, content);
                    if (unionType.isSetNillable() && unionType.isNillable()) {
                        currentTypeListener.processStartElement(name);
                    }
                } else {
                    QName emptyBranchContentQName = 
                        new QName(name.getNamespaceURI(), branchName);
                    content = CorbaHandlerUtils.initializeObjectHandler(orb,
                                                                        emptyBranchContentQName,
                                                                        branch.getIdltype(),
                                                                        typeMap,
                                                                        serviceInfo);
                }                
                ((CorbaUnionHandler)handler).addCase(content);
            }
        } else {
            currentTypeListener.processStartElement(name);
        }
    }

    public void processCharacters(String text) {
        if (currentTypeListener != null) {
            currentTypeListener.processCharacters(text);
        } else {
            //Nillable primitive cases, you do not get the start element            
            CorbaPrimitiveHandler discObj =
                new CorbaPrimitiveHandler(new QName("discriminator"),
                                          unionType.getDiscriminator(),
                                          orb.get_primitive_tc(TCKind.from_int(TCKind._tk_boolean)),
                                          null);
            discObj.setValue(Boolean.TRUE);
            ((CorbaUnionHandler)handler).setDiscriminator(discObj);           
            CorbaTypeListener typeListener =
                CorbaHandlerUtils.getTypeListener(handler.getName(),
                                                  branches.get(0).getIdltype(),
                                                  typeMap,
                                                  orb,
                                                  serviceInfo);
            typeListener.setNamespaceContext(ctx);
            ((CorbaUnionHandler)handler).setValue("value", typeListener.getCorbaObject());
            typeListener.processCharacters(text);
        }
    }

    public void processEndElement(QName name) {
        if (currentTypeListener != null) {
            currentTypeListener.processEndElement(name);
        }
    }

    private String determineDescriminatorValue(Unionbranch branch) {
        String descriminatorValue;
        // Determine the value of the discriminator.  
        List<CaseType> branchCases = branch.getCase();
        if (branchCases.size() != 0) {
            CaseType caseLabel = branchCases.get(0);
            descriminatorValue = caseLabel.getLabel();
        } else {
            // This represents the default case.
            descriminatorValue = ((CorbaUnionHandler)handler).createDefaultDiscriminatorLabel();
        }
        return descriminatorValue;
    }

    public void processWriteAttribute(String prefix,
                                      String namespaceURI,
                                      String localName,
                                      String val) {
        if ("nil".equals(localName)) {
            CorbaPrimitiveHandler discObj =
                new CorbaPrimitiveHandler(new QName("discriminator"),
                                          unionType.getDiscriminator(),
                                          orb.get_primitive_tc(TCKind.from_int(TCKind._tk_boolean)),
                                          null);
            discObj.setValue(Boolean.FALSE);
            ((CorbaUnionHandler)handler).setDiscriminator(discObj);
            Unionbranch branch = branches.get(0);
            QName emptyBranchContentQName = 
                new QName(handler.getName().getNamespaceURI(), branch.getName());
            CorbaObjectHandler content = CorbaHandlerUtils.initializeObjectHandler(orb,
                                                                                   emptyBranchContentQName,
                                                                                   branch.getIdltype(),
                                                                                   typeMap,
                                                                                   serviceInfo);
            ((CorbaUnionHandler)handler).addCase(content);
        } else if (currentTypeListener != null) {
            currentTypeListener.processWriteAttribute(prefix, namespaceURI, localName, val);
        }
    }

    public void processWriteNamespace(String prefix, String namespaceURI) {
        if (currentTypeListener != null) {
            currentTypeListener.processWriteNamespace(prefix, namespaceURI);
        }
    }

}
