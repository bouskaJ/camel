package org.apache.camel.component.openstack.it.swift;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.openstack.it.AbstractITSupport;
import org.apache.camel.component.openstack.swift.SwiftConstants;

import org.junit.Assert;
import org.junit.Test;

import org.hamcrest.Matchers;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.storage.object.SwiftContainer;
import org.openstack4j.model.storage.object.options.ContainerListOptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class ContainerIT extends AbstractITSupport {

	@Test
	public void createSwiftContainer() throws IOException, InterruptedException {

		Map<String, Object> headers = new HashMap();
		headers.put(SwiftConstants.OPERATION, SwiftConstants.CREATE);

		headers.put(SwiftConstants.CONTAINER_NAME, name);

		template.requestBodyAndHeaders("direct:start", null, headers);

		await().until(new ListCallable(name), Matchers.hasItem(Matchers.<Flavor>hasProperty("name", Matchers.equalTo(name))));
	}

	@Test
	public void get() {

		osclient.objectStorage().containers().create(name);
		await().until(new ListCallable(name), Matchers.hasItem(Matchers.<Flavor>hasProperty("name", Matchers.equalTo(name))));

		Map<String, Object> headers = new HashMap();
		headers.put(SwiftConstants.OPERATION, SwiftConstants.GET);

		List<SwiftContainer> get = template.requestBodyAndHeaders("direct:start", ContainerListOptions.create().startsWith(name), headers, List.class);
		Assert.assertThat(get, Matchers.hasItem(Matchers.<Flavor>hasProperty("name", Matchers.equalTo(name))));

	}

	@Override
	protected RouteBuilder createRouteBuilder() throws Exception {
		return new RouteBuilder() {
			public void configure() {
				from("direct:start").to(String.format("openstack-swift:%s?subsystem=containers&username=%s&password=%s&project=%s",
						properties.getProperty("OPENSTACK_URI"),
						properties.getProperty("OPENSTACK_USERNAME"),
						properties.getProperty("OPENSTACK_PASSWORD"),
						properties.getProperty("PROJECT_ID")));
			}
		};
	}

	private class ListCallable implements Callable<List<SwiftContainer>> {
		private String name;

		public ListCallable(String name) {
			this.name = name;
		}

		@Override
		public List<SwiftContainer> call() throws Exception {
			return (List<SwiftContainer>) createClientForThread(osclient).objectStorage().containers().list(ContainerListOptions.create().startsWith(name));
		}
	}
}
