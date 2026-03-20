# Jira TestNG Integration Library

A standalone, reusable Maven library that automatically parses TestNG test failures, creates Bugs in Jira, and uploads screenshot attachments via the Jira REST API. 

This library is designed to be published via **JitPack** and consumed by any Selenium Java + TestNG framework without requiring authentication tokens.

---

## 🌎 1. Publishing via JitPack

JitPack builds GitHub repositories on-demand. To publish:

1. Push this project to a **public** GitHub repository (e.g., `Blitzer0007/jira-testng-integration`).
2. Create a **GitHub Release** or **Tag** (e.g., `v1.0.0`).
3. That's it! JitPack will automatically pick it up when someone requests it.

---

## 🚀 2. Consuming in a Test Automation Framework

Any automation framework can pull this library directly from JitPack.

### Step A: Add JitPack Repository
Add this to the consumer framework's `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

### Step B: Add Dependency
Add the dependency pointing to your GitHub repo and tag:

```xml
<dependency>
    <groupId>com.github.Blitzer0007</groupId>
    <artifactId>jira-testng-integration</artifactId>
    <version>v1.0.0</version>
</dependency>
```
*(Note: Replace `jira-testng-integration` with the exact name of your GitHub repo if you named it differently in Step 1).*

### Step C: Configure properties
Set up the environment variables or JVM system properties before tests run:
```properties
jira.url=https://yourcompany.atlassian.net
jira.username=your-email@company.com
jira.api.token=YOUR_API_TOKEN
jira.project.key=PROJ
```

### Step D: Register the Listener
Register the Jira listener in the consumer's `testng.xml`:
```xml
<listeners>
    <listener class-name="com.automation.jira.JiraTestListener"/>
</listeners>
```

---

## 📸 Passing Screenshots to Jira

Because this library is independent of Selenium WebDriver, the consumer framework is responsible for taking the screenshot and passing the physical file path to `JiraTestListener` via the TestNG `ITestResult` attribute.

Example consumer `TestListener.java`:
```java
@Override
public void onTestFailure(ITestResult result) {
    // 1. Consumer takes the screenshot
    String screenshotPath = ScreenshotUtils.captureScreenshot(driver, result.getName());
    
    // 2. Set attribute so the Jira library picks it up automatically
    result.setAttribute("screenshot_path", screenshotPath);
}
```
*Note: Make sure your custom listener runs **before** the `JiraTestListener` (order them correctly in your `testng.xml`, or use listener ordering logic).*
