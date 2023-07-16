SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `gcd_publisher` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `country_id` INTEGER NOT NULL,
    `year_began` INTEGER DEFAULT NULL,
    `modified` DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS `gcd_series` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `year_began` INTEGER NOT NULL,
    `publisher_id` INT NOT NULL,
    `country_id` INTEGER NOT NULL,
    `language_id` INTEGER NOT NULL,
    `first_issue_id` INT DEFAULT NULL,
    `last_issue_id` INT DEFAULT NULL,
    `modified` DATETIME NOT NULL,
    FOREIGN KEY (`publisher_id`) REFERENCES `gcd_publisher`(`id`),
    FOREIGN KEY (`first_issue_id`) REFERENCES `gcd_issue`(`id`),
    FOREIGN KEY (`last_issue_id`) REFERENCES `gcd_issue`(`id`)
);

CREATE TABLE IF NOT EXISTS `gcd_issue` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `number` VARCHAR(50) DEFAULT NULL,
    `series_id` INT NOT NULL,
    `variant_of_id` INT DEFAULT NULL,
    `indicia_publisher_id` INT DEFAULT NULL,
    `modified` DATETIME NOT NULL,
    FOREIGN KEY (`series_id`) REFERENCES `gcd_series`(`id`),
    FOREIGN KEY (`variant_of_id`) REFERENCES `gcd_issue`(`id`),
    FOREIGN KEY (`indicia_publisher_id`) REFERENCES `gcd_indicia_publisher`(`id`)
);

CREATE TABLE IF NOT EXISTS `gcd_story` (
    `id` INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `title` varchar(255) NOT NULL DEFAULT '',
    `issue_id` INTEGER NOT NULL REFERENCES `gcd_issue`(`id`),
    `script` longtext NOT NULL DEFAULT 'NULL',
    `pencils` longtext NOT NULL DEFAULT 'NULL',
    `inks` longtext NOT NULL DEFAULT 'NULL',
    `colors` longtext NOT NULL DEFAULT 'NULL',
    `letters` longtext NOT NULL DEFAULT 'NULL',
    `editing` longtext NOT NULL DEFAULT 'NULL',
    `characters` longtext NOT NULL DEFAULT 'NULL',
    `type_id` INTEGER NOT NULL DEFAULT 19,
    `modified` DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS `gcd_credit_type`(
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR (255) NOT NULL,
    `modified` DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS `gcd_creator_name_detail`(
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR (255) NOT NULL,
    `modified` DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS `gcd_story_credit`(
    `id` int (11) NOT NULL AUTO_INCREMENT,
    `creator_id` int (11) NOT NULL,
    `credit_type_id` int (11) NOT NULL,
    `story_id` int (11) NOT NULL,
    `is_sourced` tinyint (1) NOT NULL DEFAULT 0,
    `sourced_by` varchar (255) DEFAULT NULL,
    `modified` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`creator_id`) REFERENCES `gcd_creator_name_detail`(`id`),
    FOREIGN KEY (`credit_type_id`) REFERENCES `gcd_credit_type`(`id`),
    FOREIGN KEY (`story_id`) REFERENCES `gcd_story`(`id`)
);

CREATE TABLE IF NOT EXISTS `gcd_indicia_publisher` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR (255) NOT NULL,
    `parent_id` INT NOT NULL REFERENCES `gcd_publisher`(`id`),
    `modified` DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS `gcd_series_bond` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `origin_id` INT NOT NULL REFERENCES `gcd_series`(`id`),
    `target_id` INT NOT NULL REFERENCES `gcd_series`(`id`),
    `origin_issue_id` INT NOT NULL REFERENCES `gcd_issue`(`id`),
    `target_issue_id` INT NOT NULL REFERENCES `gcd_issue`(`id`),
    `modified` DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS `gcd_reprint` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `origin_id` INT NOT NULL REFERENCES `gcd_story`(`id`),
    `target_id` INT NOT NULL REFERENCES `gcd_story`(`id`),
    `origin_issue_id` INT NOT NULL REFERENCES `gcd_issue`(`id`),
    `target_issue_id` INT NOT NULL REFERENCES `gcd_issue`(`id`),
    `modified` DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS `gcd_issue_credit` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `creator_id` INT NOT NULL REFERENCES `gcd_creator_name_detail`(`id`),
    `credit_type_id` INT NOT NULL REFERENCES `gcd_credit_type`(`id`),
    `issue_id` INT NOT NULL REFERENCES `gcd_issue`(`id`),
    `modified` DATETIME NOT NULL
);

SET FOREIGN_KEY_CHECKS = 1;