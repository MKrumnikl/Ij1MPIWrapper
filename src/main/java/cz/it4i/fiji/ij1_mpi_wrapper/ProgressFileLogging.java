
package cz.it4i.fiji.ij1_mpi_wrapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ProgressFileLogging implements ProgressLogging {

	private Logger logger = Logger.getLogger(MPIWrapper.class.getName());

	private static final String LOG_FILE_PROGRESS_PREFIX = "progress_";
	private static final String LOG_FILE_REPORT_PREFIX = "report_";

	private Map<Integer, String> tasks = new HashMap<>();

	private Integer numberOfTasks = 0;

	private Map<Integer, Integer> lastWrittenTaskPercentage = new HashMap<>();

	private boolean tasksWereReported = false;

	@Override
	public int addTask(String description) {
		// No new tasks should be added after they were reported:
		if (tasksWereReported) {
			logger.warning(
				"addTask call was ignored - No new tasks should be added after they were reported.");
			return -1;
		}

		tasks.put(numberOfTasks, description);
		return numberOfTasks++;
	}

	@Override
	public void reportTasks(int rank, int size) {
		if (tasks.isEmpty()) {
			logger.warning(
				"reportTasks call was ignored, there are no tasks to report.");
			return;
		}

		// The tasks should not be reported twice:
		if (tasksWereReported) {
			return;
		}

		try {
			String text = "";

			Path progressLogFilePath = Paths.get(LOG_FILE_PROGRESS_PREFIX + String
				.valueOf(rank) + ".plog");

			Files.write(progressLogFilePath, Integer.toString(size).concat(System
				.lineSeparator()).getBytes(), StandardOpenOption.TRUNCATE_EXISTING,
				StandardOpenOption.CREATE);

			for (Integer counter = 0; counter < numberOfTasks; counter++) {
				text = String.valueOf(counter).concat(",").concat(tasks.get(counter))
					.concat(System.lineSeparator());
				Files.write(progressLogFilePath, text.getBytes(),
					StandardOpenOption.APPEND, StandardOpenOption.CREATE);
			}

		}
		catch (IOException exc) {
			logger.warning("" + exc.getMessage());
		}

		// The tasks should not be reported twice:
		tasksWereReported = true;
	}

	@Override
	public int reportProgress(int taskId, int progress, int rank) {
		// Check that task exists:
		if (!tasks.containsKey(taskId)) {
			logger.warning("Task " + taskId +
				" does not exist. Progress can not be reported for a task that does not exist.");
			return -1;
		}

		// Ignore impossible new progress percentages:
		if (progress > 100 || progress < 0) {
			return lastWrittenTaskPercentage.get(taskId);
		}

		// Do not write progress percentage that has already been written to avoid
		// writing gigantic progress log files:
		if (!lastWrittenTaskPercentage.containsKey(taskId) ||
			progress > lastWrittenTaskPercentage.get(taskId))
		{
			lastWrittenTaskPercentage.put(taskId, progress);

			try {
				Path progressLogFilePath = Paths.get(LOG_FILE_PROGRESS_PREFIX + String
					.valueOf(rank) + ".plog");
				String text = String.valueOf(taskId).concat(",").concat(String.valueOf(
					progress)).concat(System.lineSeparator());
				Files.write(progressLogFilePath, text.getBytes(),
					StandardOpenOption.APPEND, StandardOpenOption.CREATE);
			}
			catch (IOException exc) {
				logger.warning("reportProgress error - " + exc.getMessage());
				return -1;
			}
		}
		return 0;
	}

	@Override
	public int reportText(String textToReport, int rank) {
		try {
			Files.write(Paths.get(LOG_FILE_REPORT_PREFIX + String.valueOf(rank) +
				".tlog"), textToReport.concat(System.lineSeparator()).getBytes(),
				StandardOpenOption.APPEND, StandardOpenOption.CREATE);
		}
		catch (IOException exc) {
			logger.warning("reportText error - " + exc.getMessage());
			return -1;
		}
		return 0;
	}

}
