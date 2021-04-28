package de.deadlocker8.budgetmaster.services;

import de.deadlocker8.budgetmaster.accounts.Account;
import de.deadlocker8.budgetmaster.categories.Category;
import de.deadlocker8.budgetmaster.categories.CategoryRepository;
import de.deadlocker8.budgetmaster.categories.CategoryType;
import de.deadlocker8.budgetmaster.charts.Chart;
import de.deadlocker8.budgetmaster.charts.ChartService;
import de.deadlocker8.budgetmaster.database.Database;
import de.deadlocker8.budgetmaster.database.accountmatches.AccountMatch;
import de.deadlocker8.budgetmaster.database.accountmatches.AccountMatchList;
import de.deadlocker8.budgetmaster.images.Image;
import de.deadlocker8.budgetmaster.images.ImageService;
import de.deadlocker8.budgetmaster.repeating.RepeatingTransactionUpdater;
import de.deadlocker8.budgetmaster.tags.Tag;
import de.deadlocker8.budgetmaster.tags.TagRepository;
import de.deadlocker8.budgetmaster.templates.Template;
import de.deadlocker8.budgetmaster.templates.TemplateRepository;
import de.deadlocker8.budgetmaster.transactions.Transaction;
import de.deadlocker8.budgetmaster.transactions.TransactionBase;
import de.deadlocker8.budgetmaster.transactions.TransactionRepository;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.*;

@Service
public class ImportService
{
	private static final Logger LOGGER = LoggerFactory.getLogger(ImportService.class);

	private final CategoryRepository categoryRepository;
	private final TransactionRepository transactionRepository;
	private final TemplateRepository templateRepository;
	private final TagRepository tagRepository;
	private final ChartService chartService;
	private final ImageService imageService;
	private final RepeatingTransactionUpdater repeatingTransactionUpdater;

	private Database database;

	@Autowired
	public ImportService(CategoryRepository categoryRepository, TransactionRepository transactionRepository, TemplateRepository templateRepository,
						 TagRepository tagRepository, ChartService chartService, ImageService imageService, RepeatingTransactionUpdater repeatingTransactionUpdater)
	{
		this.categoryRepository = categoryRepository;
		this.transactionRepository = transactionRepository;
		this.templateRepository = templateRepository;
		this.tagRepository = tagRepository;
		this.chartService = chartService;
		this.imageService = imageService;
		this.repeatingTransactionUpdater = repeatingTransactionUpdater;
	}

	public Map<EntityType, Integer> importDatabase(Database database, AccountMatchList accountMatchList, Boolean importTemplates, Boolean importCharts)
	{
		this.database = database;

		final Map<EntityType, Integer> numberOfImportedEntitiesByType = new LinkedHashMap<>();

		LOGGER.debug("Importing database...");
		numberOfImportedEntitiesByType.put(EntityType.CATEGORY, importCategories());
		numberOfImportedEntitiesByType.put(EntityType.ACCOUNT, importAccounts(accountMatchList));
		numberOfImportedEntitiesByType.put(EntityType.TRANSACTION, importTransactions());

		numberOfImportedEntitiesByType.put(EntityType.IMAGE, importImages());

		if(importTemplates)
		{
			numberOfImportedEntitiesByType.put(EntityType.TEMPLATE, importTemplates());
		}
		else
		{
			numberOfImportedEntitiesByType.put(EntityType.TEMPLATE, 0);
		}

		if(importCharts)
		{
			numberOfImportedEntitiesByType.put(EntityType.CHART, importCharts());
		}
		else
		{
			numberOfImportedEntitiesByType.put(EntityType.CHART, 0);
		}


		LOGGER.debug("Updating repeating transactions...");
		repeatingTransactionUpdater.updateRepeatingTransactions(DateTime.now());

		LOGGER.debug("Importing database DONE");

		return numberOfImportedEntitiesByType;
	}

	public Database getDatabase()
	{
		return database;
	}

	private Integer importCategories()
	{
		List<Category> categories = database.getCategories();
		LOGGER.debug(MessageFormat.format("Importing {0} categories...", categories.size()));
		List<TransactionBase> alreadyUpdatedTransactions = new ArrayList<>();
		List<TransactionBase> alreadyUpdatedTemplates = new ArrayList<>();

		for(Category category : categories)
		{
			LOGGER.debug(MessageFormat.format("Importing category {0}", category.getName()));
			int oldCategoryID = category.getID();
			int newCategoryID = importCategory(category);

			if(oldCategoryID == newCategoryID)
			{
				continue;
			}

			List<TransactionBase> transactions = new ArrayList<>(database.getTransactions());
			transactions.removeAll(alreadyUpdatedTransactions);
			alreadyUpdatedTransactions.addAll(updateCategoriesForItems(transactions, oldCategoryID, newCategoryID));

			List<TransactionBase> templates = new ArrayList<>(database.getTemplates());
			templates.removeAll(alreadyUpdatedTemplates);
			alreadyUpdatedTemplates.addAll(updateCategoriesForItems(templates, oldCategoryID, newCategoryID));
		}

		LOGGER.debug("Importing categories DONE");
		return categories.size();
	}

	private int importCategory(Category category)
	{
		Category existingCategory;
		if(category.getType().equals(CategoryType.NONE) || category.getType().equals(CategoryType.REST))
		{
			existingCategory = categoryRepository.findByType(category.getType());
		}
		else
		{
			existingCategory = categoryRepository.findByNameAndColorAndType(category.getName(), category.getColor(), category.getType());
		}

		int newCategoryID;
		if(existingCategory == null)
		{
			//category does not exist --> create it
			Category categoryToCreate = new Category(category.getName(), category.getColor(), category.getType(), category.getIcon());
			categoryRepository.save(categoryToCreate);

			Category newCategory = categoryRepository.findByNameAndColorAndType(category.getName(), category.getColor(), category.getType());
			newCategoryID = newCategory.getID();
		}
		else
		{
			//category already exists
			newCategoryID = existingCategory.getID();
		}
		return newCategoryID;
	}

	public List<TransactionBase> updateCategoriesForItems(List<TransactionBase> items, int oldCategoryID, int newCategoryID)
	{
		List<TransactionBase> updatedItems = new ArrayList<>();
		for(TransactionBase item : items)
		{
			final Category category = item.getCategory();
			if(category == null)
			{
				continue;
			}

			if(category.getID() == oldCategoryID)
			{
				category.setID(newCategoryID);
				updatedItems.add(item);
			}
		}

		return updatedItems;
	}

	private Integer importAccounts(AccountMatchList accountMatchList)
	{
		LOGGER.debug(MessageFormat.format("Importing {0} accounts...", accountMatchList.getAccountMatches().size()));
		List<TransactionBase> alreadyUpdatedTransactions = new ArrayList<>();
		List<TransactionBase> alreadyUpdatedTransferTransactions = new ArrayList<>();
		List<TransactionBase> alreadyUpdatedTemplates = new ArrayList<>();
		List<TransactionBase> alreadyUpdatedTransferTemplates = new ArrayList<>();

		for(AccountMatch accountMatch : accountMatchList.getAccountMatches())
		{
			LOGGER.debug(MessageFormat.format("Importing account {0} -> {1}", accountMatch.getAccountSource().getName(), accountMatch.getAccountDestination().getName()));

			List<TransactionBase> transactions = new ArrayList<>(database.getTransactions());
			transactions.removeAll(alreadyUpdatedTransactions);
			alreadyUpdatedTransactions.addAll(updateAccountsForItems(transactions, accountMatch.getAccountSource().getID(), accountMatch.getAccountDestination()));

			List<TransactionBase> transferTransactions = new ArrayList<>(database.getTransactions());
			transferTransactions.removeAll(alreadyUpdatedTransferTransactions);
			alreadyUpdatedTransferTransactions.addAll(updateTransferAccountsForItems(transferTransactions, accountMatch.getAccountSource().getID(), accountMatch.getAccountDestination()));

			List<TransactionBase> templates = new ArrayList<>(database.getTemplates());
			templates.removeAll(alreadyUpdatedTemplates);
			alreadyUpdatedTemplates.addAll(updateAccountsForItems(templates, accountMatch.getAccountSource().getID(), accountMatch.getAccountDestination()));

			List<TransactionBase> transferTemplates = new ArrayList<>(database.getTemplates());
			transferTemplates.removeAll(alreadyUpdatedTransferTemplates);
			alreadyUpdatedTransferTemplates.addAll(updateTransferAccountsForItems(transferTemplates, accountMatch.getAccountSource().getID(), accountMatch.getAccountDestination()));
		}

		LOGGER.debug("Importing accounts DONE");
		return accountMatchList.getAccountMatches().size();
	}

	public List<TransactionBase> updateAccountsForItems(List<TransactionBase> items, int oldAccountID, Account newAccount)
	{
		List<TransactionBase> updatedTransactions = new ArrayList<>();
		for(TransactionBase item : items)
		{
			// legacy database
			if(oldAccountID == -1)
			{
				item.setAccount(newAccount);
				updatedTransactions.add(item);
				continue;
			}

			if(item.getAccount() == null)
			{
				continue;
			}

			if(item.getAccount().getID() != oldAccountID)
			{
				continue;
			}

			// account needs to be updated
			item.setAccount(newAccount);
			updatedTransactions.add(item);
		}

		return updatedTransactions;
	}

	public List<TransactionBase> updateTransferAccountsForItems(List<TransactionBase> transactions, int oldAccountID, Account newAccount)
	{
		List<TransactionBase> updatedTransactions = new ArrayList<>();
		for(TransactionBase transaction : transactions)
		{
			if(transaction.getTransferAccount() != null && transaction.getTransferAccount().getID() == oldAccountID)
			{
				transaction.setTransferAccount(newAccount);
				updatedTransactions.add(transaction);
			}
		}

		return updatedTransactions;
	}

	private Integer importTransactions()
	{
		List<Transaction> transactions = database.getTransactions();
		LOGGER.debug(MessageFormat.format("Importing {0} transactions...", transactions.size()));
		for(int i = 0; i < transactions.size(); i++)
		{
			Transaction transaction = transactions.get(i);
			LOGGER.debug(MessageFormat.format("Importing transaction {0}/{1} (name: {2}, date: {3})", i + 1, transactions.size(), transaction.getName(), transaction.getDate()));
			updateTagsForItem(transaction);
			transaction.setID(null);
			transactionRepository.save(transaction);
		}
		LOGGER.debug("Importing transactions DONE");
		return transactions.size();
	}

	public void updateTagsForItem(TransactionBase item)
	{
		List<Tag> tags = item.getTags();
		for(int i = 0; i < tags.size(); i++)
		{
			Tag currentTag = tags.get(i);
			Tag existingTag = tagRepository.findByName(currentTag.getName());
			if(existingTag == null)
			{
				final Tag newTag = tagRepository.save(new Tag(currentTag.getName()));
				tags.set(i, newTag);
			}
			else
			{
				tags.set(i, existingTag);
			}
		}
	}

	private Integer importTemplates()
	{
		List<Template> templates = database.getTemplates();
		LOGGER.debug(MessageFormat.format("Importing {0} templates...", templates.size()));
		for(int i = 0; i < templates.size(); i++)
		{
			Template template = templates.get(i);
			LOGGER.debug(MessageFormat.format("Importing template {0}/{1} (templateName: {2})", i + 1, templates.size(), template.getTemplateName()));
			updateTagsForItem(template);
			template.setID(null);
			templateRepository.save(template);
		}
		LOGGER.debug("Importing templates DONE");
		return templates.size();
	}

	private Integer importCharts()
	{
		List<Chart> charts = database.getCharts();
		LOGGER.debug(MessageFormat.format("Importing {0} charts...", charts.size()));
		for(int i = 0; i < charts.size(); i++)
		{
			Chart chart = charts.get(i);
			LOGGER.debug(MessageFormat.format("Importing chart {0}/{1} (name: {2})", i + 1, charts.size(), chart.getName()));

			final int highestUsedID = chartService.getHighestUsedID();
			chart.setID(highestUsedID + 1);

			chartService.getRepository().save(chart);
		}
		LOGGER.debug("Importing charts DONE");
		return charts.size();
	}

	private Integer importImages()
	{
		List<Image> images = database.getImages();
		LOGGER.debug(MessageFormat.format("Importing {0} images...", images.size()));
		List<Account> alreadyUpdatedAccounts = new ArrayList<>();
		List<Template> alreadyUpdatedTemplates = new ArrayList<>();

		for(int i = 0; i < images.size(); i++)
		{
			Image image = images.get(i);
			LOGGER.debug(MessageFormat.format("Importing image {0}/{1} (ID: {2})", i + 1, images.size(), image.getID()));

			// always create new image
			int oldImageID = image.getID();
			Image imageToCreate = new Image(image.getImage(), image.getFileName(), image.getFileExtension());

			final Image savedImage = imageService.getRepository().save(imageToCreate);
			int newImageID = savedImage.getID();

			List<Account> accounts = new ArrayList<>(database.getAccounts());
			accounts.removeAll(alreadyUpdatedAccounts);
			alreadyUpdatedAccounts.addAll(updateImagesForAccounts(accounts, oldImageID, newImageID));

			List<Template> templates = new ArrayList<>(database.getTemplates());
			templates.removeAll(alreadyUpdatedTemplates);
			alreadyUpdatedTemplates.addAll(updateImagesForTemplates(templates, oldImageID, newImageID));
		}

		LOGGER.debug("Importing images DONE");
		return images.size();
	}

	public List<Account> updateImagesForAccounts(List<Account> items, int oldImageId, int newImageID)
	{
		List<Account> updatedItems = new ArrayList<>();
		for(Account item : items)
		{
			final Image image = item.getIcon();
			if(image == null)
			{
				continue;
			}

			if(image.getID() == oldImageId)
			{
				image.setID(newImageID);
				updatedItems.add(item);
			}
		}

		return updatedItems;
	}

	public List<Template> updateImagesForTemplates(List<Template> items, int oldImageId, int newImageID)
	{
		List<Template> updatedItems = new ArrayList<>();
		for(Template item : items)
		{
			final Image image = item.getIcon();
			if(image == null)
			{
				continue;
			}

			if(image.getID() == oldImageId)
			{
				image.setID(newImageID);
				updatedItems.add(item);
			}
		}

		return updatedItems;
	}
}
