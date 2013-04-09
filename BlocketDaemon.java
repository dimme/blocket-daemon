import java.io.IOException;
import java.net.SocketException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/**
 * Blocket.se easteregg competition daemon
 * 
 * Features:
 * 1. Listening on UDP ports 2600 - 2610 using threads.
 * 2. Debug mode for beautiful output. It can be enabled by starting the daemon
 *    with "java BlocketDaemon DEBUG" from console.
 * 3. Error messages alá Amiga style.
 * 4. Thoroughly commented code.
 * 
 * How to compile and run:
 * $ javac BlocketDaemon.java
 * $ java BlocketDaemon [ DEBUG | debug ]
 * 
 * Released under the New BSD license: http://opensource.org/licenses/BSD-3-Clause
 * 
 * @author Dimitrios Vlastaras, mail@dimme.net
 */
public class BlocketDaemon extends Thread {
	
	/**
	 * Set DEBUG to true from console by appending "DEBUG" to the launch command
	 * if you want to see debug information. Set DEBUG to false if you run it as
	 * a daemon and you don't want to flood the console.
	 */
	public static boolean DEBUG = false; 
	
	/**
	 * Pick a port interval to listen for incoming datagrams.
	 * wasp.blocket.se uses ports 2600 - 2610 for different contestants
	 */
	public static final int STARTING_PORT = 2600, ENDING_PORT = 2610;
	
	/*
	 * Socket and packet for datagram communications.
	 */
	private DatagramSocket socket;
	private DatagramPacket packet;

	/**
	 * Class constructor.
	 * 
	 * @param listeningPort The port where you want the daemon to run
	 * @throws SocketException When the listeningPort was occupied or access was denied.
	 */
	public BlocketDaemon(int listeningPort) throws SocketException {
		
		// Create a socket for receiving and sending datagrams
		socket = new DatagramSocket(listeningPort);
	}
	
	/**
	 * Runs the daemon
	 */
	public void run() {
		while (true) {
			try {
				// Wait to receive a datagram, this is blocking
				this.waitForDatagram();
				
				// Once a datagram has been received, reply with IP and timestamp
				this.replyWithIPAndUNIXTimestamp();
			} catch (IOException e) {
				Utils.printError(e);
				System.exit(1);
			}
		}
	}

	/**
	 * Waits for an incoming datagram.
	 * 
	 * @throws IOException When trying to receive a packet
	 */
	public void waitForDatagram() throws IOException {
		
		// A UDP datagram including headers, contained within an IP frame,
		// can never exceed 1500 bytes
		byte[] data = new byte[1500]; 
		
		// Prepare the datagram for receiving data
		packet = new DatagramPacket(data, data.length);
		
		// Receive incoming data, this is blocking
		socket.receive(packet);
		
		// We don't really care what the incoming data consists of but it you want
		// to see it set DEBUG to true.
		if (DEBUG) {
			String stringData = new String(data);
			System.out.println("Received datagram (Port " + socket.getLocalPort() + "): " + stringData + "\n");
		}
	}
	
	/**
	 * Replies with a datagram containing the senders IP address and
	 * the current UNIX timestamp.
	 * 
	 * @throws IOException
	 */
	public void replyWithIPAndUNIXTimestamp() throws IOException {
		
		// Create the data that will be sent (Remote IP address + UNIX timestamp)
		String ipAddress = packet.getAddress().toString().replaceFirst("/", "");
		String decodedStringData = ipAddress + (System.currentTimeMillis() / 1000L);
		byte[] data = decodedStringData.getBytes();
		
		// Convert this data into a String of 1:s and 0:s and add the appropriate line breaks
		String encodedStringData = "";
		for (int i = 0; i < data.length; i++) {
			encodedStringData += String.format("%8s", Integer.toBinaryString(data[i] & 0xFF)).replace(' ', '0')
					+ ((i == ipAddress.length() - 1 || i == data.length - 1) ? "\n" : " ");
		}
		
		// Convert the string into a byte array
		data = encodedStringData.getBytes();
		
		// Create a datagram with the byte array containing the binary "encoded" data
		packet = new DatagramPacket(data, data.length, packet.getAddress(), packet.getPort());
		
		// ...and send it! =)
		socket.send(packet);
		
		// If you want to see the data that you sent, set DEBUG to true
		if (DEBUG) {
			System.out.println("Sent data:\n" + decodedStringData + "\nEncoded as:\n" + encodedStringData + "\n");
		}
	}

	/**
	 * The main method to start the daemon.
	 * 
	 * @param args The arguments from the console.
	 */
	public static void main(String[] args) {
		
		// Checking DEBUG mode
		if (args.length > 0 && args[0].equalsIgnoreCase("DEBUG"))
			DEBUG = true;
		
		// Try to create and start daemon threads on the predefined ports
		BlocketDaemon[] daemons = new BlocketDaemon[ENDING_PORT - STARTING_PORT + 1];
		for(int i = 0; i < ENDING_PORT - STARTING_PORT + 1; i++) {
			try {
				daemons[i] = new BlocketDaemon(STARTING_PORT + i);
				
				// Start the daemon thread and let it run until the user terminates it
				// or an IOException gets thrown.
				daemons[i].start();
				if (DEBUG) {
					System.out.println("BlocketDaemon started on port " + (STARTING_PORT + i));
				}
			} catch (SocketException e) {
				Utils.printError(e);
				System.exit(1);
			} 
		}
		
		// Line break to separate init messages from the rest
		if (DEBUG) {
			System.out.println("");
		}
	}
}

/**
 * A static class with utilities
 * 
 * @author Dimitrios Vlastaras, mail@dimme.net
 */
class Utils {
	
	/**
	 * It prints the localizedMessage of an exception to stderr alá Amiga style ;)
	 * 
	 * @param e The exception thrown
	 */
	public static void printError(Exception e) {
		System.err.println("Software Failure. Press left mouse button to continue.\nGuru Meditation: "
				+ e.getLocalizedMessage() + "\n");
		e.printStackTrace();
	}
}
