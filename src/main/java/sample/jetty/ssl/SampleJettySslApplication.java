/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.jetty.ssl;

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NegotiatingServerConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyServerCustomizer;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SampleJettySslApplication {

	private static final int PORT = 8443;

	public static void main(String[] args) throws Exception {
		SpringApplication.run(SampleJettySslApplication.class, args);
	}

	@Bean
	public EmbeddedServletContainerCustomizer servletContainerCustomizer() {
		return new EmbeddedServletContainerCustomizer() {

			@Override
			public void customize(ConfigurableEmbeddedServletContainer container) {
				if (container instanceof JettyEmbeddedServletContainerFactory) {
					configureJetty((JettyEmbeddedServletContainerFactory) container);
				}
			}

			private void configureJetty(JettyEmbeddedServletContainerFactory jettyFactory) {
				jettyFactory.addServerCustomizers(new JettyServerCustomizer() {

					private HttpConfiguration getHttpConfiguration() {
						HttpConfiguration config = new HttpConfiguration();
						config.setSecureScheme("https");
						config.setSecurePort(PORT);
						config.setSendXPoweredBy(true);
						config.setSendServerVersion(true);
						config.addCustomizer(new SecureRequestCustomizer());
						return config;
					}

					@Override
					public void customize(Server server) {
						HttpConfiguration config = getHttpConfiguration();

						HttpConnectionFactory http1 = new HttpConnectionFactory(config);
						HTTP2ServerConnectionFactory http2 = new HTTP2ServerConnectionFactory(config);

						NegotiatingServerConnectionFactory.checkProtocolNegotiationAvailable();
						ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
						alpn.setDefaultProtocol(http1.getProtocol());

						// SSL Connection Factory
						SslContextFactory sslContextFactory = new SslContextFactory();
						sslContextFactory.setKeyStoreResource(Resource.newClassPathResource("/sample.jks"));
						sslContextFactory.setKeyStorePassword("secret");
						sslContextFactory.setKeyManagerPassword("password");
						sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
						sslContextFactory.setUseCipherSuitesOrder(true);
						SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());

						ServerConnector connector = new ServerConnector(server, ssl, alpn, http2, http1);
						connector.setPort(PORT);
						server.addConnector(connector);
					}
				});
			}
		};
	}
}
