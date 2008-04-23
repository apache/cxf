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
package org.apache.cxf.binding.http.mtom;

import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;

import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.binding.http.AbstractRestTest;
import org.apache.cxf.binding.http.HttpBindingFactory;
import org.apache.cxf.endpoint.ServerImpl;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.person.Person;
import org.apache.cxf.service.invoker.BeanInvoker;
import org.junit.Test;

public class MtomTest extends AbstractRestTest {

    @Test
    public void testService() throws Exception {
        BindingFactoryManager bfm = getBus().getExtension(BindingFactoryManager.class);
        HttpBindingFactory factory = new HttpBindingFactory();
        factory.setBus(getBus());
        bfm.registerBindingFactory(HttpBindingFactory.HTTP_BINDING_ID, factory);

        PeopleServiceImpl impl = new PeopleServiceImpl();

        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setBus(getBus());
        sf.setBindingId(HttpBindingFactory.HTTP_BINDING_ID);        
        sf.setServiceBean(impl);
        sf.getServiceFactory().setInvoker(new BeanInvoker(impl));
        sf.getServiceFactory().setWrapped(true);
        sf.setAddress("http://localhost:9001/");

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("mtom-enabled", Boolean.TRUE);
        sf.setProperties(props);

        ServerImpl svr = (ServerImpl)sf.create();

        // TEST POST/GETs

        Person p = new Person();
        p.setName("Dan");
        DataHandler handler = new DataHandler(new ByteArrayDataSource("foo".getBytes(),
                                                                      "application/octet-stream"));
        p.setPhoto(handler);
        impl.addPerson(p);

        byte[] res = doMethodBytes("http://localhost:9001/people", null, "GET", null);
        assertNotNull(res);

        // TODO: Test response
        // IOUtils.copy(new ByteArrayInputStream(res), System.out);

        String ct = "multipart/related; type=\"application/xop+xml\"; " + "start=\"<addPerson.xml>\"; "
                    + "start-info=\"text/xml; charset=utf-8\"; "
                    + "boundary=\"----=_Part_4_701508.1145579811786\"";

        res = doMethodBytes("http://localhost:9001/people", "addPerson", "POST", ct);
        assertNotNull(res);

        // TODO: Test response
        // IOUtils.copy(new ByteArrayInputStream(res), System.out);

        assertEquals(2, impl.getPeople().getPerson().size());

        svr.stop();
    }

}
