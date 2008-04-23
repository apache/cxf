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

package org.apache.cxf.tools.corba.processors.wsdl;

import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.wsdl.Binding;
import javax.wsdl.Definition;
import javax.wsdl.PortType;
import javax.xml.namespace.QName;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.cxf.binding.corba.wsdl.Anonarray;
import org.apache.cxf.binding.corba.wsdl.Anonfixed;
import org.apache.cxf.binding.corba.wsdl.Anonsequence;
import org.apache.cxf.binding.corba.wsdl.Anonstring;
import org.apache.cxf.binding.corba.wsdl.Array;
import org.apache.cxf.binding.corba.wsdl.BindingType;
import org.apache.cxf.binding.corba.wsdl.CaseType;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;
import org.apache.cxf.binding.corba.wsdl.Fixed;
import org.apache.cxf.binding.corba.wsdl.MemberType;
import org.apache.cxf.binding.corba.wsdl.Sequence;
import org.apache.cxf.binding.corba.wsdl.Union;
import org.apache.cxf.binding.corba.wsdl.Unionbranch;
import org.apache.cxf.binding.corba.wsdl.W3CConstants;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.ws.commons.schema.XmlSchemaAnnotation;
import org.apache.ws.commons.schema.XmlSchemaAppInfo;
import org.apache.ws.commons.schema.XmlSchemaChoice;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaFacet;
import org.apache.ws.commons.schema.XmlSchemaFractionDigitsFacet;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.XmlSchemaTotalDigitsFacet;


public final class WSDLTypes {
    
    private static final Logger LOG = LogUtils.getL7dLogger(WSDLTypes.class);

    private WSDLTypes() {
    }

    public static CorbaTypeImpl processObject(Definition definition, XmlSchemaComplexType complex,  
                                              XmlSchemaAnnotation annotation, QName typeName,
                                              QName defaultName, String idlNamespace) 
        throws Exception {
        CorbaTypeImpl corbaTypeImpl = null;
                
        if (annotation != null) {
            Iterator i = annotation.getItems().getIterator();
            while (i.hasNext()) {
                XmlSchemaAppInfo appInfo = (XmlSchemaAppInfo)i.next();
                if (appInfo != null) {
                    NodeList nlist = appInfo.getMarkup();
                    Node node = nlist.item(0);
                    String info = node.getNodeValue();
                    
                    info.trim();                
        
                    if ("corba:binding=".equals(info.substring(0, 14))) {
                        String bindingName = info.substring(14);
                        QName bqname = new QName(definition.getTargetNamespace(), bindingName);

                        //Check if the Binding with name already exists
                        Binding binding = null;
                        if (WSDLToCorbaHelper.queryBinding(definition, bqname)) {
                            binding = definition.getBinding(bqname);
                        }

                        if (binding != null) {    
                            org.apache.cxf.binding.corba.wsdl.Object obj = 
                                new org.apache.cxf.binding.corba.wsdl.Object();
                            PortType portT = binding.getPortType();
                            QName name = new QName(idlNamespace, portT.getQName().getLocalPart(), 
                                                   definition.getPrefix(idlNamespace));
                            obj.setName(name.getLocalPart());
                            obj.setQName(name);  
                            QName bName = binding.getQName();                           
                            obj.setBinding(bName);
                            // get the repository id of the binding.
                            String repId = null;
                            Iterator bindIter = binding.getExtensibilityElements().iterator();
                            while (bindIter.hasNext()) {
                                BindingType type = (BindingType)bindIter.next();
                                repId = type.getRepositoryID();                               
                            }
                            obj.setRepositoryID(repId);
                            obj.setType(typeName);
                            corbaTypeImpl = obj;
                        } else {
                            //if (isVerboseOn()) {
                            System.out.println("Could not find binding for: " + bqname);
                            //}
                        }
                    }
                }
            }
        }

        if (corbaTypeImpl == null) {
            org.apache.cxf.binding.corba.wsdl.Object obj = 
                new org.apache.cxf.binding.corba.wsdl.Object();
            QName name = new QName(idlNamespace, "CORBA.Object", definition.getPrefix(idlNamespace));
            obj.setName(name.getLocalPart());
            obj.setQName(name);             
            obj.setRepositoryID("IDL:omg.org/CORBA/Object/1.0");
            obj.setType(typeName);                       
            corbaTypeImpl = obj;
        }
        
        return corbaTypeImpl;

    }         

    
    public static CorbaTypeImpl processStringType(CorbaTypeImpl corbaTypeImpl, QName name, 
                                                  String maxLength, String length) throws Exception {
        boolean boundedString = true;             
        int bound = 0;

        try {
            if (maxLength != null) {
                bound = Integer.parseInt(maxLength);
            } else if (length != null) {
                bound = Integer.parseInt(length);
            } else {
                boundedString = false;
            }
        } catch (NumberFormatException ex) {
            throw new Exception("illegal number" , ex);
        } catch (Exception e) {
            throw new Exception("illegal number" , e);
        }

        if (boundedString) {
            // bounded string 
            Anonstring anonString = new Anonstring();
            anonString.setBound(bound);
            anonString.setName(name.getLocalPart());
            anonString.setQName(name);
            anonString.setType(corbaTypeImpl.getQName());            
            corbaTypeImpl = anonString;           
        } 
        return corbaTypeImpl;
    }
    
    public static CorbaTypeImpl mapToArray(QName name, QName schematypeName, QName arrayType,
                                           QName elName, int bound, boolean anonymous) {
        CorbaTypeImpl corbatype = null;
            
        //schematypeName = checkPrefix(schematypeName);

        if (!anonymous) {
            //Create an Array
            Array corbaArray = new Array();
            corbaArray.setName(name.getLocalPart());
            corbaArray.setType(schematypeName);
            corbaArray.setElemtype(arrayType);
            corbaArray.setElemname(elName);
            corbaArray.setBound(bound);
            corbaArray.setRepositoryID(WSDLToCorbaHelper.REPO_STRING
                                       + name.getLocalPart().replace('.', '/')
                                       + WSDLToCorbaHelper.IDL_VERSION);
            corbaArray.setQName(name);            
            corbatype = corbaArray;
        } else {
            //Create an Anonymous Array
            Anonarray corbaArray = new Anonarray();
            corbaArray.setName(name.getLocalPart());
            corbaArray.setType(schematypeName);            
            corbaArray.setElemtype(arrayType);
            corbaArray.setElemname(elName);
            corbaArray.setBound(bound);
            corbaArray.setQName(name);                        
            corbatype = corbaArray;
        }           
        return corbatype;
    }

    public static CorbaTypeImpl mapToSequence(QName name, QName schematypeName, QName arrayType,
                                              QName elName, int bound, boolean anonymous) {
        CorbaTypeImpl corbaTypeImpl = null;

        //schematypeName = checkPrefix(schematypeName);
        if (!anonymous) {
            // Create a Sequence
            Sequence corbaSeq = new Sequence();
            corbaSeq.setName(name.getLocalPart());
            corbaSeq.setQName(name);
            corbaSeq.setType(schematypeName);
            corbaSeq.setElemtype(arrayType);
            corbaSeq.setElemname(elName);
            corbaSeq.setBound(bound);
            corbaSeq.setRepositoryID(WSDLToCorbaHelper.REPO_STRING
                                     + name.getLocalPart().replace('.', '/')
                                     + WSDLToCorbaHelper.IDL_VERSION);
            corbaTypeImpl = corbaSeq;
        } else {
            // Create a Anonymous Sequence
            Anonsequence corbaSeq = new Anonsequence();
            corbaSeq.setName(name.getLocalPart());
            corbaSeq.setQName(name);
            corbaSeq.setType(schematypeName);
            corbaSeq.setElemtype(arrayType);
            corbaSeq.setElemname(elName);
            corbaSeq.setBound(bound);

            corbaTypeImpl = corbaSeq;
        }
        return corbaTypeImpl;
    }
    
    public static Union processUnionBranches(Union corbaUnion, List fields, List<String> caselist) {
        int caseIndex = 0;

        for (int i = 0; i < fields.size(); i++) {
            MemberType field = (MemberType)fields.get(i);
            Unionbranch branch = new Unionbranch();
            branch.setName(field.getName());
            branch.setIdltype(field.getIdltype());
            if (field.isSetQualified() && field.isQualified()) {
                branch.setQualified(true);
            }
            branch.setDefault(false);                         

            CaseType c = new CaseType();
            c.setLabel((String)caselist.get(caseIndex));
            caseIndex++;
            branch.getCase().add(c);
            corbaUnion.getUnionbranch().add(branch);
        }
        return corbaUnion;
    }    

    
    public static boolean isOMGUnion(XmlSchemaComplexType type) {
        boolean isUnion = false;

        if (type.getParticle() instanceof XmlSchemaSequence 
            && type.getAttributes().getCount() == 0) {
        
            XmlSchemaSequence stype = (XmlSchemaSequence)type.getParticle();                

            if (stype.getItems().getCount() == 2) {
                Iterator it = stype.getItems().getIterator();
                XmlSchemaParticle st1 = (XmlSchemaParticle)it.next();
                XmlSchemaParticle st2 = (XmlSchemaParticle)it.next();
                XmlSchemaElement discEl = null;

                if (st1 instanceof XmlSchemaChoice && st2 instanceof XmlSchemaElement) {
                    isUnion = true;
                    discEl = (XmlSchemaElement)st2;
                } else if (st2 instanceof XmlSchemaChoice && st1 instanceof XmlSchemaElement) {
                    isUnion = true;
                    discEl = (XmlSchemaElement)st1;
                }
                if (isUnion && !"discriminator".equals(discEl.getQName().getLocalPart())) {
                    isUnion = false;
                }                
            }
        }
        return isUnion;
    }
        
    public static boolean isUnion(XmlSchemaComplexType type) {
        boolean isUnion = false;
        
        if (type.getParticle() instanceof XmlSchemaChoice && type.getAttributes().getCount() == 0) {
            isUnion = true;
        }

        return isUnion;
    }

    
    public static CorbaTypeImpl processDecimalType(XmlSchemaSimpleTypeRestriction restrictionType, 
                                                   QName name, CorbaTypeImpl corbaTypeImpl,
                                                   boolean anonymous) throws Exception {
                
        String tdigits = null;
        String fdigits = null;
        boolean boundedDecimal = false;
        boolean boundedScale = false;
        Iterator iter = restrictionType.getFacets().getIterator();
        while (iter.hasNext()) {
            XmlSchemaFacet val = (XmlSchemaFacet)iter.next();
            if (val instanceof XmlSchemaTotalDigitsFacet) {            
                tdigits = val.getValue().toString();
                boundedDecimal = true;
            }
            if (val instanceof XmlSchemaFractionDigitsFacet) {
                fdigits = val.getValue().toString();
                boundedScale = true;
            }
        }
        
        int digits = 0;
        int scale = 0;
        
        if (boundedDecimal) {
            try {
                digits = Integer.parseInt(tdigits);

                if ((digits > 31) || (digits < 1)) {
                    String msg = "totalDigits facet for the type " + name 
                        + " cannot be more than 31 for corba fixed types";
                    LOG.log(Level.WARNING, msg);
                    boundedDecimal = false;
                } else if (digits == 31) {
                    boundedDecimal = false;
                }
            } catch (NumberFormatException ex) {
                String msg = "totalDigits facet on the simple type restriction for type" 
                    + name.getLocalPart() + "is incorrect.";
                throw new Exception(msg);                                 
            }
        }
        
        if (boundedScale) {
            try {
                scale = Integer.parseInt(fdigits);

                if ((scale > 6) || (scale < 0)) {
                    String msg = "fixedDigits facet for the type " + name
                             + " cannot be more than 6 for corba fixed types";
                    LOG.log(Level.WARNING, msg);
                    boundedScale = false;
                } else if (scale == 6) {
                    boundedScale = false;
                }
            } catch (NumberFormatException ex) {
                String msg = "fractionDigits facet on the simple type restriction for type" 
                    + name.getLocalPart() + " is incorrect.";                     
                throw new Exception(msg);
            }
        }

        if (!boundedDecimal) {
            if (anonymous) {
                Anonfixed fixed = (Anonfixed)corbaTypeImpl;
                digits = Integer.parseInt(String.valueOf(fixed.getDigits()));
            } else {
                Fixed fixed = (Fixed)corbaTypeImpl;
                digits = Integer.parseInt(String.valueOf(fixed.getDigits()));
            }            
        }

        if (!boundedScale) {
            if (anonymous) {
                Anonfixed fixed = (Anonfixed)corbaTypeImpl;
                scale = Integer.parseInt(String.valueOf(fixed.getScale()));
            } else {
                Fixed fixed = (Fixed)corbaTypeImpl;
                scale = Integer.parseInt(String.valueOf(fixed.getScale()));
            }
        }

        if (boundedDecimal || boundedScale) {
            if (anonymous) { 
                corbaTypeImpl = (CorbaTypeImpl)getAnonFixedCorbaType(name, W3CConstants.NT_SCHEMA_DECIMAL, 
                                                                 digits, scale);
            } else {
                corbaTypeImpl = (CorbaTypeImpl)getFixedCorbaType(name, W3CConstants.NT_SCHEMA_DECIMAL, 
                                          digits, scale);
            }
        }
        return corbaTypeImpl;
    }   
    
    
    public static CorbaTypeImpl processBase64Type(CorbaTypeImpl corbaTypeImpl, QName name, 
                                                  String maxLength, String length) 
        throws Exception {
        int bound = 0;
        boolean boundedOctet = true;    

        try {
            if (maxLength != null) {
                bound = Integer.parseInt(maxLength);
            } else if (length != null) {
                bound = Integer.parseInt(length);
            } else {
                boundedOctet = false;
            }
        } catch (NumberFormatException ex) {
            String msg = "length facet on the simple type restriction for type"                 
                + name.getLocalPart() + " is incorrect.";                     
            throw new Exception(msg);
        }

        if (boundedOctet) {
            corbaTypeImpl = getOctetCorbaType(name, corbaTypeImpl.getType(), bound);
        }

        return corbaTypeImpl;
    }
    
    //  checks if the type is an anonymous type.
    public static boolean isAnonymous(String typeName) {
        boolean anonymous = false;
        
        if (typeName == null) {
            anonymous = true;
        } else {                                                  
            StringTokenizer strtok = new StringTokenizer(typeName, ".");
            for (int i = 0; strtok.hasMoreTokens(); ++i) {
                String token = strtok.nextToken();
                if (token.startsWith("_")
                    && Character.isDigit(token.charAt(1))) {
                    anonymous = true;
                    break;
                }
            }
        }
        return anonymous;
    }
    
    public static CorbaTypeImpl getFixedCorbaType(QName name, QName stype, int digits, int scale) {        
        Fixed fixed = new Fixed();
        fixed.setName(name.getLocalPart());
        fixed.setQName(name);
        fixed.setType(stype);
        fixed.setDigits(digits);
        fixed.setScale(scale);
        fixed.setRepositoryID(WSDLToCorbaHelper.REPO_STRING
                              + name.getLocalPart().replace('.', '/')
                              + WSDLToCorbaHelper.IDL_VERSION);       
        return fixed;
    }
    
    public static CorbaTypeImpl getAnonFixedCorbaType(QName name, QName stype, int digits, int scale) {
        Anonfixed fixed = new Anonfixed();
        fixed.setName(name.getLocalPart());
        fixed.setQName(name);
        fixed.setType(stype);
        fixed.setDigits(digits);
        fixed.setScale(scale);
        return fixed;
    }
    
    public static CorbaTypeImpl getOctetCorbaType(QName name, QName stype, int bound) {
        Sequence seq = new Sequence();
        seq.setName(name.getLocalPart());
        seq.setQName(name);
        seq.setType(stype);        
        seq.setElemtype(CorbaConstants.NT_CORBA_OCTET);
        seq.setBound(bound);
        seq.setRepositoryID(WSDLToCorbaHelper.REPO_STRING
                            + name.getLocalPart().replace('.', '/')
                            + WSDLToCorbaHelper.IDL_VERSION);
        return seq;
    }
        
    
}
