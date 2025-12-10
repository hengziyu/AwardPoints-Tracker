package org.example.config;

public final class Config {
    private Config() {
    }

    // 文件路径
    public static final String RAW_SOURCE_PATH = "Raw_Source.xlsx";
    public static final String AWARDS_SUMMARY_PATH = "Awards_Summary.xlsx";
    public static final String STUDENT_AWARDS_PATH = "Student_Awards.xlsx";
    public static final String DB_PATH = "student.db";
    public static final String NULL_TEMPLATE_FILE = "null.xlsx"; // 修正与实际文件名一致

    // 随机生成控制
    public static final boolean USE_RANDOM_DATA = true;
    public static final int QUANTITY = 10;
    public static final int MAX_AWARDS = 20;
    public static final boolean RANDOM_AWARDS_COUNT = true;

    // 工作表
    public static final String SHEET_MAIN = "Sheet1";

    // Excel 列标题 (与已产生文件严格一致)
    public static final String COL_STUDENT_ID = "学号";
    public static final String COL_NAME = "姓名";
    public static final String COL_CLASS = "班级";
    public static final String COL_CERT_TOTAL = "证书总分"; // 统一为“总分”
    public static final String COL_AWARD_TOTAL = "奖项总分";
    public static final String COL_RECORDED_COUNT = "已录入奖项数";
    public static final String COL_AWARD_LABEL_PREFIX = "奖项"; // 去掉“标签_”保持与文件一致
    public static final String COL_AWARD_IMAGE_PREFIX = "证书图片";    // 证书图片1..N (仅源构建使用)

    // 图片
    public static final String PLACEHOLDER_IMAGE = "https://via.placeholder.com/1200x800?text=Loading";
    public static final String[] RANDOM_IMAGE_POOL = {"https://pics0.baidu.com/feed/0e2442a7d933c8959b2c26032f0464fe82020019.jpeg", "https://pics4.baidu.com/feed/a2cc7cd98d1001e91284fb12d96e6ee255e797e5.jpeg", "https://pics6.baidu.com/feed/18d8bc3eb13533fa9cf6bda9c9b3e81140345b66.jpeg", "https://pics0.baidu.com/feed/0824ab18972bd4077a0bfd7819e98b5f0db309d0.jpeg"};

    // 随机学生生成
    public static final String[] RANDOM_SURNAME_POOL = {
            "王", "李", "张", "刘", "陈", "杨", "赵", "黄", "周", "吴",
            "徐", "孙", "朱", "马", "胡", "郭", "何", "高", "林", "罗"
    };
    public static final String[] RANDOM_GIVEN_NAME_POOL = {
            "伟", "芳", "娜", "敏", "静", "秀英", "丽", "强", "磊", "军",
            "洋", "艳", "峰", "婷", "浩", "婷", "超", "丽丽", "娟", "豪"
    };
    public static final String[] RANDOM_CLASS_PREFIX_POOL = {
            "网络", "物联网", "信安", "移动互联", "软件", "大数据", "人工智能", "云计算"
    };
    public static final int CLASS_SUFFIX_MIN = 1000;
    public static final int CLASS_SUFFIX_MAX = 9999;
    public static final long STUDENT_ID_MIN = 1_000_000_000L;      // 10位起
    public static final long STUDENT_ID_MAX = 99_999_999_999L;     // 11位止

    // 奖项随机生成
    public static final String[] RANDOM_AWARD_NAME_POOL = {
            "码蹄杯", "蓝桥杯", "西门子杯", "网络安全", "全国职业技能大赛",
            "数学竞赛", "程序设计大赛", "创新创业", "电子设计", "人工智能挑战赛"
    };
    public static final String[] RANDOM_AWARD_GRADE_POOL = {
            "国奖", "省奖", "校奖", "市奖", "院奖"
    };

    // 分类标签与积分
    public static final String CATEGORY_CERT = "证书";
    public static final String CATEGORY_NATIONAL = "国";
    public static final String CATEGORY_PROVINCE_CITY = "省市";
    public static final String CATEGORY_SCHOOL = "校";
    public static final String CATEGORY_COLLEGE = "院";
    public static final String CATEGORY_NONE = "无";

    public static final double SCORE_CERT = 0.2;
    public static final double SCORE_NATIONAL = 0.8;
    public static final double SCORE_PROVINCE_CITY = 0.5;
    public static final double SCORE_SCHOOL = 0.3;
    public static final double SCORE_COLLEGE = 0.2;
    public static final double SCORE_NONE = 0.0;

    // AwardDialog 尺寸与缩放
    public static final double IMAGE_MIN_PADDING = 40.0;
    public static final double IMAGE_MIN_VERTICAL_RESERVED = 160.0;
    public static final double IMAGE_MIN_FIT_WIDTH = 50.0;
    public static final double IMAGE_MIN_FIT_HEIGHT = 50.0;

    // 预加载开关
    public static final boolean ENABLE_IMAGE_PRELOAD = true;

    // （暂未接入）日志前缀预留：如需要再在 Logger 输出中拼接
    public static final String LOG_PREFIX_BUILD_NULL = "[BuildNull]";
    public static final String LOG_PREFIX_AWARD_DIALOG = "[AwardDialog]";
}