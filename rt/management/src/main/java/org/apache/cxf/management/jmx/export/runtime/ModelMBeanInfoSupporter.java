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

package org.apache.cxf.management.jmx.export.runtime;

import java.lang.reflect.Constructor;

import java.util.HashMap;
import java.util.Map;


import javax.management.Descriptor;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.modelmbean.DescriptorSupport;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanConstructorInfo;
import javax.management.modelmbean.ModelMBeanInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanNotificationInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import org.apache.cxf.management.annotation.ManagedAttribute;
import org.apache.cxf.management.annotation.ManagedOperation;
import org.apache.cxf.management.annotation.ManagedResource;

public class ModelMBeanInfoSupporter {
    protected Map<String, ModelMBeanAttributeInfo> attributes
        = new HashMap<String, ModelMBeanAttributeInfo>();
    protected Map<String, ModelMBeanNotificationInfo> notifications
        = new HashMap<String, ModelMBeanNotificationInfo>();
    protected Map<Constructor<?>, ModelMBeanConstructorInfo> constructors 
        = new HashMap<Constructor<?>, ModelMBeanConstructorInfo>();
    protected Map<String, ModelMBeanOperationInfo> operations 
        = new HashMap<String, ModelMBeanOperationInfo>();
    
    public ModelMBeanInfoSupporter() {
        
    }
    
    public void clear() {
        attributes.clear();
        notifications.clear();
        constructors.clear();
        operations.clear();
    }
    public void addModelMBeanMethod(String name,
                                    String[] paramTypes,
                                    String[] paramNames,
                                    String[] paramDescs,
                                    String description, 
                                    String rtype,                                    
                                    Descriptor desc) {
        MBeanParameterInfo[] params = null;
        if (paramTypes != null) {
            params = new MBeanParameterInfo[ paramTypes.length ];
            for (int i = 0; i < paramTypes.length; i++) {
                params[i] = new MBeanParameterInfo(paramNames[i],
                                                    paramTypes[i], paramDescs[i]);
            }
        }
    
        operations.put(name,
                        new ModelMBeanOperationInfo(name,
                                                    description,
                                                    params, 
                                                    rtype,
                                                    MBeanOperationInfo.ACTION,
                                                    desc));
    }
    
    public void addModelMBeanNotification(String[] type,
                                          String className,
                                          String description,
                                          Descriptor desc) {
        notifications.put(className,
                           new ModelMBeanNotificationInfo(type, className, description, desc));
    }
    
    public boolean checkAttribute(String attributeName) {
        return attributes.containsKey(attributeName);
    }
    
    public void addModelMBeanAttribute(String fname,
                                       String ftype,
                                       boolean read,
                                       boolean write,
                                       boolean is,
                                       String description,
                                       Descriptor desc) {
        attributes.put(fname, new ModelMBeanAttributeInfo(fname,
                                                           ftype,
                                                           description,
                                                           read, 
                                                           write,
                                                           is, 
                                                           desc));
    }
      
    
    public void addModelMBeanConstructor(Constructor<?> c,
                                          String description,
                                          Descriptor desc) {
        this.constructors.put(c,
                               new ModelMBeanConstructorInfo(description,
                                                             c,
                                                             desc));
    }
    
    public ModelMBeanInfo buildModelMBeanInfo(Descriptor desc)  {        
        
        ModelMBeanOperationInfo[] ops = 
            (ModelMBeanOperationInfo[])operations.values().toArray(new ModelMBeanOperationInfo[0]);        
        
        ModelMBeanAttributeInfo[] atts = 
            (ModelMBeanAttributeInfo[])attributes.values().toArray(new ModelMBeanAttributeInfo[0]);
        
        ModelMBeanConstructorInfo[] cons = 
            (ModelMBeanConstructorInfo[])constructors.values().toArray(new ModelMBeanConstructorInfo[0]);
        
        ModelMBeanNotificationInfo[] notifs = 
            (ModelMBeanNotificationInfo[])notifications.values().toArray(new ModelMBeanNotificationInfo[0]);
                
        return new ModelMBeanInfoSupport("javax.management.modelmbean.ModelMBeanInfo",
                                         "description",
                                         atts,
                                         cons,
                                         ops,
                                         notifs, desc);
    }
    
    
    public Descriptor buildAttributeDescriptor(
        ManagedAttribute ma, String attributeName, boolean is, boolean read, boolean write) {
         
        Descriptor desc = new DescriptorSupport();

        desc.setField("name", attributeName);
       
        desc.setField("descriptorType", "attribute");
        
        if (read) {
            if (is) {
                desc.setField("getMethod", "is" + attributeName);
            } else {
                desc.setField("getMethod", "get" + attributeName);
            }                
        }
        
        if (write) {
            desc.setField("setMethod", "set" + attributeName);
        }
       
       
        if (ma.currencyTimeLimit() >= -1) {
            desc.setField("currencyTimeLimit", ma.currencyTimeLimit());
        }
           
        if (ma.persistPolicy().length() > 0) {
            desc.setField("persistPolicy", ma.persistPolicy());
        }
           
        if (ma.persistPeriod() >= -1) {
            desc.setField("persistPeriod", ma.persistPeriod());
        }
           
        if (ma.defaultValue() != null) {
            desc.setField("default", ma.defaultValue());
        }
           
        return desc;
    }  
    
    public Descriptor buildOperationDescriptor(ManagedOperation mo, String operationName) {
        Descriptor desc = new DescriptorSupport();
        
        desc.setField("name", operationName); 
        
        desc.setField("descriptorType", "operation");
        
        desc.setField("role", "operation");
        
        if (mo.description() != null) {
            desc.setField("displayName", mo.description());
        }
                    
        if (mo.currencyTimeLimit() >= -1) {
            desc.setField("currencyTimeLimit", mo.currencyTimeLimit());
        }
        
        return desc; 
    }
    
    public Descriptor buildAttributeOperationDescriptor(String operationName) {
        
        Descriptor desc = new DescriptorSupport();
        
        desc.setField("name", operationName); 
        
        desc.setField("descriptorType", "operation");
        
        if (operationName.indexOf("set") == 0) {
            desc.setField("role", "setter");
        } else {
            desc.setField("role", "getter");
        }   
        
        return desc; 
    }
            
    
    public Descriptor buildMBeanDescriptor(ManagedResource mr) {
        Descriptor desc = new DescriptorSupport();
        
        if (mr.componentName() != null) {
            desc.setField("name", mr.componentName());
        }
        
        desc.setField("descriptorType", "mbean");
        
        if (mr.description() != null) {
            desc.setField("displayName", mr.description());
        }
        
        if (mr.persistLocation() != null) {
            desc.setField("persistLocation", mr.persistLocation());
        }
            
        if (mr.persistName() != null) {
            desc.setField("persistName", mr.persistName());
        }
        
        if (mr.log()) {
            desc.setField("log", "true");
        } else {
            desc.setField("log", "false");
        }
            
        if (mr.persistPolicy() != null) {
            desc.setField("persistPolicy", mr.persistPolicy());
        }
        
        if (mr.persistPeriod() >= -1) {
            desc.setField("persistPeriod", mr.persistPeriod());
        }
        
        if (mr.logFile() != null) {
            desc.setField("logFile", mr.logFile());
        }
        
        if (mr.currencyTimeLimit() >= -1) {
            desc.setField("currencyTimeLimit", mr.currencyTimeLimit());
        }
        
        return desc;
        
    }    
               
     
}

