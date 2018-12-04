package LFTP;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import Exception.IllegalPacketLengthException;
import tools.LFTPGet;
import tools.LFTPSend;
import tools.MyList;
import tools.Packet;

public class LFTP_Client {
	
	static final int UDPSendPort = 9900;
	static final int UDPGetPort = 9902;
	
	public static void main(String[] args) {
		if (args.length != 3) {
			System.out.println("Usage: java LFTP_Client lsend/lget myserver mylargefile");
			return ;
		}
		
		Object listLock = new Object();
		Object socketLock = new Object();
		MyList list = MyList.getInstance();
		DatagramSocket socket = null;
		DatagramSocket recSocket = null;
		try {
			socket = new DatagramSocket(UDPSendPort);
			recSocket = new DatagramSocket(UDPGetPort);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		InetAddress addr;
		try {
			addr = InetAddress.getByName(args[1]);
			if (args[0].equals("lsend")) {
				LFTPSend send = new LFTPSend(addr, 10000, 0, 9904, listLock, list, socketLock, socket);
				send.setFilePath(args[2]);
				send.sayHello();
				send.start();
				
				DatagramPacket p = new DatagramPacket(new byte[1500], 1500);
				while (!send.isFinished()) {
					recSocket.receive(p);
					synchronized(listLock) {
						list.add(new Packet(p.getData()));
						send.notify();					
					}
				}
			} else if (args[0].equals("lget")) {
				LFTPGet get = new LFTPGet(addr, 10000, 0, 9904, listLock, list, socketLock, socket);
				get.sayHello();
				get.start();
				
				DatagramPacket p = new DatagramPacket(new byte[2000], 2000);
				while (!get.isFinished()) {
					recSocket.receive(p);
					synchronized(listLock) {
						list.add(new Packet(p.getData()));
						get.notify();					
					}
				}
			} else {
				System.out.println("Usage: java LFTP_Client lsend/lget myserver mylargefile");
				return ;
			}
			
		} catch (IOException | IllegalPacketLengthException e) {
			e.printStackTrace();
		}
	}
}
