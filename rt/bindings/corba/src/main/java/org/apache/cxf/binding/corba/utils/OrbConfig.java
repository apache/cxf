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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.binding.corba.CorbaBindingException;
import org.apache.cxf.binding.corba.interceptors.SystemExceptionHelper;
import org.apache.cxf.binding.corba.wsdl.AddressType;
import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.CORBA.SystemException;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAManager;

public class OrbConfig {

    protected String orbClass;
    protected String orbSingletonClass;
    protected List<String> orbArgs = new ArrayList<String>();
    
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
    
    
    public void addPOAPolicies(ORB orb, 
                               String poaName,
                               POA parentPOA,
                               POAManager poaManager,
                               List<Policy> policies) {
        //nothing
    }
    
    
    public Any createSystemExceptionAny(ORB orb, SystemException sysEx) {
        Any exAny = orb.create_any();
        SystemExceptionHelper.insert(exAny, sysEx);
        return exAny;
    }
    
    public void exportObjectReference(ORB orb,
                                       org.omg.CORBA.Object ref,
                                       String url,
                                       AddressType address) 
        throws URISyntaxException, IOException {
        
        if ((url.startsWith("ior:")) || (url.startsWith("IOR:"))) {
            // make use of Thread cache of last exported IOR
            String ior = CorbaUtils.exportObjectReference(ref, orb);
            address.setLocation(ior);
        } else if (url.startsWith("file:")) {
            URI uri = new URI(url);
            exportObjectReferenceToFile(orb, ref, uri);
        } else if (url.startsWith("relfile:")) {
            URI uri = new URI(url.substring(3));
            exportObjectReferenceToFile(orb, ref, uri);
        } else if (url.startsWith("corbaloc:")) {
            exportObjectReferenceToCorbaloc(orb, ref, url);
        } else if (url.startsWith("corbaname:")) {
            exportObjectReferenceToNamingService(orb,
                                                 ref,
                                                 url);
        } else {
            String ior = orb.object_to_string(ref);
            address.setLocation(ior);
            URI uri = new URI("endpoint.ior");
            exportObjectReferenceToFile(orb, ref, uri);
        }
    }
    public void exportObjectReferenceToNamingService(ORB orb,
                                                     org.omg.CORBA.Object ref,
                                                     String location) {
        int idx = location.indexOf("#");
        String name = location.substring(idx + 1);
        
        //Register in NameService
        try {
            org.omg.CORBA.Object nsObj = orb.resolve_initial_references("NameService");
            NamingContextExt rootContext = NamingContextExtHelper.narrow(nsObj);
            NameComponent[] nc = rootContext.to_name(name);
            rootContext.rebind(nc, ref);
        } catch (Exception ex) {
            throw new CorbaBindingException(ex);
        }
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


    public void exportObjectReferenceToFile(ORB orb,
                                              org.omg.CORBA.Object obj,
                                              URI iorFile) 
        throws IOException {
        String ref = orb.object_to_string(obj);
        File f = null;
        if (iorFile.isOpaque()) {
            f = new File(iorFile.getSchemeSpecificPart());
        } else {
            f = new File(iorFile);
        }
        FileOutputStream file = new FileOutputStream(f);
        PrintWriter out = new PrintWriter(file);
        out.println(ref);
        out.flush();
        file.close();
    }

}
