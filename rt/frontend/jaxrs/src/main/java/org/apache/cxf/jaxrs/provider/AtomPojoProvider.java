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
package org.apache.cxf.jaxrs.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.abdera.Abdera;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Content;
import org.apache.abdera.model.Element;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.ExtensibleElement;
import org.apache.abdera.model.Feed;
import org.apache.abdera.writer.Writer;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.atom.AbstractAtomElementBuilder;
import org.apache.cxf.jaxrs.ext.atom.AbstractEntryBuilder;
import org.apache.cxf.jaxrs.ext.atom.AbstractFeedBuilder;
import org.apache.cxf.jaxrs.ext.atom.AtomElementReader;
import org.apache.cxf.jaxrs.ext.atom.AtomElementWriter;
import org.apache.cxf.jaxrs.utils.InjectionUtils;

@Produces({"application/atom+xml", "application/atom+xml;type=feed", "application/atom+xml;type=entry" })
@Consumes({"application/atom+xml", "application/atom+xml;type=feed", "application/atom+xml;type=entry" })
@Provider
public class AtomPojoProvider extends AbstractConfigurableProvider
    implements MessageBodyWriter<Object>, MessageBodyReader<Object> {
    
    private static final Logger LOG = LogUtils.getL7dLogger(AtomPojoProvider.class);
    private static final Abdera ATOM_ENGINE = new Abdera();
    
    private JAXBElementProvider jaxbProvider = new JAXBElementProvider();
    private Map<String, String> collectionGetters = Collections.emptyMap();
    private Map<String, String> collectionSetters = Collections.emptyMap();
    private Map<String, AtomElementWriter<?, ?>> atomWriters = Collections.emptyMap();
    private Map<String, AtomElementReader<?, ?>> atomReaders = Collections.emptyMap();
    private Map<String, AbstractAtomElementBuilder<?>> atomBuilders = Collections.emptyMap();
    
    private MessageContext mc;   
    private boolean formattedOutput;
    
    @Context
    public void setMessageContext(MessageContext context) {
        mc = context;
        for (AbstractAtomElementBuilder builder : atomBuilders.values()) {
            builder.setMessageContext(context);
        }
        for (AtomElementWriter writer : atomWriters.values()) {
            tryInjectMessageContext(writer);
        }
        for (AtomElementReader reader : atomReaders.values()) {
            tryInjectMessageContext(reader);
        }
    }
    
    protected void tryInjectMessageContext(Object handler) {
        try {
            Method m = handler.getClass().getMethod("setMessageContext",
                                                    new Class[]{MessageContext.class});
            InjectionUtils.injectThroughMethod(handler, m, mc);
        } catch (Throwable t) {
            LOG.warning("Message context can not be injected into " + handler.getClass().getName() 
                        + " : " + t.getMessage());
        }
    }
    
    public long getSize(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return -1;
    }
    
    public void setCollectionGetters(Map<String, String> methods) {
        collectionGetters = methods;
    }
    
    public void setCollectionSetters(Map<String, String> methods) {
        collectionSetters = methods;
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return !Feed.class.isAssignableFrom(type) && !Entry.class.isAssignableFrom(type);
    }

    public void writeTo(Object o, Class<?> clazz, Type genericType, Annotation[] annotations, 
                        MediaType mt, MultivaluedMap<String, Object> headers, OutputStream os)
        throws IOException {
        boolean isFeed = isFeedRequested(mt);        
        boolean isCollection = InjectionUtils.isSupportedCollectionOrArray(clazz);
        
        
        if (isFeed && isCollection) {
            reportError("Atom feed can only be created from a collection wrapper", null);
        } else if (!isFeed && isCollection) {
            reportError("Atom entry can only be created from a single object", null);
        }
        
        Element atomElement = null;
        try {
            if (isFeed && !isCollection) {
                atomElement = createFeedFromCollectionWrapper(o);
            } else if (!isFeed && !isCollection) {
                atomElement = createEntryFromObject(o, clazz);
            }
        } catch (Exception ex) {
            throw new WebApplicationException(ex);
        }
        
        try {
            writeAtomElement(atomElement, os);
        } catch (IOException ex) {
            reportError("Atom element can not be serialized", ex);
        }
    }
    
    private void writeAtomElement(Element atomElement, OutputStream os) throws IOException {
        if (formattedOutput) {
            Writer w = ATOM_ENGINE.getWriterFactory().getWriter("prettyxml");
            atomElement.writeTo(w, os);
        } else {
            atomElement.writeTo(os);
        }
    }
    
    public void setFormattedOutput(boolean formattedOutput) {
        this.formattedOutput = formattedOutput;
    }
    
    protected Feed createFeedFromCollectionWrapper(Object o) throws Exception {
        
        Factory factory = Abdera.getNewFactory();
        Feed feed = factory.newFeed();
        
        boolean writerUsed = buildFeed(feed, o);
        
        if (feed.getEntries().size() > 0) {
            return feed;
        }
        
        String methodName = getCollectionMethod(o.getClass(), true);
        Object collection = null;
        Method m = null;
        try {
            m = o.getClass().getMethod(methodName, new Class[]{});
            collection = m.invoke(o, new Object[]{});
        } catch (Exception ex) {
            reportError("Collection for " + o.getClass().getName() + " can not be retrieved", ex);
        }
        
        setFeedFromCollection(factory, feed, o, collection, m.getReturnType(), m.getGenericReturnType(), 
                              writerUsed);
        return feed;
    }
    
    private String getCollectionMethod(Class<?> cls, boolean getter) {
        Map<String, String> map = getter ? collectionGetters : collectionSetters; 
        String methodName = map.get(cls.getName());
        if (methodName == null) {
            try {
                methodName = (getter ? "get" : "set") + cls.getSimpleName();
                Class[] params = getter ? new Class[]{} : new Class[]{List.class};
                cls.getMethod(methodName, params);
            } catch (Exception ex) {
                String type = getter ? "getter" : "setter";
                reportError("Collection " + type + " method for " + cls.getName()
                    + " has not been specified and no default " + methodName + " is available", null);
            }
        }
        return methodName;
    }
    
    @SuppressWarnings("unchecked")
    protected boolean buildFeed(Feed feed, Object o) {
        AtomElementWriter<?, ?> builder = atomWriters.get(o.getClass().getName());
        if (builder != null) {
            ((AtomElementWriter)builder).writeTo(feed, o);
            return true;
        }
        return false;
    }
    
    protected void setFeedFromCollection(Factory factory, Feed feed, Object wrapper, Object collection,
        Class<?> collectionCls, Type collectionType, boolean writerUsed) throws Exception {
        
        Object[] arr = collectionCls.isArray() ? (Object[])collection : ((Collection)collection).toArray();
        Class<?> memberClass = InjectionUtils.getActualType(collectionType);
        
        for (Object o : arr) {
            Entry entry = createEntryFromObject(o, memberClass);
            feed.addEntry(entry);
        }
        if (!writerUsed) {
            setFeedProperties(factory, feed, wrapper, collection, collectionCls, collectionType);
        }
    }
    
    @SuppressWarnings("unchecked")
    protected void setFeedProperties(Factory factory, Feed feed, Object wrapper, Object collection, 
                                     Class<?> collectionCls, Type collectionType) {
        
        AbstractAtomElementBuilder<?> builder = atomBuilders.get(wrapper.getClass().getName());
        if (builder == null) {
            return;
        }
        setCommonElementProperties(factory, feed, builder, wrapper);
        
        AbstractFeedBuilder theBuilder = (AbstractFeedBuilder)builder;
        
        // the hierarchy is a bit broken in that we can not set author/title.etc on some
        // common Feed/Entry super type
        
        String author = theBuilder.getAuthor(wrapper);
        if (author != null) {
            feed.addAuthor(author);
        } else {
            feed.addAuthor("CXF JAX-RS");
        }
        String title = theBuilder.getTitle(wrapper);
        if (title != null) {
            feed.setTitle(title);
        } else {
            feed.setTitle(String.format(wrapper.getClass().getSimpleName()
                          + " collection with %d entry(ies)", feed.getEntries().size()));
        }
        
        String id = theBuilder.getId(wrapper);
        if (id != null) {
            feed.setId(id);
        } else {
            feed.setId("uuid:" + UUID.randomUUID().toString());
        }
        String updated = theBuilder.getUpdated(wrapper);
        if (updated != null) {
            feed.setUpdated(updated);
        } else {
            feed.setUpdated(new Date());
        }
        
        
        Map<String, String> links = theBuilder.getLinks(wrapper);
        if (links != null) {
            for (Map.Entry<String, String> entry : links.entrySet()) {
                feed.addLink(entry.getKey(), entry.getValue());
            }
        }
        List<String> terms = theBuilder.getCategories(wrapper);
        if (terms != null) {
            for (String term : terms) {
                feed.addCategory(term);
            }
        }
        
        
        // feed specific
        
        String logo = theBuilder.getLogo(wrapper);
        if (logo != null) {
            feed.setLogo(logo);
        }
        String icon = theBuilder.getLogo(wrapper);
        if (icon != null) {
            feed.setIcon(icon);
        }
        
    }
    
    
    
    protected Entry createEntryFromObject(Object o, Class<?> cls) throws Exception {
        
        Factory factory = Abdera.getNewFactory();
        Entry entry = factory.getAbdera().newEntry();
        
        if (!buildEntry(entry, o)) {
            setEntryProperties(factory, entry, o, cls);
        }
        
        if (entry.getContentElement() == null 
            && entry.getExtensions().size() == 0) {
            createEntryContent(entry, o, cls);    
        }
        return entry;
    
    }
    
    @SuppressWarnings("unchecked")
    protected boolean buildEntry(Entry entry, Object o) {
        AtomElementWriter<?, ?> builder = atomWriters.get(o.getClass().getName());
        if (builder != null) {
            ((AtomElementWriter)builder).writeTo(entry, o);
            return true;
        }
        return false;
    }
    
    protected void createEntryContent(Entry e, Object o, Class<?> cls) throws Exception {
    
        Factory factory = Abdera.getNewFactory();
        JAXBContext jc = jaxbProvider.getJAXBContext(cls, cls);
        
        StringWriter writer = new StringWriter();
        jc.createMarshaller().marshal(o, writer);
        
        e.setContentElement(factory.newContent());
        e.getContentElement().setContentType(Content.Type.XML);
        e.getContentElement().setValue(writer.toString());
        
    }
    
    @SuppressWarnings("unchecked")
    protected void setEntryProperties(Factory factory, Entry entry, Object o, Class<?> cls) {
        AbstractAtomElementBuilder<?> builder = atomBuilders.get(o.getClass().getName());
        if (builder == null) {
            return;
        }
        
        setCommonElementProperties(factory, entry, builder, o);
        
        AbstractEntryBuilder theBuilder = (AbstractEntryBuilder)builder;
        String author = theBuilder.getAuthor(o);
        if (author != null) {
            entry.addAuthor(author);
        } else {
            entry.addAuthor("CXF JAX-RS");
        }
        String title = theBuilder.getTitle(o);
        if (title != null) {
            entry.setTitle(title);
        } else {
            entry.setTitle(o.getClass().getSimpleName());
        }
        
        String id = theBuilder.getId(o);
        if (id != null) {
            entry.setId(id);
        } else {
            entry.setId("uuid:" + UUID.randomUUID().toString());
        }
        String updated = theBuilder.getUpdated(o);
        if (updated != null) {
            entry.setUpdated(updated);
        } else {
            entry.setUpdated(new Date());
        }
        
        Map<String, String> links = theBuilder.getLinks(o);
        if (links != null) {
            for (Map.Entry<String, String> e : links.entrySet()) {
                entry.addLink(e.getKey(), e.getValue());
            }
        }
        
        // entry specific
        
        String published = theBuilder.getPublished(o);
        if (published != null) {
            entry.setPublished(published);    
        }
        
        String summary = theBuilder.getSummary(o);
        if (summary != null) {
            entry.setSummary(summary);    
        }
        
        List<String> terms = theBuilder.getCategories(o);
        if (terms != null) {
            for (String term : terms) {
                entry.addCategory(term);
            }
        }
        
    }

    @SuppressWarnings("unchecked")
    private void setCommonElementProperties(Factory factory, ExtensibleElement element, 
                                            AbstractAtomElementBuilder builder,
                                            Object o) {
        String baseUri = builder.getBaseUri(o);
        if (baseUri != null) {
            element.setBaseUri(baseUri);
        }
        
    }
    
    private void reportError(String message, Exception ex) {
        LOG.warning(message);
        Response response = Response.status(500).type("text/plain").entity(message).build();
        if (ex == null) {
            throw new WebApplicationException(response);
        } else {
            throw new WebApplicationException(ex, response);
        }
    }
    
    private boolean isFeedRequested(MediaType mt) {
        if ("entry".equals(mt.getParameters().get("type"))) {
            return false;
        }
        return true;
    }

    public void setAtomWriters(Map<String, AtomElementWriter<?, ?>> writers) {
        this.atomWriters = writers;
    }
    
    public void setAtomReaders(Map<String, AtomElementReader<?, ?>> readers) {
        this.atomReaders = readers;
    }

    public void setAtomBuilders(Map<String, AbstractAtomElementBuilder<?>> builders) {
        this.atomBuilders = builders;
    }

    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, 
                              MediaType mediaType) {
        return true;
    }

    public Object readFrom(Class<Object> cls, Type type, Annotation[] anns, MediaType mt, 
                           MultivaluedMap<String, String> headers, InputStream is) 
        throws IOException, WebApplicationException {
        boolean isFeed = isFeedRequested(mt);
        
        if (isFeed) {
            return readFromFeed(cls, mt, headers, is);
        } else {
            Entry entry = new AtomEntryProvider().readFrom(Entry.class, Entry.class, 
                                                           new Annotation[]{}, mt, headers, is);
            return readFromEntry(entry, cls, mt, headers, is);
        }
    }
    
    @SuppressWarnings("unchecked")
    private Object readFromFeed(Class<Object> cls, MediaType mt, 
                                MultivaluedMap<String, String> headers, InputStream is) 
        throws IOException {
        
        AtomFeedProvider p = new AtomFeedProvider();
        Feed feed = p.readFrom(Feed.class, Feed.class, new Annotation[]{}, mt, headers, is);
        
        AtomElementReader<?, ?> reader = atomReaders.get(cls.getName());
        if (reader != null) {
            return ((AtomElementReader)reader).readFrom(feed);
        }
        Object instance = null;
        try {
            String methodName = getCollectionMethod(cls, false);
            Method m = cls.getMethod(methodName, new Class[]{List.class});
            Class<?> realCls = InjectionUtils.getActualType(m.getGenericParameterTypes()[0]);
            List<Object> objects = new ArrayList<Object>();
            for (Entry e : feed.getEntries()) {
                objects.add(readFromEntry(e, realCls, mt, headers, is));
            }
            instance = cls.newInstance();
            m.invoke(instance, new Object[]{objects});
            
        } catch (Exception ex) {
            reportError("Object of type " + cls.getName() + " can not be deserialized from Feed", ex);
        }
        return instance;
    }
    
    @SuppressWarnings("unchecked")
    private Object readFromEntry(Entry entry, Class<?> cls, MediaType mt, 
                                MultivaluedMap<String, String> headers, InputStream is) 
        throws IOException {
        
        AtomElementReader<?, ?> reader = atomReaders.get(cls.getName());
        if (reader != null) {
            return ((AtomElementReader)reader).readFrom(entry);
        }
        try {
            Unmarshaller um = 
                new JAXBElementProvider().getJAXBContext(cls, cls).createUnmarshaller();
            return um.unmarshal(new StringReader(entry.getContent()));
        } catch (Exception ex) {
            reportError("Object of type " + cls.getName() + " can not be deserialized from Entry", ex);
        }
        return null;
    }

    
}
