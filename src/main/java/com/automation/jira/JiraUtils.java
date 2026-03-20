package com.automation.jira;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * JiraUtils — helper to communicate with Jira REST API using Rest-Assured.
 */
public class JiraUtils {

    private static final Logger log = LoggerFactory.getLogger(JiraUtils.class);

    private String jiraUrl;
    private String username;
    private String apiToken;
    private String projectKey;

    public JiraUtils() {
        // Read from System properties (which can be loaded from .properties by the consumer)
        this.jiraUrl = System.getProperty("jira.url");
        this.username = System.getProperty("jira.username");
        this.apiToken = System.getProperty("jira.api.token");
        this.projectKey = System.getProperty("jira.project.key");

        if (jiraUrl != null) {
            RestAssured.baseURI = jiraUrl;
        }
    }

    public boolean isConfigured() {
        return jiraUrl != null && username != null && apiToken != null && projectKey != null;
    }

    /**
     * Creates a Bug in Jira.
     * @return the issue key (e.g. "PROJ-123") or null on failure.
     */
    public String createIssue(String summary, String description) {
        if (!isConfigured()) {
            log.warn("Jira is not configured. Skipping issue creation.");
            return null;
        }

        log.info("Creating Jira Bug in project: {}...", projectKey);

        Map<String, Object> project = new HashMap<>();
        project.put("key", projectKey);

        Map<String, Object> issuetype = new HashMap<>();
        issuetype.put("name", "Bug");

        Map<String, Object> fields = new HashMap<>();
        fields.put("project", project);
        fields.put("summary", summary);
        fields.put("description", description);
        fields.put("issuetype", issuetype);

        Map<String, Object> payload = new HashMap<>();
        payload.put("fields", fields);

        Response response = RestAssured.given()
                .auth().preemptive().basic(username, apiToken)
                .contentType(ContentType.JSON)
                .body(payload)
                .post("/rest/api/2/issue");

        if (response.statusCode() == 201) {
            String issueKey = response.jsonPath().get("key");
            log.info("Bug created successfully! Key: {}", issueKey);
            return issueKey;
        } else {
            log.error("Failed to create Jira issue. Response code: {}", response.statusCode());
            log.error(response.asPrettyString());
            return null;
        }
    }

    /**
     * Uploads a file attachment to an existing Jira issue.
     */
    public void attachScreenshotToIssue(String issueKey, String filePath) {
        if (issueKey == null || filePath == null) return;

        File file = new File(filePath);
        if (!file.exists()) {
            log.warn("Screenshot file not found at: {}", filePath);
            return;
        }

        log.info("Uploading screenshot to issue {}...", issueKey);

        Response response = RestAssured.given()
                .auth().preemptive().basic(username, apiToken)
                .header("X-Atlassian-Token", "no-check")
                .multiPart("file", file)
                .post("/rest/api/2/issue/" + issueKey + "/attachments");

        if (response.statusCode() == 200) {
            log.info("Screenshot uploaded successfully!");
        } else {
            log.error("Failed to upload screenshot. Response code: {}", response.statusCode());
            log.error(response.asPrettyString());
        }
    }
}
