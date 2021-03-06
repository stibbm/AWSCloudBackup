# AWS Cloud Backup Utility
Backs up the files in the current directory to Amazon S3 storage.

# Video Demo Link
	https://www.dropbox.com/s/02c25wuds7zjz7m/CloudBackupDemoVideo.mp4?dl=0

# Download Link
Link to download the utility "backup.jar"

	https://www.dropbox.com/s/hdgvxpooxq22n1i/backup.jar?dl=1

# Instructions
1) First Configure your AWS credentials following the instructions in “How_To_Enter_AWS_Account_Keys”

2) Then move the “backup.jar” into the directory that you would like to backup to AWS.

3) From terminal or command line in that folder run the following command:

		java -jar backup.jar



Files with the following character sequences contained in their Names them will not  be uploaded when backup.jar is run. ( “>” means that > is an illegal character sequence)

Any ASCII character greater than or equal to 128 and the following characters should not exit in file names

	“\\”, “{“, “^”, “}”, “%”, “\”, “>”, “<”, “]”, “[“, “~”, “#”, “&”, “@”, “:”, “=”, “;”, “+”, “,”, “?”




# How to configure AWS Command Line Interface with your account credentials

1 ) To allow program to upload to your AWS account you need to install AWS CLI

	Guide to installing AWS CLI on linux : 
	https://docs.aws.amazon.com/cli/latest/userguide/install-linux.html
	
	Guide to installing AWS CLI on Windows :
	https://docs.aws.amazon.com/cli/latest/userguide/install-windows.html





2 ) To configure access you need to create and IAM user and save the Access key and Secret key that are given to you at the end.

	Guide to creating an IAM User through the AWS console
	https://docs.aws.amazon.com/IAM/latest/UserGuide/id_users_create.html#id_users_create_console





3 ) To configure the CLI run the following command from your terminal or command line:

	aws configure
	
	You will then enter your Access Key and Secret Key when prompted. Then hit enter twice.






