package ru.asocial.scheduler.security;

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
		"scheduler.enabled=false",
		"spring.datasource.url=jdbc:h2:mem:schedsec;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
		"app.jwt.secret=test-secret-key-at-least-32-characters-long",
		"app.jwt.issuer=jobflow-auth",
		"app.jwt.access-token-ttl=PT1H"
})
@AutoConfigureMockMvc
class ScheduleSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JobflowJwtProperties jwtProperties;

	@Test
	void getSchedulesWithoutTokenReturns401() throws Exception {
		mockMvc.perform(get("/api/schedules"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void viewerCanGetSchedules() throws Exception {
		String token = JobflowJwtSupport.createAccessToken(jwtProperties, "user", "VIEWER");

		mockMvc.perform(get("/api/schedules")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void viewerCannotCreateSchedule() throws Exception {
		String token = JobflowJwtSupport.createAccessToken(jwtProperties, "user", "VIEWER");

		mockMvc.perform(post("/api/schedules")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"taskType":"DELAY","parameters":"5","nextRunAt":"2026-01-01T00:00:00","repeatIntervalMinutes":60}
								"""))
				.andExpect(status().isForbidden());
	}
}
