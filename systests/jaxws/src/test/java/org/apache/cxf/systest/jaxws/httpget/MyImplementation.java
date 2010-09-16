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
package org.apache.cxf.systest.jaxws.httpget;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javax.jws.WebParam;


public class MyImplementation implements MyInterface {
    public Date test1() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return sdf.parse("1973-10-20");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public String test2(Date date) {
        return date.toString();
    }

    public MyEnum test3() {
        return MyEnum.A;
    }

    public String test4(@WebParam(name = "myEnum")MyEnum myEnum) {
        return myEnum.toString();
    }

    public Calendar test5() {
        Calendar c = Calendar.getInstance();

        c.clear();
        c.set(23, 1, 3);

        return c;
    }

    public String test6(Calendar calendar) {
        System.out.println("" + calendar.getTime());
        return calendar.getTime().toString();
    }

    public String test7(@WebParam(name = "d")Double d) {
        System.out.println("d is " + d);
        return d == null ? "<null>" : d.toString();
    }

    public String test8(@WebParam(name = "d")double d) {
        return "" + d;        
    }


}