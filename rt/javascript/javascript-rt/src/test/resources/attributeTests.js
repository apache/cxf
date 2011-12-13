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

function parseXml(xmlString) 
{
	var parser = new DOMParser();
	return parser.parseFromString(xmlString, "text/xml").documentElement;
}

function deserializeAttributeTestBean(xmlString)
{
	var dom = parseXml(xmlString);
	var bean = org_apache_cxf_javascript_testns_attributeTestBean_deserialize(jsutils, dom);
	if(bean.getElement1() != "e1")
		assertionFailed("element1 " + bean.getElement1());
}


