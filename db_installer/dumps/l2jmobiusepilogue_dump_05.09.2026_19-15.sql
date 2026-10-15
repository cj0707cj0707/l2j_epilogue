/* MySQL Dump: l2jmobiusepilogue */

CREATE TABLE `account_data` (
	`account_name` varchar(45) NOT NULL DEFAULT 'PRI',
	`var` varchar(20) NOT NULL DEFAULT 'PRI',
	`value` varchar(255) DEFAULT ''
);

CREATE TABLE `account_gsdata` (
	`account_name` varchar(45) NOT NULL DEFAULT 'PRI',
	`var` varchar(191) NOT NULL DEFAULT 'PRI',
	`value` text NOT NULL DEFAULT ''
);

CREATE TABLE `account_premium` (
	`account_name` varchar(45) NOT NULL DEFAULT 'PRI',
	`enddate` decimal(20,0) NOT NULL DEFAULT ''
);

CREATE TABLE `accounts` (
	`login` varchar(45) NOT NULL DEFAULT 'PRI',
	`password` varchar(45) DEFAULT '',
	`email` varchar(255) DEFAULT '',
	`created_time` timestamp NOT NULL DEFAULT '',
	`lastactive` bigint(13) unsigned NOT NULL DEFAULT '',
	`accessLevel` tinyint(4) NOT NULL DEFAULT '',
	`lastIP` char(15) DEFAULT '',
	`lastServer` tinyint(4) DEFAULT '',
	`pcIp` char(15) DEFAULT '',
	`hop1` char(15) DEFAULT '',
	`hop2` char(15) DEFAULT '',
	`hop3` char(15) DEFAULT '',
	`hop4` char(15) DEFAULT ''
);

INSERT INTO `accounts` VALUES ('admin', '0DPiKuNIrrVmD8IUCuw1hQxNqZc=', 