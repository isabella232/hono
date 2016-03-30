/**
 * Copyright (c) 2016 Red Hat.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hono.adapter.rest;

import static java.util.Optional.empty;
import static org.apache.camel.ExchangePattern.InOnly;
import static org.apache.camel.component.amqp.AMQPComponent.amqp10Component;
import static org.eclipse.hono.adapter.rest.RestProtocolAdapter.HONO_IN_ONLY;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PUT;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.apache.camel.EndpointInject;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

public class BodyUnawareRestProtocolAdapterTest extends CamelTestSupport {

    private static InetSocketAddress brokerAddress;
    private static BrokerService broker;

    @EndpointInject(uri = "mock:test")
    private MockEndpoint mockEndpoint;

    // Fixtures

    @BeforeClass
    public static void beforeClass() throws Exception {
        broker = new BrokerService();
        broker.setPersistent(false);
        TransportConnector connector = broker
                .addConnector("amqp://0.0.0.0:0?transport.transformer=jms&deliveryPeristent=false");
        broker.start();
        brokerAddress = connector.getServer().getSocketAddress();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (broker != null) {
            broker.stop();
        }
    }

    @Override
    protected RouteBuilder[] createRouteBuilders() throws Exception {
        String brokerUri = String.format("amqp://guest:guest@%s:%d", brokerAddress.getHostString(),
                brokerAddress.getPort());
        context.addComponent("amqp", amqp10Component(brokerUri));
        DefaultHttpRequestMapping requestMapping = new DefaultHttpRequestMapping(empty());
        RestProtocolAdapter adapter = new RestProtocolAdapter(requestMapping, 8888);

        RouteBuilder serviceRoute = new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("amqp:hello").setBody().constant("hello service");
                from("amqp:echo").log("Executing echo service.");
                from("amqp:echoheader").setBody().header("foo");

                from("amqp:telemetry").setExchangePattern(InOnly).to("mock:test");
            }
        };

        return new RouteBuilder[]{adapter, serviceRoute};
    }

    // Tests

    @Test
    public void shouldReceiveResponseFromService() throws IOException {
        String response = new RestTemplate().getForObject("http://localhost:8888/hello", String.class);
        assertThat(response, is("hello service"));
    }

    @Test
    public void shouldReceiveBodyFromService() throws IOException {
        String response = new RestTemplate().postForObject("http://localhost:8888/echo", "foo", String.class);
        assertThat(response, is("foo"));
    }

    @Test
    public void shouldReceiveHeader() throws IOException {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("foo", "bar");
        HttpEntity<?> requestEntity = new HttpEntity<>(requestHeaders);
        String response = new RestTemplate().exchange("http://localhost:8888/echoheader", GET, requestEntity, String.class).getBody();
        assertThat(response, is("bar"));
    }

    @Test
    public void shouldHaveReceivedMessage() throws Exception {
        // Given
        String json = "{\"temp\" : 15}";
        mockEndpoint.expectedBodiesReceived(json);
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(HONO_IN_ONLY, "true");
        HttpEntity<?> requestEntity = new HttpEntity<>(json, requestHeaders);

        // When
        new RestTemplate().exchange("http://localhost:8888/telemetry", PUT, requestEntity, String.class);

        // Then
        mockEndpoint.assertIsSatisfied();
    }

}
