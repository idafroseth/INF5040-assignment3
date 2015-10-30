package model;

import java.io.File;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

import spread.BasicMessageListener;
import spread.MembershipInfo;
import spread.MessageFactory;
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

public class Client {
	
	File inputfile;
	public static final String DEPOSIT = "DEPOSIT";
	public static final String ADDINTREST = "ADDINTREST";
	public static final String WITHDRAW = "WITHDRAW";
	public static final String GETBALANCE = "GETBALANCE";

	public Client(String fileName, String accountName){
		setBankAccount(accountName);
		bankAccount.setBalance(0.0);
	//	inputfile = new File(fileName);
	}
	
	/**
	 * A bankaccount that should be replicated accross replicas
	 */
	BankAccount bankAccount;

	/**
	 * The number of replicas in our group
	 */
	Integer numberOfReplicas;

	/**
	 * Connection to a spread server
	 */
	SpreadConnection connection;

	/**
	 * The group of the replicas
	 */
	SpreadGroup group;
	
	MessageListener listener = new MessageListener(this);


	/**
	 * Connect to a spread server with no priority and no groupMembership
	 * 
	 * @param serverAdress
	 * @param port
	 * @param uniqueConnName
	 * @return
	 */
	public boolean connectToSpreadServer(String serverAdress, Integer port, String uniqueConnName) {
		
		try {
			connection = new SpreadConnection();
			connection.connect(InetAddress.getByName(serverAdress), port, uniqueConnName, false, true);
			connection.add(listener);
			return true;
		} catch (UnknownHostException | SpreadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Disconnecting the connection
	 * 
	 * @return true/false if the connection was successful/unsuccessful
	 */
	public boolean disconnect() {
		try {
			group.leave();
			connection.remove(listener);
			connection.disconnect();
			return true;
		} catch (SpreadException e) {
			System.out.println("Disconnection failed");
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Tries to join a group, the connection has to be established before this
	 * can be run
	 * 
	 * @param groupName
	 *            of the group
	 * @return true/false if the join was successfull/unsuccessful
	 */
	public boolean joinGroup(String groupName) {
		try {
			this.group = new SpreadGroup();
			group.join(connection, groupName);
			return true;
		} catch (SpreadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}


	/**
	 * Tries to leave the connected group
	 * 
	 * @return true if it is successful
	 */
	public boolean leaveGroup() {
		try {
			this.group.leave();
			return true;
		} catch (SpreadException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Setters and getters for the Client class attributes
	 * @param bankAccountName
	 */
	public void setBankAccount(String bankAccountName){
		bankAccount = new BankAccount(bankAccountName);
	}
	public BankAccount getBankAccount(){
		return this.bankAccount;
	}
	public void setNumberOfReplicas(Integer numberReplicas){
		this.numberOfReplicas = numberReplicas;
	}
	public Integer getNumberOfReplicas(){
		return this.numberOfReplicas;
	}


	/**
	 * Print a menu that wait for input from the user
	 */
	public synchronized void showMenu(){
		
		synchronized(listener){
			try {
				
				System.out.println(
						"******************************************************\n"
						+"*** Waiting for all the members to join the group ***\n"
						+"*****************************************************");
				listener.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(inputfile == null){
			Menu menu = new Menu();
			menu.showMenu();
		}else{
			//ParseFile...
		}
	}

	/**
	 * If the connection and the group is ok, the client will try to multicast a
	 * realiable message to all the users in the group
	 * 
	 * @param data
	 *            to send
	 * @return true/false if the message was delivered successful
	 */
	public boolean sendMessage(byte[] data) {

		if (connection == null || group == null) {
			System.out.println("The client is not correctly connected or has not joined a group");
			return false;
		}
		//Another way to recieve a message is by adding listeners
		SpreadMessage message = new SpreadMessage();
		message.setSafe();
		message.setData(data);
		message.addGroup(group);
		message.setReliable();
		try {
			connection.multicast(message);
			return true;
		} catch (SpreadException e) {
			e.printStackTrace();
			return false;
		}

	}


	class Menu{
		Scanner scin = new Scanner(System.in);
		public void showMenu(){
			System.out.printf(
					  "*****************************\n"
					+ "*           WELCOME         *\n"
					+ "*  REPLICATED BANK ACCOUNT  *\n"
					+ "*****************************\n");
			printCommands();

			while(true){
				respondToInput(scin.nextLine());
			}
		}
		public void printCommands(){
			
				System.out.printf(
						"Supported commands: \n"
						+ "$ balance \n"
						+ "$ deposit <amount> \n"
						+ "$ withdraw <amount> \n"
						+ "$ addinterest <precent> \n"
						+ "$ sleep <duration>\n"
						+ "$ exit\n");
		
		}

		
		public void respondToInput(String input){
			System.out.println("");
			String[] inputCmd = input.toLowerCase().split("\\s+");
			if(inputCmd.length==0){
				System.out.println("Bad command try:");
				printCommands();
				return;
			}
			System.out.println(inputCmd[0]);
			switch (inputCmd[0]){
				case "balance":
					System.out.println("The balance is: " + bankAccount.getBalance());	
					break;
					
				case "deposit":
					if(inputCmd.length!=2){
						System.out.println("Please use: balance <amount>");
						break;
					}
					sendMessage((Client.DEPOSIT + ":" + inputCmd[1]).getBytes());
					System.out.println("Deposing "+ inputCmd[1]);
					break;
					
				case "withdraw":
					if(inputCmd.length!=2){
						System.out.println("Please use: withdraw <amount>");
						break;
					}
					sendMessage((Client.WITHDRAW + ":" + inputCmd[1]).getBytes());
					System.out.println("Withdrawing "+ inputCmd[1]);
					break;
					
				case "addinterest":
					if(inputCmd.length!=2){
						System.out.println("Please use: addinterest <percent>");
						break;
					}
					sendMessage((Client.ADDINTREST + ":" + inputCmd[1]).getBytes());
					System.out.println("Adding interest "+ inputCmd[1]);
					break;	
					
				case "sleep":
					if(inputCmd.length!=2){
						System.out.println("Please use: sleep <duration>");
						break;
					}
					System.out.println("Sleeping for "+ inputCmd[1]);
					System.out.println("SLEEP IS NOT IMPLEMENTED YET");
					break;	
					
				case "exit":
					System.out.println("System exiting");
					disconnect();				
					System.exit(1);
					break;
					
				default:
					System.out.println(inputCmd[0] + " is not a supported command");
					printCommands();
					break;
			}
				
		}
	}
	public static void main(String[] args) {
		//1 connect to the spread server
//		args = new String[]{"172.0.0.1", "NewGroup", "1"};
		if(args.length<3){
			System.out.printf("Wrong number of arguments, correct use: \n"
					+ "$java accountReplica <server address> <account name> <number of replicas> <clientName> [file name]");
			System.exit(0);
		}
		Client client;
		if (args.length == 5) {
			client = new Client(args[3], args[1]);

		} else {
			client = new Client(null, args[1]);
			// parsefile
		}
		client.setBankAccount(args[1]);
		client.setNumberOfReplicas(Integer.parseInt(args[2]));
		
		if(!client.connectToSpreadServer(args[0], 4803, args[3] )){
			System.out.println("Could not connect to the provided server address: " + args[0]+", try again");
			System.exit(2);
		}
		if(!client.joinGroup(args[1])){
			System.out.println("Could not connect to the desired group: " + args[0]+", please try again");
			System.exit(3);
		}
//		byte[] byteString = "HELLO".getBytes();
//		client.sendMessage(byteString);
//		Thread.wait();

		client.showMenu();
	}
}
