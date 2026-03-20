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
    private String issueType;

    public JiraUtils() {
        // We do not load system properties in the constructor anymore.
        // TestNG instantiates listener classes very early in the lifecycle,
        // often before the consumer's @BeforeSuite has had a chance to set System Properties.
    }

    private String getJiraUrl() {
        String url = System.getProperty("jira.url");
        if (url != null && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
    private String getUsername() { return System.getProperty("jira.username"); }
    private String getApiToken() { return System.getProperty("jira.api.token"); }
    private String getProjectKey() { return System.getProperty("jira.project.key"); }
    private String getIssueType() { return System.getProperty("jira.issue.type", "Task"); }

    public boolean isConfigured() {
        return getJiraUrl() != null && getUsername() != null && getApiToken() != null && getProjectKey() != null;
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

        log.info("Creating Jira Bug in project: {}...", getProjectKey());

        if (getJiraUrl() != null) {
            RestAssured.baseURI = getJiraUrl();
        }

        Map<String, Object> project = new HashMap<>();
        project.put("key", getProjectKey());

        Map<String, Object> issuetype = new HashMap<>();
        issuetype.put("name", getIssueType());

        Map<String, Object> fields = new HashMap<>();
        fields.put("project", project);
        fields.put("summary", summary);
        fields.put("description", description);
        fields.put("issuetype", issuetype);

        Map<String, Object> payload = new HashMap<>();
        payload.put("fields", fields);

        Response response = RestAssured.given()
                .auth().preemptive().basic(getUsername(), getApiToken())
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
                .auth().preemptive().basic(getUsername(), getApiToken())
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

    /**
     * Adds a text comment to an existing Jira issue.
     */
    public void addCommentToIssue(String issueKey, String commentBody) {
        if (issueKey == null || commentBody == null) return;

        Map<String, String> payload = new HashMap<>();
        payload.put("body", commentBody);

        Response response = RestAssured.given()
                .auth().preemptive().basic(getUsername(), getApiToken())
                .contentType(ContentType.JSON)
                .body(payload)
                .post("/rest/api/2/issue/" + issueKey + "/comment");

        if (response.statusCode() == 201) {
            log.info("Comment added successfully to issue {}!", issueKey);
        } else {
            log.error("Failed to add comment. Response code: {}", response.statusCode());
            log.error(response.asPrettyString());
        }
    }
}
