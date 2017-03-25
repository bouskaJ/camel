package org.apache.camel.component.openstack.it.nova;

import static org.awaitility.Awaitility.to;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.openstack.it.AbstractITSupport;
import org.apache.camel.component.openstack.nova.NovaConstants;

import org.junit.Assert;
import org.junit.Test;

import org.hamcrest.Matchers;
import org.openstack4j.model.compute.Keypair;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

public class KeypairIT extends AbstractITSupport {

	@Test
	public void createKeypair() throws IOException, InterruptedException {

		Map<String, Object> headers = new HashMap<>();
		headers.put(NovaConstants.OPERATION, NovaConstants.CREATE);
		headers.put(NovaConstants.NAME, name);
		Keypair created = template.requestBodyAndHeaders("direct:start", null, headers, Keypair.class);

		Assert.assertEquals(name, created.getName());
		Assert.assertNotNull(created.getPublicKey());

		await().until(new ListCallable(), Matchers.hasItem(Matchers.<Keypair>hasProperty("name", Matchers.equalTo(name))));
	}

	@Test
	public void getKeypair() {

		osclient.compute().keypairs().create(name, null);

		await().until(new ListCallable(), Matchers.hasItem(Matchers.<Keypair>hasProperty("name", Matchers.equalTo(name))));

		Map<String, Object> headers = new HashMap<>();
		headers.put(NovaConstants.OPERATION, NovaConstants.GET);
		headers.put(NovaConstants.NAME, name);
		Keypair result = template.requestBodyAndHeaders("direct:start", null, headers, Keypair.class);

		Assert.assertEquals(name, result.getName());
		Assert.assertNotNull(result.getPublicKey());
	}

	@Test
	public void getAllFlavor() throws InterruptedException {
		final String name1 = UUID.randomUUID().toString();
		final String name2 = UUID.randomUUID().toString();

		osclient.compute().keypairs().create(name1, null);
		osclient.compute().keypairs().create(name2, null);

		await().until(new ListCallable(), Matchers.hasItems(Matchers.<Keypair>hasProperty("name", Matchers.equalTo(name1)),
				Matchers.<Keypair>hasProperty("name", Matchers.equalTo(name2))));

		await().untilCall((List<Keypair>) to(template).requestBodyAndHeader("direct:start", null, NovaConstants.OPERATION, NovaConstants.GET_ALL, List.class),
				Matchers.hasItems(Matchers.<Keypair>hasProperty("name", Matchers.equalTo(name1)),
						Matchers.<Keypair>hasProperty("name", Matchers.equalTo(name2))));
	}

	@Override
	protected RouteBuilder createRouteBuilder() throws Exception {
		return new RouteBuilder() {
			public void configure() {
				from("direct:start").to(String.format("openstack-nova:%s?subsystem=keypairs&username=%s&password=%s&project=%s",
						properties.getProperty("OPENSTACK_URI"),
						properties.getProperty("OPENSTACK_USERNAME"),
						properties.getProperty("OPENSTACK_PASSWORD"),
						properties.getProperty("PROJECT_ID")));
			}
		};
	}

	private class ListCallable implements Callable<List<Keypair>> {

		@Override
		public List<Keypair> call() throws Exception {
			return (List<Keypair>) createClientForThread(osclient).compute().keypairs().list();
		}
	}
}
