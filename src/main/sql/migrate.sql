BEGIN;

INSERT INTO gcdb2.stddata_country
SELECT *
FROM new_gcd_dump.stddata_country;

INSERT INTO gcdb2.stddata_language
SELECT *
FROM new_gcd_dump.stddata_language;

INSERT INTO gcdb2.gcd_indicia_publisher
SELECT *
FROM new_gcd_dump.gcd_indicia_publisher
WHERE modified > '2021-02-01'
  AND parent_id IN (
    SELECT ngdp.id
    FROM new_gcd_dump.good_publishers ngdp
);

INSERT INTO gcdb2.gcd_brand
SELECT *
FROM new_gcd_dump.gcd_brand
WHERE modified > '2021-02-01';

INSERT INTO gcdb2.gcd_publisher
SELECT *
FROM new_gcd_dump.good_publishers
WHERE modified > '2021-02-01';

INSERT INTO gcdb2.gcd_series
SELECT *
FROM new_gcd_dump.good_series
WHERE modified > '2021-02-01';

INSERT INTO gcdb2.gcd_issue
SELECT *
FROM new_gcd_dump.good_issue
WHERE modified > '2021-02-01';

INSERT INTO gcdb2.gcd_story
SELECT *
FROM new_gcd_dump.good_story
WHERE modified > '2021-02-01';

INSERT INTO gcdb2.gcd_story_credit
SELECT *
FROM new_gcd_dump.good_story_credit
WHERE modified > '2021-02-01';

