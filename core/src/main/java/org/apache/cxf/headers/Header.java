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
package org.apache.cxf.headers;

import javax.xml.namespace.QName;

import org.apache.cxf.databinding.DataBinding;

public class Header {
    public enum Direction  { 
        DIRECTION_IN, 
        DIRECTION_OUT, 
        DIRECTION_INOUT 
    };
        
    public static final String HEADER_LIST = Header.class.getName() + ".list";
   
    
    private DataBinding dataBinding;
    private QName name;
    private Object object;
    
    private Direction direction = Header.Direction.DIRECTION_OUT;

    public Header(QName q, Object o) {
        this(q, o, null);
    }
    
    public Header(QName q, Object o, DataBinding b) {
        object = o;
        name = q;
        dataBinding = b;
    }
    
    public DataBinding getDataBinding() {
        return dataBinding;
    }
    public void setDataBinding(DataBinding dataBinding) {
        this.dataBinding = dataBinding;
    }
    public QName getName() {
        return name;
    }
    public void setName(QName name) {
        this.name = name;
    }
    public Object getObject() {
        return object;
    }
    public void setObject(Object object) {
        this.object = object;
    }
    
    public void setDirection(Direction hdrDirection) {
        this.direction = hdrDirection;
    }
    
    public Direction getDirection() {
        return direction;
    }
    
}
