package LFTP;

import Exception.IllegalPacketLengthException;
import tools.Packet;

public class Test {

	public static void main(String[] args) {
		Packet packet = new Packet(1758, 123, true, false, true, false, 111, 221, 16516, new byte[2]);
		Packet packet2 = null;
		try {
			packet2 = new Packet(packet.getBytes());
		} catch (IllegalPacketLengthException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(packet2.getSrcPort() + " " + packet2.getDstPort() + " " + packet2.isSYN() + " " + packet2.isACK() + " " + packet2.isFIN() + " " + packet2.isREQ() + " " + packet2.getAckNum() + " " + packet2.getSeqNum() + " " + packet2.getWindowLen() + " " + packet2.getData().length);
		System.out.println(packet.getBytes());
		System.out.println(packet2.getBytes());
	}
}
