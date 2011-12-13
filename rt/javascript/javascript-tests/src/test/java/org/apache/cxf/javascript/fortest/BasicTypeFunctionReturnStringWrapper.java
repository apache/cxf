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

package org.apache.cxf.javascript.fortest;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Wrapper class.
 */
@XmlRootElement(namespace = "uri:org.apache.cxf.javascript.testns")
// specify alphabetical order explicitly to remind us that there is JavaScript code that knows this order!
@XmlType(namespace = "uri:org.apache.cxf.javascript.testns", propOrder = {"d", "f", "i", "l", "s" })
public class BasicTypeFunctionReturnStringWrapper {
    private String s;
    private int i;
    private long l;
    private float f;
    private double d;
    
    public String getS() {
        return s;
    }
    public void setS(String s) {
        this.s = s;
    }
    public int getI() {
        return i;
    }
    public void setI(int i) {
        this.i = i;
    }
    public long getL() {
        return l;
    }
    public void setL(long l) {
        this.l = l;
    }
    public float getF() {
        return f;
    }
    public void setF(float f) {
        this.f = f;
    }
    public double getD() {
        return d;
    }
    public void setD(double d) {
        this.d = d;
    }

}
