package wdutil.ui.automation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class AutomationTest {

	private static Properties tenantProperties;

	@BeforeAll
	public static void init() throws Exception {
		tenantProperties = new Properties();
		tenantProperties.load(Files.newInputStream(Paths.get("tenant.properties")));

	}

	private static WebDriver newDriver() throws Exception {
		if (tenantProperties.containsKey("chromedriver.path")) {
			return AutomationUtil.chromeDriver(Paths.get(tenantProperties.getProperty("chromedriver.path")), Paths.get(tenantProperties.getProperty("fileDownloadPath")));
		} else if (tenantProperties.containsKey("geckodriver.path")) {
			return AutomationUtil.fireFoxDriver(Paths.get(tenantProperties.getProperty("geckodriver.path")), Paths.get(tenantProperties.getProperty("fileDownloadPath")));
		} else {
			throw new Exception("webdriver path is missing");
		}
	}

	private static WebElement login(AutomationUtil util) throws Exception {
		return util.workdayLogin(tenantProperties.getProperty("workday.url"), tenantProperties.getProperty("workday.username"), tenantProperties.getProperty("workday.password"), Boolean.parseBoolean(tenantProperties.getProperty("workday.trustedDevice")));
	}

	//@Test
	public void integrationScreenshot() throws Exception {
		AutomationUtil util = new AutomationUtil(newDriver());
		try {
			WebElement globalSearch = login(util);

			String integrationPrefix = tenantProperties.getProperty("integrationScreenshot.prefix");
			WebElement intLink = util.search(globalSearch, "intsys", integrationPrefix);
			// intLink.click();
			util.relatedAction(intLink, "Integration", "Launch / Schedule");

			TakesScreenshot takeScreenShot = ((TakesScreenshot) util.getDriver());
			byte[] screenShot = takeScreenShot.getScreenshotAs(OutputType.BYTES);
			Files.write(Paths.get(tenantProperties.getProperty("integrationScreenshot.file")), screenShot);
		} finally {
			util.getDriver().close();
		}
		assertTrue(Files.exists(Paths.get(tenantProperties.getProperty("integrationScreenshot.file"))));
	}

	//@Test
	public void xpressoEnrollmentCount() throws Exception {
		AutomationUtil util = new AutomationUtil(newDriver());
		try {

			WebElement globalSearch = login(util);

			util.search(globalSearch, "Enrollment Count").click();

			// util.selectOption("Benefit Group", "U.S. Hourly", "Aus");
			// WebElement active = util.searchOptionByGroup("Benefit Group", "Benefit Groups Available");
			// active.sendKeys(Keys.chord(Keys.CONTROL, "a")); active.sendKeys(Keys.ENTER);
			util.selectSearchOption("Benefit Group", "U.S. Hourly", "Aus", "Ret");
			util.clickOk();

			Path excelFile = util.clickExcelDownload(Paths.get(tenantProperties.getProperty("fileDownloadPath")), 60000);
			assertTrue(Files.exists(excelFile));
		} finally {
			util.getDriver().close();
		}

	}

	private class IntegrationAuditRowHandler implements Consumer<WebElement> {
		private String lastIntegration;
		private int rowCount = 0;

		@Override
		public void accept(WebElement row) {
			rowCount++;
			// System.out.format("row id: %d %s %s \n", rowCount, row.getAttribute("rowid"), row.getAttribute("data-automation-id"));
			List<WebElement> firstRowValue = row.findElements(By.xpath("./td[1]//div[@data-automation-label]"));
			if (!firstRowValue.isEmpty()) {
				lastIntegration = firstRowValue.get(0).getText();
			}
			String severity = row.findElement(By.xpath("./td[2]//div[@data-automation-label]")).getText();
			String problem = row.findElement(By.xpath("./td[3]//div[@data-automation-label]")).getText();
			String solution = row.findElement(By.xpath("./td[4]//div[@class='gwt-HTML']")).getText().replace("\n", "");
			System.out.format("\"%s\",\"%s\",\"%s\",\"%s\"\n", lastIntegration, severity, problem, solution);
			rowCount++;
		}

	}

	//@Test
	public void xpressoIntegrationAudit() throws Exception {
		AutomationUtil util = new AutomationUtil(newDriver(), 120);
		try {
			WebElement globalSearch = login(util);

			util.search(globalSearch, "Integration Exception Audit").click();
			util.clickOk();

			IntegrationAuditRowHandler rowHandler = new IntegrationAuditRowHandler();
			util.scrollTable(rowHandler);
			//System.out.format("Processed %d rows\n", rowHandler.rowCount);
			assertTrue(rowHandler.rowCount > 0);
		} finally {
			util.getDriver().close();
		}

	}

	//@Test
	public void deleteUnusedCalcFields() throws Exception {
		AutomationUtil util = new AutomationUtil(newDriver(), 60);
		try {
			WebElement globalSearch = login(util);
			util.search(globalSearch, "Delete Calculated Field").click();
			util.getWait().until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//label[text()='Calculated Field for Delete']")));
			String deleteUrl = util.getDriver().getCurrentUrl();
			String search = tenantProperties.getProperty("deleteUnusedCalcFields.prefix");
			final AtomicBoolean finished = new AtomicBoolean(false);

			Consumer<List<WebElement>> resultHandler = w -> {
				if (!w.isEmpty()) {
					w.get(0).click();
				} else {
					finished.set(true);
				}
			};
			while (!finished.get()) {
				util.selectSearchOption("Calculated Field for Delete", resultHandler, search);
				util.clickOk();
				util.getWait().until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[text()='Are you sure you want to delete:']")));
				util.clickOk();
				util.getWait().until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[text()='The instance has been deleted.']")));
				util.getDriver().get(deleteUrl);
				// driver.navigate().back();
				// util.clickOk();
			}

		} finally {
			util.getDriver().close();
		}
	}

	//@Test
	public void createPositions() throws Exception {
		AutomationUtil util = new AutomationUtil(newDriver(), 60);
		try {
			login(util);
			util.startProxy("tserrano");
			WebElement globalSearch = util.findSearch();
			util.search(globalSearch, "Create Position").click();
			util.selectSearchOption("Supervisory Organization", "AD IT Helpdesk");
			util.clickOk();

			// close the current open positions popup so it doesn't interfer with the prompt input automation.
			util.getWait().until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@data-automation-id='closeButton']"))).click();
			util.searchOptionByGroup("Position Request Reason", "Create Position > Position Request", "Create Position > Position Request > Staff New Project");

			util.enterText("Job Posting Title", "Automation Test");
			LocalDate date = LocalDate.now().withDayOfMonth(1);
			util.enterDate("Availability Date", date);
			util.enterDate("Earliest Hire Date", date);

			util.enterCheckbox("No Job Restrictions", true);

			util.clickSubmit();
			util.clickDone();

			util.startProxy("jtaylor");
			util.openInbox();

			//util.getWait().until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@data-automation-id='closeButton']"))).click();

			//approve position
			//util.clickApprove();

			//util.clickDone();

			util.stopProxy();
		} finally {
			util.getDriver().close();
		}

	}

}
