# Cron com
# 45 */1 * * * /etc/incremental_backup_1hr.sh

export AWS_ACCESS_KEY_ID=<YOUR KEY>
export AWS_SECRET_ACCESS_KEY=<YOUR KEY>
export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"

echo "Starting incremental backup..."
#thisincrementalbackup=`date +%Y-%m-%d-%H-%M-%Sincremental`
thisincrementalbackup=`hostname -f`-`date +%Y-%m-%d-%H-%M-%S-incremental`

echo "Removing the previous incremental backup file"
rm -rf /etc/mysqlbackup/inc1/*

echo "Starting incremental backup $thisincrementalbackup"
/usr/bin/xtrabackup --backup --target-dir=/etc/mysqlbackup/inc1 --incremental-basedir=/etc/mysqlbackup/current --datadir=/var/lib/mysql/

echo "Compressing files in /etc/mysqlbackup/inc1 as $thisincrementalbackup.tar using tar"
tar -cvf /etc/mysqlbackup/incremental/$thisincrementalbackup.tar /etc/mysqlbackup/inc1

echo "Compressing file $thisincrementalbackup.tar as $thisincrementalbackup.qp using qpress"
qpress -v /etc/mysqlbackup/incremental/$thisincrementalbackup.tar /etc/mysqlbackup/incrementalUpload/$thisincrementalbackup.qp

echo "Syncing the incremental backup folder with s3"
aws s3 sync /etc/mysqlbackup/incrementalUpload/ s3://arc-mysql-db-backup/incremental-backup/

echo "Incremental Backup is over at $thisincrementalbackup"

