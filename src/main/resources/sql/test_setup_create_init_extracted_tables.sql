ALTER TABLE `{{targetSchema}}`.`gcd_story_credit` ADD `issue_id` INT(11) DEFAULT NULL;
ALTER TABLE `{{targetSchema}}`.`gcd_story_credit` ADD `series_id` INT(11) DEFAULT NULL AFTER `issue_id`;
ALTER TABLE `{{targetSchema}}`.`gcd_story_credit` ADD FOREIGN KEY (`issue_id`) REFERENCES `{{targetSchema}}`.`gcd_issue`(`id`);
ALTER TABLE `{{targetSchema}}`.`gcd_story_credit` ADD FOREIGN KEY (`series_id`) REFERENCES `{{targetSchema}}`.`gcd_series`(`id`);

CREATE TABLE IF NOT EXISTS `{{targetSchema}}`.m_character (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    alter_ego VARCHAR(255),
    publisher_id INTEGER NOT NULL REFERENCES `{{targetSchema}}`.gcd_publisher (id)
);

CREATE TABLE IF NOT EXISTS `{{targetSchema}}`.m_character_appearance (
    id INTEGER PRIMARY KEY AUTO_INCREMENT,
    details VARCHAR(255),
    character_id INTEGER NOT NULL,
    story_id INTEGER NOT NULL,
    notes VARCHAR(255),
    membership LONGTEXT,
    issue_id INTEGER DEFAULT NULL,
    series_id INTEGER DEFAULT NULL,
    FOREIGN KEY (character_id) REFERENCES `{{targetSchema}}`.m_character (id),
    FOREIGN KEY (story_id) REFERENCES `{{targetSchema}}`.gcd_story (id),
    FOREIGN KEY (issue_id) REFERENCES `{{targetSchema}}`.gcd_issue (id),
    FOREIGN KEY (series_id) REFERENCES `{{targetSchema}}`.gcd_series (id)
);

CREATE TABLE `{{targetSchema}}`.`m_story_credit` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `created` datetime(6) NOT NULL DEFAULT '1970-01-01 00:00:00.000000',
  `modified` datetime(6) NOT NULL,
  `deleted` tinyint(1) NOT NULL DEFAULT '0',
  `is_credited` tinyint(1) NOT NULL DEFAULT '1',
  `is_signed` tinyint(1) NOT NULL DEFAULT '0',
  `uncertain` tinyint(1) NOT NULL DEFAULT '0',
  `signed_as` varchar(255) NOT NULL DEFAULT '',
  `credited_as` varchar(255) NOT NULL DEFAULT '',
  `credit_name` varchar(255) NOT NULL DEFAULT '',
  `creator_id` int(11) NOT NULL,
  `credit_type_id` int(11) NOT NULL,
  `story_id` int(11) NOT NULL,
  `signature_id` int(11) DEFAULT NULL,
  `issue_id` int(11) DEFAULT NULL,
  `series_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`creator_id`) REFERENCES `{{targetSchema}}`.`gcd_creator_name_detail` (`id`),
  FOREIGN KEY (`credit_type_id`) REFERENCES `{{targetSchema}}`.`gcd_credit_type` (`id`),
  FOREIGN KEY (`issue_id`) REFERENCES `{{targetSchema}}`.`gcd_issue` (`id`),
  FOREIGN KEY (`series_id`) REFERENCES `{{targetSchema}}`.`gcd_series` (`id`),
  FOREIGN KEY (`signature_id`) REFERENCES `{{targetSchema}}`.`gcd_creator_signature` (`id`),
  FOREIGN KEY (`story_id`) REFERENCES `{{targetSchema}}`.`gcd_story` (`id`)
);
