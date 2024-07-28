CREATE TABLE departments
(
    id         SERIAL PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    name       VARCHAR(255) UNIQUE      NOT NULL
);

CREATE TABLE employees
(
    id             SERIAL PRIMARY KEY,
    created_at     TIMESTAMP WITH TIME ZONE        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP WITH TIME ZONE        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    department_id  INT REFERENCES departments (id) NOT NULL,
    first_name     VARCHAR(255),
    last_name      VARCHAR(255),
    year_of_birth  INT,
    address_line_1 VARCHAR(255),
    address_line_2 VARCHAR(255),
    postal_code    VARCHAR(10),
    postal_area    VARCHAR(32),
    country        VARCHAR(32),
    birth_date     DATE,
    last_login     TIMESTAMP WITH TIME ZONE,
    last_logout    TIMESTAMP WITH TIME ZONE,
    password       VARCHAR(255),
    is_part_time   BOOLEAN                         NOT NULL DEFAULT FALSE
);

CREATE TABLE projects
(
    id         SERIAL PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    leader_id  INT REFERENCES employees (id) NOT NULL,
    name       VARCHAR(255) UNIQUE           NOT NULL,
    status     VARCHAR(32)                   NOT NULL DEFAULT 'ACTIVE', -- enum (ACTIVE, INACTIVE, ARCHIVED)
    start_date DATE                          NOT NULL DEFAULT CURRENT_DATE,
    end_date   DATE
);

CREATE TABLE project_members
(
    id          SERIAL PRIMARY KEY,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    project_id  INT REFERENCES projects (id),
    employee_id INT REFERENCES employees (id),
    since       DATE                              DEFAULT CURRENT_DATE,
    until       DATE
);




