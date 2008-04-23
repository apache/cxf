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

package org.apache.cxf.jaxb;


import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.commons.lang.builder.ToStringBuilder;

import org.junit.Assert;
import org.junit.Test;

public class JAXBElementToStringStyleTest {
    
    class Holder {
        String name = "HolderName";
        Object obj;
        Holder(Object o) {
            this.obj = o;
        }
    }
    
    JAXBElement<String> nel = 
        new JAXBElement<String>(
            new QName("ab", "bv"),
            String.class, "SomeText");

    Holder h = new Holder(nel);
    
    @Test
    public void testToStringDefault() throws Exception {    
                
        String ts = ToStringBuilder.reflectionToString(h);

        validateHolderString(ts);

        // JAXBElement contents not present
        Assert.assertTrue("has no value", ts.indexOf("value") == -1);
        Assert.assertTrue("has no bv", ts.indexOf("bv") == -1);

    }

    @Test
    public void testToStringMultiLineStyle() throws Exception {
        String ts = 
            ToStringBuilder.reflectionToString(h, JAXBToStringStyle.MULTI_LINE_STYLE);

        validateHolderString(ts);
        validateElementString(ts);
    }

    @Test
    public void testToStringSimpleStyle() throws Exception {
        String ts = 
            ToStringBuilder.reflectionToString(h, JAXBToStringStyle.SIMPLE_STYLE);

        // field names are missing
        Assert.assertTrue("has no obj field", ts.indexOf("obj") == -1);
        Assert.assertTrue("has HolderName", ts.indexOf("HolderName") != -1);
        Assert.assertTrue("has SomeText", ts.indexOf("SomeText") != -1);
    }

    private void validateHolderString(String ts) {
        Assert.assertTrue("has HolderName", ts.indexOf("HolderName") != -1);
        Assert.assertTrue("has JAXBElement", ts.indexOf("JAXBElement") != -1);
        Assert.assertTrue("has obj", ts.indexOf("obj") != -1);
    }
    
    private void validateElementString(String ts) {
        Assert.assertTrue("has  value", ts.indexOf("value") != -1);
        Assert.assertTrue("has scope", ts.indexOf("scope") != -1);
        Assert.assertTrue("has bv", ts.indexOf("bv") != -1);
       
        
    }


}
