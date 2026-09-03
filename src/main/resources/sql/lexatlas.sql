-- ============================================================
-- LexAtlas 法枢 · RAG-Powered Legal Intelligence Platform — 数据库初始化脚本
-- 版本: v2.0 (开源版)
-- 说明: 本脚本包含建表语句与演示数据，不包含真实用户信息或固定账号。
--       初始管理员由 ADMIN_USERNAME / ADMIN_PASSWORD 环境变量安全创建。
-- 适用: MySQL 8.0+
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for law_conversation
-- ----------------------------
DROP TABLE IF EXISTS `law_conversation`;
CREATE TABLE `law_conversation`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '关联用户ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '会话标题(首次提问自动生成)',
  `message_count` int NOT NULL DEFAULT 0 COMMENT '消息条数',
  `last_active` datetime NULL DEFAULT NULL COMMENT '最后活跃时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_last_active`(`last_active` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '法律咨询会话表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for law_feedback
-- ----------------------------
DROP TABLE IF EXISTS `law_feedback`;
CREATE TABLE `law_feedback`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `message_id` bigint NOT NULL COMMENT '关联消息ID',
  `user_id` bigint NOT NULL COMMENT '评价用户ID',
  `rating` tinyint NOT NULL COMMENT '评分: 1有用 -1无用',
  `comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '补充说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_message_id`(`message_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户反馈表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for law_knowledge_base
-- ----------------------------
DROP TABLE IF EXISTS `law_knowledge_base`;
CREATE TABLE `law_knowledge_base`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文档名称',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '文档描述',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '法律领域分类',
  `file_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '原始文件存储路径(MinIO)',
  `file_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件类型: pdf/docx/txt',
  `file_size` bigint NULL DEFAULT NULL COMMENT '文件大小(字节)',
  `chunk_count` int NOT NULL DEFAULT 0 COMMENT '切片数量',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'uploading' COMMENT '状态: uploading/processing/ready/failed',
  `error_msg` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '错误信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '法律知识库文档表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for law_message
-- ----------------------------
DROP TABLE IF EXISTS `law_message`;
CREATE TABLE `law_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `conversation_id` bigint NOT NULL COMMENT '关联会话ID',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色: user/assistant',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容(支持Markdown)',
  `sources` json NULL COMMENT '引用来源(文档名+章节+页码)',
  `retrieval_log` json NULL COMMENT '检索过程日志(用于可视化)',
  `feedback` tinyint NOT NULL DEFAULT 0 COMMENT '反馈: 1有用 -1无用 0未评',
  `is_fallback` tinyint NOT NULL DEFAULT 0 COMMENT '是否触发兜底回答',
  `tokens_used` int NULL DEFAULT NULL COMMENT 'Token消耗',
  `response_time` int NULL DEFAULT NULL COMMENT '响应时间(毫秒)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_conversation_id`(`conversation_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '对话消息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for sys_ai_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_ai_config`;
CREATE TABLE `sys_ai_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置键',
  `config_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '配置值',
  `value_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'string' COMMENT '值类型: string/integer/float',
  `group_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分组: rag/llm/cache/safety',
  `label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '前端展示名称',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '描述',
  `default_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '出厂默认值',
  `min_value` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最小值约束',
  `max_value` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '最大值约束',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_config_key`(`config_key` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = 'AI配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_ai_config (RAG 运行时参数，可在管理后台调整)
-- ----------------------------
INSERT INTO `sys_ai_config` VALUES (1, 'rag.vector_top_k', '20', 'integer', 'rag', '向量召回 TopK', '向量检索返回的最大候选数量', '20', '5', '100', NOW(), NOW(), 0);
INSERT INTO `sys_ai_config` VALUES (2, 'rag.bm25_top_k', '20', 'integer', 'rag', 'BM25 召回 TopK', '关键词检索返回的最大候选数量', '20', '5', '100', NOW(), NOW(), 0);
INSERT INTO `sys_ai_config` VALUES (3, 'rag.rrf_top_n', '20', 'integer', 'rag', 'RRF 融合 TopN', 'RRF 融合后保留的最大数量', '25', '5', '200', NOW(), NOW(), 0);
INSERT INTO `sys_ai_config` VALUES (4, 'rag.rerank_top_k', '5', 'integer', 'rag', '重排序 TopK', 'Cross-Encoder 重排序后的最终数量', '5', '1', '20', NOW(), NOW(), 0);
INSERT INTO `sys_ai_config` VALUES (5, 'safety.confidence_threshold', '0.3', 'float', 'safety', '置信度阈值', '低于此值触发知识库未匹配兜底', '0.3', '0.0', '1.0', NOW(), NOW(), 0);
INSERT INTO `sys_ai_config` VALUES (6, 'cache.freq_threshold', '3', 'integer', 'cache', '缓存频次阈值', '同一问题被问几次后触发缓存写入', '3', '1', '100', NOW(), NOW(), 0);
INSERT INTO `sys_ai_config` VALUES (7, 'llm.model', 'qwen-plus', 'string', 'llm', '模型名称', '调用的大语言模型名称（如 qwen-plus / qwen-turbo / qwen-max）', 'qwen-plus', NULL, NULL, NOW(), NOW(), 0);
INSERT INTO `sys_ai_config` VALUES (8, 'llm.chat_temperature', '0.3', 'float', 'llm', '普通对话温度', '非流式对话（QueryRewriter/SafetyGuard）的采样温度', '0.3', '0.0', '1.0', NOW(), NOW(), 0);
INSERT INTO `sys_ai_config` VALUES (9, 'llm.streaming_temperature', '0.7', 'float', 'llm', '流式对话温度', '流式生成回答的采样温度，越高越发散', '0.7', '0.0', '1.0', NOW(), NOW(), 0);
INSERT INTO `sys_ai_config` VALUES (10, 'llm.timeout_seconds', '60', 'integer', 'llm', '超时时间(秒)', '单次 LLM 请求的最长等待时间（秒）', '60', '10', '300', NOW(), NOW(), 0);
INSERT INTO `sys_ai_config` VALUES (11, 'cache.ttl_hours', '24', 'integer', 'cache', '缓存有效时间(小时)', '高频问题缓存在 Redis 中的保留时长', '24', '1', '168', NOW(), NOW(), 0);
INSERT INTO `sys_ai_config` VALUES (12, 'rag.rrf_k_constant', '60', 'integer', 'rag', 'RRF 常数 K', 'RRF 公式经验常数，值越大头部优势越小', '60', '1', '200', NOW(), NOW(), 0);

-- ----------------------------
-- Table structure for sys_audit_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_audit_log`;
CREATE TABLE `sys_audit_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NULL DEFAULT NULL COMMENT '操作用户ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '操作用户名',
  `operation` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '操作描述',
  `method` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT 'HTTP方法: GET/POST/PUT/DELETE',
  `request_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '请求URL',
  `ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '客户端IP',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '操作状态: 1成功 0失败',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_username`(`username` ASC) USING BTREE,
  INDEX `idx_operation`(`operation`(64) ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '操作审计日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_favorite
-- ----------------------------
DROP TABLE IF EXISTS `sys_favorite`;
CREATE TABLE `sys_favorite`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '收藏用户ID',
  `message_id` bigint NOT NULL COMMENT '被收藏的消息ID',
  `conversation_id` bigint NULL DEFAULT NULL COMMENT '所属会话ID',
  `note` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '收藏备注',
  `create_time` datetime NOT NULL COMMENT '收藏时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_message`(`user_id` ASC, `message_id` ASC) USING BTREE COMMENT '同一用户不可重复收藏同一条消息',
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_message_id`(`message_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '消息收藏表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for sys_notification
-- ----------------------------
DROP TABLE IF EXISTS `sys_notification`;
CREATE TABLE `sys_notification`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '接收用户ID',
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '通知内容',
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'system' COMMENT '通知类型: system/security/activity',
  `is_read` tinyint NOT NULL DEFAULT 0 COMMENT '是否已读: 0未读 1已读',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_is_read`(`is_read` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统通知表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_notification
-- ----------------------------
-- 默认不插入用户通知，避免引用尚未创建的用户。

-- ----------------------------
-- Table structure for sys_question_template
-- ----------------------------
DROP TABLE IF EXISTS `sys_question_template`;
CREATE TABLE `sys_question_template`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '模板内容（完整问题文本）',
  `category` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'general' COMMENT '分类',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序序号，越小越靠前',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0禁用',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0正常 1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_category`(`category` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_sort_order`(`sort_order` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '问题模板表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_question_template (常见法律问题快捷模板)
-- ----------------------------
INSERT INTO `sys_question_template` VALUES (1, '劳动合同被违约怎么办', '公司突然要求我在无任何违纪情况下离职，并且拒绝支付经济补偿金，我应该如何通过法律途径维权？', '劳动法', 1, 1, NOW(), NOW(), 0);
INSERT INTO `sys_question_template` VALUES (2, '试用期被辞退有补偿吗', '我在试用期内被公司以"不符合录用条件"为由辞退，但公司没有明确告知过录用条件，我能获得赔偿吗？', '劳动法', 2, 1, NOW(), NOW(), 0);
INSERT INTO `sys_question_template` VALUES (3, '工资被拖欠怎么维权', '公司已经连续两个月没有发放工资，HR一直推脱，我可以通过哪些法律途径追讨拖欠工资？', '劳动法', 3, 1, NOW(), NOW(), 0);
INSERT INTO `sys_question_template` VALUES (4, '婚前财产离婚怎么分', '婚前我个人购买的房产，结婚后用共同积蓄还贷，离婚时这套房产应该如何分割？', '婚姻家庭', 4, 1, NOW(), NOW(), 0);
INSERT INTO `sys_question_template` VALUES (5, '离婚后孩子抚养权归谁', '我和配偶协议离婚，孩子3岁，我们双方都想要抚养权，法院一般会如何判决？', '婚姻家庭', 5, 1, NOW(), NOW(), 0);
INSERT INTO `sys_question_template` VALUES (6, '合同违约如何索赔', '我与供应商签订了采购合同，对方逾期交货导致我损失了客户订单，我可以要求哪些赔偿？', '合同法', 6, 1, NOW(), NOW(), 0);
INSERT INTO `sys_question_template` VALUES (7, '电商购物遭遇假货维权', '在某电商平台购买了一件商品，收到后确认是假冒伪劣产品，我可以要求退款并索赔吗？', '消费者权益', 7, 1, NOW(), NOW(), 0);
INSERT INTO `sys_question_template` VALUES (8, '房东不退押金怎么办', '租房合同到期，我按时搬离且房屋没有损坏，但房东以各种理由拒绝退还押金，我该如何处理？', '合同法', 8, 1, NOW(), NOW(), 0);
INSERT INTO `sys_question_template` VALUES (9, '遭遇网络诈骗如何报案', '我被网络诈骗损失了2万元，已经掌握了对方的账号和转账记录，应该向哪个部门报案，能追回来吗？', '刑法', 9, 1, NOW(), NOW(), 0);
INSERT INTO `sys_question_template` VALUES (10, '交通事故对方不赔怎么办', '我被对方车辆追尾，对方全责，但保险公司迟迟不赔付，我应该怎么处理？', '民法', 10, 1, NOW(), NOW(), 0);
INSERT INTO `sys_question_template` VALUES (11, '个人信息被泄露如何维权', '我发现某公司将我的个人信息出售给第三方，导致我频繁接到骚扰电话，我有哪些法律救济途径？', '个人信息保护', 11, 1, NOW(), NOW(), 0);
INSERT INTO `sys_question_template` VALUES (12, '房屋买卖合同纠纷', '我已支付购房定金，但卖方反悔不想卖了，我是否可以要求双倍返还定金？', '合同法', 12, 1, NOW(), NOW(), 0);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码(BCrypt加密)',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '手机号',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '头像URL',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'user' COMMENT '角色: admin/user',
  `legal_profile` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '法律档案 JSON',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态: 1正常 0禁用',
  `last_login` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
  INDEX `idx_phone`(`phone` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '系统用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
-- 不提供公开的固定密码账号。应用首次启动时按环境变量创建管理员。

SET FOREIGN_KEY_CHECKS = 1;
