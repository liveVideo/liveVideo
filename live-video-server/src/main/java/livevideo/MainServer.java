package livevideo;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;

class SerializableFile implements Serializable {

	private String fileName;

	private static final long serialVersionUID = 1L;
	private byte[] array;
	private String token;

	public SerializableFile() {

	}

	public SerializableFile(String token) {
		this.token = token;
	}

	public SerializableFile(File file, String token) throws IOException {
		this.token = token;
		fileName = file.getName();
		this.array = FileUtils.readFileToByteArray(file);
	}

	public String getName() {
		return fileName;
	}

	public String getToken() {
		return token;
	}

	public byte[] getBytes() {
		return array;
	}
}

public class MainServer {
	public static String folderIndex;
	static private ServerSocket socket;
	private static ObjectInputStream objectInputStream;
	ByteArrayInputStream bais;

	public static void main(String[] args) throws Exception {
		socket = new ServerSocket(8888);
		System.out.println("Server started");
		while (true) {
			Socket clientSocket = socket.accept();
			final ArrayList<Object> ar = new ArrayList<Object>();
			ar.add(clientSocket);
			(new Thread(new Runnable() {

				public void run() {
					System.out.println("Socket accepted*************");
					try {
						Socket s = ((Socket) ar.get(0));
						objectInputStream = new ObjectInputStream(s
								.getInputStream());
						SerializableFile aviFragment = (SerializableFile) objectInputStream
								.readObject();
						if (aviFragment.getToken().equalsIgnoreCase("0")) {
							ObjectOutputStream oos = new ObjectOutputStream(s
									.getOutputStream());
							SerializableFile ob = new SerializableFile(""
									+ System.currentTimeMillis());
							oos.writeObject(ob);
							oos.flush();
							s.close();
							return;
						}
						System.out.println("Object read "
								+ aviFragment.getName());
						System.out.println(aviFragment.getToken());
						putS3(aviFragment, aviFragment.getToken());
						s.close();
					} catch (Exception e) {
						e.printStackTrace();
					}

				}
			})).start();

		}

	}

	protected static void putS3(SerializableFile fis,
			String directoryPartialPath) {
		AWSCredentials cred = new BasicAWSCredentials("AKIAIAPMM5ZOSXNPLSXA",
				"u8Fu+CVkH7SBPWWeF2SLzaWRS66x8+z4aF62YpWU");
		AmazonS3Client s3 = new AmazonS3Client(cred);
		ObjectMetadata md = new ObjectMetadata();
		md.setContentLength(fis.getBytes().length);
		PutObjectRequest req = new PutObjectRequest("livevideo342", "dir/"
				+ directoryPartialPath + "/" + fis.getName(),
				new ByteArrayInputStream(fis.getBytes()), md);
		req.setCannedAcl(CannedAccessControlList.PublicRead);
		PutObjectResult putObjectResult = s3.putObject(req);
		System.out.println(putObjectResult.toString());
	}
}