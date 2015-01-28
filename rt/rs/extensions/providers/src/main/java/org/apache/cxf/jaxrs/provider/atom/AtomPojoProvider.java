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
package org.apache.cxf.jaxrs.provider.atom;

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
import javax.xml.stream.XMLStreamReader;

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
import org.apache.cxf.jaxrs.provider.AbstractConfigurableProvider;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.staxutils.StaxUtils;

@Produces({"application/atom+xml", "application/atom+xml;type=feed", "application/atom+xml;type=entry" })
@Consumes({"application/atom+xml", "application/atom+xml;type=feed", "application/atom+xml;type=entry" })
@Provider
public class AtomPojoProvider extends AbstractConfigurableProvider
    implements MessageBodyWriter<Object>, MessageBodyReader<Object> {
    
    private static final Logger LOG = LogUtils.getL7dLogger(AtomPojoProvider.class);
    private static final Abdera ATOM_ENGINE = new Abdera();
    private static final String DEFAULT_ENTRY_CONTENT_METHOD = "getContent";
    
    private JAXBElementProvider<Object> jaxbProvider = new JAXBElementProvider<Object>();
    private Map<String, String> collectionGetters = Collections.emptyMap();
    private Map<String, String> collectionSetters = Collections.emptyMap();
    
    private Map<Class<?>, AtomElementWriter<?, ?>> atomClassWriters = Collections.emptyMap();
    private Map<Class<?>, AtomElementReader<?, ?>> atomClassReaders = Collections.emptyMap();
    private Map<Class<?>, AbstractAtomElementBuilder<?>> atomClassBuilders = Collections.emptyMap();
    
    //Consider deprecating String based maps 
    private Map<String, AtomElementWriter<?, ?>> atomWriters = Collections.emptyMap();
    private Map<String, AtomElementReader<?, ?>> atomReaders = Collections.emptyMap();
    private Map<String, AbstractAtomElementBuilder<?>> atomBuilders = Collections.emptyMap();
    
    private MessageContext mc;   
    private boolean formattedOutput;
    private boolean useJaxbForContent = true;
    private boolean autodetectCharset;
    private String entryContentMethodName = DEFAULT_ENTRY_CONTENT_METHOD;
    
    public void setUseJaxbForContent(boolean use) {
        this.useJaxbForContent = use;
    }
    
    public void setEntryContentMethodName(String name) {
        this.entryContentMethodName = name;
    }
    
    @Context
    public void setMessageContext(MessageContext context) {
        mc = context;
        for (AbstractAtomElementBuilder<?> builder : atomClassBuilders.values()) {
            builder.setMessageContext(context);
        }
        for (AtomElementWriter<?, ?> writer : atomClassWriters.values()) {
            tryInjectMessageContext(writer);
        }
        for (AtomElementReader<?, ?> reader : atomClassReaders.values()) {
            tryInjectMessageContext(reader);
        }
        for (AbstractAtomElementBuilder<?> builder : atomBuilders.values()) {
            builder.setMessageContext(context);
        }
        for (AtomElementWriter<?, ?> writer : atomWriters.values()) {
            tryInjectMessageContext(writer);
        }
        for (AtomElementReader<?, ?> reader : atomReaders.values()) {
            tryInjectMessageContext(reader);
        }
    }
    
    protected void tryInjectMessageContext(Object handler) {
        Method m = null;
        try {
            m = handler.getClass().getMethod("setMessageContext", new Class[]{MessageContext.class});
        } catch (Throwable t) {
            return;
        }
        try {
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

    public void writeTo(Object o, Class<?> cls, Type genericType, Annotation[] annotations, 
                        MediaType mt, MultivaluedMap<String, Object> headers, OutputStream os)
        throws IOException {
        boolean isFeed = isFeedRequested(mt);        
        boolean isCollection = InjectionUtils.isSupportedCollectionOrArray(cls);
        
        
        if (isFeed && isCollection) {
            reportError("Atom feed can only be created from a collection wrapper", null);
        } else if (!isFeed && isCollection) {
            reportError("Atom entry can only be created from a single object", null);
        }
        Factory factory = Abdera.getNewFactory();
        
        Element atomElement = null;
        try {
            if (isFeed && !isCollection) {
                atomElement = createFeedFromCollectionWrapper(factory, o, cls);
            } else if (!isFeed && !isCollection) {
                atomElement = createEntryFromObject(factory, o, cls);
            }
        } catch (Exception ex) {
            throw ExceptionUtils.toInternalServerErrorException(ex, null);
        }
        
        try {
            writeAtomElement(atomElement, os);
        } catch (IOException ex) {
            reportError("Atom element can not be serialized", ex);
        }
    }
    
    private void writeAtomElement(Element atomElement, OutputStream os) throws IOException {
        Writer w = formattedOutput ? createWriter("prettyxml") : null;
        if (w != null) {
            atomElement.writeTo(w, os);
        } else {
            atomElement.writeTo(os);
        }
    }
    
    protected Writer createWriter(String writerName) {
        return ATOM_ENGINE.getWriterFactory().getWriter(writerName);
    }
    
    public void setFormattedOutput(boolean formattedOutput) {
        this.formattedOutput = formattedOutput;
    }
    
    protected Feed createFeedFromCollectionWrapper(Factory factory, Object o, Class<?> pojoClass) 
        throws Exception {
        
        Feed feed = factory.newFeed();
        
        boolean writerUsed = buildFeed(feed, o, pojoClass);
        
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
        
        setFeedFromCollection(factory, feed, o, pojoClass, collection, m.getReturnType(), 
                              m.getGenericReturnType(), writerUsed);
        return feed;
    }
    
    private String getCollectionMethod(Class<?> cls, boolean getter) {
        Map<String, String> map = getter ? collectionGetters : collectionSetters; 
        String methodName = getCollectionMethod(map, cls);
        if (methodName == null) {
            try {
                methodName = (getter ? "get" : "set") + cls.getSimpleName();
                Class<?>[] params = getter ? new Class[]{} : new Class[]{List.class};
                cls.getMethod(methodName, params);
            } catch (Exception ex) {
                String type = getter ? "getter" : "setter";
                reportError("Collection " + type + " method for " + cls.getName()
                    + " has not been specified and no default " + methodName + " is available", null);
            }
        }
        return methodName;
    }
    
    private String getCollectionMethod(Map<String, String> map, Class<?> pojoClass) {
        if (pojoClass == Object.class) {
            return null;
        }
        String method = map.get(pojoClass.getName());
        if (method != null) {
            return method;
        } else {
            return getCollectionMethod(map, pojoClass.getSuperclass());
        }
    }
    
    @SuppressWarnings("unchecked")
    protected <X> boolean buildFeed(Feed feed, X o, Class<?> pojoClass) {
        AtomElementWriter<?, ?> builder = getAtomWriter(pojoClass);
        if (builder != null) {
            ((AtomElementWriter<Feed, X>)builder).writeTo(feed, o);
            return true;
        }
        return false;
    }
    
    protected AtomElementWriter<?, ?> getAtomWriter(Class<?> pojoClass) {
        AtomElementWriter<?, ?> writer = getAtomClassElementHandler(atomClassWriters, pojoClass);
        return writer == null && atomWriters != null 
            ? getAtomElementHandler(atomWriters, pojoClass) : writer; 
    }
    
    protected AtomElementReader<?, ?> getAtomReader(Class<?> pojoClass) {
        AtomElementReader<?, ?> reader = getAtomClassElementHandler(atomClassReaders, pojoClass);
        return reader == null && atomReaders != null 
            ? getAtomElementHandler(atomReaders, pojoClass) : reader; 
    }
    
    private <T> T getAtomClassElementHandler(Map<Class<?>, T> handlers, Class<?> pojoClass) {
        for (Map.Entry<Class<?>, T> entry : handlers.entrySet()) {
            if (entry.getKey().isAssignableFrom(pojoClass)) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    protected <T> T getAtomElementHandler(Map<String, T> handlers, Class<?> pojoClass) {
        T handler = getAtomElementHandlerSuperClass(handlers, pojoClass);
        if (handler == null) {
            Class<?>[] interfaces = pojoClass.getInterfaces();
            for (Class<?> inter : interfaces) {
                handler = handlers.get(inter.getName());
                if (handler != null) {
                    break;
                }
            }
        }
        return handler;
    }
    
    private <T> T getAtomElementHandlerSuperClass(Map<String, T> handlers, Class<?> pojoClass) {
        if (pojoClass == null || pojoClass == Object.class) {
            return null;
        }
        T handler = handlers.get(pojoClass.getName());
        if (handler != null) {
            return handler;
        } else {
            return getAtomElementHandlerSuperClass(handlers, pojoClass.getSuperclass());
        }
    }
    
    //CHECKSTYLE:OFF
    protected void setFeedFromCollection(Factory factory, 
                                         Feed feed, 
                                         Object wrapper,
                                         Class<?> wrapperCls,
                                         Object collection,
                                         Class<?> collectionCls, 
                                         Type collectionType, 
                                         boolean writerUsed) throws Exception {
    //CHECKSTYLE:ON    
        Object[] arr = collectionCls.isArray() ? (Object[])collection : ((Collection<?>)collection).toArray();
        Class<?> memberClass = InjectionUtils.getActualType(collectionType);
        
        for (Object o : arr) {
            Entry entry = createEntryFromObject(factory, o, memberClass);
            feed.addEntry(entry);
        }
        if (!writerUsed) {
            setFeedProperties(factory, feed, wrapper, wrapperCls, collection, collectionCls, collectionType);
        }
    }
    
    protected AbstractAtomElementBuilder<?> getAtomBuilder(Class<?> pojoClass) {
        AbstractAtomElementBuilder<?> builder = getAtomClassElementHandler(atomClassBuilders, pojoClass);
        return builder == null && atomBuilders != null 
            ? getAtomElementHandler(atomBuilders, pojoClass) : builder;
    }
    
    @SuppressWarnings("unchecked")
    protected void setFeedProperties(Factory factory, 
                                     Feed feed, 
                                     Object wrapper, 
                                     Class<?> wrapperCls,
                                     Object collection, 
                                     Class<?> collectionCls, 
                                     Type collectionType) {
        
        AbstractAtomElementBuilder<Object> builder = 
            (AbstractAtomElementBuilder<Object>)getAtomBuilder(wrapperCls);
        if (builder == null) {
            return;
        }
        setCommonElementProperties(factory, feed, builder, wrapper);
        
        AbstractFeedBuilder<Object> theBuilder = (AbstractFeedBuilder<Object>)builder;
        
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
    
    
    
    protected Entry createEntryFromObject(Factory factory, Object o, Class<?> cls) throws Exception {
        Entry entry = factory.getAbdera().newEntry();
        
        if (!buildEntry(entry, o, cls)) {
            setEntryProperties(factory, entry, o, cls);
        }
        
        if (entry.getContentElement() == null 
            && entry.getExtensions().size() == 0) {
            createEntryContent(factory, entry, o, cls);    
        }
        return entry;
    
    }
    
    @SuppressWarnings("unchecked")
    protected boolean buildEntry(Entry entry, Object o, Class<?> pojoClass) {
        AtomElementWriter<?, ?> builder = getAtomWriter(pojoClass);
        if (builder != null) {
            ((AtomElementWriter<Entry, Object>)builder).writeTo(entry, o);
            return true;
        }
        return false;
    }
    
    protected void createEntryContent(Factory factory, Entry e, Object o, Class<?> cls) throws Exception {
    
        String content = null;
        
        if (useJaxbForContent) {
            JAXBContext jc = jaxbProvider.getJAXBContext(cls, cls);
            StringWriter writer = new StringWriter();
            jc.createMarshaller().marshal(o, writer);
            content = writer.toString();
        } else {
            Method m = cls.getMethod(entryContentMethodName, new Class[]{});
            content = (String)m.invoke(o, new Object[]{});
        }
        
        setEntryContent(factory, e, content);
        
    }
    
    protected void setEntryContent(Factory factory, Entry e, String content) {
        Content ct = factory.newContent(Content.Type.XML);
        ct.setValue(content);
        e.setContentElement(ct);
    }
    
    protected void setEntryProperties(Factory factory, Entry entry, 
                                          Object o, Class<?> cls) {
        @SuppressWarnings("unchecked")
        AbstractAtomElementBuilder<Object> builder 
            = (AbstractAtomElementBuilder<Object>)getAtomBuilder(cls);
        if (builder == null) {
            return;
        }
        
        setCommonElementProperties(factory, entry, builder, o);
        
        AbstractEntryBuilder<Object> theBuilder = (AbstractEntryBuilder<Object>)builder;
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
        
        String content = theBuilder.getContent(o);
        if (content != null) {
            setEntryContent(factory, entry, content);    
        }
        
    }

    private void setCommonElementProperties(Factory factory, ExtensibleElement element, 
                                            AbstractAtomElementBuilder<Object> builder,
                                            Object o) {
        String baseUri = builder.getBaseUri(o);
        if (baseUri != null) {
            element.setBaseUri(baseUri);
        }
        
    }
    private void reportError(String message, Exception ex, int status) {
        LOG.warning(message);
        Response response = JAXRSUtils.toResponseBuilder(status).type("text/plain").entity(message).build();
        throw ExceptionUtils.toHttpException(ex, response);
    }
    private void reportError(String message, Exception ex) {
        reportError(message, ex, 500);
    }
    
    protected boolean isFeedRequested(MediaType mt) {
        if ("entry".equalsIgnoreCase(mt.getParameters().get("type"))) {
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
    
    public void setAtomClassWriters(Map<Class<?>, AtomElementWriter<?, ?>> writers) {
        this.atomClassWriters = writers;
    }
    
    public void setAtomClassReaders(Map<Class<?>, AtomElementReader<?, ?>> readers) {
        this.atomClassReaders = readers;
    }

    public void setAtomClassBuilders(Map<Class<?>, AbstractAtomElementBuilder<?>> builders) {
        this.atomClassBuilders = builders;
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
            return readFromFeedOrEntry(cls, mt, headers, is);
        } else {
            AtomEntryProvider p = new AtomEntryProvider();
            p.setAutodetectCharset(autodetectCharset);
            Entry entry = p.readFrom(Entry.class, Entry.class, 
                                     new Annotation[]{}, mt, headers, is);
            return readFromEntry(entry, cls);
        }
    }
    
    @SuppressWarnings("unchecked")
    private Object readFromFeedOrEntry(Class<Object> cls, MediaType mt, 
                           MultivaluedMap<String, String> headers, InputStream is) 
        throws IOException {
        
        AtomFeedProvider p = new AtomFeedProvider();
        p.setAutodetectCharset(autodetectCharset);
        Object atomObject = p.readFrom(Feed.class, Feed.class, new Annotation[]{}, mt, headers, is);
        if (atomObject instanceof Entry) {
            return this.readFromEntry((Entry)atomObject, cls);
        }
            
        Feed feed = (Feed)atomObject;
        AtomElementReader<?, ?> reader = getAtomReader(cls);
        if (reader != null) {
            return ((AtomElementReader<Feed, Object>)reader).readFrom(feed);
        }
        Object instance = null;
        try {
            String methodName = getCollectionMethod(cls, false);
            Method m = cls.getMethod(methodName, new Class[]{List.class});
            Class<Object> realCls 
                = (Class<Object>)InjectionUtils.getActualType(m.getGenericParameterTypes()[0]);
            List<Object> objects = new ArrayList<Object>();
            for (Entry e : feed.getEntries()) {
                objects.add(readFromEntry(e, realCls));
            }
            instance = cls.newInstance();
            m.invoke(instance, new Object[]{objects});
            
        } catch (Exception ex) {
            reportError("Object of type " + cls.getName() + " can not be deserialized from Feed", ex, 400);
        }
        return instance;
    }
    
    @SuppressWarnings("unchecked")
    private Object readFromEntry(Entry entry, Class<Object> cls) 
        throws IOException {
        
        AtomElementReader<?, ?> reader = getAtomReader(cls);
        if (reader != null) {
            return ((AtomElementReader<Entry, Object>)reader).readFrom(entry);
        }
        String entryContent = entry.getContent();
        if (entryContent != null) {
            XMLStreamReader xreader = StaxUtils.createXMLStreamReader(new StringReader(entryContent));
            try {
                Unmarshaller um = 
                    jaxbProvider.getJAXBContext(cls, cls).createUnmarshaller();
                return cls.cast(um.unmarshal(xreader));
            } catch (Exception ex) {
                reportError("Object of type " + cls.getName() + " can not be deserialized from Entry", ex, 400);
            } finally {
                if (xreader != null) {
                    StaxUtils.close(xreader);
                }
            }
        }
        return null;
    }

    public void setAutodetectCharset(boolean autodetectCharset) {
        this.autodetectCharset = autodetectCharset;
    }

    
}
