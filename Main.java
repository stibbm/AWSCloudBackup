// Matthew Stibbins
// CSS 490A
// AWS Backup

// Program Description : All file names should be legal file names in both Windows in Linux 

package com;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.TimeZone;
import java.io.PrintWriter;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.Bucket;

public class Main {

	// [all, yes, no] logic
	static File generatedMetadataFile;
	static HashMap<String, String> key_lastModified_map = new HashMap<>();
	static HashSet<String> local_fileSet = new HashSet<>();
	static HashMap<String, Long> map = new HashMap<>();
	static boolean previousBackupExists = false;
	static String currentDirectory = null;
	static String backupTarget = null;
	static String backupTargetFullPath = null;
	static boolean windows = false;
	static long startTime = 0;
	static long endTime = 0;
	static ArrayList<String> illegalSequences = new ArrayList<>();
	static ArrayList<String> invalidNames = new ArrayList<>();

	/**
	 * For Testing purposes only. This is used to ensure the full directory is being
	 * uploaded to the cloud and no files are being missed.
	 * 
	 * @param dir
	 */
	public static void printDirectory(java.io.File dir) {
		if (dir.isDirectory()) {
			System.out.println(dir.getAbsolutePath());
			java.io.File[] files = dir.listFiles();
			if (files != null) {
				for (java.io.File file : files) {
					if (file.isDirectory()) {
						printDirectory(file);
					} else {
						System.out.println(file.getAbsolutePath());
					}
				}
			}
		}
	}

	/**
	 * Checks if the file name contains any of the illegal ASCII characters which
	 * are character values greater than 128.
	 * 
	 * @param token
	 * @return
	 */
	public static boolean containsIllegalAsciiChar(String token) {
		for (int i = 0; i < token.length(); i++) {
			char ch = token.charAt(i);
			int asciiValue = ((int) ch);
			if (ch >= 128) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Initialized the list of illegal characters.
	 */
	public static void initIllegalChars() {
		illegalSequences.add("\\");
		illegalSequences.add("{");
		illegalSequences.add("^");
		illegalSequences.add("}");
		illegalSequences.add("%");
		illegalSequences.add("}");
		illegalSequences.add("\"");
		illegalSequences.add(">");
		illegalSequences.add("<");
		illegalSequences.add("]");
		illegalSequences.add("[");
		illegalSequences.add("~");
		illegalSequences.add("#");
		illegalSequences.add("|");
		illegalSequences.add("&");
		illegalSequences.add("@");
		illegalSequences.add(":");
		illegalSequences.add("=");
		illegalSequences.add(";");
		illegalSequences.add(",");
		illegalSequences.add("+");
		illegalSequences.add("?");
	}

	/**
	 * Checks if the file name contains any illegal characters
	 * 
	 * @param token
	 * @return
	 */
	public static boolean containsIllegalFileChar(String token) {
		String s = "";
		for (int i = 0; i < illegalSequences.size(); i++) {
			s = illegalSequences.get(i);
			if (token.contains(s)) {
				System.out.println(token + " contains illegal char : " + s);
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks the file name for illegal characters.
	 * 
	 * @param fileName
	 * @return
	 */
	public static boolean isValidFileName(String fileName) {
		boolean check = containsIllegalFileChar(fileName);
		boolean check2 = containsIllegalAsciiChar(fileName);
		if (check == false && check2 == false) {
			return true;
		}
		invalidNames.add(fileName);
		return false;
	}

	/**
	 * Put the jar file of this program into a directory. Then when run the program
	 * all of the files in that directory and in subdirectories will be uploaded to
	 * S3 in the AWS cloud.
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		initIllegalChars();
		String currDir = System.getProperty("user.dir");
		// Scanner console = new Scanner(System.in);
		// String currDir = console.nextLine();
		String os = System.getProperty("sun.desktop");
		System.out.println(os);
		if (os.equals("windows")) {
			currDir = currDir.substring(currDir.indexOf(":") + 1);
			windows = true;
			currDir = currDir.replaceAll("\\\\", "/");
		}
		currentDirectory = currDir;

		startTime = System.currentTimeMillis();

		AWSCredentials credentials = null;

		try {
			credentials = new ProfileCredentialsProvider("default").getCredentials();
		} catch (Exception e) {
			// e.printStackTrace();
		}

		AmazonS3 client = AmazonS3ClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(credentials)).withRegion("eu-west-1").build();

		String rootDir = null;

		// java.util.Scanner console = new java.util.Scanner(System.in);
		// System.out.println("Enter directory you want to backup");
		backupTargetFullPath = currDir;

		startTime = System.currentTimeMillis();

		backupTarget = "";
		if (backupTargetFullPath.endsWith("/")) {
			backupTarget = backupTargetFullPath.substring(0, backupTargetFullPath.length() - 1);
		} else {
			backupTarget = backupTargetFullPath;
		}
		backupTarget = backupTarget.substring(backupTarget.lastIndexOf("/") + 1);
		System.out.println("backup target " + backupTarget);

		String bucketName = backupTarget.toLowerCase() + "backup";

		List<Bucket> buckets = client.listBuckets();
		for (Bucket bucket : buckets) {

			if (bucket.getName().contains(bucketName)) {
				try {
					S3Object targetCheck = client
							.getObject(new GetObjectRequest(bucket.getName(), backupTarget + ".targetFile"));
					bucketName = bucket.getName();
					previousBackupExists = true;
					System.out.println("\nprevious backup found\n\n");
					break;
				} catch (Exception e) {
					continue;
				}

				// bucketName = bucket.getName();
				// previousBackupExists = true;
				// break;
			}
		}

		if (!previousBackupExists) {
			boolean done = false;
			int count = 0;
			while (!done) {
				try {

					client.createBucket(bucketName + "" + count);
					bucketName += "" + count;
					uploadBackupTargetFile(bucketName, client);
					// createFolder(buck)
					done = true;

				} catch (Exception e) {
					// e.printStackTrace();
					count++;
					if (count == 10000) {
						System.out.println(count);
						// System.exit(0);
					}
				}

			}

		}

		else {
			S3Object metadata = null;
			try {
				metadata = client.getObject(new GetObjectRequest(bucketName, "metadata"));

				// no metadata exists

				BufferedReader br = new BufferedReader(new InputStreamReader(metadata.getObjectContent()));
				String line = null;

				while ((line = br.readLine()) != null) {
					String[] tokens = new String[2];
					tokens[0] = line;
					tokens[1] = br.readLine();

					if (tokens == null || tokens.length != 2) {
						continue;
					}
					key_lastModified_map.put(tokens[0], tokens[1]);
				}

			} catch (Exception e) {
//				//e.printStackTrace();
				previousBackupExists = false;
			}
		}

		java.io.File dir = new java.io.File(backupTargetFullPath);

		backupDir(bucketName, dir, backupTarget, client);

		boolean success = generateAndUploadMetadataFile(bucketName, client);

		if (invalidNames.size() > 0) {

			System.out.println(
					"\nThe following files had invalid file names, and were not backed up. Change their names to "
							+ "contain no illegal characters and backup again if you want those files to be backed up\n");

			for (String fileName : invalidNames) {
				System.out.println(" * " + fileName);
			}
		}

		endTime = System.currentTimeMillis();

		File localMD = new File("metadatapw");
		localMD.delete();
		// System.out.println(backupTarget);
		File localTargetFile = new File(backupTarget + ".targetFile");
		localTargetFile.delete();

		 boolean remoteFilesDeleted = deleteRemoteFilesNotFoundLocally(bucketName,
				client);
		// boolean remoteFilesDeleted = false;

		if (remoteFilesDeleted) {
			success = generateAndUploadMetadataFile(bucketName, client);
			try {
				localMD = new File("metadatapw");
				localMD.delete();
			} catch (Exception e) {
			}

			if (!success) {
				System.out.println("failed to update the metadata file");
			}
		}



		System.out.println();
		printTimeToRunProgram();

		// printDirectory(dir);

	}

	/**
	 * Prints to console the time the program spent running in seconds
	 */
	public static void printTimeToRunProgram() {
		long runtime = endTime - startTime;
		double seconds = ((double) runtime) / 1000;
		System.out.println("runtime was " + seconds + " seconds");
	}

	/**
	 * Generates a metadata file and uploads it
	 * 
	 * @param bucketName
	 * @param client
	 * @return success
	 */
	public static boolean generateAndUploadMetadataFile(String bucketName, AmazonS3 client) {
		try {
			PrintWriter pw = new PrintWriter("metadatapw");
			for (String key : key_lastModified_map.keySet()) {
				pw.println(key);
				pw.println(key_lastModified_map.get(key));
			}
			pw.close();

			generatedMetadataFile = new File("metadatapw");
			uploadFileToAWS(bucketName, "metadata", generatedMetadataFile, client);

			return true;

		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Uploads all of the files in the target location to the amazon client account
	 * 
	 * @param bucketName
	 * @param client
	 */
	public static void uploadBackupTargetFile(String bucketName, AmazonS3 client) {
		try {
			File file = new File(backupTarget + ".targetFile");
			file.createNewFile();
			uploadFileToAWS(bucketName, backupTarget + ".targetFile", file, client);

		} catch (Exception e) {
			// e.printStackTrace();
		}

	}

	/**
	 * Returns true if user chose to delete the files and returns false otherwise
	 * 
	 * @param bucketName
	 * @param client
	 * @return true if user chooses to delete the files
	 * @return false otherwise
	 */
	public static boolean deleteRemoteFilesNotFoundLocally(String bucketName, AmazonS3 client) {
		boolean filesToDelete = false;
		// create a hashSet of files marked for possible deletion
		HashSet<String> marked = new HashSet<>();
		// System.out.println(
		// "\n\nThe following files no longer exist on your local machine but are still
		// backed-up to the cloud\n");

		for (String key : key_lastModified_map.keySet()) {
			if (!(key.equals(backupTarget + "/" + backupTarget + ".targetFile"))
					&& !(key.equals(backupTarget + ".targetFile")) && !(key.equals("metadatapw"))
					&& !local_fileSet.contains(key)
					&& (!key.equals(bucketName + ".target"))) {
				filesToDelete = true;
				marked.add(key);
				// System.out.println(key + " no longer exits in the local directory");
				// System.out.println(" > " + key);
			}
		}
		if (marked.size() > 0) {

		}
		if (filesToDelete) {
			System.out.println(
					"\n\nThe following files no longer exist on your local machine but are still backed-up to the cloud\n");
			for (String key : marked) {

				System.out.println(" > " + key);
			}
			System.out.println();
			Scanner s = new Scanner(System.in);
			System.out.println("Would you like to delete these files from the cloud backup?");
			System.out.println("Y/n");
			String choice = s.nextLine();
			if (choice.equals("Y")) {
				for (String key : marked) {
					client.deleteObject(bucketName, key);
					System.out.println(key + " deleted");
					key_lastModified_map.remove(key);
				}
			}
			if (choice.equals("Y")) {
				return true;
			}
			return false;
		}
		return false;
	}

	/**
	 * Recursively back up files in directory to AWS bucket
	 * 
	 * @param bucketName
	 * @param dir
	 * @param key
	 * @param client
	 * @throws IOException
	 */
	public static void backupDir(String bucketName, java.io.File dir, String key, AmazonS3 client) throws IOException {
		// System.out.println(dir.getAbsolutePath());
		java.io.File[] files = null;
		try {
			files = dir.listFiles();
			if (files.length == 0) {
				String fileKey = key + "/";
				createFolder(bucketName, fileKey, client);
			}
		} catch (Exception e) {
			// e.printStackTrace();
		}
		if (files != null) {
			for (java.io.File file : files) {
				if (file.getName().equals("backup.jar")) {
					continue;
				}
				if (file.isDirectory()) {
					if (!isValidFileName(file.getName())) {
						// System.out.println("invalid file name found : " + file.getName());
						// System.out.println(
						// "change the name to a valid name and then rerun backup.jar if you want this
						// file to be backed up");
						continue;
					}
					// uploadFileToAWS(bucketName, key+"/" + file.getName(), )

					backupDir(bucketName, file, key + "/" + file.getName(), client);
				} else {
					if (!isValidFileName(file.getName())) {
						// System.out.println("invalid file name found : " + file.getName());
						// System.out.println(
						// "change the name to a valid name and then rerun backup.jar if you want this
						// file to be backed up");
						continue;
					}
					String fileKey = key + "/" + file.getName();
					local_fileSet.add(fileKey);
					// the (!file.getName().equals("placeholder")) part is because we should never
					// have to reupload the placeholders
					// nvm its fine if it reuploads placeholders but it needs to like somehow delete
					// them when an actual item is put into the folder
					if (previousBackupExists && (key_lastModified_map.containsKey(fileKey))) {
						// System.out.println(key_lastModified_map.get(fileKey));
						long previousBackupLastModified = Long.parseLong(key_lastModified_map.get(fileKey));
						long currentLastModified = convertSystemTimeMilliToUTC(file.lastModified());
						if (currentLastModified > previousBackupLastModified) {
							uploadFileToAWS(bucketName, fileKey, file, client);
							String lastModified = getFileLastModifiedDateAsUTC(file);
							// pw.println(fileKey + " " + lastModified);

							key_lastModified_map.put(fileKey, "" + currentLastModified);

							// if (!(fileKey.equals("metadatapw"))) {

							System.out.println("newer version found for fileKey = " + fileKey);
							// }

						}
					} else {
						// if (!file.getName().toLowerCase().endsWith(".targetfile")) {
						uploadFileToAWS(bucketName, fileKey, file, client);
						String lastModified = getFileLastModifiedDateAsUTC(file);
						key_lastModified_map.put(fileKey, lastModified);
						map.put(fileKey, Long.parseLong(lastModified));
						// pw.println(fileKey + " " + lastModified);
						// }
					}

				}
			}
		}

	}

	/**
	 * Create an empty folder in the bucket with the passed bucketName and folder
	 * name will be key and upload if bucket belongs to client.
	 * 
	 * @param bucketName
	 * @param key
	 * @param client
	 */
	public static void createFolder(String bucketName, String key, AmazonS3 client) {
		// System.out.println("bfolder : " + bucketName + " " + key);
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(0);

		InputStream emptyContent = new ByteArrayInputStream(new byte[0]);
		PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, emptyContent, metadata);
		client.putObject(putObjectRequest);
	}

	/**
	 * Uploads file to the bucket with the specified bucket name and stores the file
	 * under key
	 * 
	 * Maybe should add a try catch and have the method return a boolean so that can
	 * have it return a false if an error is caught and a true if the upload did not
	 * throw any errors
	 * 
	 * @param bucketName
	 * @param key
	 * @param file
	 * @param client
	 */
	public static void uploadFileToAWS(String bucketName, String key, java.io.File file, AmazonS3 client) {
		// System.out.println("key = " + key);
		if (!(file.getName().equals("metadatapw"))) {
			int start = (currentDirectory.length() - backupTarget.length());
			System.out.println("uploading file = " + file.getAbsolutePath().substring(start));
		}
			client.putObject(new PutObjectRequest(bucketName, key, file));


	}

	/**
	 * Given system time in milliseconds, returns the corresponding comparison value
	 * used for comparing file last modified times in the application
	 * 
	 * @param timeMilli
	 * @return
	 */
	private static long convertSystemTimeMilliToUTC(long timeMilli) {
		DateFormat formatterUTC = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		Date date = new Date(timeMilli);
		formatterUTC.setTimeZone(TimeZone.getTimeZone("UTC"));
		String out = formatterUTC.format(date);
		long res = Long.parseLong(out);
		return res;
	}

	/**
	 * Returns the last modified date of the file in the format the application uses
	 * for comparisons
	 * 
	 * @param file
	 * @return
	 */
	public static String getFileLastModifiedDateAsUTC(File file) {
		long timeMilli = file.lastModified();
		long timeUTC = convertSystemTimeMilliToUTC(timeMilli);
		String _timeUTC = "" + timeUTC;
		return _timeUTC;
	}
}
