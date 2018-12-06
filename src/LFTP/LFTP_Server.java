package LFTP;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import Exception.IllegalPacketLengthException;
import tools.LFTP;
import tools.LFTPGet;
import tools.LFTPSend;
import tools.MyList;
import tools.Packet;

//LFTP服务器端
public class LFTP_Server {

	//记录客户端和服务器端的UDP发送与接收端口
	static final int ClientUDPSendPort = 9900;
	static final int ClientUDPGetPort = 9902;
	static final int ServerUDPSendPort = 9903;
	static final int ServerUDPGetPort = 9904;
	
	//线程池
	static ArrayList<LFTP> threadPool = new ArrayList<>();
	
	public static void main(String[] args) {
		
		//LFTP的端口，每新增一个LFTP线程，端口号就+1，保证每个线程有不同的LFTP端口号
		int port = 10000;
		
		//创建相应的UDP套接字和线程锁
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
				
				//判断数据包是否是请求连接的数据包，若是，则开启相应的新线程响应该连接
				if (packet.isSYN()) {
					//请求发送数据的连接
					if (packet.isREQ()) {
						System.out.println("Server >" + "get a \"send\" request");
						LFTPGet temp = new LFTPGet(p.getAddress(), port++, packet.getSrcPort(), ClientUDPGetPort, listLock, list, socketLock, socket);
						temp.setFileName(new String(packet.getData()));
						temp.setAckNum(packet.getSeqNum() + packet.getData().length);
						temp.replyHello();
						threadPool.add(temp);
						temp.start();
						
					//请求接收数据的连接
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
				
				//将接受到的数据包放入数据包队列
				synchronized(listLock) {
					list.add(packet);
					System.out.println("Server > add packet " + packet.getSeqNum() + " to list.");
					
					Iterator<LFTP> iter = threadPool.iterator();
					while (iter.hasNext()) {
						LFTP temp = iter.next();
						if (temp.isEnd()) {
							iter.remove();
						} else {
							synchronized(temp) {
								temp.notify();
							}
						}
					}
				}
			}
			
		} catch (IOException | IllegalPacketLengthException e) {
			e.printStackTrace();
		}
		
	}
}
