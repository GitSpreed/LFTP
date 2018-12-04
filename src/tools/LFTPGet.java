package tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class LFTPGet extends LFTP {
	
	private ArrayList<Packet> cache = new ArrayList<Packet>();
	private ArrayList<Integer> indexTable = new ArrayList<Integer>();

	public LFTPGet(InetAddress dstAddr, int srcPort, int dstPort, int UDPDstPort, Object listLock, MyList list,
			Object socketLock, DatagramSocket socket) throws SocketException {
		super(dstAddr, srcPort, dstPort, UDPDstPort, listLock, list, socketLock, socket);
	}
	
	protected void receiveFile() {
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
				//this.updateLastByteRecv(packet.getAckNum());
				this.setFinished(packet.isFIN());
				this.sendBack();
				cache.add(packet);
				
				/* write cache into file */
				Iterator<Packet> iter = cache.iterator();
				while (iter.hasNext()) {
					Packet temp = iter.next();
					if (temp.getSeqNum() < this.getAckNum()) {
						packet.save();
						indexTable.add(packet.getSeqNum());
						iter.remove();
					}
				}
			}
		}
	}
	
	private void mergeFile() {
		try {
			File inFile = new File("Cache/name.cache");
			FileInputStream in = new FileInputStream(inFile);
			byte[] data = new byte[1500];
			in.read(data);
			in.close();
			String name = new String(data);
			
			File outFile = new File("/download/" + name);
			if (!outFile.exists()) {
				outFile.createNewFile();
			}
			FileOutputStream out = new FileOutputStream(outFile);
			Collections.sort(indexTable, new Comparator< Integer >() {
			    @Override
			    public int compare(Integer lhs, Integer rhs) {
			        if ( lhs > rhs ) {
			            return 1;
			        } else {
			            return -1;
			        }
			    }
			});
			for (int iter : indexTable) {
				inFile = new File("/Cache/" + iter + ".cache");
				in = new FileInputStream(inFile);
				int len = in.read(data);
				in.close();
				out.write(data, 0, len);
				inFile.delete();
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	@Override
	public void run() {
		this.replyHello();
		while (!this.isFinished()) {
			this.receiveFile();
		}
		this.mergeFile();
	}

}
