-- insert-data.sql
-- Insert test data into departments
INSERT INTO departments (name) VALUES ('Human Resources');
INSERT INTO departments (name) VALUES ('Research and Development');
INSERT INTO departments (name) VALUES ('Sales');
INSERT INTO departments (name) VALUES ('Customer Service');
INSERT INTO departments (name) VALUES ('Information Technology');

INSERT INTO employees (department_id, first_name, last_name, year_of_birth, address_line_1, address_line_2, postal_code, postal_area, country, birth_date, last_login, last_logout, password)
VALUES (1, 'Emma', 'Johnson', 1989, '2589 Oak Street', 'Apt 21', '10005', 'New York', 'USA', '1989-04-14', '2023-01-08 09:00:00-05', '2023-01-08 17:30:00-05', '*Emma*');

INSERT INTO employees (department_id, first_name, last_name, year_of_birth, address_line_1, address_line_2, postal_code, postal_area, country, birth_date, last_login, last_logout, password)
VALUES (2, 'Noah', 'Williams', 1992, '72 Valley View', NULL, '94107', 'San Francisco', 'USA', '1992-08-25', '2023-01-15 08:30:00-08', '2023-01-15 18:45:00-08', '$*Noah$?$');

INSERT INTO employees (department_id, first_name, last_name, year_of_birth, address_line_1, address_line_2, postal_code, postal_area, country, birth_date, last_login, last_logout, password)
VALUES (3, 'Oliver', 'Brown', 1980, '1054 Grand Ave', NULL, 'N1 9LA', 'London', 'UK', '1980-11-05', '2023-02-20 09:15:00+00', '2023-02-20 17:00:00+00', '1li_er');

INSERT INTO employees (department_id, first_name, last_name, year_of_birth, address_line_1, address_line_2, postal_code, postal_area, country, birth_date, last_login, last_logout, password)
VALUES (4, 'Ava', 'Jones', 1995, '49 Gilbert St', NULL, '4000', 'Brisbane', 'Australia', '1995-03-17', '2023-03-07 08:45:00+10', '2023-03-07 16:30:00+10', 'Ava$$%Jones');

INSERT INTO employees (department_id, first_name, last_name, year_of_birth, address_line_1, address_line_2, postal_code, postal_area, country, birth_date, last_login, last_logout)
VALUES (5, 'Elijah', 'Miller', 1990, '5140 Main St', 'Suite 12', 'M4C 1B5', 'Toronto', 'Canada', '1990-06-23', '2023-04-11 09:00:00-04', '2023-04-11 17:45:00-04');

INSERT INTO employees (department_id, first_name, last_name, year_of_birth, address_line_1, address_line_2, postal_code, postal_area, country, birth_date, last_login, last_logout)
VALUES (1, 'Isabella', 'Davis', 1991, '671 Hillcrest Ave', NULL, '75008', 'Paris', 'France', '1991-09-19', '2023-04-25 08:30:00+02', '2023-04-25 17:00:00+02');

INSERT INTO employees (department_id, first_name, last_name, year_of_birth, address_line_1, address_line_2, postal_code, postal_area, country, birth_date, last_login, last_logout)
VALUES (2, 'Sophia', 'Garcia', 1986, '9205 James St', NULL, '10115', 'Berlin', 'Germany', '1986-12-12', '2023-05-05 09:15:00+02', '2023-05-05 18:00:00+02');

INSERT INTO employees (department_id, first_name, last_name, year_of_birth, address_line_1, address_line_2, postal_code, postal_area, country, birth_date, last_login, last_logout)
VALUES (3, 'James', 'Anderson', 1988, '289 Cherry St', 'Floor 6', '200030', 'Shanghai', 'China', '1988-10-07', '2023-05-15 08:45:00+08', '2023-05-15 18:30:00+08');

INSERT INTO employees (department_id, first_name, last_name, year_of_birth, address_line_1, address_line_2, postal_code, postal_area, country, birth_date, last_login, last_logout)
VALUES (4, 'Amelia', 'Martin', 1994, '11 First St', 'Building C2', '20100', 'Nairobi', 'Kenya', '1994-07-21', '2023-06-01 09:00:00+03', '2023-06-01 17:45:00+03');

INSERT INTO employees (department_id, first_name, last_name, year_of_birth, address_line_1, address_line_2, postal_code, postal_area, country, birth_date, last_login, last_logout)
VALUES (5, 'Mia', 'Thompson', 1993, '4876 Sunset Blvd', NULL, '100000', 'Moscow', 'Russia', '1993-05-30', '2023-06-18 09:30:00+03', '2023-06-18 19:00:00+03');

INSERT INTO employees (department_id, first_name, last_name, year_of_birth, address_line_1, address_line_2, postal_code, postal_area, country, birth_date, last_login, last_logout)
VALUES (1, 'Lucas', 'White', 1992, '7002 6th Ave', NULL, '111045', 'Delhi', 'India', '1992-04-15', '2023-07-10 08:50:00+05:30', '2023-07-10 17:20:00+05:30');

INSERT INTO employees (department_id, first_name, last_name, year_of_birth, address_line_1, address_line_2, postal_code, postal_area, country, birth_date, last_login, last_logout)
VALUES (2, 'Michael', 'Harris', 1990, '3057 Maple Lane', NULL, '33130', 'Miami', 'USA', '1990-02-28', '2023-07-22 09:00:00-04', '2023-07-22 18:30:00-04');

INSERT INTO employees (department_id, first_name, last_name, year_of_birth, address_line_1, address_line_2, postal_code, postal_area, country, birth_date, last_login, last_logout)
VALUES (3, 'Charlotte', 'Clark', 1987, '91 Western Road', NULL, '6020', 'Cape Town', 'South Africa', '1987-08-14', '2023-08-05 09:15:00+02', '2023-08-05 17:45:00+02');

INSERT INTO employees (department_id, first_name, last_name, year_of_birth, address_line_1, address_line_2, postal_code, postal_area, country, birth_date, last_login, last_logout)
VALUES (4, 'Benjamin', 'Lewis', 1993, '60 Shire Oak Road', NULL, 'B6 6DJ', 'Birmingham', 'UK', '1993-11-22', '2023-08-20 09:05:00+01', '2023-08-20 18:35:00+01');

INSERT INTO employees (department_id, first_name, last_name, year_of_birth, address_line_1, address_line_2, postal_code, postal_area, country, birth_date, last_login, last_logout)
VALUES (5, 'Harper', 'Lee', 1992, '120 High Road', 'East Wing', 'SW19 2BY', 'London', 'UK', '1992-03-05', '2023-09-12 08:40:00+01', '2023-09-12 17:15:00+01');

INSERT INTO employees (department_id, first_name, last_name, year_of_birth, address_line_1, address_line_2, postal_code, postal_area, country, birth_date, last_login, last_logout)
VALUES (1, 'Evelyn', 'Walker', 1991, '2 Old York Street', 'Room 101', '2148', 'Sydney', 'Australia', '1991-07-16', '2023-09-25 09:25:00+10', '2023-09-25 18:05:00+10');

INSERT INTO employees (department_id, first_name, last_name, year_of_birth, address_line_1, address_line_2, postal_code, postal_area, country, birth_date, last_login, last_logout)
VALUES (2, 'Ethan', 'Hall', 1989, '8001 King Street', NULL, '4810', 'Townsville', 'Australia', '1989-09-12', '2023-10-04 08:55:00+10', '2023-10-04 17:30:00+10');

INSERT INTO employees (department_id, first_name, last_name, year_of_birth, address_line_1, address_line_2, postal_code, postal_area, country, birth_date, last_login, last_logout)
VALUES (3, 'Grace', 'Young', 1985, '2403 Heather Sees Way', NULL, '74135', 'Tulsa', 'USA', '1985-10-28', '2023-10-18 09:00:00-05', '2023-10-18 18:45:00-05');

INSERT INTO employees (department_id, first_name, last_name, year_of_birth, address_line_1, address_line_2, postal_code, postal_area, country, birth_date, last_login, last_logout)
VALUES (4, 'Aiden', 'Allen', 1990, '4894 Desert Broom Court', NULL, '89002', 'Henderson', 'USA', '1990-01-30', '2023-11-07 08:35:00-07', '2023-11-07 17:20:00-07');

INSERT INTO employees (department_id, first_name, last_name, year_of_birth, address_line_1, address_line_2, postal_code, postal_area, country, birth_date, last_login, last_logout, is_part_time)
VALUES (4, 'Zoe', 'De La Mar', 1988, '37 Jefferson Street', NULL, '02127', 'Boston', 'USA', '1988-12-09', '2023-11-21 09:50:00-05', '2023-11-21 18:00:00-05', true);


INSERT INTO projects (leader_id, name, status, start_date) VALUES (1, 'Apollo', 'ACTIVE', '2023-01-01');
INSERT INTO projects (leader_id, name, status, start_date) VALUES (2, 'Gemini', 'ACTIVE', '2023-02-01');
INSERT INTO projects (leader_id, name, status, start_date) VALUES (3, 'Orion', 'ACTIVE', '2023-03-01');
INSERT INTO projects (leader_id, name, status, start_date) VALUES (4, 'Artemis', 'ACTIVE', '2023-04-01');
INSERT INTO projects (leader_id, name, status, start_date) VALUES (5, 'Prometheus', 'ACTIVE', '2023-05-01');

-- Assign each employee to the project they lead
INSERT INTO project_members (project_id, employee_id) VALUES (1, 1);
INSERT INTO project_members (project_id, employee_id) VALUES (2, 2);
INSERT INTO project_members (project_id, employee_id) VALUES (3, 3);
INSERT INTO project_members (project_id, employee_id) VALUES (4, 4);
INSERT INTO project_members (project_id, employee_id) VALUES (5, 5);

-- Assign some employees to additional projects for variety
INSERT INTO project_members (project_id, employee_id) VALUES (1, 6);
INSERT INTO project_members (project_id, employee_id) VALUES (2, 7);
INSERT INTO project_members (project_id, employee_id) VALUES (3, 8);
INSERT INTO project_members (project_id, employee_id) VALUES (4, 9);
INSERT INTO project_members (project_id, employee_id) VALUES (5, 10);

-- Some employees can be part of multiple projects
INSERT INTO project_members (project_id, employee_id) VALUES (2, 1); -- Employee 1 is also a member of Project Beta
INSERT INTO project_members (project_id, employee_id) VALUES (3, 2); -- Employee 2 is also a member of Project Gamma

-- Assigning the remaining employees to at least one project
INSERT INTO project_members (project_id, employee_id) VALUES (1, 11);
INSERT INTO project_members (project_id, employee_id) VALUES (1, 12);
INSERT INTO project_members (project_id, employee_id) VALUES (1, 13);
INSERT INTO project_members (project_id, employee_id) VALUES (1, 14);
INSERT INTO project_members (project_id, employee_id) VALUES (1, 15);
INSERT INTO project_members (project_id, employee_id) VALUES (1, 16);
-- INSERT INTO project_members (project_id, employee_id) VALUES (1, 17);
-- INSERT INTO project_members (project_id, employee_id) VALUES (1, 18);
-- INSERT INTO project_members (project_id, employee_id) VALUES (1, 19);
-- INSERT INTO project_members (project_id, employee_id) VALUES (1, 20);

-- Employees being part of multiple projects
INSERT INTO project_members (project_id, employee_id) VALUES (2, 11);
INSERT INTO project_members (project_id, employee_id) VALUES (2, 12);
INSERT INTO project_members (project_id, employee_id) VALUES (3, 13);
INSERT INTO project_members (project_id, employee_id) VALUES (3, 14);
INSERT INTO project_members (project_id, employee_id) VALUES (4, 15);
INSERT INTO project_members (project_id, employee_id) VALUES (4, 16);
-- INSERT INTO project_members (project_id, employee_id) VALUES (5, 17);
-- INSERT INTO project_members (project_id, employee_id) VALUES (5, 18);
-- INSERT INTO project_members (project_id, employee_id) VALUES (2, 19);
-- INSERT INTO project_members (project_id, employee_id) VALUES (3, 20);




