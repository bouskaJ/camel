package org.apache.camel.test.wiremock;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import org.apache.camel.CamelContext;
import org.apache.camel.test.junit4.CamelTestSupport;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.github.tomakehurst.wiremock.recording.SnapshotRecordResult;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class CamelWiremockTestSupport extends CamelTestSupport {

	public static final String REDIRECT_WIREMOCK_URL = "redirect-wiremock-url";
	public static final String WIREMOCK_RECORD = "wiremock-record";

	private static final Logger LOG = LoggerFactory.getLogger(CamelWiremockTestSupport.class);

	@Rule
	public  WireMockRule wireMockRule = new WireMockRule(options().dynamicPort());

	protected abstract  String mockedUrl();


	@Override
	protected void doPreSetup() throws Exception {
		startRecording();
		super.doPreSetup();
	}

	@Override
	protected void doPostTearDown() throws Exception {
		super.doPostTearDown();
		stopRecording();
	}


	public void startRecording(){
		if(!isRecordingEnable()){
			return;
		}
		setProxies();
		wireMockRule.startRecording(mockedUrl());
		LOG.info("Recording on {} started.", mockedUrl());
		LOG.debug("Proxies configured: {}", wireMockRule.getStubMappings());
	}


	public void stopRecording(){
		if(!isRecordingEnable()){
			return;
		}
		// check that mappings dir is created - it is required by wiremock
		final File mappings = Paths.get("src","test","resources", "mappings").toFile();
		mappings.mkdirs();
		SnapshotRecordResult snapshots = wireMockRule.stopRecording();
		LOG.info("Recording on {} stopped.", mockedUrl());
		LOG.debug("Stubs recorded: {}", snapshots);
	}

	protected void setProxies(){
		wireMockRule.stubFor(any(urlMatching("/.*"))
				.willReturn(aResponse().proxiedFrom(mockedUrl())));
	}


	public String getWiremockHostUrl(){
		final String redirect =  System.getProperty(REDIRECT_WIREMOCK_URL);
		return redirect == null ? wireMockRule.baseUrl() : redirect;
	}

	private boolean isRecordingEnable() {
		return System.getProperty(WIREMOCK_RECORD) != null;
	}



}
