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

package org.apache.cxf.systest.jaxrs;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.customer.book.BookNotFoundFault;

public class BookSubresourceImpl implements BookSubresource {

    private Long id;
    
    public BookSubresourceImpl() {
        id = 123L;
    }
    
    public BookSubresourceImpl(Long id) {
        this.id = id;
    }
    
    public Book getTheBook() throws BookNotFoundFault {
        
        if (id == 0) {
            return null;
        }
        
        Book b = new Book();
        b.setId(id);
        b.setName("CXF in Action");
        return b;
    }

    public Book getTheBook2(String n1, String n2, String n3, String n4, String n5, String n6) 
        throws BookNotFoundFault {
        
        Book b = new Book();
        b.setId(id); 
        b.setName(n1 + n2 + n3 + n4 + n5 + n6);
        return b;
    }
    
    public Book getTheBook3(MultivaluedMap<String, String> form) throws BookNotFoundFault {
        Book b = new Book();
        
        b.setId(Long.valueOf(form.getFirst("id"))); 
        b.setName(form.getFirst("name"));
        return b;
    }

}
