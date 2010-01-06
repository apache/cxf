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
import java.util.WeakHashMap;
import java.util.logging.Handler;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import org.apache.abdera.model.Element;
import org.apache.abdera.model.Feed;
import org.apache.cxf.jaxrs.ext.MessageContext;
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
    private WeakHashMap<Integer, Feed> feeds = new WeakHashMap<Integer, Feed>();
    private int pageSize = 20;
    private boolean useArchivedFeeds;
    
    @Context
    private MessageContext context;
    
    @GET
    @Produces("application/atom+xml")
    public Feed getRecords() {
        int page = getPageValue();
        synchronized (feeds) {
            Feed f = feeds.get(page);
            if (f != null) {
                return f;
            }
        }
        
        List<? extends Element> elements = null;
        synchronized (records) {
            List<LogRecord> list = getSubList(page);
            elements = converter.convert(list);
        }
        Feed feed = (Feed)(elements.get(0));
        setFeedPageProperties(feed, page);
        synchronized (feeds) {
            feeds.put(page, feed);
        }
        return feed;
    }

    protected List<LogRecord> getSubList(int page) {
        if (records.size() == 0) {
            return records;
        }
        
        if (!useArchivedFeeds) {
        
            int fromIndex = page == 1 ? 0 : (page - 1) * pageSize;
            if (fromIndex > records.size()) {
                // this should not happen really
                page = 1;
                fromIndex = 0;
            }
            int toIndex = page == 1 ? pageSize : fromIndex + pageSize;
            if (toIndex > records.size()) {
                toIndex = records.size();
            }
            return records.subList(fromIndex, toIndex);
        } else {
            int fromIndex = records.size() - pageSize * page;
            if (fromIndex < 0) {
                fromIndex = 0;
            }
            int toIndex = pageSize < records.size() ? records.size() : pageSize;
            return records.subList(fromIndex, toIndex);
        }
    }
    
    protected void setFeedPageProperties(Feed feed, int page) {
        String self = context.getUriInfo().getRequestUri().toString();
        feed.addLink(self, "self");
        
        String uri = context.getUriInfo().getAbsolutePath().toString();
        
        if (!useArchivedFeeds) {
        
            if (page > 2) {
                feed.addLink(uri, "first");
            }
            
            if (page * pageSize < records.size()) {
                feed.addLink(uri + "?page=" + (page + 1), "next");
            }
            
            if (page * (pageSize + 1) < records.size()) {
                feed.addLink(uri + "?page=" + (records.size() / pageSize + 1), "last");
            }
            
            if (page > 1) {
                uri = page > 2 ? uri + "?page=" + (page - 1) : uri;
                feed.addLink(uri, "previous");
            }
        } else {
            feed.addLink(uri, "current");
            // TODO : add prev-archive and next-archive; next-archive should not be set if it will point to
            // current
            // and xmlns:fh="http://purl.org/syndication/history/1.0":archive extension but only if
            // it is not current
        }
        
    }
    
        
    protected int getPageValue() {
        String pageValue = context.getUriInfo().getQueryParameters().getFirst("page");
        int page = 1;
        try {
            if (pageValue != null) {
                page = Integer.parseInt(pageValue);
            } 
        } catch (Exception ex) {
            // default to 1
        }
        return page;
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
        
    }
    
    public void setPageSize(int size) {
        pageSize = size;
    }
}
