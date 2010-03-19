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

package org.apache.cxf.systest.jaxrs;

import java.awt.Image;

import javax.activation.DataHandler;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "xopType", namespace = "http://xop/jaxrs")
@XmlType(name = "XopType", propOrder = {
        "name",
        "attachinfo",
        "attachinfo2",
        "image" })
public class XopType {

    private String name;
    private DataHandler attachinfo;
    private byte[] attachinfo2;
    private Image image;

    @XmlElement(required = true)
    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    
    @XmlElement(required = true)
    @XmlMimeType("application/octet-stream")
    public byte[] getAttachinfo2() {
        return attachinfo2;
    }

    
    public void setAttachinfo2(byte[] value) {
        this.attachinfo2 = value;
    }
    
    
    @XmlElement(required = true)
    @XmlMimeType("application/octet-stream")
    public DataHandler getAttachinfo() {
        return attachinfo;
    }

    
    public void setAttachinfo(DataHandler value) {
        this.attachinfo = value;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public Image getImage() {
        return image;
    }

}
