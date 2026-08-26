-- Local shenyu-match minimal schema + seed for MatchSelectDialog / jc-match/list-by-date
-- Apply (Windows): Get-Content -Raw -Encoding UTF8 scripts/integration-config/seed-local-shenyu-match.sql | mysql -uroot -proot --default-character-set=utf8mb4 shenyu-match

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE TABLE IF NOT EXISTS data_competition (
  sclass_id INT NOT NULL PRIMARY KEY,
  category_id INT DEFAULT NULL,
  country_id INT DEFAULT NULL,
  name_en VARCHAR(128) DEFAULT NULL,
  name_zh VARCHAR(128) DEFAULT NULL,
  name_zht VARCHAR(128) DEFAULT NULL,
  short_name_zh VARCHAR(64) DEFAULT NULL,
  short_name_zht VARCHAR(64) DEFAULT NULL,
  short_name_en VARCHAR(64) DEFAULT NULL,
  first_letter VARCHAR(8) DEFAULT '#',
  is_hot TINYINT DEFAULT 0,
  logo VARCHAR(512) DEFAULT NULL,
  type TINYINT DEFAULT 1,
  title_holder VARCHAR(128) DEFAULT NULL,
  most_titles VARCHAR(128) DEFAULT NULL,
  newcomers VARCHAR(256) DEFAULT NULL,
  divisions VARCHAR(256) DEFAULT NULL,
  host_country VARCHAR(64) DEFAULT NULL,
  host_city VARCHAR(64) DEFAULT NULL,
  primary_color VARCHAR(32) DEFAULT NULL,
  secondary_color VARCHAR(32) DEFAULT NULL,
  updated_at BIGINT DEFAULT NULL,
  created_at BIGINT DEFAULT NULL,
  deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS data_match_info (
  schedule_id INT NOT NULL PRIMARY KEY,
  sclass_id INT DEFAULT NULL,
  sclass_name VARCHAR(128) DEFAULT NULL,
  sclass_grade VARCHAR(32) DEFAULT NULL,
  country_id INT DEFAULT NULL,
  country_name VARCHAR(64) DEFAULT NULL,
  pinyin_country VARCHAR(64) DEFAULT NULL,
  issue_name VARCHAR(32) DEFAULT NULL,
  match_state INT DEFAULT 1,
  match_state_name VARCHAR(32) DEFAULT '未开',
  match_time DATETIME DEFAULT NULL,
  match_time_str VARCHAR(16) DEFAULT NULL,
  residue_minute VARCHAR(16) DEFAULT NULL,
  home_team_id INT DEFAULT NULL,
  home_team_name VARCHAR(128) DEFAULT NULL,
  home_logo VARCHAR(512) DEFAULT NULL,
  home_score INT DEFAULT NULL,
  home_half_score INT DEFAULT NULL,
  home_corner INT DEFAULT NULL,
  home_red INT DEFAULT NULL,
  home_yellow INT DEFAULT NULL,
  home_order VARCHAR(16) DEFAULT NULL,
  home_scores VARCHAR(64) DEFAULT NULL,
  guest_team_id INT DEFAULT NULL,
  guest_team_name VARCHAR(128) DEFAULT NULL,
  guest_logo VARCHAR(512) DEFAULT NULL,
  guest_score INT DEFAULT NULL,
  guest_half_score INT DEFAULT NULL,
  guest_corner INT DEFAULT NULL,
  guest_red INT DEFAULT NULL,
  guest_yellow INT DEFAULT NULL,
  guest_order VARCHAR(16) DEFAULT NULL,
  guest_scores VARCHAR(64) DEFAULT NULL,
  is_live TINYINT DEFAULT 0,
  is_mlive TINYINT DEFAULT 0,
  is_live_room TINYINT DEFAULT 0,
  is_focus TINYINT DEFAULT 0,
  let_goal VARCHAR(32) DEFAULT NULL,
  let_stop_live VARCHAR(32) DEFAULT NULL,
  let_stop_live_int INT DEFAULT NULL,
  to_stop_live VARCHAR(32) DEFAULT NULL,
  index_let VARCHAR(64) DEFAULT NULL,
  first_index_let VARCHAR(64) DEFAULT NULL,
  index_total VARCHAR(64) DEFAULT NULL,
  first_index_total VARCHAR(64) DEFAULT NULL,
  index_let_goals VARCHAR(64) DEFAULT NULL,
  neutrality TINYINT DEFAULT 0,
  adivce_num INT DEFAULT 0,
  intelligence_num INT DEFAULT 0,
  is_traditional_lottery TINYINT DEFAULT 0,
  lottery_issue BIGINT DEFAULT NULL,
  deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS data_odds_jc (
  id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  lottery_id INT DEFAULT NULL,
  lottery_type INT NOT NULL DEFAULT 1,
  schedule_id INT NOT NULL,
  comp VARCHAR(128) DEFAULT NULL,
  home VARCHAR(128) DEFAULT NULL,
  away VARCHAR(128) DEFAULT NULL,
  short_comp VARCHAR(64) DEFAULT NULL,
  short_home VARCHAR(64) DEFAULT NULL,
  short_away VARCHAR(64) DEFAULT NULL,
  sport_type INT DEFAULT 1,
  odds_time INT DEFAULT NULL,
  issue INT DEFAULT NULL,
  issue_num INT DEFAULT NULL,
  match_time BIGINT DEFAULT NULL,
  sell_status VARCHAR(32) DEFAULT NULL,
  spf VARCHAR(128) DEFAULT NULL,
  rq VARCHAR(128) DEFAULT NULL,
  bf VARCHAR(512) DEFAULT NULL,
  jq VARCHAR(128) DEFAULT NULL,
  bqc VARCHAR(256) DEFAULT NULL,
  sf VARCHAR(64) DEFAULT NULL,
  rf VARCHAR(64) DEFAULT NULL,
  dxf VARCHAR(64) DEFAULT NULL,
  sfc VARCHAR(256) DEFAULT NULL,
  KEY idx_odds_jc_schedule (schedule_id),
  KEY idx_odds_jc_match_time (match_time),
  KEY idx_odds_jc_lottery_type (lottery_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DELETE FROM data_odds_jc;
DELETE FROM data_match_info;
DELETE FROM data_competition;

INSERT INTO data_competition (sclass_id, name_zh, short_name_zh, first_letter, is_hot, deleted) VALUES
  (36, '英格兰超级联赛', '英超', 'Y', 1, 0),
  (31, '西班牙甲级联赛', '西甲', 'X', 1, 0),
  (34, '意大利甲级联赛', '意甲', 'Y', 1, 0),
  (8,  '德国甲级联赛', '德甲', 'D', 1, 0);

INSERT INTO data_match_info (
  schedule_id, sclass_id, sclass_name, issue_name, match_state, match_state_name,
  match_time, match_time_str, home_team_name, guest_team_name, deleted
) VALUES
  (1002201, 36, '英格兰超级联赛', '周五001', 1, '未开', '2026-08-22 20:00:00', '20:00', '曼联', '切尔西', 0),
  (1002202, 31, '西班牙甲级联赛', '周五002', 1, '未开', '2026-08-22 22:00:00', '22:00', '皇马', '巴萨', 0),
  (1002301, 34, '意大利甲级联赛', '周六001', 1, '未开', '2026-08-23 19:00:00', '19:00', 'AC米兰', '国际米兰', 0),
  (1002302, 8,  '德国甲级联赛',     '周六002', 1, '未开', '2026-08-23 21:00:00', '21:00', '拜仁', '多特蒙德', 0);

INSERT INTO data_odds_jc (
  lottery_type, schedule_id, comp, home, away, short_comp, short_home, short_away,
  sport_type, odds_time, issue, issue_num, match_time
) VALUES
  (1, 1002201, '英格兰超级联赛', '曼联', '切尔西', '英超', '曼联', '切尔西', 1, 1787400000, 20260822, 1, 1787400000),
  (1, 1002202, '西班牙甲级联赛', '皇马', '巴萨', '西甲', '皇马', '巴萨', 1, 1787407200, 20260822, 2, 1787407200),
  (1, 1002301, '意大利甲级联赛', 'AC米兰', '国际米兰', '意甲', 'AC米兰', '国际米兰', 1, 1787482800, 20260823, 1, 1787482800),
  (1, 1002302, '德国甲级联赛', '拜仁', '多特蒙德', '德甲', '拜仁', '多特蒙德', 1, 1787490000, 20260823, 2, 1787490000);
