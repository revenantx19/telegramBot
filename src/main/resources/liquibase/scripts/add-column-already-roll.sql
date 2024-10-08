--liquidbase formated sql

-- changeset konstantin:2
ALTER TABLE register ADD COLUMN already_roll BOOLEAN DEFAULT false;
