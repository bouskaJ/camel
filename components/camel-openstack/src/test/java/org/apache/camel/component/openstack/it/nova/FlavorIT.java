package org.apache.camel.component.openstack.it.nova;

import static org.awaitility.Awaitility.to;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.openstack.it.AbstractITSupport;
import org.apache.camel.component.openstack.nova.NovaConstants;

import org.junit.Assert;
import org.junit.Test;

import org.hamcrest.Matchers;
import org.openstack4j.api.Builders;
import org.openstack4j.model.compute.Flavor;
import org.openstack4j.model.compute.builder.FlavorBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

public class FlavorIT extends AbstractITSupport {

	@Test
	public void createFlavor() throws IOException, InterruptedException {

		final Flavor flavor = Builders.flavor()
				.name(name.toString())
				.vcpus(2)
				.ram(3).build();

		Flavor created = template.requestBodyAndHeader("direct:flavors", flavor, NovaConstants.OPERATION, NovaConstants.CREATE, Flavor.class);

		Assert.assertEquals(name, created.getName());
		Assert.assertNotNull(created.getId());

		await().until(new FlavorListCallable(), Matchers.hasItem(Matchers.<Flavor>hasProperty("name", Matchers.equalTo(name))));
	}

	@Test
	public void getFlavor() {


		//create flavor
		final org.openstack4j.model.compute.Flavor flavor = osclient.compute().flavors().create(
				Builders.flavor().name(name)
						.ram(4096)
						.vcpus(6)
						.disk(120)
						.build());

		await().until(new FlavorListCallable(), Matchers.hasItem(Matchers.<Flavor>hasProperty("name", Matchers.equalTo(name))));

		Map<String, Object> headers = new HashMap<>();
		headers.put(NovaConstants.OPERATION, NovaConstants.GET);
		headers.put(NovaConstants.ID, flavor.getId());
		Flavor get = template.requestBodyAndHeaders("direct:flavors", flavor, headers, Flavor.class);

		Assert.assertEquals(flavor.getId(), get.getId());
		Assert.assertEquals(flavor.getName(), get.getName());
		Assert.assertEquals(flavor.getRam(), get.getRam());
	}

	@Test
	public void getAllFlavor() throws InterruptedException {
		final String flavor1Name = name;
		final String flavor2Name = UUID.randomUUID().toString();

		//create flavors
		FlavorBuilder builder = Builders.flavor().name(flavor1Name)
				.ram(4096)
				.vcpus(6)
				.disk(120);

		final org.openstack4j.model.compute.Flavor flavor1 = osclient.compute().flavors().create(
				builder.build());

		builder.name(flavor2Name);
		final org.openstack4j.model.compute.Flavor flavor2 = osclient.compute().flavors().create(
				builder.build());

		await().until(new FlavorListCallable(), Matchers.hasItems(Matchers.<Flavor>hasProperty("name", Matchers.equalTo(flavor1Name)),
				Matchers.<Flavor>hasProperty("name", Matchers.equalTo(flavor2Name))));

		await().untilCall((List<Flavor>)to(template).requestBodyAndHeader("direct:flavors", null, NovaConstants.OPERATION, NovaConstants.GET_ALL, List.class),
				Matchers.hasItems(Matchers.<Flavor>hasProperty("id", Matchers.equalTo(flavor1.getId())),
						Matchers.<Flavor>hasProperty("id", Matchers.equalTo(flavor2.getId()))));
	}

	@Override
	protected RouteBuilder createRouteBuilder() throws Exception {
		return new RouteBuilder() {
			public void configure() {
				from("direct:flavors").to(String.format("openstack-nova:%s?subsystem=flavors&username=%s&password=%s&project=%s",
						properties.getProperty("OPENSTACK_URI"),
						properties.getProperty("OPENSTACK_USERNAME"),
						properties.getProperty("OPENSTACK_PASSWORD"),
						properties.getProperty("PROJECT_ID")));
			}
		};
	}

	private class FlavorListCallable implements Callable<List<Flavor>> {

		@Override
		public List<Flavor> call() throws Exception {
			return (List<Flavor>) createClientForThread(osclient).compute().flavors().list();
		}
	}
}
