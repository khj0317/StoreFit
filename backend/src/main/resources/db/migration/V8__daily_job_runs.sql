-- 하루에 한 번만 해야 하는 작업(찾는 날 알림·연체 알림)을 그날 이미 했는지 기록한다.
-- 배포 서버는 쉬는 동안 잠들어서 정해진 시각에 깨어 있다는 보장이 없으므로,
-- 깨어 있을 때 "오늘 아직 안 했으면" 실행하고 여기에 남긴다. (job_name, run_date)가 겹치면 이미 한 것이다.
CREATE TABLE daily_job_runs (
    job_name VARCHAR(50) NOT NULL,
    run_date DATE NOT NULL,
    ran_at TIMESTAMP NOT NULL DEFAULT now(),
    PRIMARY KEY (job_name, run_date)
);
