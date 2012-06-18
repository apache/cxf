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
package org.apache.cxf.jaxrs.ext.search.jpa;

import java.util.Collections;
import java.util.Map;

import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.sql.SQLPrinterVisitor;

public class JPALanguageVisitor<T> extends SQLPrinterVisitor<T> {

    
    public JPALanguageVisitor(Class<T> tClass) {
        this(tClass, "t", null);
    }
    
    public JPALanguageVisitor(Class<T> tClass, String tableAlias) {
        this(tClass, tableAlias, null);
    }
    
    public JPALanguageVisitor(Class<T> tClass,
                              Map<String, String> fieldMap) {
        this(tClass, "t", fieldMap);
    }
    
    public JPALanguageVisitor(Class<T> tClass,
                              String tableAlias,
                              Map<String, String> fieldMap) {
        super(fieldMap, 
              tClass.getSimpleName(), 
              tableAlias, 
              tableAlias != null ? Collections.singletonList(tableAlias) : null);
    }
        
    public void visit(SearchCondition<T> sc) {
        // provide more customizations as needed
        super.visit(sc);
    }
}
