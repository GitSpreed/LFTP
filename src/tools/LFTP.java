package tools;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

public class LFTP extends Thread{

	private InetAddress dstAddr;
	private int dstPort;
	private int srcPort;
	private int UDPDstPort;
	private DatagramSocket socket;
	
	private boolean isFinished = false;
	
	private Object listLock;
	private Object socketLock;

	private MyList<Packet> list;
	
	private int cwnd, fwnd;
	private int seqNum = 0;
	private int ackNum = 0;
	
	private int lastByteRecv = 0;
	
	private Map<Integer, Integer> map = new HashMap<Integer, Integer>();
	
	public LFTP(InetAddress dstAddr, int srcPort, int dstPort, int UDPDstPort, Object listLock, MyList<Packet> list, Object socketLock, DatagramSocket socket) throws SocketException {
		this.dstAddr = dstAddr;
		this.dstPort = dstPort;
		this.UDPDstPort = UDPDstPort;
		this.socket = socket;
		this.listLock = listLock;
		this.list = list;
		this.socketLock = socketLock;
		this.socket = socket;
	}
	
	protected void send(Packet data){
		synchronized(socketLock) {
			DatagramPacket datagramPacket = new DatagramPacket(data.getBytes(), data.getLength(), dstAddr, UDPDstPort);
			try {
				socket.send(datagramPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	protected Packet receive(){
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
	
	protected void sayHello() {
		seqNum = (int)(1 + Math.random() * 1000);
		Packet packet = new Packet(srcPort, dstPort, true, false, false, seqNum++, 0, fwnd, new byte[1]);
		this.send(packet);
	}
	
	protected void replyHello() {
		seqNum = (int)(1 + Math.random() * 1000);
		Packet packet = new Packet(srcPort, dstPort, true, true, false, seqNum++, ackNum, fwnd, new byte[1]);
		this.send(packet);
	}
	
	protected void sendBack() {
		Packet packet = new Packet(srcPort, dstPort, false, true, false, seqNum++, ackNum, fwnd, new byte[1]);
		this.send(packet);
	}
	
	protected void sendFin() {
		Packet packet = new Packet(srcPort, dstPort, false, false, true, seqNum++, ackNum, fwnd, new byte[1]);
		this.send(packet);
	}
	
	protected boolean updateAckNum(int seq, int ack) {
		if (seq == ackNum + 1) {
			ackNum = ack;
			Integer n = map.get(ackNum + 1);
			while (n != null) {
				map.remove(ackNum + 1);
				ackNum = n;
				n = map.get(ackNum + 1);
			}
		} else {
			if (map.get(seq) != null) return false;
			map.put(seq, ack);
		}
		return true;
	}
	
	protected void updateLastByteRecv(int byteNum) {
		lastByteRecv = Math.max(lastByteRecv, byteNum);
	}
	
	protected boolean isWindowFull() {
		return (seqNum - lastByteRecv) > Math.min(fwnd, cwnd);
	}

	@Override
	public void run() throws UnsupportedOperationException{
		throw new UnsupportedOperationException();
	}
	
	//getter and setter
	
	public int getSrcPort() {
		return srcPort;
	}

	public void setSrcPort(int srcPort) {
		this.srcPort = srcPort;
	}

	public int getFwnd() {
		return fwnd;
	}

	public void setFwnd(int fwnd) {
		this.fwnd = fwnd;
	}

	public int getSeqNum() {
		return seqNum;
	}

	public void setSeqNum(int seqNum) {
		this.seqNum = seqNum;
	}
	
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
	
	public boolean isFinished() {
		return isFinished;
	}

	public void setFinished(boolean isFinished) {
		this.isFinished = isFinished;
	}
}
