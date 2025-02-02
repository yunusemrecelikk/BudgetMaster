package de.deadlocker8.budgetmaster.utils;

import de.deadlocker8.budgetmaster.BudgetMasterServerMain;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.FileSystemResource;

import java.nio.file.Path;

@Configuration
@SuppressWarnings("squid:S1118")
public class PropertiesConfiguration
{
	@Bean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer()
	{
		PropertySourcesPlaceholderConfigurer properties = new PropertySourcesPlaceholderConfigurer();
		Path settingsPath = BudgetMasterServerMain.getApplicationSupportFolder().resolve("settings.properties");
		properties.setLocation(new FileSystemResource(settingsPath.toString()));
		properties.setIgnoreResourceNotFound(false);

		return properties;
	}
}