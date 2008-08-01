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

package org.apache.cxf.binding.corba.utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.binding.corba.CorbaBindingException;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;

public class OrbConfig {

    String orbClass;
    String orbSingletonClass;
    List<String> orbArgs = new ArrayList<String>();
    List<Policy> policies = new ArrayList<Policy>();
    
    boolean persistentPoa;
    
    public OrbConfig() {
        //nothing
    }

    public void setOrbClass(String cls) {
        orbClass = cls;
    }
    
    public String getOrbClass() {
        return orbClass;
    }
    
    public void setOrbSingletonClass(String cls) {
        orbSingletonClass = cls;
    }
    
    public String getOrbSingletonClass() {
        return orbSingletonClass;
    }
    
    public void setOrbArgs(List<String> args) {
        orbArgs = args;
    }
    
    public List<String> getOrbArgs() {
        return orbArgs;
    }
    
    public void setPersistentPoa(boolean b) {
        persistentPoa = b;
    }
    public boolean isPersistentPoa() {
        return persistentPoa;
    }
    
    public List<Policy> getExtraPolicies() {
        return policies;
    }
    
    
    public void exportObjectReferenceToCorbaloc(ORB orb,
                                                org.omg.CORBA.Object object,
                                                String location) {
        int keyIndex = location.indexOf('/');
        String key = location.substring(keyIndex + 1);
        try {
            Class<?> bootMgrHelperClass = Class.forName("org.apache.yoko.orb.OB.BootManagerHelper");
            Class<?> bootMgrClass = Class.forName("org.apache.yoko.orb.OB.BootManager");
            Method narrowMethod =
                bootMgrHelperClass.getMethod("narrow", org.omg.CORBA.Object.class);
            java.lang.Object bootMgr = narrowMethod.invoke(null,
                                                           orb.resolve_initial_references("BootManager"));
            Method addBindingMethod = 
                bootMgrClass.getMethod("add_binding", byte[].class, org.omg.CORBA.Object.class);
            addBindingMethod.invoke(bootMgr, key.getBytes(), object);
        } catch (ClassNotFoundException ex) {
            //Not supported by the orb. skip it.
        } catch (java.lang.reflect.InvocationTargetException ex) {
            //Not supported by the orb. skip it.
        } catch (java.lang.Exception ex) {
            throw new CorbaBindingException(ex.getMessage(), ex);
        }
    }

}
