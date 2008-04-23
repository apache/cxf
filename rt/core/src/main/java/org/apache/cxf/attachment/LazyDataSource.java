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
package org.apache.cxf.attachment;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

import javax.activation.DataSource;

import org.apache.cxf.message.Attachment;

/**
 * A DataSource which will search through a Collection of attachments so as to 
 * lazily load the attachment from the collection. This allows streaming attachments
 * with databinding toolkits like JAXB.
 */
public class LazyDataSource implements DataSource {
    private DataSource dataSource;
    private Collection<Attachment> attachments;
    private String id;
    
    public LazyDataSource(String id, Collection<Attachment> attachments) {
        super();
        this.id = id;
        this.attachments = attachments;
    }

    private synchronized void load() {
        if (dataSource == null) {
            for (Attachment a : attachments) {
                if (a.getId().equals(id)) {
                    this.dataSource = a.getDataHandler().getDataSource();
                    break;
                }
            }
        }
    }
    
    public String getContentType() {
        load();
        
        return dataSource.getContentType();
    }

    public InputStream getInputStream() throws IOException {
        load();
        
        return dataSource.getInputStream();
    }

    public String getName() {
        load();
        
        return dataSource.getName();
    }

    public OutputStream getOutputStream() throws IOException {
        load();
        
        return dataSource.getOutputStream();
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}
