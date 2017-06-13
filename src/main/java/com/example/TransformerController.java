package com.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.PathNotFoundException;
import com.toomuchcoding.jsonassert.JsonPath;

/**
 * @author Marcin Grzejszczak
 */
@RestController
public class TransformerController {

	static final List<Pojo> DATABASE = Collections.synchronizedList(new ArrayList<>());

	@PostConstruct
	public void simulateInitialState() {
		DATABASE.add(new Pojo("test1", "test1/test1", "hook", "updated"));
		DATABASE.add(new Pojo("test2", "test2/test2", "issue", "created"));
	}

	@RequestMapping(value = "/", method = RequestMethod.POST)
	public Pojo transform(@RequestBody String message) {
		DocumentContext parsedJson = com.jayway.jsonpath.JsonPath.parse(message);
		String username = parsedJson
				.read(JsonPath.builder().field("sender").field("login").jsonPath());
		String repo;
		try {
			repo = parsedJson.read(
					JsonPath.builder().field("repository").field("full_name").jsonPath());
		}
		catch (PathNotFoundException e) {
			repo = parsedJson.read(
					JsonPath.builder().field("organization").field("login").jsonPath());
		}
		String type;
		try {
			System.err.println(JsonPath.builder().field("issue").jsonPath());
			type = parsedJson.read("$.issue") != null ? "issue" : "unknown";
		}
		catch (PathNotFoundException e) {
			try {
				type = parsedJson.read("$.hook") != null ? "hook" : "unknown";
			}
			catch (PathNotFoundException ex) {
				type = "unknown";
			}
		}
		String action;
		try {
			action = parsedJson.read("$.action", String.class);
		}
		catch (Exception e) {
			action = "updated";
		}
		Pojo pojo = new Pojo(username, repo, type, action);
		DATABASE.add(pojo);
		return pojo;
	}

	@GetMapping(value = "/")
	public Pojos pojos() {
		return new Pojos(DATABASE);
	}


}
