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
package org.apache.cxf.jaxrs.ext.logging.atom;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Handler;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.abdera.model.Element;
import org.apache.abdera.model.Feed;
import org.apache.cxf.jaxrs.ext.logging.LogRecord;
import org.apache.cxf.jaxrs.ext.logging.atom.converter.StandardConverter;
import org.apache.cxf.jaxrs.ext.logging.atom.converter.StandardConverter.Format;
import org.apache.cxf.jaxrs.ext.logging.atom.converter.StandardConverter.Multiplicity;
import org.apache.cxf.jaxrs.ext.logging.atom.converter.StandardConverter.Output;

@Path("logs")
public class AtomPullServer extends AbstractAtomBean {

    private StandardConverter converter = 
        new StandardConverter(Output.FEED, Multiplicity.MANY, Format.CONTENT);
    private List<LogRecord> records = new LinkedList<LogRecord>();
    
    @GET
    @Produces("application/atom+xml")
    public Feed getAllRecords() {
        //TODO: this is quite clumsy, think of something better
        List<? extends Element> elements = null;
        synchronized (records) {
            elements = converter.convert(records);
        }
        return (Feed)(elements.get(0));
    }

    @Override
    protected Handler createHandler() {
        return new AtomPullHandler(this);
    }
    
    public void publish(LogRecord record) {
        synchronized (records) {
            records.add(record);
        }
    }
    
    public void close() {
        // save records somehow
    }
}
