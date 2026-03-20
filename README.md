# Universal Jira Integration Library

A multi-module Maven library that automatically parses automated test failures (from TestNG or Cucumber), creates Bugs/Tasks in Jira, and uploads screenshot attachments & HTML reports directly via the Jira REST API.

This powerful library operates independently of Selenium WebDriver and can be injected into any existing automation framework simply by adding a Maven dependency.

---

## 🏗️ Project Architecture

This library is split into three core modules:

1. **`jira-core`**: Contains pure `JiraUtils` REST API interactions. (No test framework dependencies).
2. **`jira-testng-listener`**: Contains the TestNG `ITestListener` hook. Automatically detects failed `testng.xml` tests.
3. **`jira-cucumber-plugin`**: Contains the Cucumber `ConcurrentEventListener` hook. Automatically detects failed BDD scenarios.

---

## 🌎 1. How to Publish to GitHub Packages

Before anyone can download this library, it must be deployed (published) to GitHub Packages.

### Step A: Configure the Parent `pom.xml`
Ensure the top-level `pom.xml` contains the `distributionManagement` block pointing to your GitHub repository:
```xml
<distributionManagement>
    <repository>
        <id>github</id>
        <name>GitHub Packages</name>
        <url>https://maven.pkg.github.com/Blitzer0007/jira-testng-integration</url>
    </repository>
</distributionManagement>
```

### Step B: Generate a GitHub Token
1. Go to GitHub -> Settings -> Developer Settings -> Personal Access Tokens (Classic).
2. Generate a new token and give it the **`read:packages`** and **`write:packages`** permissions.
3. Copy the token.

### Step C: Configure Local Maven Authentication
Your computer needs to know your token so it can upload the `.jar` files safely.

1. Navigate to your local Maven folder (usually `C:\Users\YOUR_NAME\.m2\` or `~/.m2/`).
2. Create or edit a file called `settings.xml`.
3. Add your token under the `<id>github</id>` server:
```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>ghp_YOUR_PERSONAL_ACCESS_TOKEN</password>
    </server>
  </servers>
</settings>
```

#### GitHub Actions (Continuous Testing / CI)
If you are running your publishing or consumer tests in a GitHub Actions pipeline, you do **not** need to commit a `settings.xml`. You can use the built-in `GITHUB_TOKEN` secret to temporarily authorize Maven inline:
```yaml
      - name: Authenticate Maven & Run Tests
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: mvn clean test -s .github/settings.xml
```
*(In your repo, create `.github/settings.xml` and map the `<password>` to `${env.GITHUB_TOKEN}`)*.

#### Forcing IntelliJ / Eclipse to read `settings.xml` so you can publish:
**For IntelliJ IDEA:**
1. Open **File** -> **Settings** (or IntelliJ IDEA -> Preferences on Mac).
2. Navigate to **Build, Execution, Deployment** -> **Build Tools** -> **Maven**.
3. Under **User settings file:**, check the **Override** checkbox.
4. Set the path to `C:\Users\YOUR_NAME\.m2\settings.xml` (or `~/.m2/settings.xml` on Mac/Linux).
5. Click **Apply** and then **Reload All Maven Projects** (the little refresh icon in the Maven sidebar).

**For Eclipse:**
1. Open **Window** -> **Preferences** (or Eclipse -> Settings on Mac).
2. Navigate to **Maven** -> **User Settings**.
3. Under **User Settings:**, click `Browse...` and select your `~/.m2/settings.xml` file.
4. Click **Apply and Close**, then right-click your project -> **Maven** -> **Update Project**.

### Step D: Deploy!
1. Open this Universal Jira Library project in IntelliJ.
2. Open the Maven side-panel.
3. Expand **`jira-integration-parent`** -> **Lifecycle**.
4. Double-click **`deploy`**.
5. Maven will build the Core, TestNG, and Cucumber modules sequentially and directly upload all three to GitHub Packages securely!

---

## 🚀 2. How to Consume in an Automation Framework

Once the universally hosted library is deployed, ANY test framework can download it. 

### Step A: Tell the Consumer IDE about your Token
The consumer computer ALSO needs the exact same `~/.m2/settings.xml` setup from the publishing steps so it has permission to download the library! 

**Important IDE Override:** Just like when publishing, you must *force* the consumer's IDE to read the `settings.xml`. 
- **In IntelliJ**, go to `Settings -> Build Tools -> Maven -> User settings file`, check the `Override` box, and manually point it to your `C:\Users\YOUR_NAME\.m2\settings.xml`. **Make sure to click the Reload All Maven Projects button!**
- **In Eclipse**, go to `Preferences -> Maven -> User Settings` and point to the `.xml` file. Right-click the project -> **Maven -> Update Project**.

### Step B: Add GitHub Packages Repository
Add this to the consumer framework's `pom.xml`:
```xml
<repositories>
    <repository>
        <id>github</id>
        <name>GitHub Packages</name>
        <url>https://maven.pkg.github.com/Blitzer0007/jira-testng-integration</url>
    </repository>
</repositories>
```

### Step C: Add the Dependency (Pick ONE!)
Add the specific module you want to the consumer framework's `pom.xml`:

**If you use TestNG:**
```xml
<dependency>
    <groupId>com.github.Blitzer0007</groupId>
    <artifactId>jira-testng-listener</artifactId>
    <version>1.2.0</version>
</dependency>
```

**If you use Cucumber:**
```xml
<dependency>
    <groupId>com.github.Blitzer0007</groupId>
    <artifactId>jira-cucumber-plugin</artifactId>
    <version>1.2.0</version>
</dependency>
```

### Step D: Configure Jira Properties
Create a `config.properties` file in the consumer framework's `src/test/resources/` (or expose them as System Properties) before the suites execute:
```properties
jira.url=https://YOUR_DOMAIN.atlassian.net
jira.username=YOUR_EMAIL
jira.api.token=YOUR_JIRA_API_TOKEN
jira.project.key=YOUR_PROJECT_KEY
jira.issue.type=Task
```

### Step E: Register the Listener
**For TestNG:** Register the Jira listener in the consumer's `testng.xml`:
```xml
<listeners>
    <listener class-name="com.automation.jira.testng.JiraTestListener"/>
</listeners>
```

**For Cucumber:** Add the plugin to your `@CucumberOptions` runner class:
```java
@CucumberOptions(plugin = {"pretty", "com.automation.jira.cucumber.JiraCucumberPlugin"})
```

---

## 📸 Passing Screenshots to Jira
Because this library acts independently of Selenium, the consumer framework itself is responsible for snapping the screenshot of the broken browser and passing the physical file path.

**In TestNG**, save the path as an attribute directly to the `ITestResult`:
```java
result.setAttribute("screenshot_path", "target/screenshots/error.png,target/reports/Extent.html");
```
*(The Jira listener reads the comma-separated string, uploads the screenshot as an inline image, and uploads the HTML file as a download link!)*

**In Cucumber**, temporarily set it as a system-level property:
```java
System.setProperty("cucumber.failure.screenshot.path", "target/screenshots/error.png");
```
