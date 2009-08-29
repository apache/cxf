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

package org.apache.cxf.systest.jaxrs.sdo.impl;

import org.apache.cxf.systest.jaxrs.sdo.SdoFactory;
import org.apache.cxf.systest.jaxrs.sdo.Structure;
import org.apache.tuscany.sdo.helper.HelperContextImpl;
import org.apache.tuscany.sdo.impl.FactoryBase;
import org.apache.tuscany.sdo.model.ModelFactory;
import org.apache.tuscany.sdo.model.impl.ModelFactoryImpl;

import commonj.sdo.DataObject;
import commonj.sdo.Property;
import commonj.sdo.Type;
import commonj.sdo.helper.HelperContext;

//CHECKSTYLE:OFF
public class SdoFactoryImpl extends FactoryBase implements SdoFactory {
    public static final String NAMESPACE_URI = "http://apache.org/structure/types";
    public static final String NAMESPACE_PREFIX = "tns";
    public static final String PATTERN_VERSION = "1.2";
    public static final int STRUCTURE = 1;      
    private static SdoFactoryImpl instance = null;
    protected Type structureType = null;
    private boolean isCreated = false;
    private boolean isInitialized = false;
  
    public SdoFactoryImpl() {
        super(NAMESPACE_URI, NAMESPACE_PREFIX, "org.apache.cxf.systest.jaxrs.sdo");
    }
    
    public void register(HelperContext scope) {
    if(scope == null) {
            throw new IllegalArgumentException("Scope can not be null");
    }
    
    if (((HelperContextImpl)scope).getExtendedMetaData().getPackage(NAMESPACE_URI) != null) {
            return;
        }
    // Register this package with provided scope   
    ((HelperContextImpl)scope).getExtendedMetaData().putPackage(NAMESPACE_URI, this);
    
    //Register dependent packages with provided scope
        ModelFactory.INSTANCE.register(scope);
        }
        
        public DataObject create(int typeNumber) {
            switch (typeNumber) {
            case STRUCTURE : 
                    return (DataObject)createStructure();
            default :
                return super.create(typeNumber);
            }
        }
    
        public Structure createStructure() {
            return new StructureImpl();
        }
        
        public Type getStructure() {
        return structureType;
        }
                
        public static SdoFactoryImpl init() {
            if (instance != null) {
                return instance;
            }
            instance = new SdoFactoryImpl();
    
            instance.createMetaData();
            instance.initializeMetaData();
            return instance;
        }
      
        public void createMetaData() {
            if (isCreated) {
                return;
            }
            isCreated = true;   
    
            structureType = createType(false, STRUCTURE);
            createProperty(true, structureType,StructureImpl._INTERNAL_TEXT); 
            createProperty(true, structureType,StructureImpl._INTERNAL_INT); 
            createProperty(true, structureType,StructureImpl._INTERNAL_DBL); 
            createProperty(true, structureType,StructureImpl._INTERNAL_TEXTS); 
        }
        
        public void initializeMetaData() {
            if (isInitialized) {
                return;
            }
            isInitialized = true;
        
            ModelFactoryImpl theModelPackageImpl = (ModelFactoryImpl)ModelFactoryImpl.init();
                Property property = null;
        
                // Add supertypes to types
    
    
        initializeType(structureType, Structure.class, "Structure", false);
        property = getLocalProperty(structureType, 0);
        initializeProperty(property, theModelPackageImpl.getString(), "text", null, 1, 1, 
                           Structure.class, false, true, false);
    
        property = getLocalProperty(structureType, 1);
        initializeProperty(property, theModelPackageImpl.getInt(), "int", null, 1, 1, 
                           Structure.class, false, true, false);
    
        property = getLocalProperty(structureType, 2);
        initializeProperty(property, theModelPackageImpl.getDouble(), "dbl", null, 1, 1, 
                           Structure.class, false, true, false);
    
        property = getLocalProperty(structureType, 3);
        initializeProperty(property, theModelPackageImpl.getString(), "texts", null, 1, -1, 
                           Structure.class, false, false, false);
    
    
        createXSDMetaData(theModelPackageImpl);
    }
      
    protected void createXSDMetaData(ModelFactoryImpl theModelPackageImpl)
    {
        super.initXSD();
        
        addXSDMapping
          (structureType,
                 new String[] {
                     "name", "Structure",
                     "kind", "elementOnly"
                 });
    
        addXSDMapping
                (getLocalProperty(structureType, 0),
                 new String[] {
                     "kind", "element",
                     "name", "text",
                     "namespace", "##targetNamespace"
                 });
    
        addXSDMapping
                (getLocalProperty(structureType, 1),
                 new String[] {
                     "kind", "element",
                     "name", "int",
                     "namespace", "##targetNamespace"
                 });
    
        addXSDMapping
                (getLocalProperty(structureType, 2),
                 new String[] {
                     "kind", "element",
                     "name", "dbl",
                     "namespace", "##targetNamespace"
                 });
    
        addXSDMapping
                (getLocalProperty(structureType, 3),
                 new String[] {
                     "kind", "element",
                     "name", "texts",
                     "namespace", "##targetNamespace"
                 });
      }
    
    
    
} //SdoFactoryImpl
//CHECKSTYLE:ON