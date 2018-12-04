package tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
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
	private int seqNum = 0;
	private int ackNum = 0;
	
	private int lastByteRecv = 0;
	
	private Map<Integer, Integer> map = new HashMap<Integer, Integer>();
	
	private connectionType ctype;
	
	private ArrayList<Packet> cache = new ArrayList<Packet>();
	private InputStream in;
	
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
					return temp;
				}
			}
			return null;
		}
	}
	
	private void sendFile() {
		byte[] data = new byte[Packet.MAX_PACKET_LENGTH - Packet.MIN_PACKET_LENGTH];
		int bytesNum = 0;
		try {
			while((lastByteRecv - seqNum) < Math.max(fwnd, cwnd) && (bytesNum = in.read(data)) != -1 ) {
				Packet packet = new Packet(srcPort, dstPort, false, true, false, seqNum, ackNum, fwnd, Arrays.copyOf(data, bytesNum));
				this.send(packet);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sayHello() {
		seqNum = (int)(1 + Math.random() * 1000);
		Packet packet = new Packet(srcPort, dstPort, true, false, false, seqNum, 0, fwnd, new byte[1]);
		try {
			this.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void replyHello() {
		seqNum = (int)(1 + Math.random() * 1000);
		Packet packet = new Packet(srcPort, dstPort, true, true, false, seqNum, ackNum + 1, fwnd, new byte[1]);
		try {
			this.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void sendBack() {
		Packet packet = new Packet(srcPort, dstPort, false, true, isFinished, seqNum++, ackNum + 1, fwnd, new byte[1]);
		try {
			this.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private boolean updateAckNum(int seq, int ack) {
		if (seq == ackNum + 1) {
			ackNum = ack;
			Integer n = map.get(ackNum + 1);
			while (n != null) {
				ackNum = n;
				n = map.get(ackNum + 1);
				map.remove(ackNum + 1);
			}
		} else {
			if (map.get(seq) != null) return false;
			map.put(seq, ack);
		}
		return true;
	}
	
	public void setFilePath(String path) {
		File file = new File(path);
		if (!file.exists()) {
			isFinished = true;
		}
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
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
				this.updateAckNum(packet.getSeqNum(), packet.getSeqNum() + packet.getData().length);
				lastByteRecv = Math.max(lastByteRecv, packet.getAckNum());
				if (ctype == connectionType.GET) {
					isFinished = packet.isFIN();
					this.sendBack();
					/*TODO write cache into file */
				}
			}
		}
	}
	
	@Override
	public void run() {
		while (!isFinished) {
			try {
				if (ctype == connectionType.SEND) {
					this.sendFile();
				}	
				this.receiveFile();
			} catch (IOException e) {
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
