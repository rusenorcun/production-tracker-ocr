-- ==========================
-- VERİTABANI & GENEL AYARLAR
-- ==========================
CREATE DATABASE IF NOT EXISTS steel_ops
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
USE steel_ops;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ==========================
-- USERS & USER_DATA
-- ==========================
DROP TABLE IF EXISTS user_data;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
  id        BIGINT PRIMARY KEY AUTO_INCREMENT,
  username  VARCHAR(50)  NOT NULL,
  password  VARCHAR(255) NOT NULL,
  role      ENUM('admin','operator') NOT NULL,
  enabled   BIT(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_data (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  username    VARCHAR(255) NOT NULL,
  pass        VARCHAR(60)  NOT NULL,
  permission  VARCHAR(255),
  fullname    VARCHAR(255),
  department  VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==========================
-- PRODUCTS
-- ==========================
DROP TABLE IF EXISTS plates;
DROP TABLE IF EXISTS hot_coil;
DROP TABLE IF EXISTS cold_coil;
DROP TABLE IF EXISTS products;

CREATE TABLE products (
  product_id   INT PRIMARY KEY AUTO_INCREMENT,
  provider     VARCHAR(255),
  product_type VARCHAR(255),
  material     VARCHAR(255),
  created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  status       VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==========================
-- COLD_COIL (1-1 products)
-- ==========================
CREATE TABLE cold_coil (
  product_id  INT PRIMARY KEY,
  yield_str   INT,
  elongation  INT,
  thickness   INT,
  load_cell   INT,
  ir_piro     INT,
  termokup    INT,
  CONSTRAINT fk_cold_coil_product
    FOREIGN KEY (product_id) REFERENCES products(product_id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==========================
-- HOT_COIL (1-1 products)
-- ==========================
CREATE TABLE hot_coil (
  product_id      INT PRIMARY KEY,
  tensile_str     INT,
  elongation      FLOAT,
  thickness       INT,
  lazer_distance  DOUBLE,
  ir_piro         DOUBLE,
  pressure_value  DOUBLE,
  CONSTRAINT fk_hot_coil_product
    FOREIGN KEY (product_id) REFERENCES products(product_id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ==========================
-- PLATES (1-1 products)
-- ==========================
CREATE TABLE plates (
  product_id      INT PRIMARY KEY,
  speed_value     INT,
  pressure_value  INT,
  lvdt            INT,
  thickness       INT,
  length          INT,
  width           INT,
  ir_piro         INT,
  CONSTRAINT fk_plates_product
    FOREIGN KEY (product_id) REFERENCES products(product_id)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
