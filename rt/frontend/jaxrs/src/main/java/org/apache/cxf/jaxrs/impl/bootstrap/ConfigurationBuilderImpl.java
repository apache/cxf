package org.apache.cxf.jaxrs.impl.bootstrap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

import javax.net.ssl.SSLContext;

import jakarta.ws.rs.SeBootstrap;
import jakarta.ws.rs.SeBootstrap.Configuration.SSLClientAuthentication;

public class ConfigurationBuilderImpl implements SeBootstrap.Configuration.Builder {

	private final Map<String, Object> properties = new HashMap<>();

	@Override
	public SeBootstrap.Configuration build() {
		return new ConfigurationImpl(properties);
	}

	@Override
	public SeBootstrap.Configuration.Builder property(String name, Object value) {
		properties.put(name, value);
		return this;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public <T> SeBootstrap.Configuration.Builder from(BiFunction<String, Class<T>, Optional<T>> propertiesProvider) {
		final BiFunction<String, Class<String>, Optional<String>> stringPropertiesProvider = (BiFunction) propertiesProvider;
		final BiFunction<String, Class<Integer>, Optional<Integer>> integerPropertiesProvider = (BiFunction) propertiesProvider;
		final BiFunction<String, Class<SSLContext>, Optional<SSLContext>> sslContextPropertiesProvider = (BiFunction) propertiesProvider;
		final BiFunction<String, Class<SSLClientAuthentication>, Optional<SSLClientAuthentication>> sslClientAuthenticationPropertiesProvider = (BiFunction) propertiesProvider;

		stringPropertiesProvider.apply(SeBootstrap.Configuration.PROTOCOL, String.class).ifPresent(this::protocol);
		stringPropertiesProvider.apply(SeBootstrap.Configuration.HOST, String.class).ifPresent(this::host);
		integerPropertiesProvider.apply(SeBootstrap.Configuration.PORT, Integer.class).ifPresent(this::port);
		stringPropertiesProvider.apply(SeBootstrap.Configuration.ROOT_PATH, String.class).ifPresent(this::rootPath);
		sslContextPropertiesProvider.apply(SeBootstrap.Configuration.SSL_CONTEXT, SSLContext.class).ifPresent(this::sslContext);
		sslClientAuthenticationPropertiesProvider.apply(SeBootstrap.Configuration.SSL_CLIENT_AUTHENTICATION, SSLClientAuthentication.class).ifPresent(this::sslClientAuthentication);

		return this;
	}
	
}
