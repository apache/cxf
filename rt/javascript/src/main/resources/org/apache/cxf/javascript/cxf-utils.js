/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

// This code is structured on to require a 'new' of an object of type
// CxfApacheOrgUtil.
// Alternative, it could be made 'static', but this allowed us to use this same
// object to carry some state.
var org_apache_cxf_XSI_namespace_uri = "http://www.w3.org/2001/XMLSchema-instance";
var org_apache_cxf_XSD_namespace_uri = "http://www.w3.org/2001/XMLSchema";

function cxf_apache_org_util_null_trace(message) {
}

function CxfApacheOrgUtil() {
	// Set up tracing if there is a trace object.
	if ("function" == typeof(org_apache_cxf_trace)) {
		this.trace = org_apache_cxf_trace.trace;
		this.trace("Javascript tracing enabled.");
	} else {
		this.trace = cxf_apache_org_util_null_trace;
	}
}

// define a constant for the DOM node type for an element.
CxfApacheOrgUtil.prototype.ELEMENT_NODE = 1;

// compensate for Microsoft's weakness here.
function org_apache_cxf_getNodeLocalName(node) {
	if ("localName" in node) {
		return node.localName;
	} else {
		return node.baseName;
	}
}

CxfApacheOrgUtil.prototype.getNodeLocalName = org_apache_cxf_getNodeLocalName;

// compensate for lack of namespace support in IE.
function org_apache_cxf_getNamespaceURI(elementNode, namespacePrefix) {
	var namespaceURI = null;
	if (elementNode.nodeType == 9)
		return null;
	else {
		namespaceURI = org_apache_cxf_findNamespace(elementNode,
				namespacePrefix);
		if (namespaceURI == null)
			namespaceURI = org_apache_cxf_getNamespaceURI(
					elementNode.parentNode, namespacePrefix);
		else
			return namespaceURI;
	}
	return namespaceURI;
}

// Search through the attributes of one node to find a namespace prefix definition.
function org_apache_cxf_findNamespace(elementNode, namespacePrefix) {
	var attributes = elementNode.attributes;
	if ((attributes != null) && (attributes.length > 0)) {
		for (var x = 0;x < attributes.length; x++) {
			var attributeNodeName = attributes.item(x).nodeName;
			var attributeNamespacePrefix = org_apache_cxf_getPrefix(attributes
					.item(x).nodeName);
			var attributeNamespaceSuffix = org_apache_cxf_getLocalName(attributes
					.item(x).nodeName);

			if ((namespacePrefix == null) && (attributeNamespacePrefix == null)
					&& (attributeNamespaceSuffix == "xmlns"))
				return attributes.item(x).nodeValue;
			else if ((attributeNamespacePrefix == "xmlns")
					&& (attributeNamespaceSuffix == namespacePrefix))
				return attributes.item(x).nodeValue;
		}
		return null;
	}
}

// Get namespace for a node.
function org_apache_cxf_get_node_namespaceURI(elementNode) {
	var prefix = org_apache_cxf_getPrefix(elementNode.nodeName);
	return org_apache_cxf_getNamespaceURI(elementNode, prefix);
}

CxfApacheOrgUtil.prototype.getElementNamespaceURI = org_apache_cxf_get_node_namespaceURI;

// Supprt functions for xsd:any start here.

// Object that can test an element against an 'any' specification.
function org_apache_cxf_any_ns_matcher(style, tns, nslist, nextLocalPart) {
	this.style = style;
	this.tns = tns;
	this.nslist = nslist;
	this.nextLocalPart = nextLocalPart;
}

org_apache_cxf_any_ns_matcher.ANY = "##any";
org_apache_cxf_any_ns_matcher.OTHER = "##other";
org_apache_cxf_any_ns_matcher.LOCAL = "##local";
org_apache_cxf_any_ns_matcher.LISTED = "listed";

function org_apache_cxf_any_ns_matcher_match(namespaceURI, localName) {
	switch (this.style) {
		// should this match local elements?
		case org_apache_cxf_any_ns_matcher.ANY :
			return true;
		case org_apache_cxf_any_ns_matcher.OTHER :
			return namespaceURI != this.tns;
		case org_apache_cxf_any_ns_matcher.LOCAL :
			return namespaceURI == null || namespaceURI == '';
		case org_apache_cxf_any_ns_matcher.LISTED :
			for (var x in this.nslist) {
				var ns = this.nslist[x];
				if (ns == "##local") {
					if ((namespaceURI == null || namespaceURI == '')
							&& (this.nextLocalPart != null && localName != this.nextLocalPart))
						return true;
				} else {
					if (ns == namespaceURI)
						return true;
				}
			}
			return false;
	}
}

org_apache_cxf_any_ns_matcher.prototype.match = org_apache_cxf_any_ns_matcher_match;

function org_apache_cxf_getPrefix(tagName) {
	var prefix;
	var prefixIndex = tagName.indexOf(":");
	if (prefixIndex == -1)
		return null;
	else
		return prefix = tagName.substring(0, prefixIndex);
}

function org_apache_cxf_getLocalName(tagName) {
	var suffix;
	var prefixIndex = tagName.indexOf(":");

	if (prefixIndex == -1)
		return tagName;
	else
		return suffix = tagName.substring(prefixIndex + 1, tagName.length);
}

function org_apache_cxf_element_name_for_trace(node) {
	if (node == null)
		return "Null";
	else if (node == undefined)
		return "Undefined";
	else {
		var n = '';
		if (node.namespaceURI != null && node.namespaceURI != '') {
			n = n + "{" + node.namespaceURI + "}";
		}
		return n + this.getNodeLocalName(node);
	}
}

CxfApacheOrgUtil.prototype.traceElementName = org_apache_cxf_element_name_for_trace;

function org_apache_cxf_escapeXmlEntities(val) {
	if (val == null || val == undefined)
		return "";
	else {
		val = String(val);
		return val.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g,
				"&gt;");
	}
}

CxfApacheOrgUtil.prototype.escapeXmlEntities = org_apache_cxf_escapeXmlEntities;

// Is an element xsi:nil? Note, in IE this requires the use of the prefix 'xsi', literally.
function org_apache_cxf_isElementNil(node) {
	if (node == null)
		throw "null node passed to isElementNil";
	// we need to look for an attribute xsi:nil, where xsi is
	// http://www.w3.org/2001/XMLSchema-instance. we have the usual
	// problem here with namespace-awareness.
	if ('function' == typeof node.getAttributeNS) {
		var nillness = node.getAttributeNS(
				"http://www.w3.org/2001/XMLSchema-instance", "nil");
		return nillness != null && nillness == "true";
	} else { // we assume the standard prefix and hope for the best.
		var nillness = node.getAttribute("xsi:nil");
		return nillness != null && nillness == "true";
	}
}

CxfApacheOrgUtil.prototype.isElementNil = org_apache_cxf_isElementNil;

function org_apache_cxf_getFirstElementChild(node) {
	if (node == undefined)
		throw "undefined node to getFirstElementChild";

	var n;
	for (n = node.firstChild;n != null && n.nodeType != this.ELEMENT_NODE; n = n.nextSibling) {
	}

	return n;
}

CxfApacheOrgUtil.prototype.getFirstElementChild = org_apache_cxf_getFirstElementChild;

function org_apache_cxf_getNextElementSibling(node) {
	if (node == undefined)
		throw "undefined node to getNextElementSibling";
	if (node == null)
		throw "null node to getNextElementSibling";
	var n;
	for (n = node.nextSibling;n != null && n.nodeType != this.ELEMENT_NODE; n = n.nextSibling);
	return n;
}

CxfApacheOrgUtil.prototype.getNextElementSibling = org_apache_cxf_getNextElementSibling;

function org_apache_cxf_isNodeNamedNS(node, namespaceURI, localName) {
	if (node == undefined)
		throw "undefined node to isNodeNamedNS";

	if (namespaceURI == '' || namespaceURI == null) {
		if (node.namespaceURI == '' || node.namespaceURI == null) {
			return localName == org_apache_cxf_getNodeLocalName(node);
		} else
			return false;
	} else {
		return namespaceURI == node.namespaceURI
				&& localName == org_apache_cxf_getNodeLocalName(node);
	}
}

CxfApacheOrgUtil.prototype.isNodeNamedNS = org_apache_cxf_isNodeNamedNS;

// Firefox splits large text regions into multiple Text objects (4096 chars in
// each). Glue it back together.
function org_apache_cxf_getNodeText(node) {
	var r = "";
	for (var x = 0;x < node.childNodes.length; x++) {
		r = r + node.childNodes[x].nodeValue;
	}
	return r;
}

CxfApacheOrgUtil.prototype.getNodeText = org_apache_cxf_getNodeText;

// This always uses soap-env, soap, and xsi as prefixes.
function org_apache_cxf_begin_soap11_message(namespaceAttributes) {
	var value = '<?xml version="1.0" encoding="UTF-8"?>'
			+ '<soap-env:Envelope xmlns:soap-env="http://schemas.xmlsoap.org/soap/envelope/"'
			+ ' xmlns:soap="http://schemas.xmlsoap.org/wsdl/soap/"'
			+ ' xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"'
			+ '><soap-env:Body ' + namespaceAttributes + '>';
	return value;
}

CxfApacheOrgUtil.prototype.beginSoap11Message = org_apache_cxf_begin_soap11_message;

function org_apache_cxf_end_soap11_message() {
	return '</soap-env:Body></soap-env:Envelope>';
}

CxfApacheOrgUtil.prototype.endSoap11Message = org_apache_cxf_end_soap11_message;

var org_apache_cxf_base64_keyStr = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

function org_apache_cxf_base64_encode64array(input) {
	var output = "";
	var chr1, chr2, chr3;
	var enc1, enc2, enc3, enc4;
	var i = 0;

	do {
		var count = 1;
		chr1 = chr2 = chr3 = 0;

		chr1 = input[i++];
		if (i < input.length) {
			chr2 = input[i++];
			count++;
		}

		if (i < input.length) {
			chr3 = input[i++];
			count++;
		}

		enc1 = chr1 >> 2;
		enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
		if (count > 1) {
			enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
			if (count > 2)
				enc4 = chr3 & 63;
			else
				enc4 = 64;
		} else
			enc3 = enc4 = 64;

		output = output + org_apache_cxf_base64_keyStr.charAt(enc1)
				+ org_apache_cxf_base64_keyStr.charAt(enc2)
				+ org_apache_cxf_base64_keyStr.charAt(enc3)
				+ org_apache_cxf_base64_keyStr.charAt(enc4);
	} while (i < input.length);

	return output;
}

function org_apache_cxf_base64_encode64Unicode(input) {
	var data = new Array(2 + (input.length * 2));
	data[0] = 0xff;
	data[1] = 0xfe;
	for (var x = 0;x < input.length; x++) {
		var c = input.charCodeAt(x);
		data[2 + (x * 2)] = c & 0xff;
		data[3 + (x * 2)] = (c >> 8) & 0xff;
	}
	return encode64array(data);
}

// we may be able to do this more cleanly with unescape( encodeURIComponent(
// input ) );
function org_apache_cxf_base64_encode64UTF8(input) {

	// determine how many bytes are needed for the complete conversion
	var bytesNeeded = 0;
	for (var i = 0;i < input.length; i++) {
		if (input.charCodeAt(i) < 0x80) {
			++bytesNeeded;
		} else if (input.charCodeAt(i) < 0x0800) {
			bytesNeeded += 2;
		} else if (input.charCodeAt(i) < 0x10000) {
			bytesNeeded += 3;
		} else {
			bytesNeeded += 4;
		}
	}

	// allocate a byte[] of the necessary size
	var data = new Array(bytesNeeded);
	// do the conversion from character code points to utf-8
	var bytes = 0;
	for (var i = 0;i < input.length; i++) {
		if (input.charCodeAt(i) < 0x80) {
			data[bytes++] = input.charCodeAt(i);
		} else if (input.charCodeAt(i) < 0x0800) {
			data[bytes++] = ((input.charCodeAt(i) >> 6) | 0xC0);
			data[bytes++] = ((input.charCodeAt(i) & 0x3F) | 0x80);
		} else if (input.charCodeAt(i) < 0x10000) {
			data[bytes++] = ((input.charCodeAt(i) >> 12) | 0xE0);
			data[bytes++] = (((input.charCodeAt(i) >> 6) & 0x3F) | 0x80);
			data[bytes++] = ((input.charCodeAt(i) & 0x3F) | 0x80);
		} else {
			data[bytes++] = ((input.charCodeAt(i) >> 18) | 0xF0);
			data[bytes++] = (((input.charCodeAt(i) >> 12) & 0x3F) | 0x80);
			data[bytes++] = (((input.charCodeAt(i) >> 6) & 0x3F) | 0x80);
			data[bytes++] = ((input.charCodeAt(i) & 0x3F) | 0x80);
		}
	}
	return encode64array(data);
}

function org_apache_cxf_base64_decode64array(input) {
	var output = new Array();
	var chr1, chr2, chr3;
	var enc1, enc2, enc3, enc4;
	var i = 0;

	// remove all characters that are not A-Z, a-z, 0-9, +, /, or =
	input = input.replace(/[^A-Za-z0-9\+\/\=]/g, "");

	do {
		enc1 = org_apache_cxf_base64_keyStr.indexOf(input.charAt(i++));
		enc2 = org_apache_cxf_base64_keyStr.indexOf(input.charAt(i++));
		enc3 = org_apache_cxf_base64_keyStr.indexOf(input.charAt(i++));
		enc4 = org_apache_cxf_base64_keyStr.indexOf(input.charAt(i++));

		chr1 = (enc1 << 2) | (enc2 >> 4);
		chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
		chr3 = ((enc3 & 3) << 6) | enc4;

		output[output.length] = chr1;

		if (enc3 != 64) {
			output[output.length] = chr2;
		}
		if (enc4 != 64) {
			output[output.length] = chr3;
		}
	} while (i < input.length);

	return output;
}

var org_apache_cxf_base64_hD = "0123456789ABCDEF";
function org_apache_cxf_base64_d2h(d) {
	var h = org_apache_cxf_base64_hD.substr(d & 15, 1);
	while (d > 15) {
		d >>= 4;
		h = org_apache_cxf_base64_hD.substr(d & 15, 1) + h;
	}
	return h;
}

function org_apache_cxf_base64_decode64Unicode(input) {
	var bytes = org_apache_cxf_base64_decode64array(input);
	var swap;
	var output = "";
	if (bytes[0] == 0xff && bytes[1] == 0xfe) {
		swap = true;
	} else if (bytes[0] == 0xfe && bytes[1] == 0xff) {
		swap = false;
	} else {
		confirm("Problem with decoding utf-16");
	}
	for (var x = 2;x < bytes.length; x = x + 2) {
		var c;
		if (swap)
			c = (bytes[x + 1] << 8) | bytes[x];
		else
			c = (bytes[x] << 8) | bytes[x + 1];

		output = output + String.fromCharCode(c);
	}
	return output;
}

// we may be able to do this more cleanly with decodeURIComponent( escape( input
// ) );
function org_apache_cxf_base64_decode64UTF8(input) {
	var utftext = org_apache_cxf_base64_decode64array(input);
	var plaintext = "";
	var cRay = new Array();
	var i = 0;
	var c;
	var c2;
	var c3;
	while (i < utftext.length) {
		c = utftext[i];
		if (c < 128) {
			plaintext += String.fromCharCode(c);
			i++;
		} else if ((c > 191) && (c < 224)) {
			c2 = utftext[i + 1];
			plaintext += String.fromCharCode(((c & 31) << 6) | (c2 & 63));
			i += 2;
		} else {
			c2 = utftext[i + 1];
			c3 = utftext[i + 2];
			plaintext += String.fromCharCode(((c & 15) << 12)
					| ((c2 & 63) << 6) | (c3 & 63));
			i += 3;
		}
	}
	return plaintext;
}

// MTOM deserialization.
// This assumes that the only content type it will be asked to deal with is text/plain;charset=utf-8.
// This only handles cid: xop URNs.

var org_apache_cxf_XOP_NS = 'http://www.w3.org/2004/08/xop/include';

function org_apache_cxf_deserialize_MTOM_or_base64(element) {
	var elementChild = this.getFirstElementChild(element);
	if (elementChild == null) { // no MTOM, assume base64
		var base64Text = this.getNodeText(element);
		// we assume this is text/plain;charset=utf-8. We could check for the
		// xmime attribute.
		return org_apache_cxf_base64_decode64UTF8(base64Text);
	}
	// 
	if (!org_apache_cxf_isNodeNamedNS(elementChild, org_apache_cxf_XOP_NS, 'Include')) {
		this.trace('Invalid child element of base64 element');
		return ''; // we don't knoww what this is, so we throw it out. We could
					// throw.
	}
	var href = elementChild.getAttribute('href');
	if(!href) {
		this.trace('missing href for xop:Include');
		return ''; // we don't knoww what this is, so we throw it out. We could
					// throw.
	}
	// we only support cid:, not URLs.
	if(href.length < 4 || href.substr(0, 4) != 'cid:') {
		this.trace('Non-cid href in xop:Include: ' + href);
		return ''; 
	}
	var cid = href.substr(4);
	var partobject = this.client.parts[cid];
	if(!partobject) {
		this.trace('xop:Include href points to missing attachment: ' + href);
		return ''; 
	}
	// success.
	return partobject.data;
}

CxfApacheOrgUtil.prototype.deserializeBase64orMom = org_apache_cxf_deserialize_MTOM_or_base64;

/*
 * Client object sends requests and calls back with responses.
 */
function CxfApacheOrgClient(utils) {
	utils.trace("Client constructor");
	this.utils = utils;
	utils.client = this; // we aren't worried about multithreading!
	this.mtomparts = [];
	this.soapAction = "";
	this.messageType = "CALL";
	// handler functions
	this.onsuccess = null;
	this.onerror = null;
	// Firefox is noncompliant with respect to the defined constants,
	// so we define our own.
	this.READY_STATE_UNINITIALIZED = 0;
	this.READY_STATE_LOADING = 1;
	this.READY_STATE_LOADED = 2;
	this.READY_STATE_INTERACTIVE = 3;
	this.READY_STATE_DONE = 4;
}

var org_apache_cxf_pad_string_PAD_LEFT = 0;
var org_apache_cxf_pad_string_PAD_RIGHT = 1;
var org_apache_cxf_pad_string_PAD_BOTH = 2;

function org_apache_cxf_pad_string(string, len, pad, type) {
	var append = new String();

	len = isNaN(len) ? 0 : len - string.length;
	pad = typeof(pad) == 'string' ? pad : ' ';

	if (type == org_apache_cxf_pad_string_PAD_BOTH) {
		string = org_apache_cxf_pad_sring(Math.floor(len / 2) + string.length,
				pad, org_apache_cxf_pad_string_PAD_LEFT);
		return (org_apache_cxf_pad_string(Math.ceil(len / 2) + string.length,
				pad, org_apache_cxf_pad_string_PAD_RIGHT));
	}

	while ((len -= pad.length) > 0)
		append += pad;
	append += pad.substr(0, len + pad.length);

	return (type == org_apache_cxf_pad_string_PAD_LEFT
			? append.concat(string)
			: string.concat(append));
}

/*
 * Generate a uniformly distributed random integer within the range <min> ..
 * <max>. (min) - Lower limit: random >= min (default: 0) (max) - Upper limit:
 * random <= max (default: 1)
 */
function org_apache_cxf_random_int(min, max) {
	if (!isFinite(min))
		min = 0;
	if (!isFinite(max))
		max = 1;
	return Math.floor((Math.random() % 1) * (max - min + 1) + min);
}

function org_apache_cxf_random_hex_string(len) {
	var random = org_apache_cxf_random_int(0, Math.pow(16, len) - 1);
	return org_apache_cxf_pad_string(random.toString(16), len, '0',
			org_apache_cxf_pad_string_PAD_LEFT);
}

function org_apache_cxf_make_uuid(type) {
	switch ((type || 'v4').toUpperCase()) {
		// Version 4 UUID (Section 4.4 of RFC 4122)
		case 'V4' :
			var tl = org_apache_cxf_random_hex_string(8);
			// time_low
			var tm = org_apache_cxf_random_hex_string(4);
			// time_mid
			var thav = '4' + org_apache_cxf_random_hex_string(3);
			// time_hi_and_version
			var cshar = org_apache_cxf_random_int(0, 0xFF);
			// clock_seq_hi_and_reserved
			cshar = ((cshar & ~(1 << 6)) | (1 << 7)).toString(16);
			var csl = org_apache_cxf_random_hex_string(2);
			// clock_seq_low
			var n = org_apache_cxf_random_hex_string(12);
			// node

			return (tl + '-' + tm + '-' + thav + '-' + cshar + csl + '-' + n);

			// Nil UUID (Section 4.1.7 of RFC 4122)
		case 'NIL' :
			return '00000000-0000-0000-0000-000000000000';
	}
	return null;
}

//
// Returns XMLHttpRequest object.
//
var ORG_APACHE_CXF_XMLHTTPREQUEST_MS_PROGIDS = new Array(
    "Msxml2.XMLHTTP.7.0",
    "Msxml2.XMLHTTP.6.0",
    "Msxml2.XMLHTTP.5.0",
    "Msxml2.XMLHTTP.4.0",
    "MSXML2.XMLHTTP.3.0",
    "MSXML2.XMLHTTP",
    "Microsoft.XMLHTTP"
    );    

function org_apache_cxf_getXMLHttpRequest()
{
    var httpRequest = null;
 
    // Create the appropriate HttpRequest object for the browser.
    try {
        httpRequest = new XMLHttpRequest();
        return httpRequest;
    } catch(ex) {
    }
    
    if (window.ActiveXObject != null) {
        // Must be IE, find the right ActiveXObject.
   
        var success = false;
        //
        // Define a list of Microsoft XML HTTP ProgIDs.
        //
        for (var i = 0;
             i < ORG_APACHE_CXF_XMLHTTPREQUEST_MS_PROGIDS.length && !success;
             i++)
        {
            try
            {
                httpRequest = new ActiveXObject(ORG_APACHE_CXF_XMLHTTPREQUEST_MS_PROGIDS[i]);
                success = true;
            }
            catch (ex)
            {
                // no reason to log unless we come up empty.
            }
        }
        if(!success) {
            this.utils.trace("Unable to get any Microsoft XML HttpRequest object.");
            throw "org_apache_cxf no Microsoft XMLHttpRequest";
        }
    }
    // Return it.
    return httpRequest;
}

CxfApacheOrgClient.prototype.getXMLHttpRequest = org_apache_cxf_getXMLHttpRequest;

var ORG_APACHE_CXF_MTOM_REQUEST_HEADER = 'Content-Type: application/xop+xml; type="text/xml"; charset=utf-8\r\n';

// Caller must avoid stupid mistakes like 'GET' with a request body.
// This does not support attempts to cross-script.
// This imposes a relatively straightforward set of HTTP options.
function org_apache_cxf_client_request(url, requestXML, method, sync, headers) 
{
	this.utils.trace("request " + url);

	this.url = url;
	this.sync = sync;

	this.req = null;

	if (method) {
		this.method = method;
	} else {
		if (requestXML)
			this.method = "POST";
		else
			this.method = "GET";
	}

	try {
		this.req = this.getXMLHttpRequest();
	} catch (err) {
		this.utils.trace("Error creating XMLHttpRequest: " + err);
		this.req = null;
	}

	if (this.req == null) {
		this.utils.trace("Unable to create request object.");
		throw "ORG_APACHE_CXF_NO_REQUEST_OBJECT";
	}

	this.utils.trace("about to open " + this.method + " " + this.url);
	this.req.open(this.method, this.url, !this.sync);

	var mimeBoundary;

	// we can't do binary MTOM, but we can do 'text/plain' !
	if (this.mtomparts.length > 0) {
		var uuid = org_apache_cxf_make_uuid('v4');
		mimeBoundary = '@_bOuNDaRy_' + uuid;
		var ctHeader = 'multipart/related; start-info="text/xml"; type="application/xop+xml"; boundary="'
				+ mimeBoundary + '"';
		this.req.setRequestHeader("Content-Type", ctHeader);

	} else {
	// for now, assume SOAP 1.1. 1.2 calls for application/xml. 
		this.req.setRequestHeader("Content-Type", "text/xml");
	}

	if (headers) { // must be array indexed by header field.
		for (var h in headers) {
			this.req.setRequestHeader(h, headers[h]);
		}
	}

	this.req.setRequestHeader("SOAPAction", this.soapAction);
	this.req.setRequestHeader("MessageType", this.messageType);

	var requester = this; /* setup a closure */

	this.req.onreadystatechange = function() {
		requester.onReadyState();
	}

	// NOTE: we do not call the onerror callback for a synchronous error
	// at request time. We let the request object throw as it will.
	// onError will only be called for asynchronous errors.
	this.utils.trace("about to send data" + this.method + " " + this.url);
	var dataToSend;
	if (this.mtomparts.length == 0) {
		dataToSend = requestXML;
	} else {
		dataToSend = "--" + mimeBoundary + "\r\n";
		dataToSend = dataToSend + ORG_APACHE_CXF_MTOM_REQUEST_HEADER + "\r\n";
		dataToSend = dataToSend + requestXML;
		for (var bx in this.mtomparts) {
			var part = this.mtomparts[bx];
			dataToSend += "\r\n\r\n--" + mimeBoundary + "\r\n";
			dataToSend += part;
		}
		dataToSend += "--" + mimeBoundary + "--\r\n";
	}

	this.req.send(dataToSend);
}

CxfApacheOrgClient.prototype.request = org_apache_cxf_client_request;

function org_apache_cxf_trim_string(str) {
	return str.replace(/^\s+|\s+$/g, '');
}

// this gets an array of a=b strings, and produces a dictionary of x[a]=b;
function org_apache_cxf_parse_mime_keyword_value_pairs(strings) {
	var result = [];
	for (var x = 1;x < strings.length; x = x + 1) {
		var str = strings[x];
		var valequal = str.indexOf("=");
		if (valequal != -1) {
			var k = str.substr(0, valequal);
			var v = str.substr(valequal + 1);
			v = org_apache_cxf_trim_string(v);
			if (v.charAt(0) == '"') {
				v = v.substr(1, v.length - 2);
			}
			if (v.charAt(0) == "'") {
				v = v.substr(1, v.length - 2);
			}

			result[org_apache_cxf_trim_string(k.toLowerCase())] = v;
		}
	}
	return result;
}

function org_apache_cxf_regexp_escape(text) {
	if (!arguments.callee.sRE) {
		var specials = ['/', '.', '*', '+', '?', '|', '(', ')', '[', ']', '{',
				'}', '\\'];
		arguments.callee.sRE = new RegExp('(\\' + specials.join('|\\') + ')',
				'g');
	}
	return text.replace(arguments.callee.sRE, '\\$1');
}

// Called when we don't have response XML.
// returns true if we have multipart-related, false if we don't or can't parse
// it.
function org_apache_cxf_parse_multipart_related() {
	var contentType = this.req.getResponseHeader("content-type");
	if (!contentType)
		return false; // not bloody likely.
	var ctPart = contentType.split(/\s*;\s*/);
	var ctMain = ctPart[0].toLowerCase();
	if (ctMain != "multipart/related")
		return false;
	// now we have keyword-value pairs.
	var params = org_apache_cxf_parse_mime_keyword_value_pairs(ctPart);
	// there is a lot of noise we don't care about. all we really want is the
	// boundary.
	var boundary = params['boundary'];
	if (!boundary)
		return false;
	boundary = "--" + boundary; // the annoying 'extra-dash' convention.
	// var boundarySplitter = org_apache_cxf_regexp_escape(boundary);
	var text = this.req.responseText;
	// we are willing to use a lot of memory here.
	var parts = text.split(boundary);
	// now we have the parts.
	// now we have to pull headers off the parts.
	this.parts = [];
	// the first one is noise due to the initial boundary. The last will just be
	// -- due to MIME.
	for (var px = 1;px < parts.length - 1; px++) {
		var seenOneHeader = false;
		var x = 0; // misc index.
		var parttext = parts[px];
		var headers = [];
		nextHeaderLine : for (var endX = parttext.indexOf('\r', x);endX != -1; x = endX
				+ 1, endX = parttext.indexOf('\r', x)) {
			var headerLine = parttext.slice(x, endX);
			if (headerLine == "") {
				if (parttext.charAt(endX + 1) == '\n')
					endX++;
				if (seenOneHeader) {
					break nextHeaderLine;
				} else {
					continue nextHeaderLine;
				}
			}
			seenOneHeader = true;

			var colonIndex = headerLine.indexOf(":");
            var headerName = headerLine.slice(0, colonIndex).toLowerCase();
            var headerValue = headerLine.substr(colonIndex+1);
			headers[headerName] = org_apache_cxf_trim_string(headerValue);

			if (parttext.charAt(endX + 1) == '\n')
				endX++;
		}

		// Now, see about the mime type (if any) and the ID.
		var thispart = new Object(); // a constructor seems excessive.
		// at exit, x indicates the start of the blank line.
		if (parttext.charAt(x + 1) == '\n')
			x = x + 1;
		thispart.data = parttext.substr(x);
		thispart.contentType = headers['content-type'];
		if (px > 1) {
			var cid = headers['content-id'];
			// take of < and >
			cid = cid.substr(1, cid.length - 2);
			thispart.cid = cid;
			this.parts[cid] = thispart;
		} else {
			// the first part.
			var doc;
			if (window.ActiveXObject) {
				doc = new ActiveXObject("Microsoft.XMLDOM");
				doc.async = "false";
				doc.loadXML(thispart.data);
			} else {
				var parser = new DOMParser();
				doc = parser.parseFromString(thispart.data, "text/xml");
			}
			this.mpResponseXML = doc;
		}
	}
	return true;

}

CxfApacheOrgClient.prototype.parseMultipartRelated = org_apache_cxf_parse_multipart_related;

function org_apache_cxf_client_onReadyState() {
	var req = this.req;
	var ready = req.readyState;

	this.utils.trace("onreadystatechange " + ready);

	if (ready == this.READY_STATE_DONE) {
		var httpStatus;
		try {
			httpStatus = req.status;
		} catch (e) {
			// Firefox throws when there was an error here.
			this.utils
					.trace("onreadystatechange DONE ERROR retrieving status (connection error?)");
			if (this.onerror != null) {
				this.onerror(e);
			}
			return;

		}

		this.utils.trace("onreadystatechange DONE " + httpStatus);

		if (httpStatus == 200 || httpStatus == 0) {
			if (this.onsuccess != null) {
				// the onSuccess function is generated, and picks apart the
				// response.
				if (!req.responseXML) {
					if (this.parseMultipartRelated()) {
						this.onsuccess(this, this.mpResponseXML);
						return;
					}
					if (this.onerror != null) {
						this.onerror("Could not handle content of response.");
						return;
					}
				}
				this.onsuccess(this, req.responseXML);
			}
		} else {
			this.utils.trace("onreadystatechange DONE ERROR "
					+ req.getAllResponseHeaders() + " " + req.statusText + " "
					+ req.responseText);
			if (this.onerror != null)
				this.onerror(this);
		}
	}
}

CxfApacheOrgClient.prototype.onReadyState = org_apache_cxf_client_onReadyState;

function org_apache_cxf_package_mtom(value) {
	var uuid = org_apache_cxf_make_uuid('v4');
	var placeholder = '<xop:Include xmlns:xop="http://www.w3.org/2004/08/xop/include" '
			+ 'href="cid:' + uuid + '" />';
	var mtomObject = 'Content-Type: text/plain; charset="utf-8";\r\nContent-ID: <'
			+ uuid + '>\r\n\r\n' + value + '\r\n';
	this.client.mtomparts.push(mtomObject);
	return placeholder;
}

CxfApacheOrgUtil.prototype.packageMtom = org_apache_cxf_package_mtom;

// Holder object used for xs:any
// The namespaceURI and localName identify the global element from the schema.
// The object to go with it goes into object.
// If the Any is an array, put the array into the object slot.

function org_apache_cxf_any_holder(namespaceURI, localName, object) {
	this.typeMarker = "org_apache_cxf_any_holder";
	this.namespaceURI = namespaceURI;
	this.localName = localName;
	this.qname = "{" + namespaceURI + "}" + localName;
	this.object = object;
	this.raw = false;
}

// the following will simply dump the supplied XML into the message.
function org_apache_cxf_raw_any_holder(xml) {
	this.typeMarker = "org_apache_cxf_raw_any_holder";
	this.xml = xml;
	this.raw = true;
	this.xsiType = false;
}

// The following will get an xsi:type attribute in addition to dumping the XML
// into
// the message.
function org_apache_cxf_raw_typed_any_holder(namespaceURI, localName, xml) {
	this.typeMarker = "org_apache_cxf_raw_any_holder";
	this.namespaceURI = namespaceURI;
	this.localName = localName;
	this.xml = xml;
	this.raw = true;
	this.xsiType = true;
}

function org_apache_cxf_get_xsi_type(elementNode) {
	var attributes = elementNode.attributes;
	if ((attributes != null) && (attributes.length > 0)) {
		for (var x = 0;x < attributes.length; x++) {
			var attributeNodeName = attributes.item(x).nodeName;
			var attributeNamespacePrefix = org_apache_cxf_getPrefix(attributes
					.item(x).nodeName);
			var attributeNamespaceSuffix = org_apache_cxf_getLocalName(attributes
					.item(x).nodeName);
			if (attributeNamespaceSuffix == 'type') {
				// perhaps this is ours
				var ns = org_apache_cxf_getNamespaceURI(elementNode,
						attributeNamespacePrefix);
				if (ns == org_apache_cxf_XSI_namespace_uri) {
					return attributes.item(x).nodeValue;
				}
			}
		}
		return null;
	}
}

// Return an object if we can deserialize an object, otherwise return the
// element itself.
function org_apache_cxf_deserialize_anyType(cxfjsutils, element) {
	var type = org_apache_cxf_get_xsi_type(element);
	if (type != null) {
		// type is a :-qualified name.
		var namespacePrefix = org_apache_cxf_getPrefix(type);
		var localName = org_apache_cxf_getLocalName(type);
		var uri = org_apache_cxf_getNamespaceURI(element, namespacePrefix);
		if (uri == org_apache_cxf_XSD_namespace_uri) {
			// we expect a Text node below
			var textNode = element.firstChild;
			if (textNode == null)
				return null;
			var text = textNode.nodeValue;
			if (text == null)
				return null;
			// For any of the basic types, assume that the nodeValue is what the
			// doctor ordered,
			// converted to the appropriate type.
			// For some of the more interesting types this needs more work.
			if (localName == "int" || localName == "unsignedInt"
					|| localName == "long" || localName == "unsignedLong") {
				return parseInt(text);
			}
			if (localName == "float" || localName == "double")
				return parseFloat(text);
			if (localName == "boolean")
				return text == 'true';
			return text;
		}
		var qname = "{" + uri + "}" + localName;
		var deserializer = cxfjsutils.interfaceObject.globalElementDeserializers[qname];
		if (deserializer != null) {
			return deserializer(cxfjsutils, element);
		}
	}
	return element;
}
