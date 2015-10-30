package model;

import spread.AdvancedMessageListener;
import spread.BasicMessageListener;
import spread.MembershipInfo;
import spread.SpreadGroup;
import spread.SpreadMessage;

public class MessageListener implements AdvancedMessageListener {
	private Client client;

	public MessageListener(Client client) {
		this.client = client;
	}

	@Override
	public void regularMessageReceived(SpreadMessage message) {
		// TODO Auto-generated method stub
		System.out.println("Regular message recived");
		String msg = new String(message.getData()); 
		String command = msg.split(":")[0];
		String argument = msg.split(":")[1];
		/**
		 * http://www.spread.org/docs/spread_docs_4/docs/message_types.html: 
		 * AGREED_MESS: These messages have all the properties of FIFO messages
		 * but will be delivered in a causal ordering which will be the same at
		 * all recipients, i.e. all the recipients will 'agree' on the order of
		 * delivery. 
		 * 
		 * SAFE_MESS: These messages have all the properties of AGREED
		 * messages, but are not delivered until all daemons have received it
		 * and are ready to deliver it to the application. This guarantees that
		 * if any one application receives a SAFE message then all the
		 * applications in that group will also receive it unless the machine or
		 * program crashes.
		 * 
		 * This implies that the changes of the state should be agreed. 
		 */
		if (message.isAgreed()) {

			
		}

		else if (message.isCausal()) {

		} else if (message.isFifo()) {

		} else if (message.isReliable()) {

		} else if (message.isSafe()) {
			message.getData();
			switch(command){
				case(Client.DEPOSIT):
					client.getBankAccount().setBalance(Double.parseDouble(argument));
					break;
				case(Client.ADDINTREST):
					client.getBankAccount().addIntrest(Double.parseDouble(argument));
					break;
				case(Client.WITHDRAW):
					client.getBankAccount().withdraw(Double.parseDouble(argument));
					break;
				case(Client.GETBALANCE):
					System.out.println("The blance is: " + client.getBankAccount().getBalance());
					break;
			}

		}

	}

	@Override
	public void membershipMessageReceived(SpreadMessage message) {
		// TODO Auto-generated method stub
		MembershipInfo info = message.getMembershipInfo();
		System.out.println("------- START MEMBERSHIP INFO -------");
		synchronized (this) {
			if (info.isCausedByJoin()) {
				System.out.println(info.getJoined() + " joined");
				System.out.println("Number of members" + info.getMembers().length);
				if (info.getMembers().length > 1) {
					notify();
				}
			} else if (info.isCausedByLeave()) {
				System.out.println(info.getLeft() + " left");
			} else if (info.isCausedByDisconnect()) {
				System.out.println(info.getDisconnected() + " disconnected");
			}
		}

		System.out.println("------- END MEMBERSHIP INFO -------");

	}

}