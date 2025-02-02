package de.deadlocker8.budgetmaster.integration.selenium;

import de.deadlocker8.budgetmaster.accounts.Account;
import de.deadlocker8.budgetmaster.accounts.AccountType;
import de.deadlocker8.budgetmaster.authentication.UserService;
import de.deadlocker8.budgetmaster.integration.helpers.IntegrationTestHelper;
import de.deadlocker8.budgetmaster.integration.helpers.SeleniumTestBase;
import de.deadlocker8.budgetmaster.integration.helpers.TransactionTestHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChangeTransactionTypeTest extends SeleniumTestBase
{
	private static IntegrationTestHelper helper;

	private void openTransferTypeModal(int transactionID)
	{
		driver.get(helper.getUrl() + "/transactions/" + transactionID + "/edit");

		// move cursor away
		Actions builder = new Actions(driver);
		WebElement logo = driver.findElement(By.id("nav-logo"));
		builder.moveToElement(logo).build().perform();

		// move cursor to button
		builder = new Actions(driver);
		WebElement button = driver.findElement(By.id("transaction-actions-button"));
		builder.moveToElement(button).build().perform();

		By changeTypeButtonSelector = By.xpath("//a[contains(@data-action-type, 'changeType')][1]");
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
		wait.until(ExpectedConditions.visibilityOfElementLocated(changeTypeButtonSelector));

		WebElement buttonChangeType = driver.findElement(changeTypeButtonSelector);
		assertThat(buttonChangeType.isDisplayed()).isTrue();

		buttonChangeType.click();
		assertThat(driver.findElement(By.id("modalChangeTransactionType")).isDisplayed()).isTrue();
	}

	@Override
	protected void importDatabaseOnce()
	{
		helper = new IntegrationTestHelper(driver, port);
		helper.start();
		helper.login(UserService.DEFAULT_PASSWORD);
		helper.hideBackupReminder();
		helper.hideWhatsNewDialog();
		helper.hideMigrationDialog();

		String path = getClass().getClassLoader().getResource("SearchDatabase.json").getFile().replace("/", File.separator);
		final Account account1 = new Account("DefaultAccount0815", AccountType.CUSTOM);
		final Account account2 = new Account("Account2", AccountType.CUSTOM);

		helper.uploadDatabase(path, Arrays.asList("DefaultAccount0815", "sfsdf"), List.of(account1, account2));
	}

	@Test
	void test_availableOptions_normal()
	{
		openTransferTypeModal(2);

		final List<WebElement> typeOptions = driver.findElements(By.cssSelector("#newTypeSelect option"));
		assertThat(typeOptions).hasSize(1);
		assertThat(typeOptions.get(0).getAttribute("text")).isEqualTo("Transfer");
	}

	@Test
	void test_availableOptions_transfer()
	{
		openTransferTypeModal(3);

		final List<WebElement> typeOptions = driver.findElements(By.cssSelector("#newTypeSelect option"));
		assertThat(typeOptions).hasSize(1);
		assertThat(typeOptions.get(0).getAttribute("text")).isEqualTo("Transaction");
	}

	@Test
	void test_normal_to_transfer()
	{
		openTransferTypeModal(2);
		TransactionTestHelper.selectOptionFromDropdown(driver, By.cssSelector("#modalChangeTransactionType .select-wrapper"), "Transfer");
		driver.findElement(By.id("buttonChangeTransactionType")).click();

		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
		wait.until(ExpectedConditions.textToBe(By.cssSelector(".headline"), "Edit Transfer"));

		assertThatThrownBy(() -> driver.findElement(By.className("buttonExpenditure"))).isInstanceOf(NoSuchElementException.class);

		assertThat(driver.findElement(By.id("transaction-name")).getAttribute("value")).isEqualTo("Test");
		assertThat(driver.findElement(By.id("transaction-amount")).getAttribute("value")).isEqualTo("15.00");
		assertThat(driver.findElement(By.id("transaction-datepicker")).getAttribute("value")).isEqualTo("01.05.2019");
		assertThat(driver.findElement(By.id("transaction-description")).getAttribute("value")).isEqualTo("Lorem Ipsum");
		assertThat(driver.findElement(By.cssSelector(".category-select-wrapper .custom-select-selected-item .category-circle")).getAttribute("data-value")).isEqualTo("4");

		final List<WebElement> chips = driver.findElements(By.cssSelector("#transaction-chips .chip"));
		assertThat(chips).hasSize(1);
		assertThat(chips.get(0)).hasFieldOrPropertyWithValue("text", "123\nclose");

		assertThat(driver.findElement(By.cssSelector(".account-select-wrapper .custom-select-selected-item .category-circle")).getAttribute("data-value")).isEqualTo("3");
		assertThat(driver.findElement(By.cssSelector(".transfer-account-select-wrapper .custom-select-selected-item .category-circle")).getAttribute("data-value")).isEqualTo("2");
	}
}