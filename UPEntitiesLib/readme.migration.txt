Migration script to migrate from the previous version (i.e. you already have a up_permissions table but no up_user_permissions)

Note: If there's no relation between a user and their role in the up_user_roles table, you need to manually set it up before
doing the following steps.
Look at the users.ROLESJSON column and then execute statements like
insert into up_user_roles (role_id, user_id) values ('22c8a93f-4f69-4896-842d-e7878126dade', 1);

PostgreSQL:

-- Migrate the users table to a VARCHAR ID column
ALTER TABLE up_user_roles DROP CONSTRAINT fk_up_user_roles_user_id;
ALTER TABLE up_users ALTER COLUMN id TYPE varchar(255);
ALTER TABLE up_user_roles ALTER COLUMN user_id TYPE varchar(255);
ALTER TABLE up_user_roles ADD CONSTRAINT fk_up_user_roles_user_id FOREIGN KEY (user_id) REFERENCES up_users(id);

-- Set up link between users and their permissions
CREATE TABLE up_user_permissions(
    user_id varchar(255) REFERENCES up_users(id),
    permission_id varchar(255) REFERENCES up_permissions(id)
);

-- We're not using the rolesJson anymore
ALTER TABLE up_users DROP COLUMN rolesjson;



TSQL:
-- Migrate the users table to a VARCHAR ID column -> we need to work with "copy&paste" of the tables here...
-- We're going to drop the rolesjson column in the process
CREATE TABLE up_users_copy (id varchar(255), name varchar(255), passwordhash varchar(255), secretkey varchar(255));
GO;
INSERT INTO up_users_copy (id, name, passwordhash, secretkey) SELECT cast(id as varchar(255)) as id, name, passwordhash, secretkey from up_users;
GO;
CREATE TABLE up_user_roles_copy (role_id varchar(255), user_id varchar(255));
GO;
INSERT INTO up_user_roles_copy (role_id, user_id) select role_id, cast(user_id as varchar(255)) as user_id from up_user_roles;
GO;
DROP TABLE up_user_roles;
DROP TABLE up_users;
CREATE TABLE up_users (id varchar(255) PRIMARY KEY, name varchar(255), passwordhash varchar(255), secretkey varchar(255));
GO;
INSERT INTO up_users(id, name, passwordhash, secretkey) select id, name, passwordhash, secretkey from up_users_copy;
DROP TABLE up_users_copy;
CREATE TABLE up_user_roles (role_id varchar(255) REFERENCES up_roles(id), user_id varchar(255) REFERENCES up_users(id));
GO;
INSERT INTO up_user_roles (role_id, user_id) select role_id, user_id from up_user_roles_copy;
DROP TABLE up_user_roles_copy;
GO;

-- Set up link between users and their permissions
CREATE TABLE up_user_permissions(
    user_id varchar(255) REFERENCES up_users(id),
    permission_id varchar(255) REFERENCES up_permissions(id)
);
