package org.apache.cxf.jaxrs.impl.bootstrap;

import java.util.Map;

import jakarta.ws.rs.SeBootstrap;

public class ConfigurationImpl implements SeBootstrap.Configuration {

	private final Map<String, Object> properties;

	ConfigurationImpl(Map<String, Object> properties) {
		this.properties = properties;
	}

	@Override
	public Object property(String name) {
		return properties.get(name);
	}
	
}
