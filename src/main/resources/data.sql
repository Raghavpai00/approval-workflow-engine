-- LEAVE: 2 steps (approver, then admin)
INSERT INTO approval_step (request_type, step_order, role) VALUES ('LEAVE', 1, 'APPROVER');
INSERT INTO approval_step (request_type, step_order, role) VALUES ('LEAVE', 2, 'ADMIN');

-- EXPENSE: 3 steps (approver, approver, admin)
INSERT INTO approval_step (request_type, step_order, role) VALUES ('EXPENSE', 1, 'APPROVER');
INSERT INTO approval_step (request_type, step_order, role) VALUES ('EXPENSE', 2, 'APPROVER');
INSERT INTO approval_step (request_type, step_order, role) VALUES ('EXPENSE', 3, 'ADMIN');