-- Spring Boot runs this file before Hibernate creates/drops tables.
-- Without this, Hibernate fails trying to create tables inside a schema
-- that does not yet exist.
CREATE SCHEMA IF NOT EXISTS inventory;
