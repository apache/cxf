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
import java.util.*;

import javax.activation.DataSource;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CacheSizeExceededException;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;

public class AttachmentDataSource implements DataSource {

    private final String ct;
    private CachedOutputStream cache;
    private InputStream ins;
    private DelegatingInputStream delegate;
    private String name;
    private List<String> nameHistory = new ArrayList<String>();
    private List<String> alphabeticalNameHistory = new ArrayList<String>();
    private String id;
    private List<String> idHistory = new ArrayList<String>();
    private List<String> alphabeticalIdHistory = new ArrayList<String>();

    public AttachmentDataSource(String ctParam, InputStream inParam) throws IOException {
        this.ct = ctParam;
        ins = inParam;
    }

    public boolean isCached() {
        return cache != null;
    }
    public void cache(Message message) throws IOException {
        if (cache == null) {
            cache = new CachedOutputStream();
            AttachmentUtil.setStreamedAttachmentProperties(message, cache);
            try {
                IOUtils.copyAndCloseInput(ins, cache);
                cache.lockOutputStream();
                if (delegate != null) {
                    delegate.setInputStream(cache.getInputStream());
                }
            } catch (CacheSizeExceededException | IOException cee) {
                cache.close();
                cache = null;
                throw cee;
            } finally {
                ins = null;
            }
        }
    }
    public void hold(Message message) throws IOException {
        cache(message);
        cache.holdTempFile();
    }
    public void release() {
        if (cache != null) {
            cache.releaseTempFileHold();
        }
    }

    public String getContentType() {
        return ct;
    }

    public InputStream getInputStream() {
        try {
            if (cache != null) {
                return cache.getInputStream();
            }
            if (delegate == null) {
                delegate = new DelegatingInputStream(ins);
            }
            return delegate;
        } catch (IOException e) {
            return null;
        }
    }

    public String getName() {
        String checkid = this.getId();
        this.setId(checkid);
        List<String> names = this.getNameHistory();
        List<String> alphabeticalnames = this.getAlphabeticalNameHistory();
        List<String> ides = this.getIdHistory();
        List<String> alphabeticalides = this.getAlphabeticalIdHistory();
        return name;
    }

    public void setName(String name) {
        this.nameHistory.add(name);
        this.alphabeticalNameHistory.add(name);
        this.name = name;
    }

    public OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException();
    }
    
    public List<String> getNameHistory() {
        return nameHistory;
    }	

    public List<String> getAlphabeticalNameHistory() {
        Collections.sort(alphabeticalNameHistory);
        return alphabeticalNameHistory;
    }
    
    public String getId() {
        return id;
    }
	
    public List<String> getIdHistory() {
        return idHistory;
    }
    
	
    public List<String> getAlphabeticalIdHistory() {
        Collections.sort(alphabeticalIdHistory);
        return alphabeticalIdHistory;
    }
	
    public void setId(String id) {
        this.idHistory.add(name);
        this.alphabeticalIdHistory.add(name);
        this.id = id;
    }
}
