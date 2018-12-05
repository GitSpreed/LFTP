package LFTP;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Arrays;

import Exception.IllegalPacketLengthException;
import tools.LFTPGet;
import tools.LFTPSend;
import tools.MyList;
import tools.Packet;

public class LFTP_Server {

	static final int ClientUDPSendPort = 9900;
	static final int ClientUDPGetPort = 9902;
	static final int ServerUDPSendPort = 9903;
	static final int ServerUDPGetPort = 9904;
	
	static ArrayList<Thread> threadPool = new ArrayList<>();
	
	public static void main(String[] args) {
		
		int port = 10000;
		Object listLock = new Object();
		Object socketLock = new Object();
		MyList list = MyList.getInstance();
		DatagramSocket socket = null;
		DatagramSocket recSocket = null;
		
		System.out.println("===========================  Server start  =================================");
		
		try {
			socket = new DatagramSocket(ServerUDPSendPort);
			recSocket = new DatagramSocket(ServerUDPGetPort);
			
			DatagramPacket p = new DatagramPacket(new byte[1500], 1500);
			while (true) {
				
				recSocket.receive(p);
				Packet packet = new Packet(Arrays.copyOf(p.getData(), p.getLength()));
				//System.out.println("UDP data packet length" + p.getData().length + " " + p.getLength());
				//System.out.println("Receive packet: SrcPort=" + packet.getSrcPort() + " DstPort=" + packet.getDstPort() + " seq=" + packet.getSeqNum() + " ack=" + packet.getAckNum() + " " + packet.isSYN() + " " + packet.isACK() + " " + packet.isFIN() + " " + packet.isREQ());
				
				if (packet.isSYN()) {
					if (packet.isREQ()) {
						System.out.println("Server >" + "get a \"send\" request");
						LFTPGet temp = new LFTPGet(p.getAddress(), port++, packet.getSrcPort(), ClientUDPGetPort, listLock, list, socketLock, socket);
						temp.setFileName(new String(packet.getData()));
						temp.setAckNum(packet.getSeqNum() + packet.getData().length);
						temp.replyHello();
						threadPool.add(temp);
						temp.start();
					} else {
						System.out.println("Server >" + "get a \"get\" request");
						LFTPSend temp = new LFTPSend(p.getAddress(), port++, packet.getSrcPort(), ClientUDPGetPort, listLock, list, socketLock, socket);
						temp.setAckNum(packet.getSeqNum() + packet.getData().length);
						temp.setFileName(new String(packet.getData()));
						temp.replyHello();
						temp.setStart(true);
						threadPool.add(temp);
						temp.start();
					}
				}
				synchronized(listLock) {
					list.add(packet);
					System.out.println("Server > add packet " + packet.getSeqNum() + " to list.");
					for (Thread iter : threadPool) {
						synchronized(iter) {
							//System.out.println("main notify the thread " + iter.getId());
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
