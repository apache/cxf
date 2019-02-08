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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for myComplexStruct complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="myComplexStruct">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="elem1" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="elem2" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="elem3" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "myComplexStruct", propOrder = {"elem1", "elem2", "elem3" })
public class MyComplexStruct {

    @XmlElement(required = true, namespace = "http://apache.org/hello_world_rpclit/types")
    protected String elem1;
    @XmlElement(required = true, namespace = "http://apache.org/hello_world_rpclit/types")
    protected String elem2;
    @XmlElement(namespace = "http://apache.org/hello_world_rpclit/types")
    protected int elem3;

    /**
     * Gets the value of the elem1 property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getElem1() {
        return elem1;
    }

    /**
     * Sets the value of the elem1 property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setElem1(String value) {
        this.elem1 = value;
    }

    /**
     * Gets the value of the elem2 property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getElem2() {
        return elem2;
    }

    /**
     * Sets the value of the elem2 property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setElem2(String value) {
        this.elem2 = value;
    }

    /**
     * Gets the value of the elem3 property.
     *
     */
    public int getElem3() {
        return elem3;
    }

    /**
     * Sets the value of the elem3 property.
     *
     */
    public void setElem3(int value) {
        this.elem3 = value;
    }

}
