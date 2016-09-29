CREATE KEYSPACE IF NOT EXISTS lcmap_see_job_tracker WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1};

-- Job Management (Science Model Execution Tracking)
CREATE TABLE IF NOT EXISTS lcmap_see_job_tracker.job (
    job_id text PRIMARY KEY,
    status int,
    science_model_name text,
    execution_parameters text,
    result_keyspace text,
    result_table text,
    result_id text,

) WITH COMPRESSION = { 'sstable_compression' : 'LZ4Compressor' }
  AND COMMENT = 'Science model job results tracking and linking.';

CREATE KEYSPACE IF NOT EXISTS lcmap_see_model_results WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 3};

CREATE TABLE IF NOT EXISTS lcmap_see_model_results.result (
    result_id text PRIMARY KEY,
    result text

) WITH COMPRESSION = { 'sstable_compression' : 'LZ4Compressor' }
  AND COMMENT = 'Science models results table.';
