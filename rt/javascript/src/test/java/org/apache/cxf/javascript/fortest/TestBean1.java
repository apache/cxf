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

import java.util.Arrays;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Bean with a selection of elements suitable for testing the JavaScript client.
 */
@XmlType(namespace = "uri:org.apache.cxf.javascript.testns")
public class TestBean1 {
    
    public TestBean1() {
        intItem = 43;
        doubleItem = -1.0;
        beanTwoItem = new TestBean2("required=true");
        beanTwoNotRequiredItem = null;
    }
    
    //CHECKSTYLE:OFF
    public String stringItem;
    @XmlElement(namespace = "uri:org.apache.cxf.javascript.testns2")
    public int intItem;
    @XmlElement(defaultValue = "43")
    public long longItem;
    public byte[] base64Item;
    @XmlElement(required = false)
    public int optionalIntItem;
    @XmlElement(required = false, namespace = "uri:org.apache.cxf.javascript.testns2")
    public String optionalStringItem;
    @XmlElement(required = false)
    public int[] optionalIntArrayItem;
    @XmlElement(defaultValue = "-1.0")
    public double doubleItem;
    @XmlElement(required = true)
    public TestBean2 beanTwoItem;
    @XmlElement(required = false)
    public TestBean2 beanTwoNotRequiredItem;
    public AnEnum enumeration;
    //CHECKSTYLE:ON
    
    public AnEnum getEnumeration() {
        return enumeration;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TestBean1)) {
            return false;
        }
        TestBean1 other = (TestBean1) obj;
        boolean equalSoFar = 
            intItem == other.intItem
            && longItem == other.longItem
            && optionalIntItem == other.optionalIntItem
            && doubleItem == other.doubleItem
            && beanTwoItem.equals(other.beanTwoItem)
            && enumeration == other.enumeration;
        if (!equalSoFar) {
            return false;
        }
        
        if (null == base64Item) {
            if (null != other.base64Item) {
                return false;
            }
        } else {
            if (!base64Item.equals(other.base64Item)) {
                return false;
            }
        }

        if (null == stringItem) {
            if (null != other.stringItem) {
                return false;
            }
        } else {
            if (!stringItem.equals(other.stringItem)) {
                return false;
            }
        }

        if (null == optionalIntArrayItem) {
            if (null != other.optionalIntArrayItem) {
                return false;
            }
        } else {
            if (!Arrays.equals(optionalIntArrayItem, other.optionalIntArrayItem)) {
                return false;
            }
        }

        // decisions are simpler for the last one.
        if (null == beanTwoNotRequiredItem) {
            return other.beanTwoNotRequiredItem == null;
        } else {
            return beanTwoNotRequiredItem.equals(other.beanTwoNotRequiredItem);
        }
    }

    @Override
    public int hashCode() {
        // intentionally stupid. We don't use this object in collections.
        return super.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TestBean1");
        builder.append(" stringItem ");
        builder.append(stringItem == null ? "Null" : stringItem);
        builder.append(" intItem ");
        builder.append(intItem);
        builder.append(" longItem ");
        builder.append(longItem);
        builder.append(" base64Item ");
        if (base64Item == null) {
            builder.append("Null");
        } else {
            for (byte b : base64Item) {
                builder.append(" ");
                builder.append(Integer.toHexString(b));
            }
        }
        
        builder.append(" optionalIntItem ");
        builder.append(optionalIntItem);
        builder.append(" optionalIntArrayItem ");
        if (optionalIntArrayItem == null) {
            builder.append("Null");
        } else {
            for (int i : optionalIntArrayItem) {
                builder.append(" ");
                builder.append(i);
            }
        }
        builder.append(" beanTwoItem ");
        if (beanTwoItem == null) {
            builder.append("Null");
        } else {
            builder.append(beanTwoItem.toString()); 
        }
        builder.append(" beanTwoNotRequiredItem ");
        if (beanTwoNotRequiredItem == null) {
            builder.append("Null");
        } else {
            builder.append(beanTwoNotRequiredItem.toString()); 
        }
        builder.append(" " + enumeration);
        
        return builder.toString();
    }
    
}
