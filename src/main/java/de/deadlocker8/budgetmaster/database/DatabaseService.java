package de.deadlocker8.budgetmaster.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.deadlocker8.budgetmaster.accounts.Account;
import de.deadlocker8.budgetmaster.accounts.AccountService;
import de.deadlocker8.budgetmaster.categories.Category;
import de.deadlocker8.budgetmaster.categories.CategoryService;
import de.deadlocker8.budgetmaster.charts.Chart;
import de.deadlocker8.budgetmaster.charts.ChartService;
import de.deadlocker8.budgetmaster.charts.ChartType;
import de.deadlocker8.budgetmaster.database.model.v7.BackupDatabase_v7;
import de.deadlocker8.budgetmaster.icon.Icon;
import de.deadlocker8.budgetmaster.icon.IconService;
import de.deadlocker8.budgetmaster.images.Image;
import de.deadlocker8.budgetmaster.images.ImageService;
import de.deadlocker8.budgetmaster.repeating.RepeatingOption;
import de.deadlocker8.budgetmaster.settings.SettingsService;
import de.deadlocker8.budgetmaster.tags.TagService;
import de.deadlocker8.budgetmaster.templates.Template;
import de.deadlocker8.budgetmaster.templates.TemplateService;
import de.deadlocker8.budgetmaster.transactions.Transaction;
import de.deadlocker8.budgetmaster.transactions.TransactionService;
import de.thecodelabs.utils.io.PathUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DatabaseService
{
	public static final Gson GSON = new GsonBuilder().create();
	private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseService.class);

	private final AccountService accountService;
	private final CategoryService categoryService;
	private final TransactionService transactionService;
	private final TagService tagService;
	private final TemplateService templateService;
	private final ChartService chartService;
	private final SettingsService settingsService;
	private final ImageService imageService;
	private final IconService iconService;

	@Autowired
	public DatabaseService(AccountService accountService, CategoryService categoryService, TransactionService transactionService, TagService tagService, TemplateService templateService, ChartService chartService, SettingsService settingsService, ImageService imageService, IconService iconService)
	{
		this.accountService = accountService;
		this.categoryService = categoryService;
		this.transactionService = transactionService;
		this.tagService = tagService;
		this.templateService = templateService;
		this.chartService = chartService;
		this.settingsService = settingsService;
		this.imageService = imageService;
		this.iconService = iconService;
	}

	public void reset()
	{
		resetTransactions();
		resetTemplates();
		resetCategories();
		resetAccounts();
		resetTags();
		resetCharts();
		resetImages();
	}

	private void resetAccounts()
	{
		LOGGER.info("Resetting accounts...");
		accountService.deleteAll();
		accountService.createDefaults();
		LOGGER.info("All accounts reset.");
	}

	private void resetCategories()
	{
		LOGGER.info("Resetting categories...");
		categoryService.deleteAll();
		categoryService.createDefaults();
		LOGGER.info("All categories reset.");
	}

	private void resetTransactions()
	{
		LOGGER.info("Resetting transactions...");
		transactionService.deleteAll();
		transactionService.createDefaults();
		LOGGER.info("All transactions reset.");
	}

	private void resetTags()
	{
		LOGGER.info("Resetting tags...");
		tagService.deleteAll();
		tagService.createDefaults();
		LOGGER.info("All tags reset.");
	}

	private void resetTemplates()
	{
		LOGGER.info("Resetting templates...");
		templateService.deleteAll();
		templateService.createDefaults();
		LOGGER.info("All templates reset.");
	}

	private void resetCharts()
	{
		LOGGER.info("Resetting charts...");
		chartService.deleteAll();
		chartService.createDefaults();
		LOGGER.info("All charts reset.");
	}

	private void resetImages()
	{
		LOGGER.info("Resetting images...");
		imageService.deleteAll();
		imageService.createDefaults();
		LOGGER.info("All images reset.");
	}

	public void rotatingBackup(Path backupFolderPath)
	{
		final List<Path> filesToDelete = determineFilesToDelete(backupFolderPath);

		for(Path path : filesToDelete)
		{
			try
			{
				Files.deleteIfExists(path);
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	public List<Path> determineFilesToDelete(Path backupFolderPath)
	{
		final ArrayList<Path> filesToDelete = new ArrayList<>();

		final Integer numberOfFilesToKeep = settingsService.getSettings().getAutoBackupFilesToKeep();
		if(numberOfFilesToKeep == 0)
		{
			LOGGER.debug("Skipping backup rotation since number of files to keep is set to unlimited");
			return filesToDelete;
		}

		final List<String> existingBackups = getExistingBackups(backupFolderPath);
		if(existingBackups.size() < numberOfFilesToKeep)
		{
			LOGGER.debug(MessageFormat.format("Skipping backup rotation (existing backups: {0}, files to keep: {1})", existingBackups.size(), numberOfFilesToKeep));
			return filesToDelete;
		}

		LOGGER.debug(MessageFormat.format("Determining old backups (existing backups: {0}, files to keep: {1})", existingBackups.size(), numberOfFilesToKeep));
		// reserve 1 file for the backup created afterwards
		final int allowedNumberOfFiles = existingBackups.size() - numberOfFilesToKeep + 1;
		for(int i = 0; i < allowedNumberOfFiles; i++)
		{
			final Path oldBackup = Paths.get(existingBackups.get(i));
			LOGGER.debug(MessageFormat.format("Schedule old backup for deletion: {0}", oldBackup));
			filesToDelete.add(oldBackup);
		}

		return filesToDelete;
	}

	public List<String> getExistingBackups(Path backupFolderPath)
	{
		try(Stream<Path> walk = Files.walk(backupFolderPath))
		{
			return walk.filter(Files::isRegularFile)
					.map(Path::toString)
					.filter(path -> path.endsWith(".json"))
					.sorted()
					.collect(Collectors.toList());
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}

		return new ArrayList<>();
	}

	@Transactional
	public void backupDatabase(Path backupFolderPath)
	{
		LOGGER.info("Backup database...");
		PathUtils.createDirectoriesIfNotExists(backupFolderPath);

		rotatingBackup(backupFolderPath);

		final String fileName = getExportFileName(true);
		final Path backupPath = backupFolderPath.resolve(fileName);

		exportDatabase(backupPath);
	}

	@Transactional
	public void exportDatabase(Path backupPath)
	{
		final BackupDatabase_v7 database = getDatabaseForJsonSerialization();

		try(Writer writer = new FileWriter(backupPath.toString()))
		{
			LOGGER.info("Backup database to: {}", backupPath);
			DatabaseService.GSON.toJson(database, writer);
			LOGGER.info("Backup database DONE");
		}
		catch(IOException e)
		{
			LOGGER.error("Failed to backup database", e);
		}
	}

	public static String getExportFileName(boolean includeTime)
	{
		String formatString = "yyyy_MM_dd";
		if(includeTime)
		{
			formatString = "yyyy_MM_dd_HH_mm_ss";
		}

		return "BudgetMasterDatabase_" + DateTime.now().toString(formatString) + ".json";
	}

	public BackupDatabase_v7 getDatabaseForJsonSerialization()
	{
		List<Category> categories = categoryService.getAllEntitiesAsc();
		List<Account> accounts = accountService.getRepository().findAll();
		List<Transaction> transactions = transactionService.getRepository().findAll();
		List<Transaction> filteredTransactions = filterRepeatingTransactions(transactions);
		List<Template> templates = templateService.getRepository().findAll();
		List<Chart> charts = chartService.getRepository().findAllByType(ChartType.CUSTOM);
		List<Image> images = imageService.getRepository().findAll();
		List<Icon> icons = iconService.getRepository().findAll();
		LOGGER.debug(MessageFormat.format("Reduced {0} transactions to {1}", transactions.size(), filteredTransactions.size()));

		InternalDatabase database = new InternalDatabase(categories, accounts, filteredTransactions, templates, charts, images, icons);
		LOGGER.debug(MessageFormat.format("Created database for JSON with {0} transactions, {1} categories, {2} accounts, {3} templates, {4} charts {5} images and {6} icons", database.getTransactions().size(), database.getCategories().size(), database.getAccounts().size(), database.getTemplates().size(), database.getCharts().size(), database.getImages().size(), database.getIcons().size()));

		BackupDatabase_v7 databaseInExternalForm = BackupDatabase_v7.createFromInternalEntities(database);
		LOGGER.debug("Converted database to external form");
		return databaseInExternalForm;
	}

	private List<Transaction> filterRepeatingTransactions(List<Transaction> transactions)
	{
		List<Transaction> filteredTransactions = new ArrayList<>();

		for(Transaction transaction : transactions)
		{
			if(transaction.isRepeating())
			{
				if(isRepeatingOptionInList(transaction.getRepeatingOption(), filteredTransactions))
				{
					continue;
				}
			}

			filteredTransactions.add(transaction);
		}

		return filteredTransactions;
	}

	private boolean isRepeatingOptionInList(RepeatingOption repeatingOption, List<Transaction> transactions)
	{
		for(Transaction transaction : transactions)
		{
			if(transaction.isRepeating())
			{
				if(transaction.getRepeatingOption().equals(repeatingOption))
				{
					return true;
				}
			}
		}
		return false;
	}
}
