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
package org.apache.cxf.systests.cdi.base;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

public class AtomFeed implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String language;
    private Collection<AtomFeedEntry> entries = new ArrayList<>();

    public AtomFeed() {
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(final String language) {
        this.language = language;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getId() {
        return id;
    }

    public void addEntry(AtomFeedEntry entry) {
        entries.add(entry);
    }
    
    public Collection<AtomFeedEntry> getEntries() {
        return entries;
    }
}

