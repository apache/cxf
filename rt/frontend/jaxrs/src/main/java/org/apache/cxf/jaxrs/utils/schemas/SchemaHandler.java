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
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;

public class SchemaHandler {

    private static final Logger LOG = LogUtils.getL7dLogger(SchemaHandler.class);
    private static final String CLASSPATH_PREFIX = "classpath:";
    
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
        
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema s = null;
        try {
            List<Source> sources = new ArrayList<Source>();
            for (String loc : locations) {
                InputStream is = null;
                if (loc.startsWith(CLASSPATH_PREFIX)) {
                    String path = loc.substring(CLASSPATH_PREFIX.length());
                    is = ResourceUtils.getClasspathResourceStream(path, SchemaHandler.class, bus);
                    if (is == null) {
                        LOG.warning("No schema resource " + loc + " is available on classpath");
                        return null;
                    }
                } else {
                    File f = new File(loc);
                    if (!f.exists()) {
                        LOG.warning("No schema resource " + loc + " is available on local disk");
                        return null;
                    }
                    is = new FileInputStream(f);
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
