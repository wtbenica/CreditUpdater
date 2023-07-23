SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `{{targetSchema}}`.`gcd_publisher` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `country_id` INTEGER NOT NULL,
    `year_began` INTEGER DEFAULT NULL,
    `modified` DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS `{{targetSchema}}`.`gcd_series` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `year_began` INTEGER NOT NULL,
    `publisher_id` INT NOT NULL,
    `country_id` INTEGER NOT NULL,
    `language_id` INTEGER NOT NULL,
    `first_issue_id` INT DEFAULT NULL,
    `last_issue_id` INT DEFAULT NULL,
    `modified` DATETIME NOT NULL,
    FOREIGN KEY (`publisher_id`) REFERENCES `{{targetSchema}}`.`gcd_publisher`(`id`),
    FOREIGN KEY (`first_issue_id`) REFERENCES `{{targetSchema}}`.`gcd_issue`(`id`),
    FOREIGN KEY (`last_issue_id`) REFERENCES `{{targetSchema}}`.`gcd_issue`(`id`)
);

CREATE TABLE IF NOT EXISTS `{{targetSchema}}`.`gcd_issue` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `number` VARCHAR(50) DEFAULT NULL,
    `series_id` INT NOT NULL,
    `variant_of_id` INT DEFAULT NULL,
    `indicia_publisher_id` INT DEFAULT NULL,
    `modified` DATETIME NOT NULL,
    FOREIGN KEY (`series_id`) REFERENCES `{{targetSchema}}`.`gcd_series`(`id`),
    FOREIGN KEY (`variant_of_id`) REFERENCES `{{targetSchema}}`.`gcd_issue`(`id`),
    FOREIGN KEY (`indicia_publisher_id`) REFERENCES `{{targetSchema}}`.`gcd_indicia_publisher`(`id`)
);

CREATE TABLE IF NOT EXISTS `{{targetSchema}}`.`gcd_story` (
    `id` INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT,
    `title` varchar(255) NOT NULL DEFAULT '',
    `issue_id` INTEGER NOT NULL REFERENCES `{{targetSchema}}`.`gcd_issue`(`id`),
    `script` longtext NOT NULL DEFAULT '',
    `pencils` longtext NOT NULL DEFAULT '',
    `inks` longtext NOT NULL DEFAULT '',
    `colors` longtext NOT NULL DEFAULT '',
    `letters` longtext NOT NULL DEFAULT '',
    `editing` longtext NOT NULL DEFAULT '',
    `characters` longtext NOT NULL DEFAULT '',
    `type_id` INTEGER NOT NULL DEFAULT 19,
    `modified` DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS `{{targetSchema}}`.`gcd_credit_type` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `name` VARCHAR(50) NOT NULL,
    `sort_code` INT(11) NOT NULL,
    `modified` DATETIME NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS `{{targetSchema}}`.`gcd_creator`(
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR (255) NOT NULL,
    `modified` DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS `{{targetSchema}}`.`gcd_creator_name_detail`(
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR (255) NOT NULL,
    `creator_id` INT NOT NULL,
    `modified` DATETIME NOT NULL,
    FOREIGN KEY (`creator_id`) REFERENCES `{{targetSchema}}`.`gcd_creator`(`id`)
);

CREATE TABLE IF NOT EXISTS `{{targetSchema}}`.`gcd_story_credit` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `created` datetime(6) NOT NULL,
  `modified` datetime(6) NOT NULL,
  `deleted` tinyint(1) NOT NULL,
  `is_credited` tinyint(1) NOT NULL,
  `is_signed` tinyint(1) NOT NULL,
  `uncertain` tinyint(1) NOT NULL,
  `signed_as` varchar(255) NOT NULL,
  `credited_as` varchar(255) NOT NULL,
  `credit_name` varchar(255) NOT NULL,
  `creator_id` int(11) NOT NULL,
  `credit_type_id` int(11) NOT NULL,
  `story_id` int(11) NOT NULL,
  `signature_id` int(11) DEFAULT NULL,
  `is_sourced` tinyint(1) NOT NULL,
  `sourced_by` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`creator_id`) REFERENCES `{{targetSchema}}`.`gcd_creator_name_detail` (`id`),
  FOREIGN KEY (`credit_type_id`) REFERENCES `{{targetSchema}}`.`gcd_credit_type` (`id`),
  FOREIGN KEY (`signature_id`) REFERENCES `{{targetSchema}}`.`gcd_creator_signature` (`id`),
  FOREIGN KEY (`story_id`) REFERENCES `{{targetSchema}}`.`gcd_story` (`id`)
);

CREATE TABLE IF NOT EXISTS `{{targetSchema}}`.`gcd_indicia_publisher` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR (255) NOT NULL,
    `parent_id` INT NOT NULL REFERENCES `{{targetSchema}}`.`gcd_publisher`(`id`),
    `modified` DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS `{{targetSchema}}`.`gcd_series_bond` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `origin_id` INT NOT NULL REFERENCES `{{targetSchema}}`.`gcd_series`(`id`),
    `target_id` INT NOT NULL REFERENCES `{{targetSchema}}`.`gcd_series`(`id`),
    `origin_issue_id` INT NOT NULL REFERENCES `{{targetSchema}}`.`gcd_issue`(`id`),
    `target_issue_id` INT NOT NULL REFERENCES `{{targetSchema}}`.`gcd_issue`(`id`),
    `modified` DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS `{{targetSchema}}`.`gcd_reprint` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `origin_id` INT NOT NULL REFERENCES `{{targetSchema}}`.`gcd_story`(`id`),
    `target_id` INT NOT NULL REFERENCES `{{targetSchema}}`.`gcd_story`(`id`),
    `origin_issue_id` INT NOT NULL REFERENCES `{{targetSchema}}`.`gcd_issue`(`id`),
    `target_issue_id` INT NOT NULL REFERENCES `{{targetSchema}}`.`gcd_issue`(`id`),
    `modified` DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS `{{targetSchema}}`.`gcd_issue_credit` (
    `id` INT PRIMARY KEY AUTO_INCREMENT,
    `creator_id` INT NOT NULL REFERENCES `{{targetSchema}}`.`gcd_creator_name_detail`(`id`),
    `credit_type_id` INT NOT NULL REFERENCES `{{targetSchema}}`.`gcd_credit_type`(`id`),
    `issue_id` INT NOT NULL REFERENCES `{{targetSchema}}`.`gcd_issue`(`id`),
    `modified` DATETIME NOT NULL
);

SET FOREIGN_KEY_CHECKS = 1;