package LFTP;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;

import Exception.IllegalPacketLengthException;
import tools.LFTPGet;
import tools.LFTPSend;
import tools.MyList;
import tools.Packet;

public class LFTP_Server {

	static final int UDPSendPort = 9903;
	static final int UDPGetPort = 9904;
	static ArrayList<Thread> threadPool = new ArrayList<>();
	
	public static void main(String[] args) {
		
		int port = 10000;
		Object listLock = new Object();
		Object socketLock = new Object();
		MyList list = MyList.getInstance();
		DatagramSocket socket = null;
		DatagramSocket recSocket = null;
		
		try {
			socket = new DatagramSocket(UDPSendPort);
			recSocket = new DatagramSocket(UDPGetPort);
			
			DatagramPacket p = new DatagramPacket(new byte[1500], 1500);
			while (true) {
				recSocket.receive(p);
				Packet packet = new Packet(p.getData());
				if (packet.isSYN()) {
					if (packet.isREQ()) {
						LFTPGet temp = new LFTPGet(p.getAddress(), port++, packet.getSrcPort(), 9902, listLock, list, socketLock, socket);
						temp.replyHello();
						threadPool.add(temp);
						temp.start();
					} else {
						LFTPSend temp = new LFTPSend(p.getAddress(), port++, packet.getSrcPort(), 9902, listLock, list, socketLock, socket);
						temp.replyHello();
						threadPool.add(temp);
						temp.start();
					}
				}
				synchronized(listLock) {
					list.add(packet);
					for (Thread iter : threadPool) {
						synchronized(iter) {
							iter.notify();
						}
						
					}
				}
			}
			
		} catch (IOException | IllegalPacketLengthException e) {
			e.printStackTrace();
		}
		
	}
}
