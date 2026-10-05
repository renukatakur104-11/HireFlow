-- ====================================================================
-- Flyway Migration V2: Baseline Seed Data (Skills Master List)
-- ====================================================================

-- Master Skills Reference Data
INSERT INTO skills (name) VALUES 
('Java'),
('Spring Boot'),
('Python'),
('FastAPI'),
('JavaScript'),
('TypeScript'),
('React'),
('Node.js'),
('Next.js'),
('C++'),
('C#'),
('MySQL'),
('PostgreSQL'),
('MongoDB'),
('Redis'),
('Docker'),
('Kubernetes'),
('AWS'),
('Git'),
('REST APIs'),
('GraphQL'),
('Machine Learning'),
('Data Structures'),
('Algorithms')
ON DUPLICATE KEY UPDATE name=name;
