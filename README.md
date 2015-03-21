#Search and Analysis
This contains some customized Solr, Lucene code  and ETL (Pentaho Ketle) scripts.Also contains ELK( Elastic Logstash Kibanna) configuration for analyzing the custom application log and Nagios alert.

# MySQL BackUP- Dockerize
This contains the backup & restore ( full & incremental) of Percona MySql server as well as MariaDB with xtrabackup to local/s3.
All done inside docker with volume mapped outside to host machine.

#Note:
1) Change the AWS Secret Key, and AWS Access Key for mariadb-s-backup and restore.

2) Install xtrabackup and qpress if not installed for mariadb restore from S3.

3) Steps are given inside ReadMe file inside every project.
