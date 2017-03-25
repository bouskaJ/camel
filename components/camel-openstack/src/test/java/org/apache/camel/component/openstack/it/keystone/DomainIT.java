package org.apache.camel.component.openstack.it.keystone;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.openstack.it.AbstractITSupport;
import org.apache.camel.component.openstack.keystone.KeystoneConstants;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import org.hamcrest.Matchers;
import org.openstack4j.api.Builders;
import org.openstack4j.model.identity.v3.Domain;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

@Ignore("https://github.com/ContainX/openstack4j/issues/902")
public class DomainIT extends AbstractITSupport {

	@Test
	public void create() throws IOException, InterruptedException {
		Map<String, Object> headers = new HashMap();
		headers.put(KeystoneConstants.OPERATION, KeystoneConstants.CREATE);
		headers.put(KeystoneConstants.NAME, name);
		final String desc = "desc" + System.currentTimeMillis();
		headers.put(KeystoneConstants.DESCRIPTION, desc);

		Domain created = template.requestBodyAndHeaders("direct:start", null, headers, Domain.class);

		Assert.assertEquals(name, created.getName());
		Assert.assertEquals(desc, created.getDescription());
		assertNotNull(created.getId());

		await().until(new ListCallable(), Matchers.hasItem(
				Matchers.allOf(
						Matchers.hasProperty("name", Matchers.equalTo(name)),
						Matchers.hasProperty("description", Matchers.equalTo(desc))
				)));
	}

	@Test
	public void get() {
		Domain created = osclient.identity().domains().create(Builders.domain().name(name).description("desc" + System.currentTimeMillis()).build());

		await().until(new ListCallable(), Matchers.hasItem(
				Matchers.hasProperty("name", Matchers.equalTo(name))
		));

		Map<String, Object> headers = new HashMap();
		headers.put(KeystoneConstants.OPERATION, KeystoneConstants.GET);
		headers.put(KeystoneConstants.ID, created.getId());

		Domain k = template.requestBodyAndHeaders("direct:start", null, headers, Domain.class);
		Assert.assertEquals(k.getId(), created.getId());
		Assert.assertEquals(k.getName(), created.getName());
	}

	@Test
	public void getAll() {
		osclient.identity().domains().create(Builders.domain().name(name).description("desc").build());

		await().until(new ListCallable(), Matchers.hasItem(
				Matchers.hasProperty("name", Matchers.equalTo(name))
		));

		Map<String, Object> headers = new HashMap();
		headers.put(KeystoneConstants.OPERATION, KeystoneConstants.GET_ALL);
		List<Domain> k = template.requestBodyAndHeaders("direct:start", null, headers, List.class);

		assertThat(k, Matchers.hasItem(Matchers.hasProperty("name", Matchers.equalTo(name))));
	}

	@Test
	public void update() {
		final String description = "description " + System.currentTimeMillis();
		final String newDescription = UUID.randomUUID().toString();
		Domain created = osclient.identity().domains().create(Builders.domain().name(name).description(description).build());

		await().until(new ListCallable(), Matchers.hasItem(
				Matchers.allOf(
						Matchers.hasProperty("name", Matchers.equalTo(name)),
						Matchers.hasProperty("description", Matchers.equalTo(description))
				)));

		Map<String, Object> headers = new HashMap();
		headers.put(KeystoneConstants.OPERATION, KeystoneConstants.UPDATE);

		template.requestBodyAndHeaders("direct:start", created.toBuilder().description(newDescription).build(), headers);

		await().until(new ListCallable(), Matchers.hasItem(
				Matchers.allOf(
						Matchers.hasProperty("name", Matchers.equalTo(name)),
						Matchers.hasProperty("description", Matchers.equalTo(newDescription))
				)));
	}

	@Test
	public void delete() {
		Domain created = osclient.identity().domains().create(Builders.domain().name(name).description("desc").build());

		await().until(new ListCallable(), Matchers.hasItem(
				Matchers.hasProperty("name", Matchers.equalTo(name))
		));

		Map<String, Object> headers = new HashMap();
		headers.put(KeystoneConstants.OPERATION, KeystoneConstants.DELETE);
		headers.put(KeystoneConstants.DOMAIN_ID, created.getId());

		template.requestBodyAndHeaders("direct:start", null, headers);

		await().until(new ListCallable(), Matchers.not(
				Matchers.hasItem(
						Matchers.hasProperty("name", Matchers.equalTo(name))
				)
		));
	}

	@Override
	protected RouteBuilder createRouteBuilder() throws Exception {
		return new RouteBuilder() {
			public void configure() {
				from("direct:start").to(String.format("openstack-keystone:%s?subsystem=domains&username=%s&password=%s&project=%s",
						properties.getProperty("OPENSTACK_URI"),
						properties.getProperty("OPENSTACK_USERNAME"),
						properties.getProperty("OPENSTACK_PASSWORD"),
						properties.getProperty("PROJECT_ID")));
			}
		};
	}

	private class ListCallable implements Callable<List<Domain>> {

		@Override
		public List<Domain> call() throws Exception {
			return (List<Domain>) createClientForThread(osclient).identity().domains().list();
		}
	}
}
