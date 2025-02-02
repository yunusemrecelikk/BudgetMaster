package de.deadlocker8.budgetmaster.databasemigrator.destination;

public class TableNames
{
	private TableNames()
	{
		// empty
	}

	public static final String IMAGE = "image";
	public static final String ICON = "icon";
	public static final String CATEGORY = "category";
	public static final String ACCOUNT = "account";
	public static final String CHART = "chart";
	public static final String HINT = "hint";

	public static final String REPEATING_END = "repeating_end";
	public static final String REPEATING_END_AFTER_X_TIMES = "repeating_end_afterxtimes";
	public static final String REPEATING_END_DATE = "repeating_end_date";
	public static final String REPEATING_END_NEVER = "repeating_end_never";

	public static final String REPEATING_MODIFIER = "repeating_modifier";
	public static final String REPEATING_MODIFIER_DAYS = "repeating_modifier_days";
	public static final String REPEATING_MODIFIER_MONTHS = "repeating_modifier_months";
	public static final String REPEATING_MODIFIER_YEARS = "repeating_modifier_years";

	public static final String REPEATING_OPTION = "repeating_option";

	public static final String REPORT_COLUMN = "report_column";
	public static final String REPORT_SETTINGS = "report_settings";

	public static final String SETTINGS = "settings";

	public static final String TAG = "tag";
	public static final String TEMPLATE_TAGS = "template_tags";
	public static final String TRANSACTION_TAGS = "transaction_tags";

	public static final String USER_SOURCE = "user";
	public static final String USER_DESTINATION = "login_user";

	public static final String TRANSACTION = "transaction";
	public static final String TEMPLATE = "template";
	public static final String TEMPLATE_GROUP = "template_group";
}
