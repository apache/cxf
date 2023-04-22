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
package org.apache.cxf.jaxrs.ext.search.jpa;

import jakarta.persistence.metamodel.ListAttribute;
import jakarta.persistence.metamodel.SingularAttribute;

@jakarta.persistence.metamodel.StaticMetamodel(Book.class)
//CHECKSTYLE:OFF
public final class Book_ {
    private Book_() {

    }
    public static volatile SingularAttribute<Book, Integer> id;
    public static volatile SingularAttribute<Book, String> bookTitle;
    public static volatile SingularAttribute<Book, Library> library;
    public static volatile SingularAttribute<Book, OwnerInfo> ownerInfo;
    public static volatile SingularAttribute<Book, OwnerAddress> address;
    public static volatile ListAttribute<Book, BookReview> reviews;
    public static volatile ListAttribute<Book, String> authors;
}
//CHECKSTYLE:ON
