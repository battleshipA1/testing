import java.net.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Scanner;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import javax.swing.JOptionPane;

public class TFTPClient {
	
	private DatagramPacket sendPacket;
	private DatagramSocket transferSocket;
	private static String filename;
	private static int sendPort,listenPort;
	private String readFileName,writeFileName;
	private boolean readRequest,writeRequest;
	
	
	
	
	public TFTPClient(){
	
		
		try{
			
			transferSocket = new DatagramSocket();
		
		}
		catch(SocketException se){
			se.printStackTrace();
			System.exit(1);
			
		}
		
	}
	public void promptRequest(){
		

		System.out.println("Read or write request?");
		
		Scanner scan = new Scanner(System.in);
		String selectedValue = scan.nextLine();
		
		if(selectedValue.equals("read")||selectedValue.equals("Read")){
			
			readRequest=true;
			
		}
		else if(selectedValue.equals("write")||selectedValue.equals("Write")){
			
			writeRequest=true;
		}
		
		System.out.println("Test or Normal Mode?");
		String mode = scan.nextLine();
		if(mode.equals("Normal")||mode.equals("normal")){
			
				listenPort = 69;
			//TODO this will send to server
			
		}
		else if(mode.equals("Test")||mode.equals("test")){
			
				listenPort=23;
			//TODO this should send to intermediate
		}
	
		
		System.out.println("Enter filename");
		String filenameInput = scan.nextLine();
		readFileName=filenameInput;
		writeFileName=filenameInput;
		System.out.println("You have chosen: "+selectedValue+","+mode+" filename: "+filenameInput);
		scan.close();
		


}//promptRequest
	
	
	
	public void buildSendRequest(){
		
		String filename = null;
		String type = "octet";
		byte[] send = new byte[100];
		System.out.println("Client is creating a send request...");
		send[0]=0;//first byte is 0 regardless of read/write
		if(readRequest){
			send[1]=1;
			System.out.println("Read Request is valid...");
			filename=readFileName;
			
			}
		else if(writeRequest){
			send[1]=2;
			System.out.println("Write Request is valid...");
			filename=writeFileName;
		}
		byte[] temp=filename.getBytes();
		System.arraycopy(temp, 0, send, 2, temp.length);//send contains 0_1|2_filename(without underscores)
		send[temp.length+2]=0;//0_1|2_filename_0
		byte[] temp2 = type.getBytes();
		System.arraycopy(temp, 0, send, temp2.length+3, temp2.length);//send contains 0_1|2_filename_0_octet(in bytes)
		send[temp2.length+temp.length+3]=0;//send contains 0_1|2_filename_0_octet_0
		int length = temp2.length+temp.length+3;
		
		
		try {
			sendPacket=new DatagramPacket(send,length,InetAddress.getLocalHost(),listenPort);
			
		} catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(1);
		}
		System.out.println("Client is sending packet");
		
		System.out.println("To server: "+ sendPacket.getAddress() );//need to make this in verbose mode only
		
		try {
			
			transferSocket.send(sendPacket);
		} catch (IOException e) {
			
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Client packet is sent");
		transferSocket.close();
		
		if(readRequest==true){
			readFile();
		}
		else if(writeRequest==true){
			//write file
		}
		
		
	}
	
	private void ackSend(byte blockNumber){
		byte[] temp ={0,4,0,blockNumber};//feel free to change variable names to make it easier
		DatagramPacket sendP=null;
		
		try {
			sendP=new DatagramPacket(temp,temp.length,InetAddress.getLocalHost(),listenPort);
		} catch (UnknownHostException e) {
			System.out.println("Creating Datagram failed");
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("Sending Acknowledgment");
		
		try {
			transferSocket.send(sendP);
		} catch (IOException e) {
			System.out.println("Acknowledgment sending failed");
			e.printStackTrace();
			System.exit(1);
		}
		
		System.out.println("Acknowledgment was sent sucessfully!");
		
	}
	
	private void readFile(){
		boolean loop=true;
		OutputStream out = null;
		byte [] file,fileInfo;
		
		while(loop){
		file =receiveDataPacket();
		fileInfo=Arrays.copyOfRange(file, 4, file.length-1);//create a new byte[] with only the data/information/bytes/relevant information to be written		
		ackSend(file[3]);
		File tempFile = new File(filename);
		
		
		try {
			out =new FileOutputStream(tempFile);
		} catch (FileNotFoundException e) {
			System.out.println("Output Stream failure");
			e.printStackTrace();
			System.exit(1);
			//ITERATION 2
		}
		
		try {
			out.write(fileInfo, 0, fileInfo.length);
		} catch (IOException e) {
			System.out.println("Output stream failed to write");
			e.printStackTrace();
			System.exit(1);
			//ITERATION 2
		}
		if(fileInfo.length<512){//can't write less than 512 bytes. TODO - you have to be able to? The last packet is < 512 bytes
			loop=false;
			
		}
		}
		try {
			out.close();
		} catch (IOException e) {
			System.out.println("Output stream failed to close");
			e.printStackTrace();
			System.exit(1);
			
		}
		transferSocket.close();//we are at the last step, therefore we can close transfer socket on our way to the end
		
	}
	public byte[] receiveDataPacket(){
		//waits (currently indefinitely) to receive a packet on the specified socket
		byte[] buffer = new byte[516];
		System.out.println("waiting to receive data packet...");
		DatagramPacket p = null;
		
		try {
			p = new DatagramPacket(buffer, buffer.length,InetAddress.getLocalHost(),sendPort);
		} catch (UnknownHostException e1) {
			System.out.println("Creating Datagram failed");
			e1.printStackTrace();
			System.exit(1);
			
		}
		try {
			transferSocket.receive(p);
		} catch (IOException e) {
			System.out.println("Receiving from the port failed");
			e.printStackTrace();
			System.exit(1);
		}
		buffer=p.getData();
		System.out.println("DatagramPacket received sucesfully,contains: ");
		for(byte j: buffer){
			System.out.print(j);
		}
		return buffer;
		
	}
	
	
	
		
	public static void main(String[] args){
		TFTPClient c1 = new TFTPClient();
		c1.promptRequest();
		c1.buildSendRequest();
		//TODO is this complete? Does buildSendRequest() do everything related to the transfer?		
	}
	

	
	
	
	
	
	
	
	

}
