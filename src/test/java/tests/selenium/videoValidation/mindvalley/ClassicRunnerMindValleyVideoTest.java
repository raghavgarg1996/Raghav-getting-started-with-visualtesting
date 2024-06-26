package tests.selenium.videoValidation.mindvalley;

import com.applitools.eyes.*;
import com.applitools.eyes.selenium.ClassicRunner;
import com.applitools.eyes.selenium.Configuration;
import com.applitools.eyes.selenium.Eyes;
import com.applitools.eyes.selenium.fluent.Target;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import utilities.Driver;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import static utilities.EyesResults.displayVisualValidationResults;
import static utilities.Wait.waitFor;

public class ClassicRunnerMindValleyVideoTest {

    private static final String appName = ClassicRunnerMindValleyVideoTest.class.getSimpleName();
    private static final String userName = System.getProperty("user.name");
    private static final String APPLITOOLS_API_KEY = System.getenv("APPLITOOLS_API_KEY");
    private static EyesRunner visualGridRunner;
    private static BatchInfo batch;
    private Eyes eyes;
    private WebDriver driver;

    @BeforeAll
    public static void beforeSuite() {
        System.out.println("BeforeSuite");
        visualGridRunner = new ClassicRunner();
        visualGridRunner.setDontCloseBatches(true);
        batch = new BatchInfo(userName + "-" + appName);
        batch.setNotifyOnCompletion(false);
        batch.setSequenceName(ClassicRunnerMindValleyVideoTest.class.getSimpleName());
        batch.addProperty("REPOSITORY_NAME", new File(System.getProperty("user.dir")).getName());
        batch.addProperty("APP_NAME", appName);
    }

    @AfterAll
    public static void afterSuite() {
        System.out.println("AfterSuite");
        if (null != visualGridRunner) {
            System.out.println("Closing VisualGridRunner");
            visualGridRunner.close();
        }
        if (null != batch) {
            System.out.println("Mark batch completed");
            batch.setCompleted(true);
        }
    }

    @BeforeEach
    public void beforeMethod(TestInfo testInfo) {
        System.out.println("BeforeTest: Test: " + testInfo.getDisplayName());
        driver = Driver.create();

        eyes = new Eyes(visualGridRunner);
        Configuration config = new Configuration();

        config.setApiKey(APPLITOOLS_API_KEY);
        config.setBatch(batch);
        config.setIsDisabled(false);
        config.setSaveNewTests(false);
        config.setMatchLevel(MatchLevel.STRICT);
        config.addProperty("username", userName);
        eyes.setConfiguration(config);
        eyes.setLogHandler(new StdoutLogHandler(true));

        eyes.open(driver, appName, testInfo.getDisplayName(), new RectangleSize(1400, 1080));
    }

    @AfterEach
    void afterMethod(TestInfo testInfo) {
        System.out.println("AfterMethod: Test: " + testInfo.getDisplayName());
        AtomicBoolean isPass = new AtomicBoolean(true);
        if (null != eyes) {
            eyes.closeAsync();
            TestResultsSummary allTestResults = visualGridRunner.getAllTestResults(false);
            allTestResults.forEach(testResultContainer -> {
                System.out.printf("Test: %s\n%s%n", testResultContainer.getTestResults().getName(), testResultContainer);
                displayVisualValidationResults(testResultContainer.getTestResults());
                TestResultsStatus testResultsStatus = testResultContainer.getTestResults().getStatus();
                if (testResultsStatus.equals(TestResultsStatus.Failed) || testResultsStatus.equals(TestResultsStatus.Unresolved)) {
                    isPass.set(false);
                }
            });
        }
        if (null != driver) {
            driver.quit();
        }
        Assertions.assertTrue(isPass.get(), "Visual differences found.");
    }

    @Test
    void playVideoTest() {
        driver.get("https://login.mindvalley.com/login");
        waitFor(2);
        driver.findElement(By.id("login-tab")).click();
        waitFor(1);
        eyes.check("Login", Target.window().fully(false));

        driver.findElement(By.id("login-email")).sendKeys(System.getenv("MINDVALLEY_EMAIL"));
        driver.findElement(By.id("login-password")).sendKeys(System.getenv("MINDVALLEY_PASSWORD"));
        eyes.check("Login creds entered", Target.window().fully(false));

        driver.findElement(By.id("btn-login")).click();
        waitFor(4);
        eyes.check("After login", Target.window().fully(false).layout());

        try {
            WebElement gotItPopup = driver.findElement(By.xpath("//span[contains(text(), 'Got it')]"));
            gotItPopup.click();
            waitFor(2);
        } catch (NoSuchElementException e) {
            System.out.println("pop up is not shown");
        }

        driver.findElement(By.xpath("//div[@x-show='showVideoOverlay']//span")).click();
        waitFor(2);

        String videoLocation = "document.querySelector('video')";

        for (WebElement video : driver.findElements(By.tagName("video"))) {
            // Pause video and get details
            ((JavascriptExecutor) driver).executeScript(videoLocation + ".pause();");
            double videoLength = (double) ((JavascriptExecutor) driver).executeScript("return " + videoLocation + ".duration;");
            System.out.println("Video length: " + videoLength);

            double duration= 0.15;
            String state = "First Frame";
            System.out.println("Set to " + duration*100 + "% duration " + state);
            ((JavascriptExecutor) driver).executeScript(videoLocation + ".currentTime = " + videoLength * duration + ";");
            waitFor(2);
            eyes.check(state + "-viewport", Target.window().fully(false).layout());
            eyes.checkRegion(By.cssSelector("video"), state + "-css");

            state = "Middle Frame";
            System.out.println("Set to " + state);
            ((JavascriptExecutor) driver).executeScript(videoLocation + ".currentTime = " + videoLength / 2 + ";");
            waitFor(2);
            eyes.check(state + "-viewport", Target.window().fully(false).layout());
            eyes.checkRegion(By.cssSelector("video"), state + "-css");

            state = "Last Frame";
            duration= 0.95;
            System.out.println("Set to " + duration*100 + "% duration " + state);
            ((JavascriptExecutor) driver).executeScript(videoLocation + ".currentTime = " + videoLength * duration + ";");
            waitFor(2);
            eyes.check(state + "-viewport", Target.window().fully(false).layout());
            eyes.checkRegion(By.cssSelector("video"), state + "-css");
        }
    }
}
