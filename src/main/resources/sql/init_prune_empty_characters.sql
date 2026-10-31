DELETE m
FROM `{{targetSchema}}`.m_character m
WHERE NOT EXISTS (SELECT 1
                  FROM `{{targetSchema}}`.m_character_appearance a
                  WHERE a.character_id = m.id);
