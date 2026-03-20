package com.automation.jira;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * JiraTestListener — A TestNG listener that catches test failures
 * and automatically logs a bug in Jira using JiraUtils.
 *
 * It looks for a screenshot path in the test context attribute "screenshot_path".
 * The consumer (e.g., the actual test framework) is responsible for taking the screenshot
 * and setting that attribute.
 */
public class JiraTestListener implements ITestListener {

    private static final Logger log = LoggerFactory.getLogger(JiraTestListener.class);
    private final JiraUtils jiraUtils = new JiraUtils();

    @Override
    public void onTestFailure(ITestResult result) {
        if (!jiraUtils.isConfigured()) {
            return; // silently skip if Jira properties aren't loaded
        }

        String testName = result.getMethod().getMethodName();
        String exceptionMessage = result.getThrowable() != null ?
                result.getThrowable().getMessage() : "Unknown failure";

        String summary = "Automated Test Failed: " + testName;
        String description = String.format(
                "The automated test *%s* failed.\n\n*Error details:*\n{code}%s{code}",
                testName,
                exceptionMessage
        );

        log.info("Test failed. Triggering Jira Bug creation...");

        // 1. Create the Bug
        String issueKey = jiraUtils.createIssue(summary, description);

        // 2. Upload screenshot (if the consumer provided one)
        if (issueKey != null) {
            System.out.println("\n✅ ============================================");
            System.out.println("✅ JIRA TASK CREATED: " + issueKey);
            System.out.println("✅ ============================================\n");

            String screenshotPath = (String) result.getAttribute("screenshot_path");
            if (screenshotPath != null) {
                jiraUtils.attachScreenshotToIssue(issueKey, screenshotPath);
            } else {
                log.info("No screenshot path found in ITestResult. Skipping attachment upload.");
            }
        }
    }
}
