package tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import Exception.IllegalPacketLengthException;

public class Packet {
	
	/*The min length of a packet*/
	public final static int MIN_PACKET_LENGTH = 26;
	public final static int MAX_PACKET_LENGTH = 1500;
	
	private int srcPort;
	private int dstPort;
	
	private boolean SYN = false;
	private boolean ACK = false;
	private boolean FIN = false;
	private boolean REQ = true;
	
	private int seqNum = 0;
	private int ackNum = 0;
	
	private int windowLen = 0;
	private byte[] checkSum;
	
	private byte[] data;
	
	public Packet(byte[] packetByte) throws IllegalPacketLengthException {
		if (packetByte.length < MIN_PACKET_LENGTH || packetByte.length > MAX_PACKET_LENGTH) 
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
		REQ = (packetByte[20] & 0x10) != 0x00;
		
		windowLen = ((packetByte[21] & 0xff) << 8) + (packetByte[22] & 0xff);
		checkSum = Arrays.copyOfRange(packetByte, 23, 25);
		
		data = Arrays.copyOfRange(packetByte, MIN_PACKET_LENGTH - 1, packetByte.length);
	}
	
	public Packet(int srcPort, int dstPort, boolean SYN, boolean ACK, boolean FIN, boolean REQ, int seqNum, int ackNum, int windowLen, byte[] data) {
		this.srcPort = srcPort;
		this.dstPort = dstPort;
		this.SYN = SYN;
		this.ACK = ACK;
		this.FIN = FIN;
		this.REQ = REQ;
		this.seqNum = seqNum;
		this.ackNum = ackNum;
		this.windowLen = windowLen;
		this.data = data;
		this.comuteCheckSum();
	}
	
	/*TODO get packet bytes*/
	public byte[] getBytes() {
		return new byte[1];
	}
	
	public int getLength() {
		return MIN_PACKET_LENGTH + data.length - 1;
	}
	
	public void assign(Packet src) {
		this.srcPort = src.srcPort;
		this.dstPort = src.dstPort;
		this.SYN = src.SYN;
		this.ACK = src.ACK;
		this.FIN = src.FIN;
		this.REQ = src.REQ;
		this.seqNum = src.seqNum;
		this.ackNum = src.ackNum;
		this.windowLen = src.windowLen;
		this.checkSum = Arrays.copyOf(src.checkSum, 2);
		this.data = Arrays.copyOf(src.data, src.data.length);
	}
	
	public void save() {
		/* file IO*/
		File file = new File("/Cache/" + seqNum + ".cache");
		FileOutputStream out = null;
		
		try {
			if (!file.exists()) {
				file.createNewFile();
			}		
			out = new FileOutputStream(file);
			out.write(data);
			out.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public boolean isValid() {
		/*TODO check checksum*/
		return true;
	}
	
	public void comuteCheckSum() {
		/*TODO compute the checkSum*/
	}
	
	public int getWindowLen() {
		return windowLen;
	}

	public void setWindowLen(int windowLen) {
		this.windowLen = windowLen;
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

	public boolean isREQ() {
		return REQ;
	}

	public void setREQ(boolean rEQ) {
		REQ = rEQ;
	}
	
}
