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
package org.apache.cxf.binding.soap;

import javax.xml.namespace.QName;

import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.headers.Header;

public class SoapHeader extends Header {
    private boolean mustUnderstand;
    private String actor;

    public SoapHeader(QName q, Object o) {
        super(q, o);
    }
    public SoapHeader(QName q, Object o, DataBinding b) {
        super(q, o, b);
    }
    
    public String getActor() {
        return actor;
    }
    public void setActor(String actor) {
        this.actor = actor;
    }
    
    public boolean isMustUnderstand() {
        return mustUnderstand;
    }
    public void setMustUnderstand(boolean mustUnderstand) {
        this.mustUnderstand = mustUnderstand;
    }
    

}
