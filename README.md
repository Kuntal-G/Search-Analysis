# Search and Analysis

This repository contains the following Projects :

# MySQL BackUP- Dockerize
This contains the backup & restore ( full & incremental) of Percona MySql server as well as MariaDB with xtrabackup to local/s3.
All done inside docker with volume mapped outside to host machine.

#### Note:
1) Change the AWS Secret Key, and AWS Access Key for mariadb-s-backup and restore.

2) Install xtrabackup and qpress if not installed for mariadb restore from S3.

3) Steps are given inside ReadMe file inside every project.

# Lucene Analytics
This project consist of customize Lucene code( analyzer,filter,tokenizer). Also spatial search, highlighting ,grouping and faceting using  Lucene.


# Solr Analytics
This project consist of customize Solr code (scorer, transformer) along with the return fields exclusion capability in  the (fl) tag.
