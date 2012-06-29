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
package org.apache.cxf.management.web.logging.atom;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Handler;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.OrSearchCondition;
import org.apache.cxf.jaxrs.ext.search.PrimitiveStatement;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchConditionVisitor;
import org.apache.cxf.jaxrs.ext.search.SearchContext;
import org.apache.cxf.management.web.logging.LogLevel;
import org.apache.cxf.management.web.logging.LogRecord;
import org.apache.cxf.management.web.logging.ReadWriteLogStorage;
import org.apache.cxf.management.web.logging.ReadableLogStorage;
import org.apache.cxf.management.web.logging.atom.converter.StandardConverter;


@Path("/logs")
public class AtomPullServer extends AbstractAtomBean {

    private List<LogRecord> records = new LinkedList<LogRecord>();
    private WeakHashMap<Integer, Feed> feeds = new WeakHashMap<Integer, Feed>();
    private ReadableLogStorage storage;
    private int pageSize = 20;
    private int maxInMemorySize = 1000;
    private volatile int recordsSize;
    private volatile boolean alreadyClosed;
    private SearchCondition<LogRecord> readableStorageCondition;
        
    @Context
    private MessageContext context;
    
    private List<String> endpointAddresses;
    private String serverAddress;
    
    public void setEndpointAddress(String address) {
        setEndpointAddresses(Collections.singletonList(address));
    }
    
    public void setEndpointAddresses(List<String> addresses) {
        this.endpointAddresses = addresses;
    }
    
    public void setServerAddress(String address) {
        this.serverAddress = address;
    }
    
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
            readableStorageCondition = list.size() == 0 ? null : new OrSearchCondition<LogRecord>(list);
        }
        initBusProperty();
    }
    
    @Override
    protected Handler createHandler() {
        return new AtomPullHandler(this);
    }
    
    @SuppressWarnings("unchecked")
    protected void initBusProperty() {
        if (endpointAddresses != null && serverAddress != null && getBus() != null) {
            Bus bus = getBus();
            synchronized (bus) {
                Map<String, String> addresses = 
                    (Map<String, String>)bus.getProperty("org.apache.cxf.extensions.logging.atom.pull");
                if (addresses == null) {
                    addresses = new HashMap<String, String>();
                }
                for (String address : endpointAddresses) {
                    addresses.put(address, serverAddress + "/logs");
                }
                bus.setProperty("org.apache.cxf.extensions.logging.atom.pull", addresses);
            }
        }
    }
    
    @GET
    @Produces("application/atom+xml")
    public Feed getXmlFeed() {
        return getXmlFeedWithPage(1);
    }
    
    @GET
    @Produces("application/atom+xml")
    @Path("{id}")
    public Feed getXmlFeedWithPage(@PathParam("id") int page) {
        
        // lets check if the Atom reader is asking for a set of records which has already been 
        // converted to Feed
        
        synchronized (feeds) {
            Feed f = feeds.get(page);
            if (f != null) {
                return f;
            }
        }
        
        Feed feed = null;
        SearchCondition<LogRecord> condition = getCurrentCondition();
        synchronized (records) {
            List<LogRecord> list = new LinkedList<LogRecord>();
            int lastPage = fillSubList(list, page, condition);
            Collections.sort(list, new LogRecordComparator());
            feed = (Feed)new CustomFeedConverter(page).convert(list).get(0);
            setFeedPageProperties(feed, page, lastPage);
        }
        // if at the moment we've converted n < pageSize number of records only and
        // persist a Feed keyed by a page then another reader requesting the same page 
        // may miss latest records which might've been added since the original request
        if (condition == null && feed.getEntries().size() == pageSize) {
            synchronized (feeds) {
                feeds.put(page, feed);
            }
        }
        return feed;
    }
    
    @GET
    @Produces({"text/html", "application/xhtml+xml" })
    @Path("alternate/{id}")
    public String getAlternateFeed(@PathParam("id") int page) {
        List<LogRecord> list = new LinkedList<LogRecord>();
        fillSubList(list, page, getCurrentCondition());
        Collections.sort(list, new LogRecordComparator());
        return convertEntriesToHtml(list);
        
    }

    @GET
    @Path("entry/{id}")
    @Produces("application/atom+xml;type=entry")
    public Entry getEntry(@PathParam("id") int index) {
        List<LogRecord> list = getLogRecords(index, getCurrentCondition());
        return (Entry)new CustomEntryConverter(index).convert(list).get(0);
    }
    
    @GET
    @Path("entry/alternate/{id}")
    @Produces({"text/html", "application/xhtml+xml" })
    public String getAlternateEntry(@PathParam("id") int index) {
        List<LogRecord> logRecords = getLogRecords(index, getCurrentCondition());
        return convertEntryToHtml(logRecords.get(0));
    }
    
    @GET
    @Path("records")
    @Produces("text/plain")
    public int getNumberOfAvailableRecords() {
        return recordsSize;
    }
    
    private List<LogRecord> getLogRecords(int index, SearchCondition<LogRecord> theSearch) {
        List<LogRecord> list = new LinkedList<LogRecord>();
        if (storage != null) {
            int storageSize = storage.getSize();
            if (recordsSize == -1 || index < storageSize) {
                storage.load(list, theSearch, index, 1);
            } else if (index < recordsSize) {
                list.add(records.get(index - storageSize));   
            }
        } else {
            list.add(records.get(index));
        }
        if (list.size() != 1) { 
            throw new WebApplicationException(404);
        }
        return list;
    }
    
    
    protected int fillSubList(List<LogRecord> list, int page, SearchCondition<LogRecord> theSearch) {
        int oldListSize = list.size();
        
        if (storage != null) {
            page = storage.load(list, theSearch, page, pageSize);
        }
        
        if (recordsSize == -1 || recordsSize == 0 || list.size() == pageSize) {
            return page;
        }
        
        int fromIndex = page == 1 ? list.size() 
                                  : (page - 1) * pageSize + list.size();
        if (fromIndex > recordsSize) {
            // this should not happen really
            page = 1;
            fromIndex = 0;
        }
        int toIndex = page * pageSize;
        if (toIndex > recordsSize) {
            toIndex = recordsSize;
        }
        int offset = storage != null ? pageSize - (list.size() - oldListSize) : 0;
        fromIndex -= offset;
        toIndex -= offset;
        list.addAll(filterRecords(records.subList(fromIndex, toIndex), theSearch));
        
        
        if (theSearch != null && list.size() < pageSize && page * pageSize < recordsSize) {
            return fillSubList(list, page + 1, theSearch);    
        } else {
            return page;
        }
    }
    
    private List<LogRecord> filterRecords(List<LogRecord> list, SearchCondition<LogRecord> theSearch) {
        return theSearch == null ? list : theSearch.findAll(list);
    }
    
    private SearchCondition<LogRecord> getCurrentCondition() {
        SearchCondition<LogRecord> current = context.getContext(SearchContext.class)
            .getCondition(LogRecord.class);
        if (current == null) {
            return readableStorageCondition;
        } else {
            return current;
        }
    }
    
    private String getSearchExpression() {
        return context.getContext(SearchContext.class).getSearchExpression();
    }
    
    protected void setFeedPageProperties(Feed feed, int page, int lastPage) {
        String self = context.getUriInfo().getAbsolutePath().toString();
        feed.addLink(self, "self");
        
        int feedSize = feed.getEntries().size();
        String searchExpression = getSearchExpression();
        
        String uri = context.getUriInfo().getBaseUriBuilder().path("logs").build().toString();
        feed.addLink(uri + "/alternate/" + page, "alternate");
        if (recordsSize != -1) {
            if (page > 2) {
                feed.addLink(createLinkUri(uri, searchExpression), "first");
            }
            
            if (searchExpression == null && lastPage * pageSize < recordsSize
                || searchExpression != null && feedSize == pageSize) {
                feed.addLink(createLinkUri(uri + "/" + (lastPage + 1), searchExpression), "next");
            }
            
            if (searchExpression == null && page * (pageSize + 1) < recordsSize) {
                feed.addLink(uri + "/" + (recordsSize / pageSize + 1), "last");
            }
        } else if (feedSize == pageSize) {
            feed.addLink(createLinkUri(uri + "/" + (lastPage + 1), searchExpression), "next");
        }
        if (page > 1) {
            uri = page > 2 ? uri + "/" + (page - 1) : uri;
            feed.addLink(createLinkUri(uri, searchExpression), "previous");
        }
    }
    
    private String createLinkUri(String uri, String search) {
        return search == null ? uri : uri + "?_s=" + search; 
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
    
    public synchronized void reset() {
        records.clear();
        recordsSize = 0;
        feeds.clear();
    }
    
    // TODO : this all can be done later on in a simple xslt template
    private String convertEntriesToHtml(List<LogRecord> rs) {
        StringBuilder sb = new StringBuilder();
        startHtmlHeadAndBody(sb, "CXF Service Log Entries");
        addRecordToTable(sb, rs, true);
        sb.append("</body></html>");
        return sb.toString();
    }
    // TODO : this all can be done later on in a simple xslt template
    private String convertEntryToHtml(LogRecord r) {
        StringBuilder sb = new StringBuilder();
        startHtmlHeadAndBody(sb, r.getLevel().toString());
        addRecordToTable(sb, Collections.singletonList(r), false);
        sb.append("</body></html>");
        return sb.toString();
    }
    
    private void addRecordToTable(StringBuilder sb, List<LogRecord> list, boolean forFeed) {
        DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
        sb.append("<table border=\"1\">");
        sb.append("<tr><th>Date</th><th>Level</th><th>Logger</th><th>Message</th></tr>");
        for (LogRecord lr : list) {
            sb.append("<tr>");
            sb.append("<td>" + df.format(lr.getDate()) + "</td>");
            sb.append("<td>" + lr.getLevel().toString() + "</td>");
            sb.append("<td>" + lr.getLoggerName() + "</td>");
            String message = null;
            if (lr.getMessage().length() > 0) {
                message =  lr.getThrowable().length() > 0 ? lr.getMessage() + " : " + lr.getThrowable()
                           : lr.getMessage();
            } else if (lr.getThrowable().length() > 0) {
                message = lr.getThrowable();
            } else {
                message = "&nbsp";
            }
            if (forFeed && lr.getThrowable().length() > 0) {
                message = message.substring(0, message.length() / 2);
            }
            sb.append("<td>" + message + "</td>");
            sb.append("</tr>");
        }
        sb.append("</table><br/><br/>");
    
    }
    
    private void startHtmlHeadAndBody(StringBuilder sb, String title) {
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
        sb.append("<head>");
        sb.append("<meta http-equiv=\"content-type\" content=\"text/html;charset=UTF-8\"/>");
        sb.append("<title>" + "Log record with level " + title + "</title>");
        sb.append("</head>");
        sb.append("<body>");
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

        public List<SearchCondition<LogRecord>> getSearchConditions() {
            return null;
        }

        public List<LogRecord> findAll(Collection<LogRecord> pojos) {
            List<LogRecord> list = new LinkedList<LogRecord>();
            for (LogRecord r : pojos) {
                if (isMet(r)) {
                    list.add(r);
                }
            }
            return list;
        }

        public PrimitiveStatement getStatement() {
            return null;
        }

        public void accept(SearchConditionVisitor<LogRecord> visitor) {
        }
    }
    
    private static class LogRecordComparator implements Comparator<LogRecord> {

        public int compare(LogRecord r1, LogRecord r2) {
            return r1.getDate().compareTo(r2.getDate()) * -1;
        }
        
    }
    
    private class CustomFeedConverter extends StandardConverter {
        private int page;
        public CustomFeedConverter(int page) {
            super(Output.FEED, Multiplicity.MANY, Format.CONTENT);
            this.page = page;
        }
        
        @Override
        protected void setDefaultEntryProperties(Entry entry, List<LogRecord> rs, int entryIndex) {
            super.setDefaultEntryProperties(entry, rs, entryIndex);
            UriBuilder builder = context.getUriInfo().getAbsolutePathBuilder().path("entry");
            Integer realIndex = page == 1 ? entryIndex : page * pageSize + entryIndex;

            entry.addLink(builder.clone().path(realIndex.toString()).build().toString(), "self");
            entry.addLink(builder.path("alternate").path(realIndex.toString()).build().toString(), 
                          "alternate");
        }
        
    }
    
    private class CustomEntryConverter extends StandardConverter {
        private String selfFragment;
        private String altFragment;
        public CustomEntryConverter(int index) {
            super(Output.ENTRY, Multiplicity.ONE, Format.CONTENT);
            this.selfFragment = "logs/entry/" + index;
            this.altFragment = "logs/alternate/entry/" + index;
        }
        
        @Override
        protected void setDefaultEntryProperties(Entry entry, List<LogRecord> rs, int entryIndex) {
            super.setDefaultEntryProperties(entry, rs, entryIndex);
            entry.addLink(context.getUriInfo().getBaseUriBuilder().path(selfFragment).build().toString(),
                "self");
            entry.addLink(context.getUriInfo().getBaseUriBuilder().path(altFragment).build().toString(),
                "alternate");
        }
    }
}
