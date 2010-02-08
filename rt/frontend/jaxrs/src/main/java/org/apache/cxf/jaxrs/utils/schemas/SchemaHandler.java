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

package org.apache.cxf.jaxrs.utils.schemas;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;

public class SchemaHandler {

    private static final Logger LOG = LogUtils.getL7dLogger(SchemaHandler.class);
    
    private Schema schema;
    private Bus bus;
    
    public SchemaHandler() {
        
    }
    
    public void setBus(Bus b) {
        bus = b;
    }
    
    public void setSchemas(List<String> locations) {
        schema = createSchema(locations, bus == null ? BusFactory.getThreadDefaultBus() : bus);
    }
    
    public Schema getSchema() {
        return schema;
    }
    
    public static Schema createSchema(List<String> locations, Bus bus) {
        
        SchemaFactory factory = SchemaFactory.newInstance(WSDLConstants.NS_SCHEMA_XSD);
        Schema s = null;
        try {
            List<Source> sources = new ArrayList<Source>();
            for (String loc : locations) {
                InputStream is = ResourceUtils.getResourceStream(loc, bus);
                if (is == null) {
                    return null;
                }
                Reader r = new BufferedReader(
                               new InputStreamReader(is, "UTF-8"));
                sources.add(new StreamSource(r));
            }
            s = factory.newSchema(sources.toArray(new Source[]{}));
        } catch (Exception ex) {
            LOG.warning("Validation will be disabled, failed to create schema : " + ex.getMessage());
        }
        return s;
        
    }
}
