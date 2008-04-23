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

import java.util.ArrayList;
import java.util.List;

public class OrbConfig {

    String orbClass;
    String orbSingletonClass;
    List<String> orbArgs = new ArrayList<String>();
    
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

}
