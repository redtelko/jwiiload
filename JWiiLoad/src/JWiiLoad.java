
import java.util.prefs.Preferences;
import java.util.zip.*;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.File;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;


public class JWiiLoad {
	static Socket socket;
	private static Preferences prefs = Preferences.userRoot().node("/com/vgmoose/jwiiload");

	// host and port of receiving wii (use 4299 and your own ip) also the .dol
	private static final int    port = 4299;
	//	private static final String host = "192.168.1.105";
	private static File filename; // = new File("/Users/Ricky/Downloads/wiimod_v3_0/card/app/wiimod/boot.dol");
	private static File compressed;
	private static String arguments ="";

	private static final JFileChooser fileselect = new JFileChooser();

	static JLabel textLabel = new JLabel("What.",SwingConstants.CENTER);
	static JLabel text1 = new JLabel("",SwingConstants.CENTER);
	static JLabel text2 = new JLabel("",SwingConstants.CENTER);

	static JButton button1 = new JButton("Enter IP");
	static JButton button2 = new JButton("Arguments");

	static JButton button5= new JButton("Browse...");

	static 	JFrame frame = new JFrame("JWiiload");
	static String host;
	static String ip;

	static int startip = prefs.getInt("start", 0);
	static String lastip = prefs.get("host", "Enter IP here");
	static boolean autosend = prefs.getBoolean("auto", true);
	static int timeout = prefs.getInt("timeout",100);

	public static void main(String[] args) 
	{
		//button5.setEnabled(false);

		do{
			fileselect.showOpenDialog(null);
			filename = fileselect.getSelectedFile();
		}while(filename==null);

		System.out.println("".length());

		if (filename!=null)
		{
			//button5.setEnabled(true);
			button5.setText("Send "+filename.getName());
		}

		createWindow();		// Create the JFrame GUI

		if (filename!=null)
			compressData();	

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				compressed.delete();
			}
		}));

		tripleScan();

		if (filename!=null)
			wiisend();

	}

	public static void compressData()
	{
		try
		{
			// Compress the file to send it faster
			text1.setText("Compressing data...");
			compressed = compressFile(filename);
			text1.setText("Data compressed!");	//+ (int)(100*((compressed.length()+0.0)/filename.length()))+"% smaller)");
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	public static void tripleScan()
	{
		for (int x=0; x<4; x++)
		{
			scan(x);
			if (host!=null)
				break;
		}
	}

	public static void wiisend()
	{

		try
		{
			// Open socket to wii with host and port and setup output stream
			if (host==null)
				socket = new Socket(host, port);

			text1.setText("Talking to Wii...");

			OutputStream os = socket.getOutputStream();
			DataOutputStream dos = new DataOutputStream(os);

			text1.setText("Preparing data...");

			byte max = 0;
			byte min = 5;

			short argslength = (short) (filename.getName().length()+arguments.length()+1);

			int clength = (int) (compressed.length());  // compressed filesize
			int ulength = (int) (filename.length());	// uncompressed filesize

			// Setup input stream for sending bytes later of compressed file
			InputStream is = new FileInputStream(compressed);
			BufferedInputStream bis = new BufferedInputStream(is);

			byte b[]=new byte[128*1024];
			int numRead=0;

			text1.setText("Talking to Wii...");

			dos.writeBytes("HAXX");

			dos.writeByte(max);
			dos.writeByte(min);

			dos.writeShort(argslength);

			dos.writeInt(clength);	// writeLong() sends 8 bytes, writeInt() sends 4
			dos.writeInt(ulength);

			//dos.size();	// Number of bytes sent so far, should be 16

			text1.setText("Sending "+filename.getName()+"...");
			dos.flush();

			while ( ( numRead=bis.read(b)) > 0) {

				dos.write(b,0,numRead);
				dos.flush();

			}
			dos.flush();

			text1.setText("Talking to Wii...");

			dos.writeBytes(filename.getName()+"\0");

			String[] argue = arguments.split(" ");

			for (String x : argue)
				dos.writeBytes(x+"\0");

			text1.setText("All done!");

			compressed.delete();


		}
		catch (Exception ce)
		{
			text1.setText("No Wii found");
			String[] selections = {"Retry","Stop"};
			int a=0;

			if (host.equals("rate"))
				a = JOptionPane.showOptionDialog(frame,"Rate Limit Exceeded.\nPlease wait a little while, then try again.","Error", JOptionPane.ERROR_MESSAGE, 0, null, selections, null);
			else
				a = JOptionPane.showOptionDialog(frame,"No Wii found.\nIs the Homebrew Channel running?","Error",JOptionPane.ERROR_MESSAGE, 0, null,selections,null);

			if (a==0)
			{
				tripleScan();
				wiisend();
			}

		}
//		catch (Exception ex) {
//			text1.setText("No Wii found");
//			ex.printStackTrace();
//			if (host.equals("rate"))
//			{
//				text1.setText("Rate Limited");
//				JOptionPane.showMessageDialog(frame,"Rate Limit Exceeded.\nPlease wait a little while, then try again.");
//				tripleScan();
//				wiisend();
//			}
//		}
	}

	static void scan(int t)
	{			
		host=null;

		text1.setText("Finding Wii...");
		String output = null;

		InetAddress localhost=null;

		try {
			localhost = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// this code assumes IPv4 is used
		byte[] ip = localhost.getAddress();

		for (int i = 1; i <= 254; i++)
		{
			try
			{
				ip[3] = (byte)i; 
				InetAddress address = InetAddress.getByAddress(ip);

				if (address.isReachable(10*t))
				{
					output = address.toString().substring(1);
					System.out.println(output + " is on the network");

					// Attempt to open a socket
					try
					{
						socket = new Socket(output,port);
						System.out.println("And is potentially a Wii!");
						text1.setText("Wii found!");
						host=output;
						return;
					} catch (Exception e) {
						//e.printStackTrace();
					}

				}
			} catch (ConnectException e) {
				text1.setText("Rate Limited");
				host="rate";
				e.printStackTrace();
				return;
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		} 

		return;



	}

	private static void createWindow() {

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
		//text1.setPreferredSize(new Dimension(200, 20));
		text1.setPreferredSize(new Dimension(200, 200));
		//button1.setSize(100,100);
		Container content = frame.getContentPane();
		FlowLayout fl = new FlowLayout();
		content.setLayout(fl); 

		//	content.add(button1);
		//	content.add(button2);

		content.add(text1);

		//	content.add(button5);


		//		frame.setResizable(false);
		frame.setSize(200,400);
		frame.setLocationRelativeTo(null);
		frame.pack();
		frame.setVisible(true);


	}

	public static File compressFile(File raw) throws IOException
	{
		File compressed = new File(filename+".wiiload.gz");
		InputStream in = new FileInputStream(raw);
		OutputStream out =
			new DeflaterOutputStream(new FileOutputStream(compressed));
		byte[] buffer = new byte[1000];
		int len;
		while((len = in.read(buffer)) > 0) {
			out.write(buffer, 0, len);
		}
		in.close();
		out.close();
		return compressed;
	}

}