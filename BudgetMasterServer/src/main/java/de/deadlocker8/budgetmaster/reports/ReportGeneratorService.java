package de.deadlocker8.budgetmaster.reports;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import de.deadlocker8.budgetmaster.categories.CategoryType;
import de.deadlocker8.budgetmaster.reports.categoryBudget.CategoryBudget;
import de.deadlocker8.budgetmaster.reports.columns.ReportColumn;
import de.deadlocker8.budgetmaster.services.CurrencyService;
import de.deadlocker8.budgetmaster.services.DateFormatStyle;
import de.deadlocker8.budgetmaster.settings.SettingsService;
import de.deadlocker8.budgetmaster.tags.Tag;
import de.deadlocker8.budgetmaster.transactions.Transaction;
import de.deadlocker8.budgetmaster.utils.Strings;
import de.thecodelabs.utils.util.Color;
import de.thecodelabs.utils.util.Localization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.itextpdf.text.BaseColor.BLACK;
import static com.itextpdf.text.BaseColor.LIGHT_GRAY;

@Service
public class ReportGeneratorService
{
	private static final String FONT = Fonts.OPEN_SANS;
	private static final int DEFAULT_SPACING_AFTER_HEADLINES = 10;

	private final CurrencyService currencyService;
	private final SettingsService settingsService;

	@Autowired
	public ReportGeneratorService(CurrencyService currencyService, SettingsService settingsService)
	{
		this.currencyService = currencyService;
		this.settingsService = settingsService;
	}

	private Chapter generateHeader(ReportConfiguration reportConfiguration)
	{
		Font font = FontFactory.getFont(FONT, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 16, Font.BOLDITALIC, BLACK);
		Locale locale = settingsService.getSettings().getLanguage().getLocale();
		Chunk chunk = new Chunk(Localization.getString(Strings.REPORT_HEADLINE, reportConfiguration.getReportSettings().getDate().format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))), font);
		Chapter chapter = new Chapter(new Paragraph(chunk), 1);
		chapter.setNumberDepth(0);

		Font fontAccount = FontFactory.getFont(FONT, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 14, Font.BOLD, BLACK);
		Chunk chunkAccount = new Chunk(Localization.getString(Strings.REPORT_HEADLINE_ACCOUNT, reportConfiguration.getAccountName()), fontAccount);
		chapter.add(chunkAccount);
		chapter.add(Chunk.NEWLINE);
		return chapter;
	}

	@SuppressWarnings({"SameParameterValue", "ConstantConditions"})
	private PdfPTable generateTable(ReportConfiguration reportConfiguration, int tableWidth, AmountType amountType)
	{
		List<ReportColumn> columns = reportConfiguration.getReportSettings().getColumnsSortedAndFiltered();
		int numberOfColumns = columns.size();

		if(numberOfColumns > 0)
		{
			float[] proportions = new float[numberOfColumns];
			for(int i = 0; i < columns.size(); i++)
			{
				ReportColumn column = columns.get(i);
				proportions[i] = ColumnType.getByName(column.getLocalizationKey()).getProportion();
			}

			PdfPTable table = new PdfPTable(proportions);
			table.setWidthPercentage(tableWidth);
			Font font = FontFactory.getFont(FONT, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 8, Font.NORMAL, BLACK);
			Font fontBold = FontFactory.getFont(FONT, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 8, Font.BOLD, BLACK);

			createTableHeader(columns, table, font);

			createTableContent(reportConfiguration, amountType, columns, table, font);

			createTotal(reportConfiguration, amountType, numberOfColumns, table, fontBold);

			return table;
		}
		return null;
	}

	private void createTotal(ReportConfiguration reportConfiguration, AmountType amountType, int numberOfColumns, PdfPTable table, Font fontBold)
	{
		PdfPCell cellTotal;
		String total = "";
		String totalIncomeString = currencyService.getCurrencyString(reportConfiguration.getBudget().getIncomeSum());
		String totalExpenditureString = currencyService.getCurrencyString(reportConfiguration.getBudget().getExpenditureSum());
		switch(amountType)
		{
			case BOTH:
				total = Localization.getString(Strings.REPORT_SUM_TOTAL, totalIncomeString, totalExpenditureString);
				break;
			case INCOME:
				total = Localization.getString(Strings.REPORT_SUM, totalIncomeString);
				break;
			case EXPENDITURE:
				total = Localization.getString(Strings.REPORT_SUM, totalExpenditureString);
				break;
		}

		cellTotal = new PdfPCell(new Phrase(total, fontBold));
		cellTotal.setBackgroundColor(getBaseColor(Color.WHITE));
		cellTotal.setColspan(numberOfColumns);
		cellTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
		cellTotal.setVerticalAlignment(Element.ALIGN_MIDDLE);
		table.addCell(cellTotal);
	}

	private void createTableContent(ReportConfiguration reportConfiguration, AmountType amountType, List<ReportColumn> columns, PdfPTable table, Font font)
	{
		int index = 0;
		for(Transaction currentItem : reportConfiguration.getTransactions())
		{
			if(amountType.equals(AmountType.INCOME) && currentItem.getAmount() <= 0)
			{
				continue;
			}

			if(amountType.equals(AmountType.EXPENDITURE) && currentItem.getAmount() > 0)
			{
				continue;
			}

			index++;

			for(ReportColumn column : columns)
			{
				ColumnType columnType = ColumnType.getByName(column.getLocalizationKey());
				PdfPCell cell = getTransactionTableCell(currentItem, columnType, index, font);
				table.addCell(cell);
			}
		}
	}

	private void createTableHeader(List<ReportColumn> columns, PdfPTable table, Font font)
	{
		for(ReportColumn column : columns)
		{
			ColumnType columnType = ColumnType.getByName(column.getLocalizationKey());

			PdfPCell cell = new PdfPCell(new Phrase(columnType.getName(), font));
			cell.setBackgroundColor(LIGHT_GRAY);
			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			table.addCell(cell);
		}
	}

	private PdfPCell getTransactionTableCell(Transaction transaction, ColumnType columnType, int position, Font font)
	{
		PdfPCell cell = new PdfPCell(new Phrase(getProperty(transaction, columnType, position), font));
		cell.setBackgroundColor(getBaseColor(Color.WHITE));
		cell.setHorizontalAlignment(columnType.getAlignment());
		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		return cell;
	}

	public byte[] generate(ReportConfiguration reportConfiguration) throws DocumentException
	{
		Document document = new Document();
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		PdfWriter writer = PdfWriter.getInstance(document, byteArrayOutputStream);
		writer.setPageEvent(new HeaderFooterPageEvent());
		document.open();
		document.setMargins(50, 45, 25, 70);
		Font headerFont = FontFactory.getFont(FONT, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 14, Font.BOLD, BLACK);
		Font smallHeaderFont = FontFactory.getFont(FONT, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 12, Font.BOLD, BLACK);

		document.add(generateHeader(reportConfiguration));
		document.add(Chunk.NEWLINE);

		if(reportConfiguration.getReportSettings().isIncludeBudget())
		{
			Font fontGreen = FontFactory.getFont(FONT, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 12, Font.NORMAL, new BaseColor(36, 122, 45));
			Font fontRed = FontFactory.getFont(FONT, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 12, Font.NORMAL, BaseColor.RED);
			Font fontBlack = FontFactory.getFont(FONT, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 12, Font.BOLD, BLACK);

			Budget budget = reportConfiguration.getBudget();

			Paragraph paragraph = new Paragraph();
			paragraph.add(new Chunk(Localization.getString(Strings.REPORT_INCOMES) + currencyService.getCurrencyString(budget.getIncomeSum()), fontGreen));
			paragraph.add(new Chunk("     "));
			paragraph.add(new Chunk(Localization.getString(Strings.REPORT_PAYMENTS) + currencyService.getCurrencyString(budget.getExpenditureSum()), fontRed));
			paragraph.add(new Chunk("     "));
			paragraph.add(new Chunk(Localization.getString(Strings.REPORT_BUDGET_REST) + currencyService.getCurrencyString(budget.getIncomeSum() + budget.getExpenditureSum()), fontBlack));
			paragraph.setAlignment(Element.ALIGN_JUSTIFIED);

			final Paragraph paragraphBudgetHeadline = new Paragraph(Localization.getString(Strings.REPORT_BUDGET), headerFont);
			paragraphBudgetHeadline.setSpacingAfter(DEFAULT_SPACING_AFTER_HEADLINES);
			document.add(paragraphBudgetHeadline);
			document.add(paragraph);
			document.add(Chunk.NEWLINE);
		}

		final Paragraph paragraphTransactionsHeadline = new Paragraph(Localization.getString(Strings.REPORT_HEADLINE_TRANSACTIONS_OVERVIEW), headerFont);
		paragraphTransactionsHeadline.setSpacingAfter(DEFAULT_SPACING_AFTER_HEADLINES);
		document.add(paragraphTransactionsHeadline);

		if(reportConfiguration.getReportSettings().isSplitTables())
		{
			final Paragraph paragraphIncomes = new Paragraph(Localization.getString(Strings.TITLE_INCOMES), smallHeaderFont);
			paragraphIncomes.setSpacingAfter(DEFAULT_SPACING_AFTER_HEADLINES);
			document.add(paragraphIncomes);

			PdfPTable table = generateTable(reportConfiguration, 100, AmountType.INCOME);
			if(table != null)
			{
				document.add(table);
			}

			document.add(Chunk.NEWLINE);
			final Paragraph paragraphExpenditures = new Paragraph(Localization.getString(Strings.TITLE_EXPENDITURES), smallHeaderFont);
			paragraphExpenditures.setSpacingAfter(DEFAULT_SPACING_AFTER_HEADLINES);
			document.add(paragraphExpenditures);

			table = generateTable(reportConfiguration, 100, AmountType.EXPENDITURE);
			if(table != null)
			{
				document.add(table);
			}
		}
		else
		{
			PdfPTable table = generateTable(reportConfiguration, 100, AmountType.BOTH);
			if(table != null)
			{
				document.add(table);
			}
		}

		if(reportConfiguration.getReportSettings().isIncludeCategoryBudgets())
		{
			document.add(Chunk.NEWLINE);
			final Paragraph paragraphCategoryConsumptions = new Paragraph(Localization.getString(Strings.TITLE_CATEGORY_BUDGETS), smallHeaderFont);
			paragraphCategoryConsumptions.setSpacingAfter(DEFAULT_SPACING_AFTER_HEADLINES);
			document.add(paragraphCategoryConsumptions);

			PdfPTable table = generateCategoryBudgets(reportConfiguration);
			document.add(table);
		}

		document.close();
		return byteArrayOutputStream.toByteArray();
	}

	private PdfPTable generateCategoryBudgets(ReportConfiguration reportConfiguration)
	{
		PdfPTable table = new PdfPTable(2);
		table.setWidthPercentage(100);
		Font font = FontFactory.getFont(FONT, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, 8, Font.NORMAL, BLACK);

		//header cells
		PdfPCell cellHeaderCategory = new PdfPCell(new Phrase(Localization.getString(Strings.REPORT_CATEGORY), font));
		cellHeaderCategory.setBackgroundColor(LIGHT_GRAY);
		cellHeaderCategory.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cellHeaderCategory);
		PdfPCell cellHeaderAmount = new PdfPCell(new Phrase(Localization.getString(Strings.REPORT_AMOUNT), font));
		cellHeaderAmount.setBackgroundColor(LIGHT_GRAY);
		cellHeaderAmount.setHorizontalAlignment(Element.ALIGN_CENTER);
		table.addCell(cellHeaderAmount);

		for(CategoryBudget budget : reportConfiguration.getCategoryBudgets())
		{
			PdfPCell cellName = new PdfPCell(new Phrase(budget.getCategory().getName(), font));
			cellName.setBackgroundColor(getBaseColor(Color.WHITE));
			cellName.setHorizontalAlignment(Element.ALIGN_CENTER);
			cellName.setVerticalAlignment(Element.ALIGN_MIDDLE);
			table.addCell(cellName);

			PdfPCell cellAmount = new PdfPCell(new Phrase(currencyService.getCurrencyString(budget.getBudget() / 100.0), font));
			cellAmount.setBackgroundColor(getBaseColor(Color.WHITE));
			cellAmount.setHorizontalAlignment(Element.ALIGN_CENTER);
			cellAmount.setVerticalAlignment(Element.ALIGN_MIDDLE);
			table.addCell(cellAmount);
		}

		return table;
	}

	private String getProperty(Transaction transaction, ColumnType columnType, int position)
	{
		switch(columnType)
		{
			case ACCOUNT:
				if(transaction.getCategory().getType().equals(CategoryType.REST))
				{
					return null;
				}
				return transaction.getAccount().getName();
			case AMOUNT:
				return currencyService.getCurrencyString(transaction.getAmount());
			case CATEGORY:
				return transaction.getCategory().getName();
			case DATE:
				return transaction.getDate().format(DateTimeFormatter.ofPattern(DateFormatStyle.NO_YEAR.getKey()));
			case DESCRIPTION:
				return transaction.getDescription();
			case TAGS:
				return transaction.getTags().stream().map(Tag::getName).collect(Collectors.joining(", "));
			case NAME:
				return transaction.getName();
			case POSITION:
				return String.valueOf(position);
			case RATING:
				return transaction.getAmount() > 0 ? "+" : "-";
			case REPEATING:
				if(transaction.isRepeating())
				{
					return Localization.getString(Strings.REPORT_REPEATING_YES);
				}
				else
				{
					return Localization.getString(Strings.REPORT_REPEATING_NO);
				}
			case TRANSFER:
				if(transaction.isTransfer())
				{
					return Localization.getString(Strings.REPORT_REPEATING_YES);
				}
				else
				{
					return Localization.getString(Strings.REPORT_REPEATING_NO);
				}
			default:
				return null;
		}
	}

	@SuppressWarnings("SameParameterValue")
	private BaseColor getBaseColor(Color color)
	{
		return new BaseColor((float) (color.getRed() / 255.0), (float) (color.getGreen() / 255.0), (float) (color.getBlue() / 255.0));
	}
}