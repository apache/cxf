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

importPackage(Packages.java.io);
importPackage(Packages.javax.xml.namespace);
importPackage(Packages.org.apache.hello_world_soap_http);

var qname=new Packages.javax.xml.namespace.QName("http://apache.org/hello_world_soap_http", "SOAPService");
var curpath=new File(".");
var sepa=File.separator;
var hwpath=curpath.getAbsolutePath()+sepa+"wsdl"+sepa+"hello_world.wsdl";
var url = new File(hwpath).toURL();
var ss=new SOAPService(url,qname);
var port = ss.getSoapPort();
var resp=port.sayHi();
print("invoke sayHi().   return " + resp);
resp=port.greetMe("Jeff");
print("invoke greetMe(String).   return " + resp);
