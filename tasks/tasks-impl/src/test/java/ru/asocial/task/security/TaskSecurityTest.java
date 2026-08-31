package ru.asocial.task.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.asocial.auth.jwt.JobflowJwtProperties;
import ru.asocial.auth.jwt.JobflowJwtSupport;

@SpringBootTest(properties = {
		"spring.profiles.active=jwt",
		"worker.enabled=false",
		"app.kafka.enabled=false",
		"spring.datasource.url=jdbc:h2:mem:tasksec;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
		"app.jwt.secret=test-secret-key-at-least-32-characters-long",
		"app.jwt.issuer=jobflow-auth",
		"app.jwt.access-token-ttl=PT1H"
})
@AutoConfigureMockMvc
class TaskSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JobflowJwtProperties jwtProperties;

	@Test
	void getTasksWithoutTokenReturns401() throws Exception {
		mockMvc.perform(get("/api/tasks"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void viewerCanGetTasks() throws Exception {
		String token = JobflowJwtSupport.createAccessToken(jwtProperties, "user", "VIEWER");

		mockMvc.perform(get("/api/tasks")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void viewerCannotCreateTask() throws Exception {
		String token = JobflowJwtSupport.createAccessToken(jwtProperties, "user", "VIEWER");

		mockMvc.perform(post("/api/tasks")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"type\":\"DELAY\",\"parameters\":\"{\\\"durationSeconds\\\":5}\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminCanCreateTask() throws Exception {
		String token = JobflowJwtSupport.createAccessToken(jwtProperties, "admin", "ADMIN");

		mockMvc.perform(post("/api/tasks")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"type\":\"DELAY\",\"parameters\":\"{\\\"durationSeconds\\\":5}\"}"))
				.andExpect(status().isCreated());
	}
}
