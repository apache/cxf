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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.ws.rs.ConsumeMime;
import javax.ws.rs.ProduceMime;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.jaxrs.JAXRSUtils;

public final class ProviderFactory {
    
    private static final ProviderFactory PF = new ProviderFactory();
    
    private List<MessageBodyReader> defaultMessageReaders = new ArrayList<MessageBodyReader>();
    private List<MessageBodyWriter> defaultMessageWriters = new ArrayList<MessageBodyWriter>();
    private List<MessageBodyReader> userMessageReaders = new ArrayList<MessageBodyReader>();
    private List<MessageBodyWriter> userMessageWriters = new ArrayList<MessageBodyWriter>();
    private List<SystemQueryHandler> queryHandlers = new ArrayList<SystemQueryHandler>();
    
    private ProviderFactory() {
        // TODO : this needs to be done differently,
        // we need to use cxf-jaxrs-extensions
        setProviders(defaultMessageReaders,
                     defaultMessageWriters,
                     new JSONProvider(),
                     new BinaryDataProvider(),
                     new JAXBElementProvider(),
                     new StringProvider(),
                     new SourceProvider(),
                     new AtomFeedProvider(),
                     new AtomEntryProvider(),
                     new FormEncodingReaderProvider());
        
        queryHandlers.add(new AcceptTypeQueryHandler());
    }
    
    public static ProviderFactory getInstance() {
        return PF;
    }

    public <T> MessageBodyReader<T> createMessageBodyReader(Class<T> bodyType, MediaType mediaType) {
        // Try user provided providers
        MessageBodyReader<T> mr = chooseMessageReader(userMessageReaders, 
                                                      bodyType,
                                                      mediaType);
        
        //If none found try the default ones
        if (mr == null) {
            mr = chooseMessageReader(defaultMessageReaders,
                                     bodyType,
                                     mediaType);
        }     
        
        return mr;
    }
    
    public SystemQueryHandler getQueryHandler(MultivaluedMap<String, String> query) {
        
        for (SystemQueryHandler h : queryHandlers) {
            if (h.supports(query)) {
                return h;
            }
        }
        
        return null;
    }

    public <T> MessageBodyWriter<T> createMessageBodyWriter(Class<T> bodyType, MediaType mediaType) {
        // Try user provided providers
        MessageBodyWriter<T> mw = chooseMessageWriter(userMessageWriters,
                                                      bodyType,
                                                      mediaType);
        
        //If none found try the default ones
        if (mw == null) {
            mw = chooseMessageWriter(defaultMessageWriters,
                                     bodyType,
                                     mediaType);
        }     
        
        return mw;
    }
    
       
    private void setProviders(List<MessageBodyReader> readers, 
                              List<MessageBodyWriter> writers, 
                              Object... providers) {
        
        for (Object o : providers) {
            if (MessageBodyReader.class.isAssignableFrom(o.getClass())) {
                readers.add((MessageBodyReader)o); 
            }
            
            if (MessageBodyWriter.class.isAssignableFrom(o.getClass())) {
                writers.add((MessageBodyWriter)o); 
            }
        }
        
        sortReaders(readers);
        sortWriters(writers);
    }
    
    /*
     * sorts the available providers according to the media types they declare
     * support for. Sorting of media types follows the general rule: x/y < * x < *,
     * i.e. a provider that explicitly lists a media types is sorted before a
     * provider that lists *. Quality parameter values are also used such that
     * x/y;q=1.0 < x/y;q=0.7.
     */    
    private void sortReaders(List<MessageBodyReader> entityProviders) {
        Collections.sort(entityProviders, new MessageBodyReaderComparator());
    }
    
    private void sortWriters(List<MessageBodyWriter> entityProviders) {
        Collections.sort(entityProviders, new MessageBodyWriterComparator());
    }
    
        
    
    /**
     * Choose the first body reader provider that matches the requestedMimeType 
     * for a sorted list of Entity providers
     * Returns null if none is found.
     * @param <T>
     * @param messageBodyReaders
     * @param type
     * @param requestedMimeType
     * @return
     */
    private <T> MessageBodyReader<T> chooseMessageReader(
        List<MessageBodyReader> readers, Class<T> type, MediaType mediaType) {
        for (MessageBodyReader<T> ep : readers) {
            
            if (!ep.isReadable(type)) {
                continue;
            }
            
            List<MediaType> supportedMediaTypes =
                JAXRSUtils.getConsumeTypes(ep.getClass().getAnnotation(ConsumeMime.class));
            
            List<MediaType> availableMimeTypes = 
                JAXRSUtils.intersectMimeTypes(Collections.singletonList(mediaType),
                                              supportedMediaTypes);

            if (availableMimeTypes.size() != 0) {
                return ep;
            }
        }     
        
        return null;
        
    }
    
        
    /**
     * Choose the first body writer provider that matches the requestedMimeType 
     * for a sorted list of Entity providers
     * Returns null if none is found.
     * @param <T>
     * @param messageBodyWriters
     * @param type
     * @param requestedMimeType
     * @return
     */
    private <T> MessageBodyWriter<T> chooseMessageWriter(
        List<MessageBodyWriter> writers, Class<T> type, MediaType mediaType) {
        for (MessageBodyWriter<T> ep : writers) {
            if (!ep.isWriteable(type)) {
                continue;
            }
            List<MediaType> supportedMediaTypes =
                JAXRSUtils.getProduceTypes(ep.getClass().getAnnotation(ProduceMime.class));
            
            List<MediaType> availableMimeTypes = 
                JAXRSUtils.intersectMimeTypes(Collections.singletonList(mediaType),
                                              supportedMediaTypes);

            if (availableMimeTypes.size() != 0) {
                return ep;
            }
        }     
        
        return null;
        
    }
    
    //TODO : also scan for the @Provider annotated implementations    
    public boolean registerUserEntityProvider(Object o) {
        setProviders(userMessageReaders, userMessageWriters, o);
        return true;
    }
    
    public boolean deregisterUserEntityProvider(Object o) {
        boolean result = false;
        if (o instanceof MessageBodyReader) {
            result = userMessageReaders.remove(o);
        }
        return o instanceof MessageBodyReader 
               ? result && userMessageWriters.remove(o) : result;
                                               
    }
    
    public List<MessageBodyReader> getDefaultMessageReaders() {
        return defaultMessageReaders;
    }

    public List<MessageBodyWriter> getDefaultMessageWriters() {
        return defaultMessageWriters;
    }
    
    public List<MessageBodyReader> getUserMessageReaders() {
        return userMessageReaders;
    }
    
    public List<MessageBodyWriter> getUserMessageWriters() {
        return userMessageWriters;
    }
    
    public void clearUserMessageProviders() {
        userMessageReaders.clear();
        userMessageWriters.clear();
    }

    /**
     * Use for injection of entityProviders
     * @param entityProviders the entityProviders to set
     */
    public void setUserEntityProviders(List<?> userProviders) {
        setProviders(userMessageReaders,
                     userMessageWriters,
                     userProviders.toArray());
    }

    private static class MessageBodyReaderComparator 
        implements Comparator<MessageBodyReader> {
        
        public int compare(MessageBodyReader e1, MessageBodyReader e2) {
            ConsumeMime c = e1.getClass().getAnnotation(ConsumeMime.class);
            String[] mimeType1 = {"*/*"};
            if (c != null) {
                mimeType1 = c.value();               
            }
            
            ConsumeMime c2 = e2.getClass().getAnnotation(ConsumeMime.class);
            String[] mimeType2 = {"*/*"};
            if (c2 != null) {
                mimeType2 = c2.value();               
            }
    
            return compareString(mimeType1[0], mimeType2[0]);
            
        }

        private int compareString(String str1, String str2) {
            if (!str1.startsWith("*/") && str2.startsWith("*/")) {
                return -1;
            } else if (str1.startsWith("*/") && !str2.startsWith("*/")) {
                return 1;
            } 
            
            return str1.compareTo(str2);
        }
    }
    
    private static class MessageBodyWriterComparator 
        implements Comparator<MessageBodyWriter> {
        
        public int compare(MessageBodyWriter e1, MessageBodyWriter e2) {
            ProduceMime c = e1.getClass().getAnnotation(ProduceMime.class);
            String[] mimeType1 = {"*/*"};
            if (c != null) {
                mimeType1 = c.value();               
            }
            
            ProduceMime c2 = e2.getClass().getAnnotation(ProduceMime.class);
            String[] mimeType2 = {"*/*"};
            if (c2 != null) {
                mimeType2 = c2.value();               
            }
    
            return compareString(mimeType1[0], mimeType2[0]);
            
        }
        
        private int compareString(String str1, String str2) {
            if (!str1.startsWith("*/") && str2.startsWith("*/")) {
                return -1;
            } else if (str1.startsWith("*/") && !str2.startsWith("*/")) {
                return 1;
            } 
            
            return str1.compareTo(str2);
        }
    }
}
