package com.acme.salary.web;

import com.acme.salary.domain.Country;
import com.acme.salary.domain.JobLevel;
import com.acme.salary.domain.ReferenceCatalog;
import com.acme.salary.domain.SalaryBand;
import com.acme.salary.security.LoginThrottle;
import com.acme.salary.service.CurrentPayProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeApiTest {
    static {
        try {
            Files.deleteIfExists(Path.of("target/api-test.db"));
            Files.deleteIfExists(Path.of("target/api-test.db-wal"));
            Files.deleteIfExists(Path.of("target/api-test.db-shm"));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private LoginThrottle throttle;
    @Autowired private CurrentPayProjection projection;
    @Value("${app.demo.username}") private String username;
    @Value("${app.demo.password}") private String password;

    private final ObjectMapper json = new ObjectMapper();
    private int sequence = 1;

    @BeforeEach
    void clean() {
        jdbc.update("delete from audit_event");
        jdbc.update("delete from current_compensation");
        jdbc.update("delete from compensation_record");
        jdbc.update("delete from employee");
        jdbc.update("update employee_number_seq set next_value = 1 where id = 1");
        throttle.reset();
        projection.clearDayCache();
    }

    @Test
    void apiRejectsAnonymousCalls() throws Exception {
        mockMvc.perform(get("/api/employees")).andExpect(status().isUnauthorized());
    }

    @Test
    void badLoginDoesNotRevealWhichPartFailed() throws Exception {
        mockMvc.perform(csrf(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nobody\",\"password\":\"wrong-password\"}")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password."));
        Integer failures = jdbc.queryForObject(
                "select count(*) from audit_event where action = 'LOGIN_FAILURE' and summary like '%nobody%'",
                Integer.class
        );
        assertThat(failures).isZero();
    }

    @Test
    void loginLocksOutAfterRepeatedFailures() throws Exception {
        String bad = "{\"username\":\"" + username + "\",\"password\":\"nope\"}";
        for (int attempt = 0; attempt < 3; attempt++) {
            mockMvc.perform(csrf(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(bad)))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(csrf(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}")))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void createRejectsANumericSalaryAndAnUnknownField() throws Exception {
        MockHttpSession session = signIn();
        SalaryBand band = ReferenceCatalog.band(Country.US, JobLevel.L3);
        String body = employeeJson("ada@acme.test", money(band.min())).replace(
                "\"" + money(band.min()) + "\"",
                "100"
        );
        mockMvc.perform(csrf(post("/api/employees").session(session).contentType(MediaType.APPLICATION_JSON).content(body)))
                .andExpect(status().isBadRequest());
        String extra = employeeJson("ada@acme.test", money(band.min())).replace(
                "{",
                "{\"nickname\":\"x\","
        );
        mockMvc.perform(csrf(post("/api/employees").session(session).contentType(MediaType.APPLICATION_JSON).content(extra)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void recordsAHireAndRejectsThenAcceptsALargeMerit() throws Exception {
        MockHttpSession session = signIn();
        SalaryBand band = ReferenceCatalog.band(Country.US, JobLevel.L3);
        String base = money(band.min());
        String raised = money(band.min().multiply(new BigDecimal("1.30")));
        MvcResult created = mockMvc.perform(csrf(post("/api/employees").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson("ada@acme.test", base))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee.employeeNumber").value("E000001"))
                .andExpect(jsonPath("$.history[0].changeType").value("HIRE"))
                .andReturn();
        int id = json.readTree(created.getResponse().getContentAsString()).get("employee").get("id").asInt();
        mockMvc.perform(csrf(post("/api/employees/" + id + "/compensation").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(changeJson("MERIT", "2026-11-02", raised, "Annual merit review for this cycle.", null))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.violations[0]").exists());
        mockMvc.perform(get("/api/employees/" + id).session(session))
                .andExpect(jsonPath("$.history.length()").value(1));
        mockMvc.perform(csrf(post("/api/employees/" + id + "/compensation").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(changeJson("MERIT", "2026-11-02", raised, "Annual merit review for this cycle.",
                                "Compensation committee approved a scarce-skill exception."))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee.baseSalary").value(raised))
                .andExpect(jsonPath("$.history[0].changeType").value("MERIT"));
    }

    @Test
    void futureDatedIncreaseDoesNotReplaceCurrentPay() throws Exception {
        MockHttpSession session = signIn();
        SalaryBand band = ReferenceCatalog.band(Country.US, JobLevel.L3);
        String base = money(band.min());
        String raised = money(band.min().multiply(new BigDecimal("1.04")));
        int id = create(session, "future@acme.test", base);
        mockMvc.perform(csrf(post("/api/employees/" + id + "/compensation").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(changeJson("MERIT", "2026-12-02", raised, "Annual merit review for this cycle.", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee.baseSalary").value(base))
                .andExpect(jsonPath("$.history.length()").value(2));
    }

    @Test
    void terminatedEmployeeCannotReceiveAnotherChange() throws Exception {
        MockHttpSession session = signIn();
        SalaryBand band = ReferenceCatalog.band(Country.US, JobLevel.L3);
        int id = create(session, "gone@acme.test", money(band.min()));
        mockMvc.perform(csrf(post("/api/employees/" + id + "/terminate").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"terminationDate\":\"2026-11-02\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee.status").value("TERMINATED"));
        mockMvc.perform(csrf(post("/api/employees/" + id + "/compensation").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(changeJson("MERIT", "2026-11-02", money(band.mid()), "Annual merit review for this cycle.", null))))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(csrf(post("/api/employees/" + id + "/terminate").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"terminationDate\":\"2026-11-02\"}")))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void searchDoesNotTreatTheQueryAsSqlAndSortIsAllowlisted() throws Exception {
        MockHttpSession session = signIn();
        SalaryBand band = ReferenceCatalog.band(Country.US, JobLevel.L3);
        create(session, "ada@acme.test", money(band.min()));
        create(session, "grace@acme.test", money(band.min()));
        mockMvc.perform(get("/api/employees").session(session).param("q", "%' OR '1'='1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
        mockMvc.perform(get("/api/employees").session(session).param("q", "grace@acme.test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
        mockMvc.perform(get("/api/employees").session(session).param("sort", "salary;drop table employee"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(csrf(delete("/api/employees/1").session(session)))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void duplicateEmailConflicts() throws Exception {
        MockHttpSession session = signIn();
        SalaryBand band = ReferenceCatalog.band(Country.US, JobLevel.L3);
        create(session, "ada@acme.test", money(band.min()));
        mockMvc.perform(csrf(post("/api/employees").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson("Ada@Acme.test", money(band.min())))))
                .andExpect(status().isConflict());
    }

    private int create(MockHttpSession session, String email, String base) throws Exception {
        MvcResult result = mockMvc.perform(csrf(post("/api/employees").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(employeeJson(email, base))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode employee = json.readTree(result.getResponse().getContentAsString()).get("employee");
        return employee.get("id").asInt();
    }

    private MockHttpSession signIn() throws Exception {
        MvcResult result = mockMvc.perform(csrf(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}")))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        return session;
    }

    private MockHttpServletRequestBuilder csrf(MockHttpServletRequestBuilder builder) throws Exception {
        MvcResult health = mockMvc.perform(get("/api/health")).andExpect(status().isOk()).andReturn();
        Cookie cookie = health.getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).as("CSRF cookie").isNotNull();
        return builder.cookie(cookie).header("X-XSRF-TOKEN", java.net.URLDecoder.decode(cookie.getValue(), java.nio.charset.StandardCharsets.UTF_8));
    }

    private String employeeJson(String email, String base) {
        sequence++;
        return """
                {
                  "firstName": "Ada",
                  "lastName": "Lovelace",
                  "email": "%s",
                  "country": "US",
                  "department": "ENGINEERING",
                  "jobTitle": "Engineer",
                  "jobLevel": "L3",
                  "gender": "FEMALE",
                  "hireDate": "2024-01-15",
                  "baseSalary": "%s",
                  "annualBonus": "0.00",
                  "allowances": "0.00"
                }
                """.formatted(email, base);
    }

    private static String changeJson(String type, String date, String base, String reason, String exception) {
        String exceptionJson = exception == null ? "null" : "\"" + exception + "\"";
        return """
                {
                  "changeType": "%s",
                  "effectiveDate": "%s",
                  "baseSalary": "%s",
                  "annualBonus": "0.00",
                  "allowances": "0.00",
                  "reason": "%s",
                  "exceptionReason": %s
                }
                """.formatted(type, date, base, reason, exceptionJson);
    }

    private static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
