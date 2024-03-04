ALTER TABLE media
    ADD ref UUID default random_uuid();

ALTER TABLE media
    ALTER COLUMN ref SET NOT NULL;
