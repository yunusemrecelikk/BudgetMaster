package de.deadlocker8.budgetmaster.databasemigrator;

import de.deadlocker8.budgetmaster.databasemigrator.destination.StepNames;
import de.deadlocker8.budgetmaster.databasemigrator.destination.repeating.end.DestinationRepeatingEndAfterXTimes;
import de.deadlocker8.budgetmaster.databasemigrator.destination.repeating.end.DestinationRepeatingEndAfterXTimesIntegerRepository;
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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Import(MigrateRepeatingEndAfterXTimesTest.TestDatabaseConfiguration.class)
@EnableAutoConfiguration
class MigrateRepeatingEndAfterXTimesTest extends MigratorTestBase
{
	@TestConfiguration
	static class TestDatabaseConfiguration
	{
		@Value("classpath:transactions.mv.db")
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
	private DestinationRepeatingEndAfterXTimesIntegerRepository repeatingEndAfterXTimesRepository;

	@Test
	void test_stepMigrateRepeatingEndAfterXTimes()
	{
		final JobExecution jobExecution = jobLauncherTestUtils.launchStep(StepNames.REPEATING_END_AFTER_X_TIMES, DEFAULT_JOB_PARAMETERS);
		final List<StepExecution> stepExecutions = new ArrayList<>(jobExecution.getStepExecutions());

		assertThat(jobExecution.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

		assertThat(stepExecutions).hasSize(1);
		final StepExecution stepExecution = stepExecutions.get(0);
		assertThat(stepExecution.getReadCount()).isEqualTo(4);
		assertThat(stepExecution.getCommitCount()).isEqualTo(5);

		final DestinationRepeatingEndAfterXTimes endAfterXTimes = new DestinationRepeatingEndAfterXTimes(6, 2);

		final List<DestinationRepeatingEndAfterXTimes> repeatingEndAfterXTimes = repeatingEndAfterXTimesRepository.findAll();
		assertThat(repeatingEndAfterXTimes)
				.hasSize(4)
				.contains(endAfterXTimes);
	}
}