package org.apache.camel.component.openstack.it.neutron;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.openstack.it.AbstractITSupport;
import org.apache.camel.component.openstack.neutron.NeutronConstants;
import org.apache.camel.component.openstack.swift.SwiftConstants;

import org.junit.Before;
import org.junit.Test;

import org.hamcrest.Matchers;
import org.openstack4j.api.Builders;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.storage.object.options.ContainerListOptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

public class PortIT extends AbstractITSupport {

	private Network net;

	@Before
	public void beforeTest() {
		net = osclient.networking().network().create(Builders.network().name(UUID.randomUUID().toString()).build());
		assertNotNull(net);
	}

	@Test
	public void createPort() throws IOException, InterruptedException {

		Map<String, Object> headers = new HashMap();
		headers.put(SwiftConstants.OPERATION, SwiftConstants.CREATE);
		headers.put(NeutronConstants.NETWORK_ID, net.getId());
		headers.put(NeutronConstants.NAME, name);

		Port created = template.requestBodyAndHeaders("direct:start", null, headers, Port.class);

		await().until(new ListCallable(), Matchers.hasItem(
				Matchers.allOf(
						Matchers.hasProperty("name", Matchers.equalTo(name)),
						Matchers.hasProperty("id", Matchers.equalTo(created.getId()))
				)));
	}


	@Test
	public void get() {
		Port p = osclient.networking().port().create(Builders.port().name(name).networkId(net.getId()).build());

		await().until(new ListCallable(), Matchers.hasItem(
				Matchers.hasProperty("name", Matchers.equalTo(name))
		));

		Map<String, Object> headers = new HashMap();
		headers.put(SwiftConstants.OPERATION, SwiftConstants.GET);
		headers.put(NeutronConstants.PORT_ID, p.getId());


		Port get = template.requestBodyAndHeaders("direct:start", ContainerListOptions.create().startsWith(name), headers, Port.class);
		assertEquals(name, get.getName());
	}

	@Override
	protected RouteBuilder createRouteBuilder() throws Exception {
		return new RouteBuilder() {
			public void configure() {
				from("direct:start").to(String.format("openstack-neutron:%s?subsystem=ports&username=%s&password=%s&project=%s",
						properties.getProperty("OPENSTACK_URI"),
						properties.getProperty("OPENSTACK_USERNAME"),
						properties.getProperty("OPENSTACK_PASSWORD"),
						properties.getProperty("PROJECT_ID")));
			}
		};
	}

	private class ListCallable implements Callable<List<Port>> {

		@Override
		public List<Port> call() throws Exception {
			return (List<Port>) createClientForThread(osclient).networking().port().list();
		}
	}
}
