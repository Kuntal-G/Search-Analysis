echo "changing permissons of backup scripts"
chmod 777 /mnt/scripts/daily_backup_full.sh
chmod 777 /mnt/scripts/incremental_backup.sh
chmod 777 /mnt/scripts/run.sh

echo "Starting syslog"
service rsyslog start

echo "Starting cron..."
cron start

echo "Deleting any existing cron jobs......."
crontab -r

echo "Setting up cron jobs for backup......."
( crontab -l ; echo "59 23 * * * /mnt/scripts/daily_backup_full.sh >> /tmp/dailyBackup.log 2>&1" ) | crontab -
( crontab -l ; echo "45 */1 * * * /mnt/scripts/incremental_backup.sh >> /tmp/incrementalBackup.log 2>&1" ) | crontab -

echo "Starting MySQL..."
mysqld_safe
