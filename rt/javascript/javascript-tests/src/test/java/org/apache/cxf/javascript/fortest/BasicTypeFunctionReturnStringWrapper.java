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

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

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
    public void setS(String ss) {
        this.s = ss;
    }
    public int getI() {
        return i;
    }
    public void setI(int ii) {
        this.i = ii;
    }
    public long getL() {
        return l;
    }
    public void setL(long ll) {
        this.l = ll;
    }
    public float getF() {
        return f;
    }
    public void setF(float ff) {
        this.f = ff;
    }
    public double getD() {
        return d;
    }
    public void setD(double dd) {
        this.d = dd;
    }

}
