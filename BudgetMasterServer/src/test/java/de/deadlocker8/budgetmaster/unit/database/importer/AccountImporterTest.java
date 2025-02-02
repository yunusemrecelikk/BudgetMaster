package de.deadlocker8.budgetmaster.unit.database.importer;

import de.deadlocker8.budgetmaster.accounts.Account;
import de.deadlocker8.budgetmaster.accounts.AccountRepository;
import de.deadlocker8.budgetmaster.accounts.AccountType;
import de.deadlocker8.budgetmaster.database.accountmatches.AccountMatch;
import de.deadlocker8.budgetmaster.database.accountmatches.AccountMatchList;
import de.deadlocker8.budgetmaster.database.importer.AccountImporter;
import de.deadlocker8.budgetmaster.icon.Icon;
import de.deadlocker8.budgetmaster.icon.IconRepository;
import de.deadlocker8.budgetmaster.services.EntityType;
import de.deadlocker8.budgetmaster.services.ImportResultItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccountImporterTest extends ImporterTestBase
{
	@Override
	List<String> getTableNamesToResetSequence()
	{
		return List.of("account");
	}

	@Autowired
	private IconRepository iconRepository;

	@Autowired
	private AccountRepository accountRepository;

	@Test
	void test_importAccounts()
	{
		final Account sourceAccount = new Account("SourceAccount", AccountType.CUSTOM);
		sourceAccount.setID(2);

		final Account sourceAccount2 = new Account("SourceAccount 2", AccountType.CUSTOM);
		sourceAccount2.setID(7);

		final Account destinationAccount = new Account("DestinationAccount", AccountType.CUSTOM);
		destinationAccount.setID(1);
		accountRepository.save(destinationAccount);

		final Account destinationAccount2 = new Account("DestinationAccount 2", AccountType.CUSTOM);
		destinationAccount2.setID(2);
		accountRepository.save(destinationAccount2);

		final AccountMatch accountMatch = new AccountMatch(sourceAccount);
		accountMatch.setAccountDestination(destinationAccount);

		final AccountMatch accountMatch2 = new AccountMatch(sourceAccount2);
		accountMatch2.setAccountDestination(destinationAccount2);
		final AccountMatchList accountMatchList = new AccountMatchList(List.of(accountMatch, accountMatch2));

		final AccountImporter importer = new AccountImporter(accountRepository, iconRepository);
		final ImportResultItem resultItem = importer.importItems(List.of(sourceAccount, sourceAccount2), accountMatchList);

		final ImportResultItem expected = new ImportResultItem(EntityType.ACCOUNT, 2, 2, List.of());
		assertThat(resultItem).isEqualTo(expected);
		assertThat(sourceAccount).hasFieldOrPropertyWithValue("ID", destinationAccount.getID());
		assertThat(sourceAccount2).hasFieldOrPropertyWithValue("ID", destinationAccount2.getID());
	}

	@Test
	void test_importAccounts_updateIcon()
	{
		Icon icon = new Icon("fas fa-icons");
		icon = iconRepository.save(icon);

		final Account sourceAccount = new Account("SourceAccount", AccountType.CUSTOM);
		sourceAccount.setID(2);
		sourceAccount.setIconReference(icon);

		Icon existingIcon = new Icon("abc");
		existingIcon = iconRepository.save(existingIcon);

		Account destinationAccount = new Account("DestinationAccount", AccountType.CUSTOM);
		destinationAccount.setID(1);
		destinationAccount.setIconReference(existingIcon);
		destinationAccount = accountRepository.save(destinationAccount);

		final AccountMatch accountMatch = new AccountMatch(sourceAccount);
		accountMatch.setAccountDestination(destinationAccount);

		final AccountMatchList accountMatchList = new AccountMatchList(List.of(accountMatch));

		final AccountImporter importer = new AccountImporter(accountRepository, iconRepository);
		final ImportResultItem resultItem = importer.importItems(List.of(sourceAccount), accountMatchList);

		final ImportResultItem expected = new ImportResultItem(EntityType.ACCOUNT, 1, 1, List.of());
		assertThat(resultItem).isEqualTo(expected);
		assertThat(sourceAccount).hasFieldOrPropertyWithValue("ID", destinationAccount.getID());
		assertThat(accountRepository.getById(destinationAccount.getID())).hasFieldOrPropertyWithValue("iconReference", icon);

		assertThat(iconRepository.findAll()).hasSize(1);
	}

	@Test
	void test_skipAlreadyImportedAccounts()
	{
		final Account sourceAccount = new Account("SourceAccount", AccountType.CUSTOM);
		sourceAccount.setID(1);

		final Account sourceAccount2 = new Account("SourceAccount 2", AccountType.CUSTOM);
		sourceAccount2.setID(2);

		final Account destinationAccount = new Account("DestinationAccount", AccountType.CUSTOM);
		destinationAccount.setID(1);
		accountRepository.save(destinationAccount);

		final Account destinationAccount2 = new Account("DestinationAccount 2", AccountType.CUSTOM);
		destinationAccount2.setID(2);
		accountRepository.save(destinationAccount2);

		final AccountMatch accountMatch = new AccountMatch(sourceAccount);
		accountMatch.setAccountDestination(destinationAccount2);

		final AccountMatch accountMatch2 = new AccountMatch(sourceAccount2);
		accountMatch2.setAccountDestination(destinationAccount);
		final AccountMatchList accountMatchList = new AccountMatchList(List.of(accountMatch, accountMatch2));

		final AccountImporter importer = new AccountImporter(accountRepository, iconRepository);
		final ImportResultItem resultItem = importer.importItems(List.of(sourceAccount, sourceAccount2), accountMatchList);

		final ImportResultItem expected = new ImportResultItem(EntityType.ACCOUNT, 2, 2, List.of());
		assertThat(resultItem).isEqualTo(expected);
		assertThat(sourceAccount)
				.hasFieldOrPropertyWithValue("ID", destinationAccount2.getID())
				.hasFieldOrPropertyWithValue("name", destinationAccount2.getName());
		assertThat(sourceAccount2)
				.hasFieldOrPropertyWithValue("ID", destinationAccount.getID())
				.hasFieldOrPropertyWithValue("name", destinationAccount.getName());
	}
}