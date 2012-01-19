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

package org.apache.cxf.jaxrs;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.cxf.jaxrs.impl.PathSegmentImpl;

public class Customer extends AbstractCustomer implements CustomerInfo {
    
    public static interface CustomerBeanInterface {
        
    }
    
    @XmlRootElement(name = "CustomerBean")
    public static class CustomerBean implements CustomerBeanInterface {
        private String a;
        private Long b;
        private List<String> c;
        private CustomerBean d;
        //CHECKSTYLE:OFF
        public List<CustomerBean> e;
        //CHECKSTYLE:ON
        public void setA(String aString) {
            this.a = aString;
        }
        public void setB(Long bLong) {
            this.b = bLong;
        }
        public void setC(List<String> cStringList) {
            this.c = cStringList;
        }
        public void setD(CustomerBean dCustomerBean) {
            this.d = dCustomerBean;
        }
        public String getA() {
            return a;
        }
        public Long getB() {
            return b;
        }
        public List<String> getC() {
            return c;
        }
        public CustomerBean getD() {
            return d;
        }
        
    }
    
    @Context private ContextResolver<JAXBContext> cr;
    private UriInfo uriInfo;
    @Context private HttpHeaders headers;
    @Context private Request request;
    @Context private SecurityContext sContext;
    @Context private Providers bodyWorkers;
    @Context private Application application1;
    private Application application2;
    private int applicationInjections;
    
    @Resource private HttpServletRequest servletRequest;
    @Resource private HttpServletResponse servletResponse;
    @Resource private ServletContext servletContext;
    @Context private HttpServletRequest servletRequest2;
    @Context private HttpServletResponse servletResponse2;
    @Context private ServletContext servletContext2;
    private ServletContext servletContext3;
    
    @Context private UriInfo uriInfo2;
    private String queryParam;
    
    @QueryParam("b")
    private String b;
    private String name;
    
    private boolean postConstuctCalled;
    private boolean preDestroyCalled;
    
    public Customer() {
        
    }
    
    public Customer(@Context UriInfo info) {
        uriInfo = info;
    }
    
    public Customer(@Context UriInfo info,
                    @QueryParam("a") String queryParam) {
        uriInfo = info;
        this.queryParam = queryParam;
    }
    
    @PostConstruct
    public void postConstruct() {
        postConstuctCalled = true;
    }
    
    public boolean isPostConstuctCalled() {
        return postConstuctCalled;
    }
    
    @PreDestroy
    public void preDestroy() {
        preDestroyCalled = true;
    }
    
    public boolean isPreDestroyCalled() {
        return preDestroyCalled;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String n) {
        name = n;
    }
    
    public String getB() {
        return b;
    }
    
    public void testQueryBean(@QueryParam("") CustomerBean cb) {
        
    }
    
    public void testLocaleParam(@QueryParam("p1") Locale l) {
        
    }
    
    public void testXmlAdapter(@QueryParam("a") 
                               @XmlJavaTypeAdapter(CustomerBeanAdapter.class) 
                               CustomerBean cb) {
        
    }
    
    public void testXmlAdapter2(@QueryParam("a") 
                               @XmlJavaTypeAdapter(CustomerBeanAdapter2.class) 
                               CustomerBean cb) {
        
    }
    
    public void testXmlAdapter3(@QueryParam("") 
                                @XmlJavaTypeAdapter(CustomerBeanAdapter3.class) 
                                CustomerBeanInterface cb) {
         
    }
    
    public void testPathBean(@PathParam("") CustomerBean cb) {
        
    }
    public void testFormBean(@FormParam("") CustomerBean cb) {
        
    }
    public void testMatrixBean(@MatrixParam("") CustomerBean cb) {
        
    }
    
    public Application getApplication1() {
        return application1;
    }
    
    public Application getApplication2() {
        if (applicationInjections > 1) {
            throw new RuntimeException();
        }
        return application2;
    }
    
    @Context
    public void setApplication(Application app) {
        applicationInjections++;
        application2 = app;
    }
    
    public UriInfo getUriInfo() {
        return uriInfo;
    }
    public UriInfo getUriInfo2() {
        return uriInfo2;
    }
    
    @Context
    public void setUriInfo(UriInfo ui) {
        uriInfo = ui;
    }
    
    public void setUriInfoContext(UriInfo ui) {
    }
    
    @Context
    public void setServletContext(ServletContext sc) {
        servletContext3 = sc;
    }
    
    public ServletContext getThreadLocalServletContext() {
        return servletContext3;
    }
    
    @QueryParam("a")
    public void setA(String a) {
        queryParam = a;
    }
    
    public String getQueryParam() {
        return queryParam;
    }
    
    public HttpHeaders getHeaders() {
        return headers;
    }
    
    public Request getRequest() {
        return request;
    }
    
    public Providers getBodyWorkers() {
        return bodyWorkers;
    }
    
    public SecurityContext getSecurityContext() {
        return sContext;
    }
    
    public HttpServletRequest getServletRequest() {
        return servletRequest2;
    }
    
    public HttpServletResponse getServletResponse() {
        return servletResponse2;
    }
    
    public ServletContext getServletContext() {
        return servletContext2;
    }
    
    public HttpServletRequest getServletRequestResource() {
        return servletRequest;
    }
    
    public HttpServletResponse getServletResponseResource() {
        return servletResponse;
    }
    
    public ServletContext getServletContextResource() {
        return servletContext;
    }
    
    public ContextResolver getContextResolver() {
        return cr;
    }

    @Produces("text/xml")
    @Consumes("text/xml")
    @GET
    public void test() {
        // complete
    }
    
    @Produces("text/xml")   
    public void getItAsXML() {
        // complete
    }
    @Produces("text/plain")   
    public void getItPlain() {
        // complete
    }
    
    @Produces("text/xml")   
    public void testQuery(@QueryParam("query") String queryString, 
                          @QueryParam("query") int queryInt,
                          @QueryParam("query2") String query2,
                          @QueryParam("query3") String query3) {
        // complete
    }
    
    @Produces("text/xml")   
    public void testQueryAsList(
        @DefaultValue("default") @QueryParam("query") List<String> queryString,
        @QueryParam("query2") List<String> queryString2,
        @QueryParam("query3") List<Integer> queryString3,
        @QueryParam("query3") Integer[] queryString3Array,
        @QueryParam("query4") List<String> queryString4,
        @QueryParam("query5") List<String> queryString5) {
        // complete
    }
    
    @Produces("text/xml")   
    public void testPathSegment(@PathParam("ps") PathSegment ps, 
                                @PathParam("ps") String path) {
        // complete
    }
    
    @Produces("text/xml")   
    public void testMultipleQuery(@QueryParam("query")  String queryString, 
                                  @QueryParam("query2") String queryString2,
                                  @QueryParam("query3") Long queryString3,
                                  @QueryParam("query4") boolean queryBoolean4,
                                  @QueryParam("query5") String queryString4) {
        // complete
    }
    
    @Produces("text/xml")   
    public void testMatrixParam(@MatrixParam("p1") String mp1, 
                                @MatrixParam("p2") String mp2,
                                @MatrixParam("p3") String mp3,
                                @MatrixParam("p4") String mp4,
                                @MatrixParam("p4") List<String> mp4List,
                                @MatrixParam("p5") String mp5) {
        // complete
    }
    
    public void testCustomerParam(@QueryParam("p1") Customer c, 
                                  @QueryParam("p2") Customer[] c2,
                                  @QueryParam("p3") Customer2 c3) {
        // complete
    }
    
    public void testCustomerParam2(@QueryParam("p1") String[] p) {
        // complete
    }
    
    public void testFromStringParam(@QueryParam("p1") UUID uuid,
                                    @QueryParam("p2") CustomerGender gender,
                                    @QueryParam("p3") CustomerGender gender2) {
        // complete
    }
    
    public void testFromValueParam(@QueryParam("p1") Timezone tzone) {
        // complete
    }
    
//  CHECKSTYLE:OFF
    public void testWrongType(@QueryParam("p1") HashMap map) {
        // complete
    }
//  CHECKSTYLE:ON    
    public void testWrongType2(@QueryParam("p1") CustomerGender g) {
        // complete
    }
    
    public void testFormParam(@FormParam("p1") String fp1, 
                              @FormParam("p2") List<String> fp2) {
        // complete
    }
    
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public void testMultivaluedMapAndFormParam(MultivaluedMap<String, String> params,
                                               @FormParam("p1") String fp1, 
                                               @FormParam("p2") List<String> fp2) {
        // complete
    }
    
    public void testCookieParam(@CookieParam("c1") String c1,
                                @CookieParam("c2") @DefaultValue("c2Value") String c2) {
        // complete
    }
    
    public void testMultipleCookieParam(@CookieParam("c1") String c1,
                                        @CookieParam("c2") String c2,
                                        @CookieParam("c3") Cookie c3) {
        // complete
    }
    
    public void testParams(@Context UriInfo info,
                           @Context HttpHeaders hs,
                           @Context Request r,
                           @Context SecurityContext s,
                           @Context Providers workers,
                           @HeaderParam("Foo") String h,
                           @HeaderParam("Foo") List<String> l) {
        // complete
    }
    
    public void testServletParams(@Context HttpServletRequest req,
                                  @Context HttpServletResponse res,
                                  @Context ServletContext context,
                                  @Context ServletConfig config) {
        // complete
    }
    
    @Path("{id1}/{id2}")
    public void testConversion(@PathParam("id1") PathSegmentImpl id1,
                               @PathParam("id2") SimpleFactory f) {
        // complete
    }
    
    public void testContextResolvers(@Context ContextResolver<JAXBContext> resolver) {
        // complete
    }
    
    public static class CustomerBeanAdapter extends XmlAdapter<String, CustomerBean> {

        @Override
        public CustomerBean unmarshal(String value) throws Exception {
            CustomerBean bean = new CustomerBean();
            bean.setA(value);
            return bean;
        }

        @Override
        public String marshal(CustomerBean v) throws Exception {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
    
    public static final class ABean {
        private String value;
        private ABean(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static ABean fromString(String value) {
            return new ABean(value);
        }
    }
    
    public static class CustomerBeanAdapter2 extends XmlAdapter<ABean, CustomerBean> {

        @Override
        public CustomerBean unmarshal(ABean value) throws Exception {
            CustomerBean bean = new CustomerBean();
            bean.setA(value.getValue());
            return bean;
        }

        @Override
        public ABean marshal(CustomerBean v) throws Exception {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
    
    public class CustomerBeanAdapter3 extends XmlAdapter<CustomerBean, CustomerBeanInterface> {
        public CustomerBean marshal(CustomerBeanInterface v) throws Exception {
            return null;
        }

        public CustomerBeanInterface unmarshal(CustomerBean v) throws Exception {
            return v;
        }
    }
    
};
