package com.example;

import static org.assertj.core.api.BDDAssertions.then;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.FileCopyUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = DemoApplication.class)
@WebIntegrationTest
public class DemoApplicationTests {

	@Autowired MessageCollector messageCollector;
	@Autowired Source source;

	@Value("classpath:/github-webhook-input/issue-created.json") Resource issueCreatedInput;
	@Value("classpath:/github-webhook-input/hook-created.json") Resource hookCreatedInput;
	@Value("classpath:/github-webhook-output/issue-created.json") Resource issueCreatedOutput;
	@Value("classpath:/github-webhook-output/hook-created.json") Resource hookCreatedOutput;

	@Test
	public void issueCreated() throws InterruptedException, IOException {
		post(issueCreatedInput);
		Message<?> message = messageCollector.forChannel(source.output()).poll(5, TimeUnit.SECONDS);
		then(message).isNotNull();
		then(message.getPayload()).isEqualTo(read(issueCreatedOutput));
	}

	@Test
	public void hookCreated() throws InterruptedException, IOException {
		post(hookCreatedInput);
		Message<?> message = messageCollector.forChannel(source.output()).poll(5, TimeUnit.SECONDS);
		then(message).isNotNull();
		then(message.getPayload()).isEqualTo(read(hookCreatedOutput));
	}

	private void post(Resource resource) throws IOException {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
		HttpEntity<String> httpEntity = new HttpEntity<>(read(resource), httpHeaders);
		new TestRestTemplate().exchange("http://localhost:8080/", HttpMethod.POST, httpEntity, String.class);
	}

	private String read(Resource resource) throws IOException {
		return FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream()));
	}
}