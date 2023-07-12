CREATE TABLE IF NOT EXISTS `m_character` (
    `id` INTEGER PRIMARY KEY AUTO_INCREMENT,
    `name` VARCHAR(255) NOT NULL,
    `alter_ego` VARCHAR(255),
    `publisher_id` INTEGER NOT NULL REFERENCES `gcd_publisher` (`id`)
);

CREATE TABLE IF NOT EXISTS `m_character_appearance` (
    `id` INTEGER PRIMARY KEY AUTO_INCREMENT,
    `details` VARCHAR(255),
    `character_id` INTEGER NOT NULL,
    `story_id` INTEGER NOT NULL,
    `notes` VARCHAR(255),
    `membership` LONGTEXT,
    `issue_id` INTEGER NOT NULL,
    `series_id` INTEGER NOT NULL,
    FOREIGN KEY (`character_id`) REFERENCES `m_character` (`id`),
    FOREIGN KEY (`story_id`) REFERENCES `gcd_story` (`id`),
    FOREIGN KEY (`issue_id`) REFERENCES `gcd_issue` (`id`),
    FOREIGN KEY (`series_id`) REFERENCES `gcd_series` (`id`)
);

CREATE TABLE IF NOT EXISTS `m_story_credit` (
    `id` int (11) NOT NULL AUTO_INCREMENT,
    `creator_id` int (11) NOT NULL,
    `credit_type_id` int (11) NOT NULL,
    `story_id` int (11) NOT NULL,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`creator_id`) REFERENCES `gcd_creator_name_detail`(`id`),
    FOREIGN KEY (`credit_type_id`) REFERENCES `gcd_credit_type`(`id`),
    FOREIGN KEY (`story_id`) REFERENCES `gcd_story`(`id`)
);