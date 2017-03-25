package org.apache.camel.component.openstack.it.neutron;

import static org.awaitility.Awaitility.await;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.openstack.it.AbstractITSupport;
import org.apache.camel.component.openstack.neutron.NeutronConstants;
import org.apache.camel.component.openstack.swift.SwiftConstants;

import org.junit.Test;

import org.hamcrest.Matchers;
import org.openstack4j.api.Builders;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.storage.object.options.ContainerListOptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class RouterIT extends AbstractITSupport {

	@Test
	public void createRouter() throws IOException, InterruptedException {

		Map<String, Object> headers = new HashMap();
		headers.put(SwiftConstants.OPERATION, SwiftConstants.CREATE);
		headers.put(NeutronConstants.NAME, name);

		Router created = template.requestBodyAndHeaders("direct:start", null, headers, Router.class);

		await().until(new RouterIT.ListCallable(), Matchers.hasItem(
				Matchers.allOf(
						Matchers.hasProperty("name", Matchers.equalTo(name)),
						Matchers.hasProperty("id", Matchers.equalTo(created.getId()))
				)));
	}


	@Test
	public void get() {
		Router p = osclient.networking().router().create(Builders.router().name(name).build());

		await().until(new RouterIT.ListCallable(), Matchers.hasItem(
				Matchers.hasProperty("name", Matchers.equalTo(name))
		));

		Map<String, Object> headers = new HashMap();
		headers.put(SwiftConstants.OPERATION, SwiftConstants.GET);
		headers.put(NeutronConstants.ID, p.getId());


		Router get = template.requestBodyAndHeaders("direct:start", ContainerListOptions.create().startsWith(name), headers, Router.class);
		assertEquals(name, get.getName());
	}

	@Override
	protected RouteBuilder createRouteBuilder() throws Exception {
		return new RouteBuilder() {
			public void configure() {
				from("direct:start").to(String.format("openstack-neutron:%s?subsystem=routers&username=%s&password=%s&project=%s",
						properties.getProperty("OPENSTACK_URI"),
						properties.getProperty("OPENSTACK_USERNAME"),
						properties.getProperty("OPENSTACK_PASSWORD"),
						properties.getProperty("PROJECT_ID")));
			}
		};
	}

	private class ListCallable implements Callable<List<Router>> {

		@Override
		public List<Router> call() throws Exception {
			return (List<Router>) createClientForThread(osclient).networking().router().list();
		}
	}
}
