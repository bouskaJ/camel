package org.apache.camel.component.openstack.it.neutron;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.openstack.it.AbstractITSupport;
import org.apache.camel.component.openstack.neutron.NeutronConstants;
import org.apache.camel.component.openstack.swift.SwiftConstants;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.hamcrest.Matchers;
import org.openstack4j.api.Builders;
import org.openstack4j.model.network.IPVersionType;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Subnet;
import org.openstack4j.model.storage.object.options.ContainerListOptions;
import org.openstack4j.openstack.networking.domain.NeutronPool;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

public class SubnetIT extends AbstractITSupport {


	private Network net;

	@Before
	public void bef() {
		net = osclient.networking().network().create(Builders.network().name(UUID.randomUUID().toString()).build());
	}

	@Test
	public void createSubnet() throws IOException, InterruptedException {

		Map<String, Object> headers = new HashMap();
		headers.put(SwiftConstants.OPERATION, SwiftConstants.CREATE);
		headers.put(NeutronConstants.NAME, name);
		headers.put(NeutronConstants.NETWORK_ID, net.getId());
		headers.put(NeutronConstants.IP_VERSION, IPVersionType.V4);
		headers.put(NeutronConstants.CIDR, "192.168.1.0/24");
		headers.put(NeutronConstants.SUBNET_POOL, new NeutronPool("192.168.1.2", "192.168.1.100"));

		Subnet created = template.requestBodyAndHeaders("direct:start", null, headers, Subnet.class);

		await().until(new ListCallable(), Matchers.hasItem(
				Matchers.allOf(
						Matchers.hasProperty("name", Matchers.equalTo(name)),
						Matchers.hasProperty("id", Matchers.equalTo(created.getId()))
				)));
	}


	@Test
	public void get() {
		Subnet k = osclient.networking().subnet().create(Builders.subnet().name(name).networkId(net.getId()).ipVersion(IPVersionType.V4).cidr("192.168.0.0/24").build());

		await().until(new ListCallable(), Matchers.hasItem(
				Matchers.hasProperty("name", Matchers.equalTo(name))
		));

		Map<String, Object> headers = new HashMap();
		headers.put(SwiftConstants.OPERATION, SwiftConstants.GET);
		headers.put(NeutronConstants.SUBNET_ID, k.getId());

		Subnet get = template.requestBodyAndHeaders("direct:start", ContainerListOptions.create().startsWith(name), headers, Subnet.class);
		assertEquals(name, get.getName());

		Assert.assertEquals(k.getId(),get.getId());
		assertEquals(k.getName(), get.getName());
	}

	@Override
	protected RouteBuilder createRouteBuilder() throws Exception {
		return new RouteBuilder() {
			public void configure() {
				from("direct:start").to(String.format("openstack-neutron:%s?subsystem=subnets&username=%s&password=%s&project=%s",
						properties.getProperty("OPENSTACK_URI"),
						properties.getProperty("OPENSTACK_USERNAME"),
						properties.getProperty("OPENSTACK_PASSWORD"),
						properties.getProperty("PROJECT_ID")));
			}
		};
	}

	private class ListCallable implements Callable<List<Subnet>> {

		@Override
		public List<Subnet> call() throws Exception {
			return (List<Subnet>) createClientForThread(osclient).networking().subnet().list();
		}
	}
}
