package wdutil.ui.automation;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

public class AutomationUtil {

    private WebDriver driver;
    private WebDriverWait wait;
    private Actions actions;

    public AutomationUtil(WebDriver driver) {
        this(driver, Duration.of(20, ChronoUnit.SECONDS));
    }

    public AutomationUtil(WebDriver driver, Duration timeout) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, timeout);
        this.actions = new Actions(driver);
    }

    public WebDriver getDriver() {
        return driver;
    }

    public WebDriverWait getWait() {
        return wait;
    }

    public Actions getActions() {
        return actions;
    }

    public void waitForPageLoad() {
        ExpectedCondition<Boolean> pageLoadCondition = new ExpectedCondition<Boolean>() {
            public Boolean apply(WebDriver driver) {
                return ((JavascriptExecutor) driver).executeScript("return document.readyState").equals("complete");
            }
        };
        wait.until(pageLoadCondition);
    }

    public void workdayLogin(String workdayURL, String userName, String password, boolean skipTrustedDevice) throws Exception {
        driver.get(String.format("%s/login.htmld?redirect=n", workdayURL));

        WebElement userNameBox = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@data-automation-id='userName']/input")));
        userNameBox.sendKeys(userName);

        WebElement passwordBox = driver.findElement(By.xpath("//div[@data-automation-id='password']/input"));
        passwordBox.sendKeys(password);

        WebElement loginButton = driver.findElement(By.xpath("//button[@data-automation-id='goButton']"));
        loginButton.click();

        if (skipTrustedDevice) {
            WebElement skipButton = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//button[@data-automation-id='linkButton' and text() = 'Skip']")));
            skipButton.click();
        }

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@data-automation-id='globalSearchInput']")));

    }

    public WebElement search(String searchValue) {
        return search((String) null, searchValue);
    }

    public WebElement search(String prefix, String searchValue) {
        StringBuilder request = new StringBuilder();
        if (prefix != null) {
            request.append(prefix).append(": ");
        }
        request.append(searchValue).append(Keys.ENTER);

        //search box is re-rendered on text changes so reobtain it after every interaction.
        ExpectedCondition<WebElement> searchCondition = ExpectedConditions.elementToBeClickable(By.xpath("//input[@data-automation-id='globalSearchInput']"));
        WebElement globalSearchBox = wait.until(searchCondition);
        globalSearchBox.clear();
        actions.click(globalSearchBox).pause(250).build().perform();
        globalSearchBox = wait.until(searchCondition);
        globalSearchBox.sendKeys(request.toString());
        By resultXPath = By.xpath(String.format("//div[starts-with(@data-automation-label,'%s')]", searchValue));
        return wait.until(ExpectedConditions.visibilityOfElementLocated(resultXPath));

    }

    public WebElement relatedAction(WebElement baseLink, String... relatedActionPath) {
        actions.moveToElement(baseLink).build().perform();
        WebElement relatedAction = baseLink.findElement(By.xpath("..//img[@data-automation-id='RELATED_TASK_charm']"));
        actions.moveToElement(relatedAction).build().perform();
        relatedAction.click();

        WebElement currentElement = baseLink;
        if (relatedActionPath != null) {
            for (String path : relatedActionPath) {
                currentElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(String.format("//div[@data-automation-id='relatedActionsItemLabel' and @data-automation-label='%s']", path))));
                actions.moveToElement(currentElement).build().perform();
            }
        } else
            throw new IllegalArgumentException("related action path required");
        return currentElement;
    }

    public AutomationUtil clickPromptIcon(String label) {
        WebElement labelElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(String.format("//label[@data-automation-id='formLabel' and text()='%s']", label))));
        WebElement prompt = labelElement.findElement(By.xpath("../..//span[@data-automation-id='promptIcon']"));
        prompt.click();

        return this;
    }

    public AutomationUtil clickPromptSearch(String label) {
        WebElement labelElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(String.format("//label[@data-automation-id='formLabel' and text()='%s']", label))));
        WebElement prompt = labelElement.findElement(By.xpath("../..//span[@data-automation-id='promptSearchButton']"));
        prompt.click();

        return this;
    }

    //

    public List<String> availablePromptOptions(String... optionGroups) throws InterruptedException {
        List<String> availableOptions = new ArrayList<>();
        ExpectedCondition<WebElement> promptCondition = ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@data-automation-activepopup='true']//div[@data-automation-id='activeListContainer'  and //div[@data-automation-id='promptOption']]"));
        WebElement popupElement = wait.until(promptCondition);
        if (optionGroups != null) {
            for (String optionGroup : optionGroups) {
                WebElement optionElement = driver.findElement(By.xpath(String.format("//div[@data-automation-activepopup='true']//div[@data-automation-id='promptOption' and @data-automation-label='%s']", optionGroup)));
                actions.click(optionElement).pause(250).build().perform();
                //waitForLoad();

            }
        }
        while (true) {
            List<WebElement> promptOptions = driver.findElements(By.xpath("//div[@data-automation-activepopup='true']//div[@data-automation-id='promptOption']"));
            List<String> chunkOptions = promptOptions.stream().map(p -> p.getAttribute("data-automation-label")).collect(Collectors.toList());
            int currentSize = availableOptions.size();
            for (String chunkOption : chunkOptions) {
                if (!availableOptions.contains(chunkOption)) {
                    availableOptions.add(chunkOption);
                }
            }
            if (currentSize == availableOptions.size()) {
                break;
            }
            popupElement = wait.until(promptCondition);
            popupElement.sendKeys(Keys.PAGE_DOWN);
            actions.pause(250).build().perform();//No easy way to know when react virtual list has finished refreshing
            //waitForLoad();

        }
        return availableOptions;
    }

    public AutomationUtil clickOption(String option, String... optionGroups) {
        ExpectedCondition<WebElement> promptCondition = ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@data-automation-activepopup='true']//div[@data-automation-id='activeListContainer'  and //div[@data-automation-id='promptOption']]"));
        WebElement popupElement = wait.until(promptCondition);
        List<String> availableOptions = new ArrayList<>();
        if (optionGroups != null) {
            for (String optionGroup : optionGroups) {
                WebElement optionElement = driver.findElement(By.xpath(String.format("//div[@data-automation-activepopup='true']//div[@data-automation-id='promptOption' and @data-automation-label='%s']", optionGroup)));
                actions.click(optionElement).pause(250).build().perform();

            }
        }
        while (true) {
            List<String> chunkOptions = new ArrayList<>();
            int currentSize = availableOptions.size();
            List<WebElement> promptOptions = driver.findElements(By.xpath("//div[@data-automation-activepopup='true']//div[@data-automation-id='promptOption']"));
            for (WebElement promptOption : promptOptions) {
                String promptOptionLabel = promptOption.getAttribute("data-automation-label");
                if (option.contentEquals(promptOptionLabel)) {
                    actions.moveToElement(promptOption).pause(500).click().build().perform();
                    return this;
                } else {
                    chunkOptions.add(promptOptionLabel);
                }
            }

            for (String chunkOption : chunkOptions) {
                if (!availableOptions.contains(chunkOption)) {
                    availableOptions.add(chunkOption);
                }
            }
            if (currentSize == availableOptions.size()) {
                break;
            }
            popupElement.sendKeys(Keys.PAGE_DOWN);
            actions.pause(250).build().perform();

        }

        return this;
    }

    public List<String> availableFileAttachments() {
        By optionXPath = By.xpath("//div[@data-automation-id='fileAttachment']//div[@data-automation-id='promptOption']");
        wait.until(ExpectedConditions.visibilityOfElementLocated(optionXPath));
        List<WebElement> promptOptions = driver.findElements(optionXPath);
        return promptOptions.stream().map(p -> p.getAttribute("data-automation-label")).collect(Collectors.toList());
    }

    //data-automation-id="fileAttachment"

    public AutomationUtil downloadFileAttachment(String option) {
        WebElement popupElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@data-automation-id='fileAttachment']//div[@data-automation-id='promptOption']")));
        WebElement promptOption = popupElement.findElement(By.xpath(String.format("//div[@data-automation-id='promptOption' and @data-automation-label='%s']", option)));
        actions.moveToElement(promptOption).pause(500).click().build().perform();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@data-automation-id='pdfDownloadButton']"))).click();
        return this;
    }

    public void waitForLoad() {
        //wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath("//div[@data-automation-id='wd-LoadingPanel']")));
        //wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath("//div[@data-automation-mode='react' and @aria-busy='true']")));
        //wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath("//div[@data-automation-id='loadingGlass' and @data-automation-loadingpanelhidden='true']"))); 
        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//span[starts-with(@title, 'Loading')]")));
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath("//span[starts-with(@title, 'Loading')]")));
    }

    public AutomationUtil enterText(String label, String value) {
        WebElement labelElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(String.format("//label[text()='%s']", label))));
        WebElement input = labelElement.findElement(By.xpath("../..//input[@data-automation-id='textInputBox']"));
        input.sendKeys(value);
        labelElement.click();
        return this;
    }

    public AutomationUtil enterDate(String label, LocalDate value) {
        WebElement labelElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(String.format("//label[text()='%s']", label))));
        WebElement input = labelElement.findElement(By.xpath("../..//input[@data-automation-id='dateWidgetInputBox']"));
        actions.moveToElement(input).build().perform();
        input.sendKeys(value.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")));
        labelElement.click();
        return this;
    }

    // click on the label so the input is committed
    public AutomationUtil enterCheckbox(String label, boolean value) {
        WebElement labelElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(String.format("//label[text()='%s']", label))));
        WebElement checkbox = labelElement.findElement(By.xpath("../..//div[@data-automation-id='checkboxPanel']"));
        actions.moveToElement(checkbox).build().perform();
        if ((checkbox.getAttribute("checked") != null && !value) || (checkbox.getAttribute("checked") == null && value)) {
            checkbox.click();
        }
        return this;
    }

    public AutomationUtil selectSearchOption(String label, String... searchOption) {
        WebElement labelElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(String.format("//label[text()='%s']", label))));
        WebElement prompt = labelElement.findElement(By.xpath("../..//span[@data-automation-id='promptIcon']"));
        prompt.click();
        waitForLoad();

        if (searchOption != null) {
            for (String option : searchOption) {
                WebElement searchElement = labelElement.findElement(By.xpath("../..//input[@data-automation-id='searchBox']"));
                searchElement.sendKeys(option);
                searchElement.sendKeys(Keys.ENTER);
                waitForLoad();
            }
            // wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//span[@data-automation-id='clearSearchButton']/div"))).click();
            labelElement.click();
        } else
            throw new IllegalArgumentException("search options are required");

        return this;
    }

    public AutomationUtil selectSearchOption(String label, Consumer<List<WebElement>> resultHandler, String... searchOption) {
        WebElement labelElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(String.format("//label[text()='%s']", label))));
        WebElement prompt = labelElement.findElement(By.xpath("../..//span[@data-automation-id='promptIcon']"));
        prompt.click();
        waitForLoad();

        if (searchOption != null) {
            for (String option : searchOption) {
                WebElement searchElement = labelElement.findElement(By.xpath("../..//input[@data-automation-id='searchBox']"));
                searchElement.sendKeys(option);
                searchElement.sendKeys(Keys.ENTER);
                waitForLoad();
                List<WebElement> promptOptions = driver.findElements(By.xpath("//div[@data-automation-activepopup='true']//div[@data-automation-id='promptOption']"));
                try {
                    promptOptions = promptOptions.stream().filter(w -> w.isDisplayed()).collect(Collectors.toList());
                    resultHandler.accept(promptOptions);
                } catch (Throwable t) {
                }

            }
            // wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//span[@data-automation-id='clearSearchButton']/div"))).click();
            //labelElement.click();
        } else
            throw new IllegalArgumentException("search options are required");

        return this;
    }

    public WebElement searchOptionByGroup(String label, String... searchOptionGroups) {
        WebElement labelElement = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(String.format("//label[text()='%s']", label))));
        WebElement prompt = labelElement.findElement(By.xpath("../..//span[@data-automation-id='promptIcon']"));
        prompt.click();

        if (searchOptionGroups != null) {
            for (String searchOptionGroup : searchOptionGroups) {
                wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(String.format("//div[@data-automation-activepopup='true']//div[@data-automation-id='promptOption' and @data-automation-label='%s']", searchOptionGroup)))).click();
                waitForLoad();
            }
            return driver.switchTo().activeElement();

        } else
            throw new IllegalArgumentException("search options are required");

    }

    public void clickOk() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(String.format("//button[@data-automation-button-type='SECONDARY' and @title='OK']")))).click();
    }

    public void clickSubmit() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(String.format("//button[@data-automation-button-type='PRIMARY' and @title='Submit']")))).click();
    }

    public void clickDone() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(String.format("//button[(@data-automation-button-type='PRIMARY' or @data-automation-button-type='SECONDARY') and @title='Done']")))).click();
    }

    public void clickApprove() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(String.format("//button[@data-automation-button-type='PRIMARY' and @title='Approve']")))).click();
    }

    public void clickOpen() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(String.format("//button[@data-automation-button-type='AUXILIARY' and @title='Open']")))).click();
    }

    public Path clickExcelDownload(Path fileDownloadPath, Duration waitTimeout) {
        final long currentTime = System.currentTimeMillis();
        new WebDriverWait(driver, waitTimeout).until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@data-automation-id='excelIconButton']"))).click();
        new WebDriverWait(driver, waitTimeout).until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@data-automation-id='popUpDialog']//button[@data-automation-id='uic_downloadButton']"))).click();

        FluentWait<WebDriver> fileWait = new FluentWait<WebDriver>(driver).withTimeout(waitTimeout).pollingEvery(Duration.ofMillis(200));
        return fileWait.<Path> until((WebDriver wd) -> {
            try {
                Optional<Path> lastFilePath = Files.list(fileDownloadPath).filter(f -> {
                    try {
                        return Files.isDirectory(f) == false && Files.getLastModifiedTime(f).toMillis() > currentTime;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return false;
                    }
                }).findFirst();
                return lastFilePath.orElse(null);
            } catch (Exception e) {
                return null;
            }
        });
    }

    public void startProxy(String user) {
        search(null, "Start Proxy").click();
        // wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpathExpression)));
        selectSearchOption("Act As", user);
        clickOk();
        // This is not optimal but the home page could be the current page prior to the proxy attempt and there would be no difference to explicit wait on
        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void stopProxy() {
        search(null, "Stop Proxy").click();
        clickOk();
        // This is not optimal but the home page could be the current page prior to the proxy attempt and there would be no difference to explicit wait on
        try {
            Thread.sleep(2000);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void openInbox() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//button[@data-automation-id='inbox_preview']"))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@data-automation-id='inboxMainPage']"))).click();

    }

    public void scrollTable(Consumer<WebElement> rowHandler) throws Exception {
        WebElement scrollTable = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@data-automation-id='scrollPanel']")));

        Wait<WebDriver> chunkWait = new FluentWait<WebDriver>(driver).withTimeout(Duration.ofMillis(250)).pollingEvery(Duration.ofMillis(50)).ignoring(NoSuchElementException.class);

        final AtomicInteger chunk = new AtomicInteger(0);
        int pageDownCount = 0;
        while (true) {
            WebElement currentChunk = null;
            try {
                currentChunk = chunkWait.until(d -> scrollTable.findElement(By.xpath(String.format("//table[@data-automation-id='gridChunk-%d']", chunk.get()))));
            } catch (TimeoutException e) {
            }
            if (currentChunk != null) {
                //System.out.format("Processing chunk %d\n", chunk.get());
                List<WebElement> rows = currentChunk.findElements(By.xpath("./tbody/tr"));
                for (WebElement row : rows) {
                    rowHandler.accept(row);

                }
                actions.moveToElement(rows.get(rows.size() - 1)).build().perform();
                chunk.incrementAndGet();
                pageDownCount = 0;

            }
            scrollTable.sendKeys(Keys.PAGE_DOWN);
            By loading = By.xpath("//div[@data-automation-id='wd-LoadingPanel']");
            if (driver.findElements(loading).size() > 0) {
                wait.until(ExpectedConditions.invisibilityOfElementLocated(loading));
            }
            if (++pageDownCount > 10) {
                break;
            }
        }
    }

    public static WebDriver fireFoxDriver(Path driverPath, Path fileDownloadPath) {
        System.setProperty("webdriver.gecko.driver", driverPath.toAbsolutePath().toString());
        FirefoxOptions options = new FirefoxOptions();

        options.addPreference("browser.download.dir", fileDownloadPath.toAbsolutePath().toString());
        options.addPreference("browser.download.folderList", 2);

        options.addPreference("browser.helperApps.neverAsk.saveToDisk", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8");
        return new FirefoxDriver(options);
    }

    @SuppressWarnings("unchecked")
    public abstract static class ChromiumDriverBuilder<T> {

        protected HashMap<String, Object> chromiumPrefs = new HashMap<String, Object>();
        protected List<String> arguments = new LinkedList<>();
        protected final Path driverPath;

        protected ChromiumDriverBuilder(Path driverPath) {
            this.driverPath = driverPath;
        }

        public T downloadPath(Path fileDownloadPath) {
            chromiumPrefs.put("download.default_directory", fileDownloadPath.toAbsolutePath().toString());
            chromiumPrefs.put("download.prompt_for_download", false);
            chromiumPrefs.put("download.directory_upgrade", true);
            chromiumPrefs.put("plugins.always_open_pdf_externally", true);
            return (T) this;
        }

        public T profile(Path userDataDirectory, String profileName) {

            arguments.add("--user-data-dir=" + userDataDirectory.toAbsolutePath().toString());
            if (profileName == null) {
                profileName = "Default";
            }
            arguments.add("--profile-directory=" + profileName);
            return (T) this;
        }

        public T headless(boolean headless, String windowSize) {
            if (headless) {
                arguments.add("--headless");
                if (windowSize == null) {
                    windowSize = "1280x1024";
                }
                arguments.add("--window-size=" + windowSize);
            }
            arguments.add("--start-maximized");
            arguments.add("--disable-infobars");
            arguments.add("--disable-extensions");
            arguments.add("--no-sandbox");
            arguments.add("--disable-dev-shm-usage");
            return (T) this;
        }

        public T automationOptimized(boolean automationOptimized) {
            if (automationOptimized) {
                //https://medium.com/@petertc/pro-tips-for-selenium-setup-1855a11f88f8
                chromiumPrefs.put("profile.managed_default_content_settings.images", 2);
                chromiumPrefs.put("profile.default_content_setting_values.notifications", 2);
                chromiumPrefs.put("profile.managed_default_content_settings.stylesheets", 2);
                //chromePrefs.put("profile.managed_default_content_settings.cookies", 2);
                chromiumPrefs.put("profile.managed_default_content_settings.javascript", 1);
                chromiumPrefs.put("profile.managed_default_content_settings.plugins", 1);
                chromiumPrefs.put("profile.managed_default_content_settings.popups", 2);
                chromiumPrefs.put("profile.managed_default_content_settings.geolocation", 2);
                chromiumPrefs.put("profile.managed_default_content_settings.media_stream", 2);
                //chromiumPrefs.put("disk-cache-size", 4096);
            }
            return (T) this;
        }

    }

    public static class ChromeDriverBuilder extends ChromiumDriverBuilder<ChromeDriverBuilder> {

        private ChromeDriverBuilder(Path driverPath) {
            super(driverPath);
        }

        public static ChromeDriverBuilder newInstance(Path driverPath) {
            return new ChromeDriverBuilder(driverPath);
        }

        public ChromeDriver build() {
            System.setProperty("webdriver.chrome.driver", driverPath.toAbsolutePath().toString());
            ChromeOptions options = new ChromeOptions();
            options.setExperimentalOption("prefs", chromiumPrefs);
            arguments.add("--test-name=workday-ui-automation");
            //https://chromium.googlesource.com/chromium/src/+/master/chrome/common/chrome_switches.cc
            options.addArguments(arguments);
            return new ChromeDriver(options);
        }

    }

    public static class EdgeDriverBuilder extends ChromiumDriverBuilder<EdgeDriverBuilder> {

        private EdgeDriverBuilder(Path driverPath) {
            super(driverPath);
        }

        public static EdgeDriverBuilder newInstance(Path driverPath) {
            return new EdgeDriverBuilder(driverPath);
        }

        public EdgeDriver build() {
            System.setProperty("webdriver.edge.driver", driverPath.toAbsolutePath().toString());
            EdgeOptions options = new EdgeOptions();
            options.setExperimentalOption("prefs", chromiumPrefs);
            arguments.add("--test-name=workday-ui-automation");
            //https://chromium.googlesource.com/chromium/src/+/master/chrome/common/chrome_switches.cc
            options.addArguments(arguments);
            return new EdgeDriver(options);
        }

    }

    public static class DownloadFileWatcher implements AutoCloseable {

        private final WatchService watchService;
        private final Duration timeout;

        public DownloadFileWatcher(Path downloadDirectory) throws IOException {
            this(Duration.ofSeconds(60L), downloadDirectory);
        }

        public DownloadFileWatcher(Duration timeout, Path downloadDirectory) throws IOException {
            this.timeout = timeout;
            watchService = FileSystems.getDefault().newWatchService();

            downloadDirectory.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);

        }

        public Path waitForDownload() throws IOException {
            try {
                boolean tempDeleted = false;
                WatchKey key;
                while ((key = watchService.poll(timeout.toMillis(), TimeUnit.MILLISECONDS)) != null) {
                    try {
                        Path dir = (Path) key.watchable();
                        for (WatchEvent<?> event : key.pollEvents()) {
                            //System.out.println("Event kind:" + event.kind() + " " + event.kind().type() + ". File affected: " + event.context() + ".");
                            Path filePath = (Path) event.context();

                            if (filePath.getFileName().toString().endsWith(".crdownload") && event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                                tempDeleted = true;
                            } else if (tempDeleted && event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) { //download should be complete after temp .crdownload file copied over 
                                return dir.resolve(((WatchEvent<Path>) event).context());
                            }
                        }
                    } finally {
                        key.reset();
                    }
                }
                throw new IOException(String.format("Download file unavailable after %d milliseconds", timeout.toMillis() * 1000));
            } catch (InterruptedException e) {

            }
            return null;
        }

        @Override
        public void close() throws Exception {
            this.watchService.close();

        }

    }

}
