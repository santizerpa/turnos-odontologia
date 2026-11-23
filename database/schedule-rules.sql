-- DDL of the schedule-rules tables generated and inspected with the
-- application-test H2 PostgreSQL-mode profile (Hibernate ddl-auto=create-drop).
-- Apply this script to PostgreSQL local BEFORE starting the local profile.

CREATE TABLE IF NOT EXISTS schedule_configurations (
  created_at timestamp(6) with time zone NOT NULL,
  deleted_at timestamp(6) with time zone,
  updated_at timestamp(6) with time zone NOT NULL,
  dentist_id uuid NOT NULL,
  id uuid NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT uk_schedule_configurations_dentist UNIQUE (dentist_id),
  CONSTRAINT fk_schedule_configurations_dentist
    FOREIGN KEY (dentist_id) REFERENCES dentists (id)
);

CREATE TABLE IF NOT EXISTS schedule_windows (
  end_time time(6) NOT NULL,
  start_time time(6) NOT NULL,
  schedule_configuration_id uuid NOT NULL,
  day_of_week varchar(10) NOT NULL,
  CONSTRAINT fk_schedule_windows_configuration
    FOREIGN KEY (schedule_configuration_id)
      REFERENCES schedule_configurations (id)
);

CREATE INDEX IF NOT EXISTS idx_schedule_windows_configuration
  ON schedule_windows (schedule_configuration_id);
