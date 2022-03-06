package de.deadlocker8.budgetmaster.integration.selenium;

import de.deadlocker8.budgetmaster.accounts.Account;
import de.deadlocker8.budgetmaster.accounts.AccountType;
import de.deadlocker8.budgetmaster.authentication.UserService;
import de.deadlocker8.budgetmaster.integration.helpers.IntegrationTestHelper;
import de.deadlocker8.budgetmaster.integration.helpers.SeleniumTestBase;
import de.deadlocker8.budgetmaster.integration.helpers.TransactionTestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NewTransactionTransferTest extends SeleniumTestBase
{
	private static IntegrationTestHelper helper;

	@Override
	protected void importDatabaseOnce()
	{
		helper = new IntegrationTestHelper(driver, port);
		helper.start();
		helper.login(UserService.DEFAULT_PASSWORD);
		helper.hideBackupReminder();
		helper.hideWhatsNewDialog();

		String path = getClass().getClassLoader().getResource("SearchDatabase.json").getFile().replace("/", File.separator);
		final Account account1 = new Account("DefaultAccount0815", AccountType.CUSTOM);
		final Account account2 = new Account("Account2", AccountType.CUSTOM);

		helper.uploadDatabase(path, Arrays.asList("DefaultAccount0815", "sfsdf"), List.of(account1, account2));
	}

	@Override
	protected void runBeforeEachTest()
	{
		// open transactions page
		driver.get(helper.getUrl() + "/transactions");
	}

	private void openNewTransactionPage()
	{
		driver.findElement(By.id("button-new-transaction")).click();

		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
		final By locator = By.xpath("//div[contains(@class, 'new-transaction-button')]//a[contains(text(),'Transfer')]");
		wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
		driver.findElement(locator).click();

		wait = new WebDriverWait(driver, Duration.ofSeconds(5));
		wait.until(ExpectedConditions.textToBePresentInElementLocated(By.cssSelector(".headline"), "New Transfer"));
	}

	@Test
	void test_newTransaction_cancel()
	{
		// open transactions page
		driver.get(helper.getUrl() + "/transactions");

		List<WebElement> transactionsRows = driver.findElements(By.cssSelector(".transaction-container .hide-on-med-and-down.transaction-row-top"));
		final int numberOfTransactionsBefore = transactionsRows.size();

		openNewTransactionPage();

		// click cancel button
		WebElement cancelButton = driver.findElement(By.id("button-cancel-save-transaction"));
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", cancelButton);
		cancelButton.click();

		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
		wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[contains(text(), 'Rest')]")));

		// assert
		assertThat(driver.getCurrentUrl()).endsWith("/transactions");

		transactionsRows = driver.findElements(By.cssSelector(".transaction-container .hide-on-med-and-down.transaction-row-top"));
		assertThat(transactionsRows).hasSize(numberOfTransactionsBefore);
	}

	@Test
	void test_newTransaction_transfer()
	{
		openNewTransactionPage();

		String name = "My transfer transaction";
		String amount = "15.00";
		String description = "Lorem Ipsum dolor sit amet";
		String categoryName = "sdfdsf";

		// fill form
		driver.findElement(By.id("transaction-name")).sendKeys(name);
		driver.findElement(By.id("transaction-amount")).sendKeys(amount);
		driver.findElement(By.id("transaction-description")).sendKeys(description);
		TransactionTestHelper.selectCategoryByName(driver, categoryName);

		// submit form
		driver.findElement(By.id("button-save-transaction")).click();

		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
		wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[contains(text(), 'Rest')]")));

		// assert
		assertThat(driver.getCurrentUrl()).endsWith("/transactions");

		List<WebElement> transactionsRows = driver.findElements(By.cssSelector(".transaction-container .hide-on-med-and-down.transaction-row-top"));
		assertThat(transactionsRows).hasSize(2);

		final WebElement row = transactionsRows.get(0);
		final List<WebElement> columns = row.findElements(By.className("col"));
		assertThat(columns).hasSize(6);

		// check columns
		final String dateString = new SimpleDateFormat("dd.MM.").format(new Date());
		TransactionTestHelper.assertTransactionColumns(columns, dateString, categoryName, "rgb(46, 124, 43)", false, true, name, description, amount);
	}

	@Test
	void test_edit()
	{
		driver.get(helper.getUrl() + "/transactions/3/edit");

		assertThatThrownBy(() -> driver.findElement(By.className("buttonExpenditure"))).isInstanceOf(NoSuchElementException.class);

		assertThat(driver.findElement(By.id("transaction-name")).getAttribute("value")).isEqualTo("Transfer dings");
		assertThat(driver.findElement(By.id("transaction-amount")).getAttribute("value")).isEqualTo("3.00");
		assertThat(driver.findElement(By.id("transaction-datepicker")).getAttribute("value")).isEqualTo("01.05.2019");
		assertThat(driver.findElement(By.id("transaction-description")).getAttribute("value")).isEmpty();
		assertThat(driver.findElement(By.cssSelector(".category-select-wrapper .custom-select-selected-item .category-circle")).getAttribute("data-value")).isEqualTo("1");

		final List<WebElement> chips = driver.findElements(By.cssSelector("#transaction-chips .chip"));
		assertThat(chips).hasSize(1);
		assertThat(chips.get(0)).hasFieldOrPropertyWithValue("text", "123\nclose");

		assertThat(driver.findElement(By.cssSelector(".account-select-wrapper .custom-select-selected-item .category-circle")).getAttribute("data-value")).isEqualTo("3");
		assertThat(driver.findElement(By.cssSelector(".transfer-account-select-wrapper .custom-select-selected-item .category-circle")).getAttribute("data-value")).isEqualTo("4");
	}
}