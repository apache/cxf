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

import org.apache.abdera.model.Feed;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.logging.LogLevel;
import org.apache.cxf.jaxrs.ext.logging.LogRecord;
import org.apache.cxf.jaxrs.ext.logging.ReadWriteLogStorage;
import org.apache.cxf.jaxrs.ext.logging.ReadableLogStorage;
import org.apache.cxf.jaxrs.ext.logging.atom.converter.StandardConverter;
import org.apache.cxf.jaxrs.ext.logging.atom.converter.StandardConverter.Format;
import org.apache.cxf.jaxrs.ext.logging.atom.converter.StandardConverter.Multiplicity;
import org.apache.cxf.jaxrs.ext.logging.atom.converter.StandardConverter.Output;
import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.OrSearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;

@Path("logs")
public class AtomPullServer extends AbstractAtomBean {

    private StandardConverter converter = 
        new StandardConverter(Output.FEED, Multiplicity.MANY, Format.CONTENT);
    private List<LogRecord> records = new LinkedList<LogRecord>();
    private WeakHashMap<Integer, Feed> feeds = new WeakHashMap<Integer, Feed>();
    private ReadableLogStorage storage;
    private int pageSize = 20;
    private int maxInMemorySize = 500;
    private boolean useArchivedFeeds;
    private int recordsSize;
    private volatile boolean alreadyClosed;
    private SearchCondition<LogRecord> condition;
        
    @Context
    private MessageContext context;
    
    @Override
    public void init() {
        // the storage might've been used to save previous records or it might
        // point to a file log entries are added to
        if (storage != null) {
            //-1 can be returned by read-only storage if it does not know in advance
            // a number of records it may contain 
            recordsSize = storage.getSize();
        }
        
        if (storage == null || storage instanceof ReadWriteLogStorage) {
            super.init();
        } else {
            // super.init() results in the additional Handler being created and publish()
            // method being called as a result. If the storage is read-only it is assumed it points to
            // the external source of log records thus no need to get the publish events here
            
            // instead we create a SearchCondition the external storage will check against when
            // loading the matching records on request
            
            List<SearchCondition<LogRecord>> list = new LinkedList<SearchCondition<LogRecord>>();
            for (LoggerLevel l : super.getLoggers()) {
                LogRecord r = new LogRecord();
                r.setLoggerName(l.getLogger());
                r.setLevel(LogLevel.valueOf(l.getLevel()));
                list.add(new SearchConditionImpl(r));
            }
            condition = new OrSearchCondition<LogRecord>(list);
        }
        
    }
    
    @GET
    @Produces("application/atom+xml")
    public Feed getRecords() {
        int page = getPageValue();
        
        // lets check if the Atom reader is asking for a set of records which has already been 
        // converted to Feed
        
        synchronized (feeds) {
            Feed f = feeds.get(page);
            if (f != null) {
                return f;
            }
        }
        
        Feed feed = null;
        synchronized (records) {
            List<LogRecord> list = getSubList(page);
            feed = (Feed)converter.convert(list).get(0);
            setFeedPageProperties(feed, page);
        }
        // if at the moment we've converted n < pageSize number of records only and
        // persist a Feed keyed by a page then another reader requesting the same page 
        // may miss latest records which might've been added since the original request
        if (feed.getEntries().size() == pageSize) {
            synchronized (feeds) {
                feeds.put(page, feed);
            }
        }
        return feed;
    }

    @GET
    @Path("records")
    @Produces("text/plain")
    public int getNumberOfAvaiableRecords() {
        return recordsSize;
    }
    
    
    protected List<LogRecord> getSubList(int page) {
        
        if (recordsSize == -1) {
            // let the external storage load the records it knows about
            List<LogRecord> list = new LinkedList<LogRecord>();
            storage.load(list, condition, page == 1 ? 0 : (page - 1) * pageSize, pageSize);
            return list;
        }
        
        if (recordsSize == 0) {
            return records;
        }
        
        int fromIndex = 0;
        int toIndex = 0;
        // see http://tools.ietf.org/html/draft-nottingham-atompub-feed-history-07
        if (!useArchivedFeeds) {
            fromIndex = page == 1 ? 0 : (page - 1) * pageSize;
            if (fromIndex > recordsSize) {
                // this should not happen really
                page = 1;
                fromIndex = 0;
            }
            toIndex = page == 1 ? pageSize : fromIndex + pageSize;
            if (toIndex > recordsSize) {
                toIndex = recordsSize;
            }
        } else {
            fromIndex = recordsSize - pageSize * page;
            if (fromIndex < 0) {
                fromIndex = 0;
            }
            toIndex = pageSize < recordsSize ? recordsSize : pageSize;
        }

        // if we have the storage then try to load from it
        if (storage != null) {
            if (fromIndex < storage.getSize()) {
                int storageSize = storage.getSize();
                int maxQuantityToLoad = toIndex > storageSize ? toIndex - storageSize : toIndex - fromIndex;
                List<LogRecord> list = new LinkedList<LogRecord>();
                storage.load(list, condition, fromIndex, maxQuantityToLoad);
                int totalQuantity = toIndex - fromIndex;
                if (list.size() < totalQuantity) {
                    int remaining = totalQuantity - list.size();
                    if (remaining > records.size()) {
                        remaining = records.size();
                    }
                    list.addAll(records.subList(0, remaining));
                }
                return list;
            } else {
                fromIndex -= storage.getSize();
                toIndex -= storage.getSize();
            }
        } 
        return records.subList(fromIndex, toIndex);
        
    }
    
    protected void setFeedPageProperties(Feed feed, int page) {
        String self = context.getUriInfo().getRequestUri().toString();
        feed.addLink(self, "self");
        
        String uri = context.getUriInfo().getAbsolutePath().toString();
        
        if (!useArchivedFeeds) {
            if (recordsSize != -1) {
                if (page > 2) {
                    feed.addLink(uri, "first");
                }
                
                if (page * pageSize < recordsSize) {
                    feed.addLink(uri + "?page=" + (page + 1), "next");
                }
                
                if (page * (pageSize + 1) < recordsSize) {
                    feed.addLink(uri + "?page=" + (recordsSize / pageSize + 1), "last");
                }
            } else if (feed.getEntries().size() == pageSize) {
                feed.addLink(uri + "?page=" + (page + 1), "next");
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
        if (alreadyClosed) {
            System.err.println("AtomPullServer has been closed, the following log record can not be saved : "
                               + record.toString());
            return;
        }
        synchronized (records) {
            if (records.size() == maxInMemorySize) {
                if (storage instanceof ReadWriteLogStorage) {
                    ((ReadWriteLogStorage)storage).save(records);
                    records.clear();
                } else {
                    LogRecord oldRecord = records.remove(0);
                    System.err.println("The oldest log record is removed : " + oldRecord.toString());
                }
            } 
            records.add(record);
            ++recordsSize;
        }
    }
    
    public void setPageSize(int size) {
        pageSize = size;
    }

    public void setMaxInMemorySize(int maxInMemorySize) {
        this.maxInMemorySize = maxInMemorySize;
    }

    public void setStorage(ReadableLogStorage storage) {
        this.storage = storage;
    }
    
    public void close() {
        if (alreadyClosed) {
            return;
        }
        alreadyClosed = true;
        if (storage instanceof ReadWriteLogStorage) {
            ((ReadWriteLogStorage)storage).save(records);
        }
    }
    
    private static class SearchConditionImpl implements SearchCondition<LogRecord> {
        private LogRecord template;
        
        public SearchConditionImpl(LogRecord l) {
            this.template = l;
        }

        public boolean isMet(LogRecord pojo) {
            
            return (template.getLevel() == LogLevel.ALL
                   || pojo.getLevel().compareTo(template.getLevel()) <= 0)
                   && template.getLoggerName().equals(pojo.getLoggerName());
        }

        public LogRecord getCondition() {
            return new LogRecord(template);
        }

        public ConditionType getConditionType() {
            return ConditionType.CUSTOM;
        }

        public List<SearchCondition<LogRecord>> getConditions() {
            return null;
        }

        public List<LogRecord> findAll(List<LogRecord> pojos) {
            // TODO Auto-generated method stub
            return null;
        }
        
        
    }
    
}
