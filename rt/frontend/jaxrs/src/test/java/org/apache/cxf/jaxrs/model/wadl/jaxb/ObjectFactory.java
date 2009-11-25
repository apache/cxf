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
package org.apache.cxf.jaxrs.model.wadl.jaxb;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

@XmlRegistry
public class ObjectFactory {
    private static final QName BOOK_QNAME = new QName("http://superbooks", "thebook");
    private static final QName CHAPTER_QNAME = new QName("http://superbooks", "thechapter");
    
    public Book createBook() {
        return new Book();
    }
    public Chapter createChapter() {
        return new Chapter();
    }
    
    @XmlElementDecl(namespace = "http://superbooks", name = "thebook")
    public JAXBElement<Book> createBook(Book value) {
        return new JAXBElement<Book>(BOOK_QNAME, Book.class, null, value);
    }
    
    @XmlElementDecl(namespace = "http://superbooks", name = "thechapter")
    public JAXBElement<Chapter> createChapter(Chapter value) {
        return new JAXBElement<Chapter>(CHAPTER_QNAME, Chapter.class, null, value);
    }
}
