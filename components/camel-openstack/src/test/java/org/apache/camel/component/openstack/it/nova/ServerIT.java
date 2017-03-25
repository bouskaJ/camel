package org.apache.camel.component.openstack.it.nova;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.openstack.it.AbstractITSupport;
import org.apache.camel.component.openstack.nova.NovaConstants;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.hamcrest.Matchers;
import org.openstack4j.api.Builders;
import org.openstack4j.model.compute.Action;
import org.openstack4j.model.compute.Server;
import org.openstack4j.model.compute.ServerCreate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class ServerIT extends AbstractITSupport {

	private String imageID;
	private String flavorID;
	private List<String> networks = new ArrayList<>();

	@Before
	public void beforeTest() {
		imageID = properties.getProperty("IMAGE_ID");
		flavorID = properties.getProperty("FLAVOR_ID");
		final String networkID = properties.getProperty("NETWORK_ID");

		assertNotNull(imageID);
		assertNotNull(flavorID);
		assertNotNull(networkID);

		networks.add(networkID);
	}

	@Test
	public void createServer() throws IOException, InterruptedException {

		final ServerCreate model = Builders.server()
				.flavor(flavorID)
				.image(imageID)
				.networks(networks)
				.name(name).build();

		final Server result = template.requestBodyAndHeader("direct:start", model, NovaConstants.OPERATION, NovaConstants.CREATE, Server.class);

		Assert.assertNotNull(result.getId());

		await().until(new ListCallable(), Matchers.hasItem(Matchers.<Server>hasProperty("name", Matchers.equalTo(name))));
	}

	@Test
	public void getServer() {
		Server s = osclient.compute().servers().boot(Builders.server()
				.name(name)
				.flavor(flavorID)
				.networks(networks)
				.image(imageID).build());

		await().until(new ListCallable(), Matchers.hasItem(Matchers.allOf(Matchers.<Server>hasProperty("name", Matchers.equalTo(name)),
				Matchers.<Server>hasProperty("status", Matchers.equalTo(Server.Status.ACTIVE)))));

		Map<String, Object> headers = new HashMap<>();
		headers.put(NovaConstants.OPERATION, NovaConstants.GET);
		headers.put(NovaConstants.ID, s.getId());

		Server created = template.requestBodyAndHeaders("direct:start", null, headers, Server.class);

		assertEquals(name, created.getName());
		assertNotNull(s.getId());
	}

	@Test
	public void serverAction() throws InterruptedException {
		Server s = osclient.compute().servers().boot(Builders.server()
				.name(name)
				.flavor(flavorID)
				.networks(networks)
				.image(imageID).build());

		await().until(new ListCallable(), Matchers.hasItem(Matchers.allOf(Matchers.<Server>hasProperty("name", Matchers.equalTo(name)),
				Matchers.<Server>hasProperty("status", Matchers.equalTo(Server.Status.ACTIVE)))));

		Map<String, Object> headers = new HashMap<>();
		headers.put(NovaConstants.OPERATION, NovaConstants.ACTION);
		headers.put(NovaConstants.ACTION, Action.PAUSE);
		headers.put(NovaConstants.ID, s.getId());

		template.sendBodyAndHeaders("direct:start", null, headers);

		await().until(new ListCallable(), Matchers.hasItem(Matchers.allOf(Matchers.<Server>hasProperty("name", Matchers.equalTo(name)),
				Matchers.<Server>hasProperty("status", Matchers.equalTo(Server.Status.PAUSED)))));
	}

	@Override
	protected RouteBuilder createRouteBuilder() throws Exception {
		return new RouteBuilder() {
			public void configure() {
				from("direct:start").to(String.format("openstack-nova:%s?subsystem=servers&username=%s&password=%s&project=%s",
						properties.getProperty("OPENSTACK_URI"),
						properties.getProperty("OPENSTACK_USERNAME"),
						properties.getProperty("OPENSTACK_PASSWORD"),
						properties.getProperty("PROJECT_ID")));
			}
		};
	}

	private class ListCallable implements Callable<List<Server>> {

		@Override
		public List<Server> call() throws Exception {
			return (List<Server>) createClientForThread(osclient).compute().servers().list();
		}
	}
}
