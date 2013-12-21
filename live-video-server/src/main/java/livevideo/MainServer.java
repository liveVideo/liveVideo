package livevideo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

class SerializableFile implements Serializable {

	private String fileName;

	private static final long serialVersionUID = 1L;
	private byte[] array;

	public SerializableFile(File file) throws IOException {
		fileName = file.getName();
		this.array = FileUtils.readFileToByteArray(file);
	}

	public String getName() {
		return fileName;
	}

	public byte[] getBytes() {
		return array;
	}
}

public class MainServer {
	
	public static int folderIndex = -1;
	static private ServerSocket socket;;
	private static ObjectInputStream objectInputStream;
	ByteArrayInputStream bais;

	public static void main(String[] args) throws Exception {
		socket = new ServerSocket(8888);
		while(true) {
			Socket clientSocket = socket.accept();
			final ArrayList<Object> ar = new ArrayList<Object>();
			folderIndex++;
			ar.add(clientSocket);
			(new Thread(new Runnable() {
				
				public void run() {
					System.out.println("Socket accepted*************");
					try {
						objectInputStream = new ObjectInputStream(((Socket)ar.get(0)).getInputStream());
						while (true) {
							SerializableFile aviFragment = (SerializableFile) objectInputStream
									.readObject();
							System.out.println("Object read " + aviFragment.getName());
							putS3(aviFragment, String.valueOf(folderIndex));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					
				}
			})).start();
			
			
		}
		
		
	}

	protected static void putS3(SerializableFile fis, String directoryPartialPath) {
		AWSCredentials cred = new BasicAWSCredentials("AKIAIAPMM5ZOSXNPLSXA",
				"u8Fu+CVkH7SBPWWeF2SLzaWRS66x8+z4aF62YpWU");
		AmazonS3Client s3 = new AmazonS3Client(cred);
		ObjectMetadata md = new ObjectMetadata();
		md.setContentLength(fis.getBytes().length);
		PutObjectRequest req = new PutObjectRequest("livevideo342", "dir/" + directoryPartialPath + "/"
				+ fis.getName(), new ByteArrayInputStream(fis.getBytes()), md);
		s3.putObject(req);
	}
}