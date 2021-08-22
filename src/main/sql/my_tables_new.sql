# If the issue and series columns don't already exist, then it adds them 
# to the gcd_story_credit and m_character_appearance tables
# Then creates the m_story_credit table;

###########################################################################
# Add issue/series columns to story_credit                                #
##########################################################################;
ALTER TABLE new_gcd_dump.gcd_story_credit
    ADD COLUMN issue_id INT DEFAULT NULL;
ALTER TABLE new_gcd_dump.gcd_story_credit
    ADD COLUMN series_id INT DEFAULT NULL;


###########################################################################
# Create extracted item tables if not exist                               #
##########################################################################;
CREATE TABLE IF NOT EXISTS new_gcd_dump.m_character_appearance AS (
    SELECT *
    FROM gcdb2.m_character_appearance
    WHERE FALSE
);

CREATE TABLE IF NOT EXISTS new_gcd_dump.m_story_credit AS (
    SELECT *
    FROM gcdb2.gcd_story_credit
    WHERE FALSE
);

CREATE TABLE IF NOT EXISTS new_gcd_dump.m_character AS (
    SELECT *
    FROM gcdb2.m_character
    WHERE FALSE
);

ALTER TABLE new_gcd_dump.m_character_appearance
    ADD FOREIGN KEY (issue_id) REFERENCES gcd_issue (id);
ALTER TABLE new_gcd_dump.m_character_appearance
    ADD FOREIGN KEY (series_id) REFERENCES gcd_series (id);

ALTER TABLE new_gcd_dump.m_story_credit
    MODIFY COLUMN id INT PRIMARY KEY AUTO_INCREMENT;
ALTER TABLE new_gcd_dump.m_story_credit
    ADD FOREIGN KEY (creator_id) REFERENCES gcd_creator_name_detail (id);
ALTER TABLE new_gcd_dump.m_story_credit
    ADD FOREIGN KEY (credit_type_id) REFERENCES gcd_credit_type (id);
ALTER TABLE new_gcd_dump.m_story_credit
    ADD FOREIGN KEY (issue_id) REFERENCES gcd_issue (id);
ALTER TABLE new_gcd_dump.m_story_credit
    ADD FOREIGN KEY (series_id) REFERENCES gcd_series (id);
ALTER TABLE new_gcd_dump.m_story_credit
    ADD FOREIGN KEY (story_id) REFERENCES gcd_story (id);
