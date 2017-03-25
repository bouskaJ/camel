package org.apache.camel.component.openstack.it.cinder;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.openstack.cinder.CinderConstants;
import org.apache.camel.component.openstack.it.AbstractITSupport;

import org.junit.BeforeClass;
import org.junit.Test;

import org.hamcrest.Matchers;
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.VolumeSnapshot;
import org.openstack4j.openstack.OSFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

public class SnapshotIT extends AbstractITSupport {

	private static OSClient.OSClientV3 beforeClassOsClient = OSFactory.builderV3().endpoint(properties.getProperty("OPENSTACK_URI"))
			.credentials(properties.getProperty("OPENSTACK_USERNAME"), properties.getProperty("OPENSTACK_PASSWORD"), Identifier.byId("default"))
			.scopeToProject(Identifier.byId(properties.getProperty("PROJECT_ID"))).authenticate();

	private static Volume vol;

	@BeforeClass
	public static void setUpTest() throws InterruptedException {
		final String volName = UUID.randomUUID().toString();
		vol = createVolume(volName);
		await().until(new Callable<List<Volume>>() {
			@Override
			public List<Volume> call() throws Exception {
				return (List<Volume>) createClientForThread(beforeClassOsClient).blockStorage().volumes().list();
			}
		}, Matchers.hasItem(Matchers.hasProperty("name", Matchers.equalTo(volName))));

		assertNotNull(vol);
	}


	@Test
	public void createVolumeSnapshot() throws IOException, InterruptedException {

		Map<String, Object> headers = new HashMap();
		headers.put(CinderConstants.OPERATION, CinderConstants.CREATE);
		headers.put(CinderConstants.NAME, name);
		headers.put(CinderConstants.VOLUME_ID, vol.getId());

		VolumeSnapshot created = template.requestBodyAndHeaders("direct:start", null, headers, VolumeSnapshot.class);

		assertEquals(name, created.getName());
		assertNotNull(created.getId());

		await().until(new ListCallable(), Matchers.hasItem(Matchers.allOf(Matchers.hasProperty("name", Matchers.equalTo(name)),
				Matchers.hasProperty("id", Matchers.equalTo(created.getId())))));
	}

	@Test
	public void getVolSnapshot() throws InterruptedException {

		VolumeSnapshot created = osclient.blockStorage().snapshots().create(Builders.volumeSnapshot().name(name).volume(vol.getId()).build());

		await().until(new ListCallable(), Matchers.hasItem(Matchers.hasProperty("name", Matchers.equalTo(name))));

		Map<String, Object> headers = new HashMap();
		headers.put(CinderConstants.OPERATION, CinderConstants.GET);
		headers.put(CinderConstants.ID, created.getId());

		VolumeSnapshot get = template.requestBodyAndHeaders("direct:start", null, headers, VolumeSnapshot.class);
		assertEquals(created.getId(), get.getId());
		assertEquals(created.getVolumeId(), get.getVolumeId());
	}

	@Test
	public void getAll() throws InterruptedException {

		osclient.blockStorage().snapshots().create(Builders.volumeSnapshot().name(name).volume(vol.getId()).build());

		await().until(new ListCallable(), Matchers.hasItem(Matchers.hasProperty("name", Matchers.equalTo(name))));

		Map<String, Object> headers = new HashMap();
		headers.put(CinderConstants.OPERATION, CinderConstants.GET_ALL);
		List<VolumeSnapshot> k = template.requestBodyAndHeaders("direct:start", null, headers, List.class);

		assertThat(k, Matchers.hasItem(Matchers.hasProperty("name", Matchers.equalTo(name))));
	}

	@Test
	public void update() throws InterruptedException {

		VolumeSnapshot created = osclient.blockStorage().snapshots().create(Builders.volumeSnapshot().name(name).volume(vol.getId()).build());

		await().until(new ListCallable(), Matchers.hasItem(Matchers.hasProperty("name", Matchers.equalTo(name))));

		final String newDesc = UUID.randomUUID().toString();

		Map<String, Object> headers = new HashMap();
		headers.put(CinderConstants.OPERATION, CinderConstants.UPDATE);
		headers.put(CinderConstants.DESCRIPTION, newDesc);
		headers.put(CinderConstants.ID, created.getId());
		headers.put(CinderConstants.NAME, created.getName());

		template.requestBodyAndHeaders("direct:start", null, headers);

		await().until(new ListCallable(), Matchers.hasItem(Matchers.allOf(
				Matchers.hasProperty("id", Matchers.equalTo(created.getId())),
				Matchers.hasProperty("name", Matchers.equalTo(name)),
				Matchers.hasProperty("description", Matchers.equalTo(newDesc)))));
	}

	@Test
	public void delete() throws InterruptedException {

		VolumeSnapshot created = osclient.blockStorage().snapshots().create(Builders.volumeSnapshot().name(name).volume(vol.getId()).build());

		await().until(new ListCallable(), Matchers.hasItem(Matchers.hasProperty("name", Matchers.equalTo(name))));

		Map<String, Object> headers = new HashMap();
		headers.put(CinderConstants.OPERATION, CinderConstants.DELETE);
		headers.put(CinderConstants.SNAPSHOT_ID, created.getId());
		template.requestBodyAndHeaders("direct:start", null, headers);

		await().until(new ListCallable(), Matchers.not(Matchers.hasItem(Matchers.hasProperty("name", Matchers.equalTo(name)))));
	}

	@Override
	protected RouteBuilder createRouteBuilder() throws Exception {
		return new RouteBuilder() {
			public void configure() {
				from("direct:start").to(String.format("openstack-cinder:%s?subsystem=snapshots&username=%s&password=%s&project=%s",
						properties.getProperty("OPENSTACK_URI"),
						properties.getProperty("OPENSTACK_USERNAME"),
						properties.getProperty("OPENSTACK_PASSWORD"),
						properties.getProperty("PROJECT_ID")));
			}
		};
	}

	private static Volume createVolume(String volName) {
		return beforeClassOsClient.blockStorage().volumes().create(Builders.volume().name(volName).size(1).build());
	}
	
	private class ListCallable implements Callable<List<VolumeSnapshot>>{

		@Override
		public List<VolumeSnapshot> call() throws Exception {
			return (List<VolumeSnapshot>) createClientForThread(beforeClassOsClient).blockStorage().snapshots().list();
		}
	}
}
