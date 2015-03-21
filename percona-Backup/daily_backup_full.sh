# Cron com
# 23 59 * * * /etc/mysql/daily_backup_full.sh

echo "Starting the daily backup process..."

thisbackup=`date +%Y-%m-%d-%H-%M-%S`
rm -rf /etc/mysqlbackup/current/*

echo "Starting xtra backup $thisbackup"
/usr/bin/xtrabackup --backup --datadir=/var/lib/mysql/ --target-dir=/etc/mysqlbackup/current
echo "Starting xtra prepare $thisbackup"
/usr/bin/xtrabackup --prepare --target-dir=/etc/mysqlbackup/current

echo "Saving remotely $thisbackup"
tar -cvf /etc/mysqlbackup/daily/$thisbackup /etc/mysqlbackup/current
#scp -r /etc/mysqlbackup/daily/$thisbackup support@a.b.c.d:~/daily/
rsync -av /etc/mysqlbackup/daily/ support@a.b.c.d:~/daily/

echo "Backup is over at $thisbackup"
