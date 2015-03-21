# Cron com
# 23 59 * * * /etc/daily_backup_full.sh

export AWS_ACCESS_KEY_ID=<YOUR KEY>
export AWS_SECRET_ACCESS_KEY=<YOUR KEY>
export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"

echo "Starting the daily backup process..."

#thisbackup=`date +%Y-%m-%d-%H-%M-%S`
thisbackup=`hostname -f`-`date +%Y-%m-%d-%H-%M-%S`
rm -rf /etc/mysqlbackup/current/*

echo "Starting xtra backup $thisbackup"
/usr/bin/xtrabackup --backup --datadir=/var/lib/mysql/ --target-dir=/etc/mysqlbackup/current

echo "Starting xtra prepare $thisbackup"
/usr/bin/xtrabackup --prepare --target-dir=/etc/mysqlbackup/current

echo "Compressing files in /etc/mysqlbackup/current as $thisbackup.tar using tar"
tar -cvf /etc/mysqlbackup/daily/$thisbackup.tar /etc/mysqlbackup/current

echo "Compressing file $thisbackup.tar as $thisbackup.qp using qpress"
qpress -v /etc/mysqlbackup/daily/$thisbackup.tar /etc/mysqlbackup/dailyUpload/$thisbackup.qp

echo "Syncing the daily backup folder with s3"
aws s3 sync /etc/mysqlbackup/dailyUpload/ s3://arc-mysql-db-backup/daily-backup/

echo "Backup is over at $thisbackup"
