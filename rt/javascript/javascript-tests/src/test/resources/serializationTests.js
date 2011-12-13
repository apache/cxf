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
 
var jsutils = new CxfApacheOrgUtil();
 
function assertionFailed(explanation)
{
 	var assert = new Assert(explanation); // this will throw out in Java.
}

var bean2 = new org_apache_cxf_javascript_testns3_testBean2();
bean2.setStringItem("bleep");
 
function serializeTestBean1_1()
{
 	var bean1 = new org_apache_cxf_javascript_testns_testBean1();
 	bean1.setStringItem("bean1<stringItem");
 	bean1.setIntItem(64);
 	bean1.setLongItem(64000000);
 	bean1.setBase64Item(''); // later
 	bean1.setOptionalIntItem(101);
 	bean1.setBeanTwoItem(bean2);
 	var a = [];
 	a.push(543);
 	bean1.setOptionalIntArrayItem(a);
 	bean1.setEnum2('Mineral');
	return bean1.serialize(jsutils, "testBean1");
} 

 function serializeTestBean1_2()
 {
 	var bean1 = new org_apache_cxf_javascript_testns_testBean1();
 	bean1.setStringItem("bean1<stringItem");
 	bean1.setIntItem(64);
 	bean1.setLongItem(64000000);
 	bean1.setBase64Item(''); // later
 	bean1.setOptionalIntItem(null);
 	bean1.setBeanTwoItem(bean2);
 	var a = [];
 	a.push(543);
 	a.push(null);
 	a.push(345);
 	bean1.setOptionalIntArrayItem(a);
	return bean1.serialize(jsutils, "testBean1");
}

 function serializeTestBean1_3()
 {
 	var bean1 = new org_apache_cxf_javascript_testns_testBean1();
 	bean1.setStringItem("bean1<stringItem");
 	bean1.setIntItem(64);
 	bean1.setBase64Item(''); // later
 	bean1.setOptionalIntItem(33);
 	bean1.setOptionalIntArrayItem(null);
 	bean1.setBeanTwoItem(bean2);
 	return bean1.serialize(jsutils, "testBean1");
} 
