# AWS Cloud Backup Utility
Backs up the files in the current directory to Amazon S3 storage.


    • First Configure your AWS credentials following the instructions in “How_To_Enter_AWS_Account_Keys”

    • Then move the “backup.jar” into the directory that you would like to backup to AWS.

    • From terminal or command line in that folder run the following command:
	
		java -jar backup.jar


Files with the following character sequences contained in their Names them will not  be uploaded when backup.jar is run. ( “>” means that > is an illegal character sequence)

Any ASCII character greater than or equal to 128 and
“\\”
“{“
“^”
“}”
“%”
“\””
“>”
“<”
“]”
“[“
“~”
“#”
“&”
“@”
“:”
“=”
“;”
“+”
“,”
“?”# 
