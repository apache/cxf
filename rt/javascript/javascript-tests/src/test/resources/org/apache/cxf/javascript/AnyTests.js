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

function assertionFailed(explanation)
{
 	var assert = new Assert(explanation); // this will throw out in Java.
}

var globalNotifier = null;
var globalErrorStatus = null;
var globalErrorStatusText = null;
var globalResponseObject = null;

function resetGlobals() {
	globalNotifier = null;
	globalErrorStatus = null;
	globalErrorStatusText = null;
	globalResponseObject = null;
}

function testAny1ToServerChalk(url)
{
	var service = new cxf_apache_org_jstest_any_AcceptAny();
	service.url = url;
	var param = new cxf_apache_org_jstest_types_any_acceptAny1();
	param.setBefore("before chalk");
	var anyOb = new cxf_apache_org_jstest_types_any_alts_alternative1();
	anyOb.setChalk("bismuth");
	var holder = new org_apache_cxf_any_holder("uri:cxf.apache.org:jstest:types:any:alts", "alternative1", anyOb);
	param.setAny(holder);
	param.setAfter("after chalk");
	service.acceptAny1(param);
}

function testAny1ToServerRaw(url)
{
	var service = new cxf_apache_org_jstest_any_AcceptAny();
	service.url = url;
	var param = new cxf_apache_org_jstest_types_any_acceptAny1();
	param.setBefore("before chalk");
	var holder = new org_apache_cxf_raw_any_holder("<walrus xmlns='uri:iam'>tusks</walrus>");
	param.setAny(holder);
	param.setAfter("after chalk");
	service.acceptAny1(param);
}

function testAnyNToServerRaw(url)
{
	var service = new cxf_apache_org_jstest_any_AcceptAny();
	service.url = url;
	var param = new cxf_apache_org_jstest_types_any_acceptAnyN();
	param.setBefore("before chalk");
	var holder = new org_apache_cxf_raw_any_holder("<walrus xmlns='uri:iam'>tusks</walrus><penguin xmlns='uri:linux'>emperor</penguin>");
	param.setAny(holder);
	param.setAfter("after chalk");
	service.acceptAnyN(param);
}

function errorCallback(httpStatus, httpStatusText) 
{
    org_apache_cxf_trace.trace("error");
	globalErrorStatus = httpStatus;
	globalStatusText = httpStatusText;
	globalNotifier.notify();
}

function any1ToClientSuccessCallback(responseObject) 
{
    org_apache_cxf_trace.trace("any1ToClient success");
	globalResponseObject = responseObject;
	globalNotifier.notify();
}

function testAny1ToClientChalk(url)
{
	resetGlobals();
	globalNotifier = new org_apache_cxf_notifier();
	var service = new cxf_apache_org_jstest_any_AcceptAny();
	service.url = url;
	
	var dummyParam = new cxf_apache_org_jstest_types_any_returnAny1();
	service.url = url;
	service.returnAny1(any1ToClientSuccessCallback, errorCallback, dummyParam);
	return globalNotifier; 
}
