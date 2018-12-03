package tools;

import java.util.Arrays;

import Exception.IllegalPacketLengthException;

public class Packet {
	
	/*The min length of a packet*/
	public final static int MIN_PACKET_LENGTH = 22;
	
	private int srcPort;
	private int dstPort;
	
	private boolean SYN = false;
	private boolean ACK = false;
	private boolean FIN = false;
	
	private int seqNum = 0;
	private int ackNum = 0;
	
	private byte[] data;
	
	public Packet(byte[] packetByte) throws IllegalPacketLengthException {
		if (packetByte.length < MIN_PACKET_LENGTH) 
			throw new IllegalPacketLengthException("Too short packet with " + packetByte.length + "bytes.");
		
		srcPort = ((packetByte[0] & 0xff) << 8) + (packetByte[1] & 0xff);
		dstPort = ((packetByte[2] & 0xff) << 8) + (packetByte[3] & 0xff);
		for (int i = 4; i < 12; i++) {
			seqNum += (packetByte[i] & 0xff) << ((11 - i) * 8);
		}
		for (int i = 12; i < 20; i++) {
			ackNum += (packetByte[i] & 0xff) << ((19 - i) * 8);
		}
		SYN = (packetByte[20] & 0x80) != 0x00;
		ACK = (packetByte[20] & 0x40) != 0x00;
		FIN = (packetByte[20] & 0x20) != 0x00;
		
		data = Arrays.copyOfRange(packetByte, 21, packetByte.length);
	}
	
	public Packet(int srcPort, int dstPort, boolean SYN, boolean ACK, boolean FIN, int seqNum, int ackNum, byte[] data) {
		this.SYN = SYN;
		this.ACK = ACK;
		this.FIN = FIN;
		this.seqNum = seqNum;
		this.ackNum = ackNum;
		this.data = data;
	}
	
	public byte[] getBytes() {
		return new byte[1];
	}
	
	public int getLength() {
		return 21 + data.length;
	}
	
	public void assign(Packet src) {
		this.srcPort = src.srcPort;
		this.dstPort = src.dstPort;
		this.SYN = src.SYN;
		this.ACK = src.ACK;
		this.FIN = src.FIN;
		this.seqNum = src.seqNum;
		this.ackNum = src.ackNum;
		this.data = Arrays.copyOf(src.data, src.data.length);
	}
	
	public int getSrcPort() {
		return srcPort;
	}

	public void setSrcPort(int srcPort) {
		this.srcPort = srcPort;
	}

	public int getDstPort() {
		return dstPort;
	}

	public void setDstPort(int dstPort) {
		this.dstPort = dstPort;
	}
	
	public boolean isSYN() {
		return SYN;
	}

	public void setSYN(boolean sYN) {
		SYN = sYN;
	}

	public boolean isACK() {
		return ACK;
	}

	public void setACK(boolean aCK) {
		ACK = aCK;
	}

	public boolean isFIN() {
		return FIN;
	}

	public void setFIN(boolean fIN) {
		FIN = fIN;
	}

	public int getSeqNum() {
		return seqNum;
	}

	public void setSeqNum(int seqNum) {
		this.seqNum = seqNum;
	}

	public int getAckNum() {
		return ackNum;
	}

	public void setAckNum(int ackNum) {
		this.ackNum = ackNum;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
	
}
