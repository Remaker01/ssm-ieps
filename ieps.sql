/*
IEPS schema DDL only.
Sample data is preserved in ieps_sample_data.sql for reference.
This file keeps the original table model and only applies low-risk compatibility fixes.
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for ieps_file_hub
-- ----------------------------
DROP TABLE IF EXISTS `ieps_file_hub`;
CREATE TABLE `ieps_file_hub` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `type_num` varchar(30) DEFAULT '-1',
  `user_num` varchar(20) DEFAULT NULL,
  `academy` varchar(100) DEFAULT NULL COMMENT '上传人的所在学院',
  `file_name` varchar(100) DEFAULT NULL,
  `object_key` varchar(512) DEFAULT NULL,
  `storage_provider` varchar(20) DEFAULT 'cos',
  `content_type` varchar(100) DEFAULT NULL,
  `file_size` int(11) DEFAULT NULL,
  `file_kind` int(10) DEFAULT '0' COMMENT '-1：普通文件；0：重要通知文件；1：常用下载文件；2：申请项目文件；3：立项评审结果附件；4：中期检查评审结果附件；5：结题评审结果附件。',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_file_hub_type_num_kind` (`type_num`, `file_kind`),
  KEY `idx_file_hub_user_num` (`user_num`),
  KEY `idx_file_hub_file_kind` (`file_kind`),
  KEY `idx_file_hub_file_name` (`file_name`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for ieps_download_task
-- ----------------------------
DROP TABLE IF EXISTS `ieps_download_task`;
CREATE TABLE `ieps_download_task` (
  `task_id` varchar(32) NOT NULL,
  `user_num` varchar(20) NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'pending',
  `file_count` int(11) NOT NULL DEFAULT '0',
  `source_files` text,
  `zip_object_key` varchar(512) DEFAULT NULL,
  `zip_file_name` varchar(255) DEFAULT NULL,
  `error_message` varchar(500) DEFAULT NULL,
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `started_time` datetime DEFAULT NULL,
  `finished_time` datetime DEFAULT NULL,
  `expire_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`task_id`),
  KEY `idx_download_task_user_status` (`user_num`, `status`),
  KEY `idx_download_task_expire_time` (`expire_time`),
  KEY `idx_download_task_finished_time` (`finished_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for ieps_inform
-- ----------------------------
DROP TABLE IF EXISTS `ieps_inform`;
CREATE TABLE `ieps_inform` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '系统通知id',
  `head` varchar(100) DEFAULT NULL COMMENT '通知标题',
  `publisher` varchar(20) DEFAULT NULL COMMENT '发布通知者',
  `role_id` int(11) DEFAULT NULL COMMENT '可见角色id',
  `subject` varchar(100) DEFAULT NULL,
  `content` text COMMENT '通知内容',
  `files` varchar(500) DEFAULT NULL,
  `pubdate` datetime DEFAULT NULL COMMENT '发布时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_inform_publisher` (`publisher`),
  KEY `idx_inform_role_id` (`role_id`),
  KEY `idx_inform_subject` (`subject`),
  KEY `idx_inform_pubdate` (`pubdate`)
) ENGINE=InnoDB AUTO_INCREMENT=400012 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for ieps_item
-- ----------------------------
DROP TABLE IF EXISTS `ieps_item`;
CREATE TABLE `ieps_item` (
  `item_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '项目id',
  `item_num` varchar(20) DEFAULT NULL COMMENT '项目编号',
  `item_name` varchar(50) DEFAULT NULL COMMENT '项目名称',
  `leader_num` varchar(20) DEFAULT NULL COMMENT '项目负责人学号',
  `leader_name` varchar(50) DEFAULT NULL COMMENT '项目负责人',
  `tutor_num` varchar(20) DEFAULT NULL COMMENT '职工号',
  `tutor_name` varchar(50) DEFAULT NULL COMMENT '指导老师姓名',
  `item_status` int(11) DEFAULT NULL COMMENT '<!-- 项目状态：1：申请中；2：立项评审；3：已立项；4：立项失败；5：中期检查; 6: 待结题；7：结题评审；8：结题成功；9：结题失败-->',
  `item_date` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '申报时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`item_id`),
  UNIQUE KEY `uk_item_num` (`item_num`),
  KEY `idx_item_leader_num` (`leader_num`),
  KEY `idx_item_tutor_num` (`tutor_num`),
  KEY `idx_item_status` (`item_status`)
) ENGINE=InnoDB AUTO_INCREMENT=500110 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for ieps_item_info
-- ----------------------------
DROP TABLE IF EXISTS `ieps_item_info`;
CREATE TABLE `ieps_item_info` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '项目详细id',
  `item_num` varchar(20) DEFAULT NULL COMMENT '项目编号',
  `item_level` int(11) DEFAULT '1' COMMENT '项目级别：1: 无；2：校级；3：省区级；4：国家级',
  `item_type` int(11) DEFAULT '1' COMMENT '项目类型：1：创新训练；2：创业训练；3：创业实践',
  `summary` varchar(500) DEFAULT NULL COMMENT '项目简介',
  `college_funds` decimal(10,2) DEFAULT '0.00' COMMENT '校拨经费',
  `govern_funds` decimal(10,2) DEFAULT '0.00' COMMENT '财政经费',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_item_info_item_num` (`item_num`)
) ENGINE=InnoDB AUTO_INCREMENT=510160 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for ieps_perm
-- ----------------------------
DROP TABLE IF EXISTS `ieps_perm`;
CREATE TABLE `ieps_perm` (
  `perm_id` int(11) NOT NULL AUTO_INCREMENT COMMENT '功能id',
  `perm_name` varchar(50) DEFAULT NULL COMMENT '功能名称',
  `perm_type` varchar(20) DEFAULT 'menu' COMMENT '权限类型（menu/permission）',
  `url` varchar(30) DEFAULT NULL COMMENT '功能链接',
  `icon` varchar(30) DEFAULT '&#xe63c;' COMMENT '菜单图标',
  `parent_id` int(11) DEFAULT NULL COMMENT '父菜单id',
  `perm_code` varchar(30) DEFAULT NULL COMMENT '具体权限',
  `perm_desc` varchar(50) DEFAULT NULL COMMENT '功能描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`perm_id`),
  KEY `idx_perm_parent_id` (`parent_id`),
  KEY `idx_perm_type` (`perm_type`)
) ENGINE=InnoDB AUTO_INCREMENT=300015 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for ieps_review
-- ----------------------------
DROP TABLE IF EXISTS `ieps_review`;
CREATE TABLE `ieps_review` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '评审id',
  `user_num` varchar(20) DEFAULT NULL COMMENT '评委职工号',
  `item_num` varchar(20) DEFAULT NULL COMMENT '项目id',
  `review_score` decimal(10,2) DEFAULT '0.00' COMMENT '评委打分',
  `review_option` varchar(200) DEFAULT NULL COMMENT '评审意见',
  `review_type` int(11) DEFAULT '1' COMMENT '评审类型：1：立项评审；2：中期检查；3：结题评审',
  `review_level` int(11) DEFAULT '1' COMMENT '评审级别： 1：院级评审；2：校级评审；3：省区级评审；4：国家级评审',
  `review_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '评审时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_review_item_stage` (`item_num`, `review_type`, `review_level`),
  KEY `idx_review_user_num` (`user_num`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for ieps_role
-- ----------------------------
DROP TABLE IF EXISTS `ieps_role`;
CREATE TABLE `ieps_role` (
  `role_id` int(11) NOT NULL COMMENT '角色id',
  `role_name` varchar(20) DEFAULT NULL COMMENT '角色名',
  `role_desc` varchar(50) DEFAULT NULL COMMENT '角色描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for ieps_role_perm
-- ----------------------------
DROP TABLE IF EXISTS `ieps_role_perm`;
CREATE TABLE `ieps_role_perm` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '角色权限id',
  `role_id` int(11) DEFAULT NULL COMMENT '角色id',
  `perm_id` int(11) DEFAULT NULL COMMENT '权限id',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_role_perm_perm_id` (`perm_id`),
  KEY `idx_role_perm_role_perm` (`role_id`, `perm_id`)
) ENGINE=InnoDB AUTO_INCREMENT=320066 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for ieps_user
-- ----------------------------
DROP TABLE IF EXISTS `ieps_user`;
CREATE TABLE `ieps_user` (
  `user_num` varchar(20) NOT NULL COMMENT '学号/教师号',
  `user_pwd` varchar(255) DEFAULT NULL COMMENT '密码哈希',
  `user_status` int(2) DEFAULT '1' COMMENT '状态禁用/启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`user_num`),
  KEY `idx_user_status` (`user_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for ieps_user_info
-- ----------------------------
DROP TABLE IF EXISTS `ieps_user_info`;
CREATE TABLE `ieps_user_info` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '用户详细表id',
  `user_num` varchar(20) DEFAULT NULL COMMENT '用户账号',
  `user_name` varchar(50) DEFAULT NULL,
  `user_img` varchar(512) DEFAULT '/static/images/default.jpg' COMMENT '用户头像',
  `photo_num` varchar(20) DEFAULT '15295893830' COMMENT '手机号码',
  `email` varchar(100) DEFAULT '911293924@qq.com' COMMENT '邮箱',
  `title` int(5) DEFAULT '0' COMMENT '职称：0：学生；1：助理研究员；2：讲师；3：高级实验师；4：副教授；5：教授',
  `sex` int(5) DEFAULT '0' COMMENT '性别：0：男；1：女',
  `academy` varchar(50) DEFAULT '计算机与信息安全学院' COMMENT '学院',
  `grade` varchar(10) DEFAULT '2015' COMMENT '年级',
  `major` varchar(30) DEFAULT '软件工程' COMMENT '专业',
  `birth_date` date DEFAULT NULL COMMENT '出生日期',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_num` (`user_num`) USING BTREE COMMENT '用户userNum唯一',
  KEY `idx_user_info_academy` (`academy`),
  KEY `idx_user_info_user_name` (`user_name`)
) ENGINE=InnoDB AUTO_INCREMENT=300362 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for ieps_user_item
-- ----------------------------
DROP TABLE IF EXISTS `ieps_user_item`;
CREATE TABLE `ieps_user_item` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '用户项目id',
  `user_num` varchar(20) DEFAULT NULL COMMENT '用户id',
  `item_num` varchar(20) DEFAULT NULL COMMENT '项目id',
  `identity` int(11) DEFAULT NULL COMMENT '身份标识：负责人/成员/指导老师/院内评委/院内评委组长/校内评委/校内评委组长',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_item_user_num` (`user_num`),
  KEY `idx_user_item_identity` (`identity`),
  KEY `idx_user_item_item_identity` (`item_num`, `identity`)
) ENGINE=InnoDB AUTO_INCREMENT=743 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Table structure for ieps_user_role
-- ----------------------------
DROP TABLE IF EXISTS `ieps_user_role`;
CREATE TABLE `ieps_user_role` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '用户角色id',
  `user_num` varchar(20) DEFAULT NULL COMMENT '用户num',
  `role_id` int(11) DEFAULT NULL COMMENT '角色id',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_role_role_id` (`role_id`),
  KEY `idx_user_role_user_role` (`user_num`, `role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=210394 DEFAULT CHARSET=utf8;

-- ----------------------------
-- Foreign key constraints
-- ----------------------------
ALTER TABLE `ieps_item_info`
  ADD CONSTRAINT `fk_item_info_item_num`
    FOREIGN KEY (`item_num`) REFERENCES `ieps_item` (`item_num`)
    ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE `ieps_review`
  ADD CONSTRAINT `fk_review_item_num`
    FOREIGN KEY (`item_num`) REFERENCES `ieps_item` (`item_num`)
    ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE `ieps_role_perm`
  ADD CONSTRAINT `fk_role_perm_perm_id`
    FOREIGN KEY (`perm_id`) REFERENCES `ieps_perm` (`perm_id`)
    ON UPDATE CASCADE ON DELETE CASCADE,
  ADD CONSTRAINT `fk_role_perm_role_id`
    FOREIGN KEY (`role_id`) REFERENCES `ieps_role` (`role_id`)
    ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE `ieps_user_info`
  ADD CONSTRAINT `fk_user_info_user_num`
    FOREIGN KEY (`user_num`) REFERENCES `ieps_user` (`user_num`)
    ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE `ieps_user_item`
  ADD CONSTRAINT `fk_user_item_item_num`
    FOREIGN KEY (`item_num`) REFERENCES `ieps_item` (`item_num`)
    ON UPDATE CASCADE ON DELETE CASCADE,
  ADD CONSTRAINT `fk_user_item_user_num`
    FOREIGN KEY (`user_num`) REFERENCES `ieps_user` (`user_num`)
    ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE `ieps_user_role`
  ADD CONSTRAINT `fk_user_role_role_id`
    FOREIGN KEY (`role_id`) REFERENCES `ieps_role` (`role_id`)
    ON UPDATE CASCADE ON DELETE CASCADE,
  ADD CONSTRAINT `fk_user_role_user_num`
    FOREIGN KEY (`user_num`) REFERENCES `ieps_user` (`user_num`)
    ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE `ieps_file_hub`
  ADD CONSTRAINT `fk_file_hub_user_num`
    FOREIGN KEY (`user_num`) REFERENCES `ieps_user` (`user_num`)
    ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE `ieps_inform`
  ADD CONSTRAINT `fk_inform_publisher`
    FOREIGN KEY (`publisher`) REFERENCES `ieps_user` (`user_num`)
    ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE `ieps_review`
  ADD CONSTRAINT `fk_review_user_num`
    FOREIGN KEY (`user_num`) REFERENCES `ieps_user` (`user_num`)
    ON UPDATE CASCADE ON DELETE CASCADE;

SET FOREIGN_KEY_CHECKS=1;
