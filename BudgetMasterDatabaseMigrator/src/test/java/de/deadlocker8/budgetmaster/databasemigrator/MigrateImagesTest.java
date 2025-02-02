package de.deadlocker8.budgetmaster.databasemigrator;

import de.deadlocker8.budgetmaster.databasemigrator.destination.StepNames;
import de.deadlocker8.budgetmaster.databasemigrator.destination.image.DestinationImage;
import de.deadlocker8.budgetmaster.databasemigrator.destination.image.DestinationImageIntegerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Import(MigrateImagesTest.TestDatabaseConfiguration.class)
@EnableAutoConfiguration
class MigrateImagesTest extends MigratorTestBase
{
	@TestConfiguration
	static class TestDatabaseConfiguration
	{
		@Value("classpath:categories.mv.db")
		private Resource databaseResource;

		@Bean(name = "primaryDataSource")
		@Primary
		public DataSource dataSource() throws IOException
		{
			final String folderName = databaseResource.getFile().getAbsolutePath().replace(".mv.db", "");
			String jdbcString = "jdbc:h2:/" + folderName + ";DB_CLOSE_ON_EXIT=TRUE";
			return DataSourceBuilder.create().username("sa").password("").url(jdbcString).driverClassName("org.h2.Driver").build();
		}
	}

	@Autowired
	private DestinationImageIntegerRepository imageRepository;

	@Test
	void test_stepMigrateImages() throws IOException, URISyntaxException
	{
		final JobExecution jobExecution = jobLauncherTestUtils.launchStep(StepNames.IMAGES, DEFAULT_JOB_PARAMETERS);
		final List<StepExecution> stepExecutions = new ArrayList<>(jobExecution.getStepExecutions());

		assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

		assertThat(stepExecutions).hasSize(1);
		final StepExecution stepExecution = stepExecutions.get(0);
		assertThat(stepExecution.getReadCount()).isOne();
		assertThat(stepExecution.getCommitCount()).isEqualTo(2);

		final byte[] imageBytes = Files.readAllBytes(Paths.get(this.getClass().getClassLoader().getResource("BudgetMaster.svg").toURI()));
		final DestinationImage expectedImage = new DestinationImage(1, imageBytes, "BudgetMaster.svg", 2);

		final List<DestinationImage> images = imageRepository.findAll();
		assertThat(images)
				.hasSize(1)
				.containsExactly(expectedImage);
	}
}