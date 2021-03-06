
package cz.it4i.fiji.parallel_macro;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MpiReflection {

	Logger logger = LoggerFactory.getLogger(MpiReflection.class);

	private Method mpiInit;
	private Method mpiIsInitialized;
	private Method mpiFinalize;
	private Method mpiBarrier;
	private Method mpiGetRank;
	private Method mpiGetSize;
	private Method mpiNewDoubleBuffer;
	private Method mpiScatterv;
	private Method mpiScatter;
	private Method mpiGather;
	private Method mpiGatherv;
	private Method mpiIsFinalized;

	private Object mpiInstance;
	private Object commWorldInstance;
	public Object mpiDoubleInstance;

	public String findMpiJarFile() {
		// Surround script with parenthesis
		// to run multiline command:
		String script = "(" + '\n';
		String jarPath = "";
		try {
			// Read script file:
			BufferedReader reader = new BufferedReader(new InputStreamReader(
				MPIParallelism.class.getClassLoader().getResourceAsStream(
					"findMpiJar.sh")));
			String line = reader.readLine();
			StringBuilder bld = new StringBuilder();
			while (line != null) {
				bld.append(line + '\n');
				line = reader.readLine();
			}
			script = bld.toString();
		}
		catch (IOException exception) {
			logger.error("Could not read script in resources! \n {} ", exception
				.getMessage());
		}
		script += '\n' + ")";

		logger.debug("Loaded the script:\n {} ", script);

		try {
			// Execute the script:
			Process p = Runtime.getRuntime().exec(new String[] { "bash", "-c",
				script });

			// Read the output of the script:
			BufferedReader output = new BufferedReader(new InputStreamReader(p
				.getInputStream()));
			List<String> files = new ArrayList<>();
			String line = "";
			logger.debug("Standard output of the find mpi.jar script.");
			while ((line = output.readLine()) != null) {
				files.add(line);
				logger.debug("{} \n", line);
			}

			// Read the error of the script:
			logger.debug("Error output of the find mpi.jar script.");
			BufferedReader error = new BufferedReader(new InputStreamReader(p
				.getErrorStream()));
			while ((line = error.readLine()) != null) {
				logger.debug("{} \n", line);
			}

			// Dynamically link the files.
			if (!files.isEmpty()) {
				jarPath = files.get(0);
			}
			else {
				logger.error("No OpenMPI was found on your system." +
					" Please install OpenMPI before using Parallel-Macro.");
				System.exit(0);
			}
		}
		catch (IOException exc) {
			logger.error("OpenMPI's mpi.jar was not found on this system. \n {}", exc
				.getMessage());
			System.exit(0);
		}

		return jarPath;
	}

	public void loadOpenMpi(String path) {
		logger.info("The path of MPI.jar is: {}", path);
		try {
			URLClassLoader child;
			child = new URLClassLoader(new URL[] { new URL("file://" + path) },
				MPIParallelism.class.getClassLoader());
			Class<?> mpiClass = Class.forName("mpi.MPI", true, child);
			mpiInstance = mpiClass.newInstance();

			// Methods:
			mpiInit = mpiClass.getDeclaredMethod("Init", String[].class);
			mpiIsInitialized = mpiClass.getDeclaredMethod("isInitialized");
			mpiIsFinalized = mpiClass.getDeclaredMethod("isFinalized");
			mpiFinalize = mpiClass.getDeclaredMethod("Finalize");
			Field commWorld = mpiClass.getDeclaredField("COMM_WORLD");
			Field mpiDoubleField = mpiClass.getDeclaredField("DOUBLE");
			Class<?> mpiDoubleClass = mpiDoubleField.getType();
			mpiDoubleInstance = mpiDoubleField.get(mpiInstance);
			commWorldInstance = commWorld.get(mpiInstance);
			mpiBarrier = commWorldInstance.getClass().getMethod("barrier",
				new Class[] {});
			mpiGetRank = commWorldInstance.getClass().getMethod("getRank",
				new Class[] {});
			mpiGetSize = commWorldInstance.getClass().getMethod("getSize",
				new Class[] {});
			mpiNewDoubleBuffer = mpiClass.getMethod("newDoubleBuffer", int.class);

			mpiScatterv = commWorldInstance.getClass().getMethod("scatterv",
				Object.class, int[].class, int[].class, mpiDoubleClass, Object.class,
				int.class, mpiDoubleClass, int.class);

			mpiScatter = commWorldInstance.getClass().getMethod("scatter",
				Object.class, int.class, mpiDoubleClass, Object.class, int.class,
				mpiDoubleClass, int.class);
			mpiGather = commWorldInstance.getClass().getMethod("gather", Object.class,
				int.class, mpiDoubleClass, Object.class, int.class, mpiDoubleClass,
				int.class);
			mpiGatherv = commWorldInstance.getClass().getMethod("gatherv",
				Object.class, int.class, mpiDoubleClass, Object.class, int[].class,
				int[].class, mpiDoubleClass, int.class);
		}
		catch (MalformedURLException | ClassNotFoundException
				| NoSuchMethodException | SecurityException | IllegalAccessException
				| IllegalArgumentException | InstantiationException
				| NoSuchFieldException exc)
		{
			exc.printStackTrace();
		}
	}

	public void initialise(String[] arg0) throws IllegalAccessException,
		IllegalArgumentException, InvocationTargetException
	{
		Object[] args = { arg0 };
		mpiInit.invoke(mpiInstance, args);
	}

	public void finalise() throws IllegalAccessException,
		IllegalArgumentException, InvocationTargetException
	{
		mpiFinalize.invoke(mpiInstance);
	}

	public boolean isInitialised() throws IllegalAccessException,
		IllegalArgumentException, InvocationTargetException
	{
		Object isInitialized = mpiIsInitialized.invoke(mpiInstance);
		return (boolean) isInitialized;
	}

	public boolean isFinalised() throws IllegalAccessException,
		IllegalArgumentException, InvocationTargetException
	{
		Object isFinalized = mpiIsFinalized.invoke(mpiInstance);
		return (boolean) isFinalized;
	}

	public int getRank() throws IllegalAccessException, IllegalArgumentException,
		InvocationTargetException
	{
		Object tempRank = mpiGetRank.invoke(commWorldInstance);
		return (int) tempRank;
	}

	public int getSize() throws IllegalAccessException, IllegalArgumentException,
		InvocationTargetException
	{
		Object tempSize = mpiGetSize.invoke(commWorldInstance);
		return (int) tempSize;
	}

	public void barrier() throws IllegalAccessException, IllegalArgumentException,
		InvocationTargetException
	{
		mpiBarrier.invoke(commWorldInstance);
	}

	public DoubleBuffer newDoubleBuffer(int size) {
		Object temp;
		try {
			temp = mpiNewDoubleBuffer.invoke(mpiInstance, size);
		}
		catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException exc)
		{
			// In case of error allocate a 0 size double buffer.
			return DoubleBuffer.allocate(0);
		}
		return (DoubleBuffer) temp;
	}

	public void scatterv(Object sendBuffer, int[] sendCounts, int[] displacements,
		Object mpiSendDataType, Object receiveBuffer, int receiveCount,
		Object mpiReceiveDataType, int sender) throws IllegalAccessException,
		IllegalArgumentException, InvocationTargetException
	{
		mpiScatterv.invoke(commWorldInstance, sendBuffer, sendCounts, displacements,
			mpiSendDataType, receiveBuffer, receiveCount, mpiReceiveDataType, sender);
	}

	public void scatter(Object sendBuffer, int sendCount, Object mpiSendDataType,
		Object receiveBuffer, int receiveCount, Object mpiReceiveDataType, int root)
		throws IllegalAccessException, IllegalArgumentException,
		InvocationTargetException
	{
		mpiScatter.invoke(commWorldInstance, sendBuffer, sendCount, mpiSendDataType,
			receiveBuffer, receiveCount, mpiReceiveDataType, root);
	}

	public void gather(Object sendBuffer, int sendCount, Object mpiSendDataType,
		Object receiveBuffer, int receiveCount, Object mpiReceiveDataType, int root)
		throws IllegalAccessException, IllegalArgumentException,
		InvocationTargetException
	{
		mpiGather.invoke(commWorldInstance, sendBuffer, sendCount, mpiSendDataType,
			receiveBuffer, receiveCount, mpiReceiveDataType, root);
	}

	public void gatherv(Object sendBuffer, int sendCount, Object mpiSendDataTypes,
		Object receivedBuffer, int[] receiveCounts, int[] displacements,
		Object mpiReceiveDataTypes, int receiver) throws IllegalAccessException,
		IllegalArgumentException, InvocationTargetException
	{
		mpiGatherv.invoke(sendBuffer, sendCount, mpiSendDataTypes, receivedBuffer,
			receiveCounts, displacements, mpiReceiveDataTypes, receiver);
	}
}
