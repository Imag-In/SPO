ALTER TABLE MEDIA
    ADD COLUMN IF NOT EXISTS rating TINYINT;

alter table MEDIA
    alter column RATING set not null;

alter table MEDIA
    alter column RATING set default 0;