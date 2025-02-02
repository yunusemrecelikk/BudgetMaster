package de.deadlocker8.budgetmaster.migration;

import de.deadlocker8.budgetmaster.authentication.UserService;
import de.deadlocker8.budgetmaster.controller.BaseController;
import de.deadlocker8.budgetmaster.database.DatabaseService;
import de.deadlocker8.budgetmaster.settings.SettingsService;
import de.deadlocker8.budgetmaster.utils.Mappings;
import de.deadlocker8.budgetmaster.utils.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;


@Controller
@RequestMapping(Mappings.MIGRATION)
public class MigrationController extends BaseController
{
	private static class ModelAttributes
	{
		public static final String ERROR = "error";
		public static final String MIGRATION_SETTINGS = "migrationSettings";
		public static final String STATUS = "status";
		public static final String SUMMARY = "summary";
		public static final String DATABASE_TYPES = "databaseTypes";
	}

	private static class ReturnValues
	{
		public static final String MIGRATION_SETTINGS = "migration/migration";
		public static final String REDIRECT_STATUS = "redirect:migration/status";
		public static final String STATUS = "migration/status";
		public static final String STATUS_FRAGMENT = "migration/statusFragment";
	}

	private final MigrationService migrationService;
	private final SettingsService settingsService;
	private final UserService userService;
	private final DatabaseService databaseService;

	@Autowired
	public MigrationController(MigrationService migrationService, SettingsService settingsService, UserService userService, DatabaseService databaseService)
	{
		this.migrationService = migrationService;
		this.settingsService = settingsService;
		this.userService = userService;
		this.databaseService = databaseService;
	}

	@GetMapping("/cancel")
	public String cancel(HttpServletRequest request)
	{
		settingsService.updateMigrationDeclined(true);
		return "redirect:" + request.getHeader("Referer");
	}

	@GetMapping
	public String migrate(Model model)
	{
		model.addAttribute(ModelAttributes.MIGRATION_SETTINGS, migrationService.getPrefilledMigrationSettings());
		model.addAttribute(ModelAttributes.DATABASE_TYPES, List.of(DatabaseType.values()));
		return ReturnValues.MIGRATION_SETTINGS;
	}

	@PostMapping
	public String post(Model model,
					   @ModelAttribute("MigrationSettings") @Valid MigrationSettings migrationSettings, BindingResult bindingResult,
					   @RequestParam(value = "databaseTypeName") String databaseTypeName,
					   @RequestParam(value = "verificationPassword") String verificationPassword)
	{
		migrationSettings.setDatabaseType(DatabaseType.fromName(databaseTypeName));
		final MigrationSettingsValidator migrationSettingsValidator = new MigrationSettingsValidator();
		migrationSettingsValidator.validate(migrationSettings, bindingResult);

		final boolean isVerificationPasswordValid = userService.isPasswordValid(verificationPassword);
		if(!isVerificationPasswordValid)
		{
			final FieldError verificationError = new FieldError("MigrationSettings", "verificationPassword", verificationPassword, false, new String[]{Strings.WARNING_WRONG_MIGRATION_VERIFICATION_PASSWORD}, null, Strings.WARNING_WRONG_MIGRATION_VERIFICATION_PASSWORD);
			bindingResult.addError(verificationError);
		}

		model.addAttribute(ModelAttributes.MIGRATION_SETTINGS, migrationSettings);

		if(bindingResult.hasErrors())
		{
			model.addAttribute(ModelAttributes.ERROR, bindingResult);
			model.addAttribute(ModelAttributes.DATABASE_TYPES, List.of(DatabaseType.values()));
			return ReturnValues.MIGRATION_SETTINGS;
		}

		final MigrationArguments migrationArguments = new MigrationArguments.MigrationArgumentBuilder()
				.withSourceUrl(migrationService.getDatabaseFromPreviousVersionPathWithoutExtension().toString())
				.withDestinationUrl(migrationSettings.databaseType(), migrationSettings.hostname(), migrationSettings.port(), migrationSettings.databaseName())
				.withDestinationCredentials(migrationSettings.username(), migrationSettings.password())
				.build();
		migrationService.startMigration(migrationArguments);

		return ReturnValues.REDIRECT_STATUS;
	}

	@GetMapping("/status")
	public String status(Model model)
	{
		model.addAttribute(ModelAttributes.STATUS, migrationService.getMigrationStatus());
		return ReturnValues.STATUS;
	}

	@GetMapping("/getStatus")
	public String getMigrationStatus(Model model)
	{
		final MigrationStatus migrationStatus = migrationService.getMigrationStatus();
		model.addAttribute(ModelAttributes.STATUS, migrationStatus);

		if(migrationStatus == MigrationStatus.SUCCESS)
		{
			model.addAttribute(ModelAttributes.SUMMARY, migrationService.getSummary());
			databaseService.createDefaults();
		}
		if(migrationStatus == MigrationStatus.ERROR)
		{
			model.addAttribute(ModelAttributes.SUMMARY, migrationService.getCollectedStdout());
		}

		return ReturnValues.STATUS_FRAGMENT;
	}
}