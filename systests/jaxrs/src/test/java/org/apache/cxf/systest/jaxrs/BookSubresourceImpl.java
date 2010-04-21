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

    public Book getTheBook2(String n1, String n2, String n3, String n33, 
                            String n4, String n5, String n6) 
        throws BookNotFoundFault {
        
        Book b = new Book();
        b.setId(id); 
        b.setName(n1 + n2 + n3 + n33 + n4 + n5 + n6);
        return b;
    }
    
    public Book getTheBook3(String sid, String name, Integer nameid) throws BookNotFoundFault {
        Book b = new Book();
        
        b.setId(Long.valueOf(sid)); 
        b.setName(name + nameid.toString());
        return b;
    }
    
    public Book getTheBook4(Book bookPath, Book bookQuery, 
                            Book bookMatrix, Book formBook) throws BookNotFoundFault {
        if (bookPath == null || bookQuery == null 
            || bookMatrix == null || formBook == null) {
            throw new RuntimeException();
        }
        long id1 = bookPath.getId();
        long id2 = bookQuery.getId();
        long id3 = bookMatrix.getId();
        long id4 = formBook.getId();
        if (id1 != 139L || id1 != id2 || id1 != id3 || id1 != id4 || id1 != id.longValue()) {
            throw new RuntimeException();
        }
        String name1 = bookPath.getName();
        String name2 = bookQuery.getName();
        String name3 = bookMatrix.getName();
        String name4 = formBook.getName();
        if (!"CXF Rocks".equals(name1) || !name1.equals(name2) 
            || !name1.equals(name3) || !name1.equals(name4)) {
            throw new RuntimeException();
        }
        return bookPath;
    }

    public Book getTheBookNoProduces() throws BookNotFoundFault {
        return getTheBook();
    }
    
    public OrderBean addOrder(OrderBean order) {
        return order;
    }

}
