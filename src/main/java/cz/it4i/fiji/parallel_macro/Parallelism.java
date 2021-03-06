
package cz.it4i.fiji.parallel_macro;

public interface Parallelism {

	public int initialise();

	public int finalise();

	public int getRank();

	public int getSize();

	public int barrier();

	public String scatterEqually(String sendString, int totalSendBufferLength,
		int root);

	public String scatter(String sendString, int sendCount, int receiveCount,
		int root);

	public String gather(String sendString, int sendCount, int receiveCount,
		int root);

	public String gatherEqually(String sendString, int totalSendBufferLength,
		int root);
}
