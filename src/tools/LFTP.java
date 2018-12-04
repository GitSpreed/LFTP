package tools;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

import Exception.IllegalPacketLengthException;

public class LFTP extends Thread{

	private InetAddress dstAddr;
	private int dstPort;
	private int srcPort;
	private int UDPDstPort;
	private DatagramSocket socket;
	
	private boolean isFinished;
	
	private Object listLock;
	private Object socketLock;
	private MyList<Packet> list;
	
	private int cwnd, fwnd;
	private int ackNum = 0;
	private Map<Integer, Integer> map = new HashMap<Integer, Integer>();
	
	private connectionType ctype;
	
	public LFTP(InetAddress dstAddr, int srcPort, int dstPort, int UDPDstPort, Object listLock, MyList<Packet> list, Object socketLock, DatagramSocket socket, connectionType ctype) throws SocketException {
		this.dstAddr = dstAddr;
		this.dstPort = dstPort;
		this.UDPDstPort = UDPDstPort;
		this.socket = new DatagramSocket(srcPort);
		isFinished = false;
		this.listLock = listLock;
		this.list = list;
		this.socketLock = socketLock;
		this.socket = socket;
		this.ctype = ctype;
	}
	
	private void send(Packet data) throws IOException {
		synchronized(socketLock) {
			DatagramPacket datagramPacket = new DatagramPacket(data.getBytes(), data.getLength(), dstAddr, UDPDstPort);
			socket.send(datagramPacket);
		}
	}
	
	private Packet receive() throws IOException {
		synchronized(listLock) {
			Packet temp = null;
			for (Packet iter : list) {
				if (iter.getDstPort() == srcPort) {
					temp = iter;
					list.remove(iter);
				}
			}
			return temp;
		}
	}
	
	private void sendFile() {
		
	}
	
	public void sayHello() {
		Packet packet = new Packet(srcPort, dstPort, true, false, false, (int)(1 + Math.random() * 1000), 0, fwnd, new byte[1]);
		try {
			this.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void replyHello() {
		
	}
	
	private void sendBack() {
		
	}
	
	private void receiveFile() throws IOException {
		boolean flag = true;
		Packet packet = null;
		while((packet = receive()) != null || flag) {
			if (packet == null) {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				flag = false;
				/*TODO write temp into file */
			}
		}
	}
	
	@Override
	public void run() {
		while (!isFinished) {
			try {
				this.sendFile();
				this.receiveFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	//getter and setter
	public InetAddress getDstAddr() {
		return dstAddr;
	}

	public void setDstAddr(InetAddress dstAddr) {
		this.dstAddr = dstAddr;
	}

	public int getDstPort() {
		return dstPort;
	}

	public void setDstPort(int dstPort) {
		this.dstPort = dstPort;
	}
	
	public int getUDPDstPort() {
		return UDPDstPort;
	}

	public void setUDPDstPort(int uDPDstPort) {
		UDPDstPort = uDPDstPort;
	}

	public int getAckNum() {
		return ackNum;
	}

	public void setAckNum(int ackNum) {
		this.ackNum = ackNum;
	}

	private enum connectionType {
		SEND, GET
	}
}
