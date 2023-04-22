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
package org.apache.cxf.jaxrs.ext.search.tika;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.ws.rs.ext.ParamConverterProvider;
import org.apache.cxf.jaxrs.ext.search.DefaultParamConverterProvider;

public class LuceneDocumentMetadata {
    public static final String SOURCE_FIELD = "source";

    private final Map< String, Class< ? > > fieldTypes;
    private final String contentFieldName;
    private String source;
    private ParamConverterProvider converterProvider = new DefaultParamConverterProvider();

    public LuceneDocumentMetadata() {
        this("contents");
    }
    public LuceneDocumentMetadata(final String contentFieldName) {
        this(contentFieldName, new LinkedHashMap< String, Class< ? > >());
    }
    public LuceneDocumentMetadata(final Map< String, Class< ? > > fieldTypes) {
        this("contents", fieldTypes);
    }
    public LuceneDocumentMetadata(final String contentFieldName, final Map< String, Class< ? > > fieldTypes) {
        this.contentFieldName = contentFieldName;
        this.fieldTypes = fieldTypes;
    }

    public LuceneDocumentMetadata withField(final String name, final Class< ? > type) {
        fieldTypes.put(name, type);
        return this;
    }

    public LuceneDocumentMetadata withFieldTypeConverter(final ParamConverterProvider provider) {
        this.converterProvider = provider;
        return this;
    }

    public LuceneDocumentMetadata withSource(final String src) {
        this.source = src;
        return this;
    }

    public String getContentFieldName() {
        return contentFieldName;
    }

    public String getSourceFieldName() {
        return SOURCE_FIELD;
    }

    public String getSource() {
        return source;
    }

    public Class<?> getFieldType(String name) {
        return fieldTypes.get(name);
    }

    public Map<String, Class<?>> getFieldTypes() {
        return fieldTypes;
    }

    public ParamConverterProvider getFieldTypeConverter() {
        return converterProvider;
    }
}
