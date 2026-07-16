package com.ieps.util;

import com.ieps.common.Const;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class RandomDataUtil {

    private static final int DEFAULT_USER_COUNT = 2000;
    private static final int COLLEGE_EXPERT_GROUP_COUNT = 4;
    private static final int ACADEMY_GROUPS_PER_ACADEMY = 2;
    private static final int REVIEW_GROUP_SIZE = 3;
    private static final int TUTOR_COUNT = 150;
    private static final int ITEM_COUNT = 800;
    private static final int INFORM_COUNT = 60;
    private static final String DEFAULT_PASSWORD = Const.UNIFORM_USERPWD;
    private static final String DEFAULT_USER_IMG = Const.UNIFORM_USERIMG;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Random RANDOM = new Random(20260629L);

    private static final List<RoleSeed> ROLES = Arrays.asList(
            new RoleSeed(Const.ROLEID_STU, "学生", "学生"),
            new RoleSeed(Const.ROLEID_TUTOR, "教师", "教师"),
            new RoleSeed(Const.ROLEID_ACADEMY_EXPERT, "院内专家", "院内专家"),
            new RoleSeed(Const.ROLEID_ACADEMY, "院内管理员", "院内管理员"),
            new RoleSeed(Const.ROLEID_COLLEGE_EXPERT, "校内专家", "校内专家"),
            new RoleSeed(Const.ROLEID_COLLEGE, "校内管理员", "校内管理员")
    );

    private static final List<PermSeed> PERMS = Arrays.asList(
            new PermSeed(300001, "基本设置", "menu", "", "&#xe631;", -1, null, "基本设置"),
            new PermSeed(300002, "个人资料", "permission", "/pages/basic/userInfo.html", "&#xe62a;", 300001, null, "个人信息"),
            new PermSeed(300003, "修改密码", "permission", "/pages/basic/modifyPwd.html", "&#xe642;", 300001, null, "修改密码"),
            new PermSeed(300004, "项目管理", "menu", null, "&#xe60a;", -1, null, "项目管理"),
            new PermSeed(300005, "所有项目", "permission", "/pages/item/allItem.html", "&#xe630;", 300004, null, "所有项目"),
            new PermSeed(300006, "立项阶段", "permission", "/pages/item/applyItem.html", "&#xe64c;", 300004, null, "立项阶段"),
            new PermSeed(300007, "中期检查", "permission", "/pages/item/inspectItem.html", "&#xe62e;", 300004, null, "中期检查"),
            new PermSeed(300008, "结题评审", "permission", "/pages/item/endItem.html", "&#xe605;", 300004, null, "结题阶段"),
            new PermSeed(300009, "系统管理", "menu", null, "&#xe66c;", -1, null, "系统管理"),
            new PermSeed(300010, "用户管理", "permission", "/pages/admin/userAdmin.html", "&#xe770;", 300009, null, "用户管理"),
            new PermSeed(300011, "角色管理", "permission", "/pages/admin/roleAdmin.html", "&#xe63c;", 300009, null, "角色管理"),
            new PermSeed(300012, "权限管理", "permission", "/pages/admin/permAdmin.html", "&#xe656;", 300009, null, "权限管理"),
            new PermSeed(300013, "通知管理", "permission", "/pages/admin/informAdmin.html", "&#xe705;", 300009, null, "通知管理"),
            new PermSeed(300014, "文件管理", "permission", "/pages/admin/fileAdmin.html", "&#xe621;", 300009, null, "文件管理")
    );

    private static final List<AcademyProfile> ACADEMIES = Arrays.asList(
            new AcademyProfile("计算机与信息安全学院", "计信", Arrays.asList("软件工程", "网络工程", "信息安全")),
            new AcademyProfile("信息与通信学院", "信通", Arrays.asList("通信工程", "电子信息工程", "物联网工程")),
            new AcademyProfile("电子工程与自动化学院", "电自", Arrays.asList("自动化", "电气工程", "智能制造")),
            new AcademyProfile("机电工程学院", "机电", Arrays.asList("机械设计制造及其自动化", "工业工程", "车辆工程")),
            new AcademyProfile("商学院", "商学", Arrays.asList("市场营销", "电子商务", "会计学")),
            new AcademyProfile("数学与计算科学学院", "数科", Arrays.asList("数学与应用数学", "数据科学与大数据技术", "统计学")),
            new AcademyProfile("外国语学院", "外院", Arrays.asList("英语", "商务英语", "翻译")),
            new AcademyProfile("艺术与设计学院", "艺设", Arrays.asList("视觉传达设计", "数字媒体艺术", "产品设计"))
    );

    private static final List<Integer> STATUS_POOL = buildStatusPool();
    private static final List<String> ITEM_PREFIXES = Arrays.asList(
            "基于云平台的", "面向校园的", "融合物联网的", "基于大数据的",
            "面向赛事管理的", "聚焦实践教学的", "服务产学研协同的", "支持智能硬件的"
    );
    private static final List<String> ITEM_TOPICS = Arrays.asList(
            "创新训练助手", "项目孵化平台", "智能评审系统", "协同服务终端",
            "实验室管理模块", "低碳应用方案", "智慧巡检系统", "成长分析平台",
            "知识服务引擎", "校园创客终端", "成果展示门户", "资源调度平台"
    );
    private static final List<String> INFORM_TITLES = Arrays.asList(
            "关于创新创业项目过程检查的通知", "关于提交阶段材料的提醒", "关于开展项目培训的公告",
            "关于评审安排的通知", "关于经费填报的说明", "关于结题答辩的工作提示"
    );
    private RandomDataUtil() {
    }

    public static void main(String[] args) throws IOException {
        int userCount = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_USER_COUNT;
        Path outputPath = args.length > 0 ? Paths.get(args[0]).toAbsolutePath() : Paths.get("ieps_sample_data.sql").toAbsolutePath();
        GeneratedDataset dataset = generateDataset(userCount);
        Files.writeString(outputPath, renderSql(dataset), StandardCharsets.UTF_8);
        System.out.println("Generated sample data file: " + outputPath);
        System.out.println("Users=" + dataset.users.size()
                + ", Items=" + dataset.items.size()
                + ", Reviews=" + dataset.reviews.size()
                + ", Informs=" + dataset.informs.size());
        System.out.println("Bootstrap admin: " + Const.USERNUM_COLLEGE + " / " + DEFAULT_PASSWORD);
    }

    private static GeneratedDataset generateDataset(int userCount) {
        int fixedUserCount = 1
                + COLLEGE_EXPERT_GROUP_COUNT * REVIEW_GROUP_SIZE
                + ACADEMIES.size()
                + ACADEMIES.size() * ACADEMY_GROUPS_PER_ACADEMY * REVIEW_GROUP_SIZE
                + TUTOR_COUNT;
        if (userCount <= fixedUserCount) {
            throw new IllegalArgumentException("userCount must be greater than " + fixedUserCount);
        }

        GeneratedDataset dataset = new GeneratedDataset();
        dataset.roles.addAll(ROLES);
        dataset.perms.addAll(PERMS);
        dataset.rolePerms.addAll(buildRolePerms());

        GenerationContext context = new GenerationContext(dataset);
        createCollegeAdmin(context);
        createCollegeExperts(context);
        createAcademyUsers(context, userCount - fixedUserCount);
        createItems(context);
        createInforms(context);
        return dataset;
    }

    private static List<RolePermSeed> buildRolePerms() {
        List<RolePermSeed> rolePerms = new ArrayList<>();
        int rolePermId = 320001;
        Map<Integer, List<Integer>> roleToPerms = new LinkedHashMap<>();
        roleToPerms.put(Const.ROLEID_STU, Arrays.asList(300001, 300002, 300003, 300004, 300005, 300006, 300007, 300008, 300013));
        roleToPerms.put(Const.ROLEID_TUTOR, Arrays.asList(300001, 300002, 300003, 300004, 300005, 300006, 300007, 300008, 300013));
        roleToPerms.put(Const.ROLEID_ACADEMY_EXPERT, Arrays.asList(300001, 300002, 300003, 300004, 300005, 300006, 300007, 300008, 300013));
        roleToPerms.put(Const.ROLEID_ACADEMY, Arrays.asList(300001, 300002, 300003, 300004, 300005, 300006, 300007, 300008, 300009, 300010, 300011, 300012, 300013, 300014));
        roleToPerms.put(Const.ROLEID_COLLEGE_EXPERT, Arrays.asList(300001, 300002, 300003, 300004, 300005, 300006, 300007, 300008, 300013));
        roleToPerms.put(Const.ROLEID_COLLEGE, Arrays.asList(300001, 300002, 300003, 300004, 300005, 300006, 300007, 300008, 300009, 300010, 300011, 300012, 300013, 300014));
        for (Map.Entry<Integer, List<Integer>> entry : roleToPerms.entrySet()) {
            for (Integer permId : entry.getValue()) {
                rolePerms.add(new RolePermSeed(rolePermId++, entry.getKey(), permId));
            }
        }
        return rolePerms;
    }

    private static void createCollegeAdmin(GenerationContext context) {
        GeneratedUser user = createUser(
                context,
                Const.USERNUM_COLLEGE,
                "系统管理员",
                Const.ROLEID_COLLEGE,
                Const.ACADEMY_COLLEGE,
                "创新创业教育",
                "教工",
                5,
                1,
                1,
                LocalDate.of(1980, 6, 18)
        );
        context.collegeAdmin = user;
    }

    private static void createCollegeExperts(GenerationContext context) {
        for (int groupIndex = 1; groupIndex <= COLLEGE_EXPERT_GROUP_COUNT; groupIndex++) {
            ReviewGroup group = new ReviewGroup(Const.ACADEMY_COLLEGE, groupIndex, Const.ROLEID_COLLEGE_EXPERT);
            for (int memberIndex = 1; memberIndex <= REVIEW_GROUP_SIZE; memberIndex++) {
                String userNum = String.format(Locale.ROOT, "105%03d", context.nextCollegeExpertSequence());
                String userName = String.format(Locale.ROOT, "校级评审组%02d组组员%03d", groupIndex, memberIndex);
                GeneratedUser user = createUser(
                        context,
                        userNum,
                        userName,
                        Const.ROLEID_COLLEGE_EXPERT,
                        Const.ACADEMY_COLLEGE,
                        "创新创业评审",
                        "教工",
                        randomTeacherTitle(),
                        memberIndex % 2,
                        1,
                        randomBirthDate(1972, 1990)
                );
                group.members.add(user);
            }
            context.collegeReviewGroups.add(group);
        }
    }

    private static void createAcademyUsers(GenerationContext context, int studentCount) {
        List<Integer> tutorDistribution = distribute(TUTOR_COUNT, ACADEMIES.size());
        List<Integer> studentDistribution = distribute(studentCount, ACADEMIES.size());
        for (int academyIndex = 0; academyIndex < ACADEMIES.size(); academyIndex++) {
            AcademyProfile academy = ACADEMIES.get(academyIndex);
            createAcademyAdmin(context, academy, academyIndex + 1);
            createAcademyExperts(context, academy);
            createTutors(context, academy, tutorDistribution.get(academyIndex));
            createStudents(context, academy, studentDistribution.get(academyIndex));
        }
    }

    private static void createAcademyAdmin(GenerationContext context, AcademyProfile academy, int academyIndex) {
        String userNum = String.format(Locale.ROOT, "104%03d", academyIndex);
        GeneratedUser admin = createUser(
                context,
                userNum,
                academy.shortName + "管理员01",
                Const.ROLEID_ACADEMY,
                academy.name,
                academy.majors.get(0),
                "教工",
                4,
                academyIndex % 2,
                1,
                randomBirthDate(1975, 1988)
        );
        context.academyAdmins.put(academy.name, admin);
    }

    private static void createAcademyExperts(GenerationContext context, AcademyProfile academy) {
        List<ReviewGroup> groups = context.academyReviewGroups.computeIfAbsent(academy.name, key -> new ArrayList<>());
        for (int groupIndex = 1; groupIndex <= ACADEMY_GROUPS_PER_ACADEMY; groupIndex++) {
            ReviewGroup group = new ReviewGroup(academy.name, groupIndex, Const.ROLEID_ACADEMY_EXPERT);
            for (int memberIndex = 1; memberIndex <= REVIEW_GROUP_SIZE; memberIndex++) {
                String userNum = String.format(Locale.ROOT, "103%03d", context.nextAcademyExpertSequence());
                String userName = String.format(Locale.ROOT, "%s评审组%02d组组员%03d", academy.shortName, groupIndex, memberIndex);
                GeneratedUser expert = createUser(
                        context,
                        userNum,
                        userName,
                        Const.ROLEID_ACADEMY_EXPERT,
                        academy.name,
                        academy.majors.get(memberIndex % academy.majors.size()),
                        "教工",
                        randomTeacherTitle(),
                        memberIndex % 2,
                        1,
                        randomBirthDate(1974, 1991)
                );
                group.members.add(expert);
            }
            groups.add(group);
        }
    }

    private static void createTutors(GenerationContext context, AcademyProfile academy, int count) {
        List<GeneratedUser> tutors = context.tutorsByAcademy.computeIfAbsent(academy.name, key -> new ArrayList<>());
        for (int index = 1; index <= count; index++) {
            String userNum = String.format(Locale.ROOT, "102%03d", context.nextTutorSequence());
            GeneratedUser tutor = createUser(
                    context,
                    userNum,
                    String.format(Locale.ROOT, "%s导师%03d", academy.shortName, index),
                    Const.ROLEID_TUTOR,
                    academy.name,
                    academy.majors.get(index % academy.majors.size()),
                    "教工",
                    randomTeacherTitle(),
                    index % 2,
                    randomStatus(0.04D),
                    randomBirthDate(1976, 1994)
            );
            tutors.add(tutor);
        }
    }

    private static void createStudents(GenerationContext context, AcademyProfile academy, int count) {
        List<GeneratedUser> students = context.studentsByAcademy.computeIfAbsent(academy.name, key -> new ArrayList<>());
        for (int index = 1; index <= count; index++) {
            int studentNo = context.nextStudentSequence();
            String userNum = String.format(Locale.ROOT, "2023%06d", studentNo);
            int grade = 2022 + (studentNo % 4);
            GeneratedUser student = createUser(
                    context,
                    userNum,
                    String.format(Locale.ROOT, "%s学生%04d", academy.shortName, index),
                    Const.ROLEID_STU,
                    academy.name,
                    academy.majors.get(index % academy.majors.size()),
                    String.valueOf(grade),
                    0,
                    index % 2,
                    randomStatus(0.03D),
                    randomBirthDate(2001, 2005)
            );
            students.add(student);
            context.studentLoad.put(student.userNum, 0);
        }
    }

    private static GeneratedUser createUser(GenerationContext context, String userNum, String userName, int roleId,
                                            String academy, String major, String grade, int title, int sex,
                                            int userStatus, LocalDate birthDate) {
        String passwordHash = PasswordUtil.hashPassword(DEFAULT_PASSWORD);
        GeneratedUser user = new GeneratedUser(userNum, userName, roleId, academy, major, grade, title, sex, userStatus, birthDate, passwordHash);
        context.dataset.users.add(user);
        return user;
    }

    private static void createItems(GenerationContext context) {
        List<Integer> itemDistribution = distribute(ITEM_COUNT, ACADEMIES.size());
        List<Integer> statuses = new ArrayList<>(STATUS_POOL);
        Collections.shuffle(statuses, RANDOM);
        int statusIndex = 0;
        for (int academyIndex = 0; academyIndex < ACADEMIES.size(); academyIndex++) {
            AcademyProfile academy = ACADEMIES.get(academyIndex);
            List<GeneratedUser> students = context.studentsByAcademy.get(academy.name);
            List<GeneratedUser> tutors = context.tutorsByAcademy.get(academy.name);
            List<ReviewGroup> academyGroups = context.academyReviewGroups.get(academy.name);
            int itemCount = itemDistribution.get(academyIndex);
            for (int i = 0; i < itemCount; i++) {
                GeneratedUser leader = pickLeader(students, context.studentLoad);
                context.studentLoad.put(leader.userNum, context.studentLoad.get(leader.userNum) + 1);
                int teamSize = 3 + RANDOM.nextInt(3);
                List<GeneratedUser> members = pickAdditionalMembers(students, context.studentLoad, leader.userNum, teamSize - 1);
                for (GeneratedUser member : members) {
                    context.studentLoad.put(member.userNum, context.studentLoad.get(member.userNum) + 1);
                }
                GeneratedUser tutor = tutors.get(RANDOM.nextInt(tutors.size()));
                int itemId = context.nextItemId();
                String itemNum = String.format(Locale.ROOT, "2024%02d%06d", academyIndex + 1, itemId - 500000);
                int itemStatus = statuses.get(statusIndex++);
                int itemType = 1 + RANDOM.nextInt(3);
                int itemLevel = randomItemLevel(itemStatus);
                BigDecimal[] funds = buildFunds(itemLevel, itemStatus);
                LocalDateTime itemDate = randomDateTime(LocalDate.of(2024, 1, 1), LocalDate.of(2026, 5, 31));
                GeneratedItem item = new GeneratedItem(
                        itemId,
                        itemNum,
                        buildItemName(academy.shortName, itemId),
                        leader.userNum,
                        leader.userName,
                        tutor.userNum,
                        tutor.userName,
                        itemStatus,
                        itemLevel,
                        itemType,
                        buildSummary(academy, itemType, itemStatus, leader.userName),
                        funds[0],
                        funds[1],
                        itemDate
                );
                context.dataset.items.add(item);

                addUserItem(context, leader.userNum, item.itemNum, 2);
                addUserItem(context, tutor.userNum, item.itemNum, 3);
                for (GeneratedUser member : members) {
                    addUserItem(context, member.userNum, item.itemNum, 1);
                }

                if (itemStatus >= 2) {
                    ReviewGroup academyGroup = academyGroups.get(context.nextAcademyReviewGroupIndex(academy.name));
                    addReviewGroupAssignments(context, item.itemNum, academyGroup, 5, 4);
                    addReviews(context, item, academyGroup.members, 1, academyReviewLevel(itemStatus), academyReviewScoreRange(itemStatus));
                }

                if (itemStatus >= 5) {
                    ReviewGroup collegeGroup = context.collegeReviewGroups.get(context.nextCollegeReviewGroupIndex());
                    addReviewGroupAssignments(context, item.itemNum, collegeGroup, 7, 6);
                    addReviews(context, item, collegeGroup.members, 1, 2, collegeReviewScoreRange(itemStatus));
                    if (item.itemLevel >= 4 && itemStatus >= 7) {
                        addReviews(context, item, collegeGroup.members, 1, 3, governReviewScoreRange(itemStatus));
                    }
                }
            }
        }
    }

    private static void addReviewGroupAssignments(GenerationContext context, String itemNum, ReviewGroup group, int leaderIdentity, int memberIdentity) {
        addUserItem(context, group.members.get(0).userNum, itemNum, leaderIdentity);
        for (int index = 1; index < group.members.size(); index++) {
            addUserItem(context, group.members.get(index).userNum, itemNum, memberIdentity);
        }
    }

    private static void addReviews(GenerationContext context, GeneratedItem item, List<GeneratedUser> reviewers,
                                   int reviewType, int reviewLevel, ScoreRange scoreRange) {
        for (GeneratedUser reviewer : reviewers) {
            BigDecimal score = randomScore(scoreRange.min, scoreRange.max);
            context.dataset.reviews.add(new ReviewSeed(
                    context.nextReviewId(),
                    reviewer.userNum,
                    item.itemNum,
                    score,
                    buildReviewOption(score),
                    reviewType,
                    reviewLevel,
                    item.itemDate.plusDays(3 + RANDOM.nextInt(20))
            ));
        }
    }

    private static void createInforms(GenerationContext context) {
        List<Integer> visibleRoles = Arrays.asList(
                Const.ROLEID_STU, Const.ROLEID_TUTOR, Const.ROLEID_ACADEMY_EXPERT,
                Const.ROLEID_ACADEMY, Const.ROLEID_COLLEGE_EXPERT, Const.ROLEID_COLLEGE
        );
        List<GeneratedUser> academyAdmins = new ArrayList<>(context.academyAdmins.values());
        for (int index = 0; index < INFORM_COUNT; index++) {
            int roleId = visibleRoles.get(index % visibleRoles.size());
            GeneratedUser publisher = index % 3 == 0
                    ? context.collegeAdmin
                    : academyAdmins.get(index % academyAdmins.size());
            String subject = publisher.roleId == Const.ROLEID_COLLEGE ? Const.ACADEMY_COLLEGE : publisher.academy;
            String title = INFORM_TITLES.get(index % INFORM_TITLES.size()) + "（第" + (index + 1) + "期）";
            LocalDateTime publishTime = randomDateTime(LocalDate.of(2024, 2, 1), LocalDate.of(2026, 6, 15));
            context.dataset.informs.add(new InformSeed(
                    context.nextInformId(),
                    title,
                    publisher.userNum,
                    roleId,
                    subject,
                    "<p>" + title + "。请相关角色按照系统时间节点完成申报、评审、过程检查与材料归档工作。</p>",
                    null,
                    publishTime
            ));
        }
    }

    private static void addUserItem(GenerationContext context, String userNum, String itemNum, int identity) {
        String key = userNum + "|" + itemNum + "|" + identity;
        if (context.userItemKeys.add(key)) {
            context.dataset.userItems.add(new UserItemSeed(context.nextUserItemId(), userNum, itemNum, identity));
        }
    }

    private static GeneratedUser pickLeader(List<GeneratedUser> students, Map<String, Integer> loadMap) {
        List<GeneratedUser> candidates = new ArrayList<>(students);
        candidates.sort(Comparator.comparingInt(user -> loadMap.get(user.userNum)));
        int minLoad = loadMap.get(candidates.get(0).userNum);
        List<GeneratedUser> leastLoaded = new ArrayList<>();
        for (GeneratedUser candidate : candidates) {
            if (loadMap.get(candidate.userNum) == minLoad) {
                leastLoaded.add(candidate);
            }
        }
        return leastLoaded.get(RANDOM.nextInt(leastLoaded.size()));
    }

    private static List<GeneratedUser> pickAdditionalMembers(List<GeneratedUser> students, Map<String, Integer> loadMap,
                                                             String leaderUserNum, int count) {
        List<GeneratedUser> candidates = new ArrayList<>();
        for (GeneratedUser student : students) {
            if (!leaderUserNum.equals(student.userNum)) {
                candidates.add(student);
            }
        }
        candidates.sort(Comparator.comparingInt(user -> loadMap.get(user.userNum)));
        List<GeneratedUser> members = new ArrayList<>();
        int minLoad = loadMap.get(candidates.get(0).userNum);
        for (GeneratedUser candidate : candidates) {
            if (members.size() >= count) {
                break;
            }
            int currentLoad = loadMap.get(candidate.userNum);
            if (currentLoad <= minLoad + 1 || RANDOM.nextBoolean()) {
                members.add(candidate);
            }
        }
        while (members.size() < count) {
            GeneratedUser fallback = candidates.get(RANDOM.nextInt(candidates.size()));
            if (!containsUser(members, fallback.userNum)) {
                members.add(fallback);
            }
        }
        return members;
    }

    private static boolean containsUser(List<GeneratedUser> users, String userNum) {
        for (GeneratedUser user : users) {
            if (user.userNum.equals(userNum)) {
                return true;
            }
        }
        return false;
    }

    private static int academyReviewLevel(int itemStatus) {
        return itemStatus >= 5 ? 1 : 1;
    }

    private static ScoreRange academyReviewScoreRange(int itemStatus) {
        if (itemStatus == 4) {
            return new ScoreRange(62, 74);
        }
        if (itemStatus == 2) {
            return new ScoreRange(75, 88);
        }
        return new ScoreRange(82, 95);
    }

    private static ScoreRange collegeReviewScoreRange(int itemStatus) {
        if (itemStatus == 9) {
            return new ScoreRange(60, 73);
        }
        if (itemStatus >= 8) {
            return new ScoreRange(84, 96);
        }
        return new ScoreRange(78, 92);
    }

    private static ScoreRange governReviewScoreRange(int itemStatus) {
        if (itemStatus == 9) {
            return new ScoreRange(58, 72);
        }
        return new ScoreRange(83, 97);
    }

    private static int randomItemLevel(int itemStatus) {
        if (itemStatus <= 2) {
            return 1 + RANDOM.nextInt(2);
        }
        int roll = RANDOM.nextInt(100);
        if (roll < 45) {
            return 2;
        }
        if (roll < 80) {
            return 3;
        }
        return 4;
    }

    private static BigDecimal[] buildFunds(int itemLevel, int itemStatus) {
        if (itemStatus == 1 || itemStatus == 2 || itemStatus == 4) {
            return new BigDecimal[]{BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2)};
        }
        if (itemLevel == 2) {
            return new BigDecimal[]{new BigDecimal("2000.00"), BigDecimal.ZERO.setScale(2)};
        }
        if (itemLevel == 3) {
            return new BigDecimal[]{new BigDecimal("3500.00"), new BigDecimal("2500.00")};
        }
        if (itemLevel >= 4) {
            return new BigDecimal[]{new BigDecimal("5000.00"), new BigDecimal("5000.00")};
        }
        return new BigDecimal[]{new BigDecimal("1000.00"), BigDecimal.ZERO.setScale(2)};
    }

    private static String buildItemName(String academyShortName, int itemId) {
        String prefix = ITEM_PREFIXES.get(itemId % ITEM_PREFIXES.size());
        String topic = ITEM_TOPICS.get((itemId / 3) % ITEM_TOPICS.size());
        return prefix + academyShortName + topic + String.format(Locale.ROOT, "%03d", itemId - 500000);
    }

    private static String buildSummary(AcademyProfile academy, int itemType, int itemStatus, String leaderName) {
        return academy.name + "围绕创新实践场景组织项目训练，由" + leaderName
                + "负责推进。项目类型为" + itemType
                + "，当前状态为" + itemStatus
                + "，重点覆盖需求调研、原型开发、成果展示与过程归档。";
    }

    private static String buildReviewOption(BigDecimal score) {
        if (score.compareTo(new BigDecimal("90")) >= 0) {
            return "项目方案完整，建议优先推进并持续跟踪。";
        }
        if (score.compareTo(new BigDecimal("80")) >= 0) {
            return "项目基础较好，建议补充过程材料后继续实施。";
        }
        if (score.compareTo(new BigDecimal("70")) >= 0) {
            return "项目具备一定可行性，建议完善计划与风险控制。";
        }
        return "项目准备不足，建议整改后重新提交评审。";
    }

    private static BigDecimal randomScore(int minInclusive, int maxInclusive) {
        double value = minInclusive + (maxInclusive - minInclusive) * RANDOM.nextDouble();
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static int randomTeacherTitle() {
        int[] titles = {2, 2, 3, 4, 5};
        return titles[RANDOM.nextInt(titles.length)];
    }

    private static int randomStatus(double disabledRatio) {
        return RANDOM.nextDouble() < disabledRatio ? 0 : 1;
    }

    private static LocalDate randomBirthDate(int startYear, int endYear) {
        int year = startYear + RANDOM.nextInt(endYear - startYear + 1);
        int month = 1 + RANDOM.nextInt(12);
        int day = 1 + RANDOM.nextInt(28);
        return LocalDate.of(year, month, day);
    }

    private static LocalDateTime randomDateTime(LocalDate startDate, LocalDate endDate) {
        int dayOffset = RANDOM.nextInt((int) (endDate.toEpochDay() - startDate.toEpochDay()) + 1);
        LocalDate day = startDate.plusDays(dayOffset);
        return day.atTime(RANDOM.nextInt(24), RANDOM.nextInt(60), RANDOM.nextInt(60));
    }

    private static List<Integer> distribute(int total, int bucketCount) {
        List<Integer> distribution = new ArrayList<>();
        int base = total / bucketCount;
        int remainder = total % bucketCount;
        for (int index = 0; index < bucketCount; index++) {
            distribution.add(base + (index < remainder ? 1 : 0));
        }
        return distribution;
    }

    private static List<Integer> buildStatusPool() {
        List<Integer> statuses = new ArrayList<>();
        addRepeatedStatus(statuses, 1, 100);
        addRepeatedStatus(statuses, 2, 83);
        addRepeatedStatus(statuses, 3, 250);
        addRepeatedStatus(statuses, 4, 67);
        addRepeatedStatus(statuses, 5, 117);
        addRepeatedStatus(statuses, 6, 67);
        addRepeatedStatus(statuses, 7, 50);
        addRepeatedStatus(statuses, 8, 50);
        addRepeatedStatus(statuses, 9, 17);
        return statuses;
    }

    private static void addRepeatedStatus(List<Integer> statuses, int status, int count) {
        for (int index = 0; index < count; index++) {
            statuses.add(status);
        }
    }

    private static String appendIndex(String fileName, int index) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0) {
            return fileName + "-" + index;
        }
        return fileName.substring(0, dotIndex) + "-" + index + fileName.substring(dotIndex);
    }

    private static void appendBatchInsert(StringBuilder sql, String table, String columns,
                                           List<String> valueRows, int batchSize) {
        sql.append("-- ").append(table).append("\n");
        for (int i = 0; i < valueRows.size(); i += batchSize) {
            int end = Math.min(i + batchSize, valueRows.size());
            sql.append("INSERT INTO `").append(table).append("` ").append(columns).append(" VALUES\n");
            for (int j = i; j < end; j++) {
                sql.append("(").append(valueRows.get(j)).append(")");
                sql.append(j == end - 1 ? ";\n" : ",\n");
            }
        }
        sql.append('\n');
    }

    private static String renderSql(GeneratedDataset dataset) {
        StringBuilder sql = new StringBuilder();
        sql.append("/*\n");
        sql.append("IEPS sample data generated by RandomDataUtil.\n");
        sql.append("Default password for all generated users: ").append(DEFAULT_PASSWORD).append("\n");
        sql.append("Bootstrap admin account: ").append(Const.USERNUM_COLLEGE).append(" / ").append(DEFAULT_PASSWORD).append("\n");
        sql.append("This file contains data only and is intended to be loaded after ieps.sql.\n");
        sql.append("*/\n\n");
        sql.append("SET NAMES utf8mb4;\n");
        sql.append("SET FOREIGN_KEY_CHECKS=0;\n\n");
        sql.append("DELETE FROM `ieps_user_item`;\n");
        sql.append("DELETE FROM `ieps_review`;\n");
        sql.append("DELETE FROM `ieps_item_info`;\n");
        sql.append("DELETE FROM `ieps_user_role`;\n");
        sql.append("DELETE FROM `ieps_role_perm`;\n");
        sql.append("DELETE FROM `ieps_file_hub`;\n");
        sql.append("DELETE FROM `ieps_inform`;\n");
        sql.append("DELETE FROM `ieps_item`;\n");
        sql.append("DELETE FROM `ieps_user_info`;\n");
        sql.append("DELETE FROM `ieps_user`;\n");
        sql.append("DELETE FROM `ieps_perm`;\n");
        sql.append("DELETE FROM `ieps_role`;\n\n");

        appendRoleInserts(sql, dataset.roles);
        appendPermInserts(sql, dataset.perms);
        appendRolePermInserts(sql, dataset.rolePerms);
        appendUserInserts(sql, dataset.users);
        appendUserInfoInserts(sql, dataset.users);
        appendUserRoleInserts(sql, dataset.users);
        appendItemInserts(sql, dataset.items);
        appendItemInfoInserts(sql, dataset.items);
        appendUserItemInserts(sql, dataset.userItems);
        appendReviewInserts(sql, dataset.reviews);
        appendInformInserts(sql, dataset.informs);
        sql.append("SET FOREIGN_KEY_CHECKS=1;\n");
        return sql.toString();
    }

    private static void appendRoleInserts(StringBuilder sql, List<RoleSeed> roles) {
        List<String> rows = new ArrayList<>();
        for (RoleSeed r : roles) {
            rows.add(r.roleId + ", " + q(r.roleName) + ", " + q(r.roleDesc));
        }
        appendBatchInsert(sql, "ieps_role", "(`role_id`, `role_name`, `role_desc`)", rows, 500);
    }

    private static void appendPermInserts(StringBuilder sql, List<PermSeed> perms) {
        List<String> rows = new ArrayList<>();
        for (PermSeed p : perms) {
            rows.add(p.permId + ", " + q(p.permName) + ", " + q(p.permType) + ", " + q(p.url)
                    + ", " + q(p.icon) + ", " + p.parentId + ", " + q(p.permCode) + ", " + q(p.permDesc));
        }
        appendBatchInsert(sql, "ieps_perm", "(`perm_id`, `perm_name`, `perm_type`, `url`, `icon`, `parent_id`, `perm_code`, `perm_desc`)", rows, 500);
    }

    private static void appendRolePermInserts(StringBuilder sql, List<RolePermSeed> rolePerms) {
        List<String> rows = new ArrayList<>();
        for (RolePermSeed rp : rolePerms) {
            rows.add(rp.id + ", " + rp.roleId + ", " + rp.permId);
        }
        appendBatchInsert(sql, "ieps_role_perm", "(`id`, `role_id`, `perm_id`)", rows, 500);
    }

    private static void appendUserInserts(StringBuilder sql, List<GeneratedUser> users) {
        List<String> rows = new ArrayList<>();
        for (GeneratedUser u : users) {
            rows.add(q(u.userNum) + ", " + q(u.passwordHash) + ", " + u.userStatus);
        }
        appendBatchInsert(sql, "ieps_user", "(`user_num`, `user_pwd`, `user_status`)", rows, 500);
    }

    private static void appendUserInfoInserts(StringBuilder sql, List<GeneratedUser> users) {
        List<String> rows = new ArrayList<>();
        int id = 300001;
        for (GeneratedUser u : users) {
            rows.add(id++ + ", " + q(u.userNum) + ", " + q(u.userName) + ", " + q(DEFAULT_USER_IMG)
                    + ", " + q(buildPhone(u.userNum)) + ", " + q(buildEmail(u.userNum))
                    + ", " + u.title + ", " + u.sex + ", " + q(u.academy)
                    + ", " + q(u.grade) + ", " + q(u.major) + ", " + q(DATE_FORMATTER.format(u.birthDate)));
        }
        appendBatchInsert(sql, "ieps_user_info", "(`id`, `user_num`, `user_name`, `user_img`, `photo_num`, `email`, `title`, `sex`, `academy`, `grade`, `major`, `birth_date`)", rows, 500);
    }

    private static void appendUserRoleInserts(StringBuilder sql, List<GeneratedUser> users) {
        List<String> rows = new ArrayList<>();
        int id = 210001;
        for (GeneratedUser u : users) {
            rows.add(id++ + ", " + q(u.userNum) + ", " + u.roleId);
        }
        appendBatchInsert(sql, "ieps_user_role", "(`id`, `user_num`, `role_id`)", rows, 500);
    }

    private static void appendItemInserts(StringBuilder sql, List<GeneratedItem> items) {
        List<String> rows = new ArrayList<>();
        for (GeneratedItem it : items) {
            rows.add(it.itemId + ", " + q(it.itemNum) + ", " + q(it.itemName)
                    + ", " + q(it.leaderNum) + ", " + q(it.leaderName)
                    + ", " + q(it.tutorNum) + ", " + q(it.tutorName)
                    + ", " + it.itemStatus + ", " + q(DATE_TIME_FORMATTER.format(it.itemDate)));
        }
        appendBatchInsert(sql, "ieps_item", "(`item_id`, `item_num`, `item_name`, `leader_num`, `leader_name`, `tutor_num`, `tutor_name`, `item_status`, `item_date`)", rows, 500);
    }

    private static void appendItemInfoInserts(StringBuilder sql, List<GeneratedItem> items) {
        List<String> rows = new ArrayList<>();
        int id = 510001;
        for (GeneratedItem it : items) {
            rows.add(id++ + ", " + q(it.itemNum) + ", " + it.itemLevel + ", " + it.itemType
                    + ", " + q(it.summary) + ", " + it.collegeFunds.toPlainString()
                    + ", " + it.governFunds.toPlainString());
        }
        appendBatchInsert(sql, "ieps_item_info", "(`id`, `item_num`, `item_level`, `item_type`, `summary`, `college_funds`, `govern_funds`)", rows, 500);
    }

    private static void appendUserItemInserts(StringBuilder sql, List<UserItemSeed> userItems) {
        List<String> rows = new ArrayList<>();
        for (UserItemSeed ui : userItems) {
            rows.add(ui.id + ", " + q(ui.userNum) + ", " + q(ui.itemNum) + ", " + ui.identity);
        }
        appendBatchInsert(sql, "ieps_user_item", "(`id`, `user_num`, `item_num`, `identity`)", rows, 500);
    }

    private static void appendReviewInserts(StringBuilder sql, List<ReviewSeed> reviews) {
        List<String> rows = new ArrayList<>();
        for (ReviewSeed rv : reviews) {
            rows.add(rv.id + ", " + q(rv.userNum) + ", " + q(rv.itemNum) + ", "
                    + rv.reviewScore.toPlainString() + ", " + q(rv.reviewOption) + ", "
                    + rv.reviewType + ", " + rv.reviewLevel + ", " + q(DATE_TIME_FORMATTER.format(rv.reviewTime)));
        }
        appendBatchInsert(sql, "ieps_review", "(`id`, `user_num`, `item_num`, `review_score`, `review_option`, `review_type`, `review_level`, `review_time`)", rows, 500);
    }

    private static void appendInformInserts(StringBuilder sql, List<InformSeed> informs) {
        List<String> rows = new ArrayList<>();
        for (InformSeed inf : informs) {
            rows.add(inf.id + ", " + q(inf.head) + ", " + q(inf.publisher) + ", " + inf.roleId
                    + ", " + q(inf.subject) + ", " + q(inf.content) + ", " + q(inf.files)
                    + ", " + q(DATE_TIME_FORMATTER.format(inf.pubdate)));
        }
        appendBatchInsert(sql, "ieps_inform", "(`id`, `head`, `publisher`, `role_id`, `subject`, `content`, `files`, `pubdate`)", rows, 500);
    }

    private static String buildPhone(String userNum) {
        String suffix = userNum.length() >= 8 ? userNum.substring(userNum.length() - 8) : String.format(Locale.ROOT, "%08d", Integer.parseInt(userNum));
        return "139" + suffix;
    }

    private static String buildEmail(String userNum) {
        return "u" + userNum + "@ieps.local";
    }

    private static String q(String value) {
        if (value == null) {
            return "NULL";
        }
        return "'" + value
                .replace("\\", "\\\\")
                .replace("'", "''")
                .replace("\r", "")
                .replace("\n", "\\n") + "'";
    }

    private static final class GeneratedDataset {
        private final List<RoleSeed> roles = new ArrayList<>();
        private final List<PermSeed> perms = new ArrayList<>();
        private final List<RolePermSeed> rolePerms = new ArrayList<>();
        private final List<GeneratedUser> users = new ArrayList<>();
        private final List<GeneratedItem> items = new ArrayList<>();
        private final List<UserItemSeed> userItems = new ArrayList<>();
        private final List<ReviewSeed> reviews = new ArrayList<>();
        private final List<InformSeed> informs = new ArrayList<>();
    }

    private static final class GenerationContext {
        private final GeneratedDataset dataset;
        private final Map<String, List<GeneratedUser>> studentsByAcademy = new LinkedHashMap<>();
        private final Map<String, List<GeneratedUser>> tutorsByAcademy = new LinkedHashMap<>();
        private final Map<String, GeneratedUser> academyAdmins = new LinkedHashMap<>();
        private final Map<String, List<ReviewGroup>> academyReviewGroups = new LinkedHashMap<>();
        private final List<ReviewGroup> collegeReviewGroups = new ArrayList<>();
        private final Map<String, Integer> studentLoad = new LinkedHashMap<>();
        private final Set<String> userItemKeys = new LinkedHashSet<>();
        private GeneratedUser collegeAdmin;
        private int collegeExpertSequence = 1;
        private int academyExpertSequence = 1;
        private int tutorSequence = 1;
        private int studentSequence = 1;
        private int itemId = 500001;
        private int userItemId = 700001;
        private int reviewId = 1;
        private int informId = 400001;
        private final Map<String, Integer> academyGroupCursor = new LinkedHashMap<>();
        private int collegeGroupCursor = 0;

        private GenerationContext(GeneratedDataset dataset) {
            this.dataset = dataset;
        }

        private int nextCollegeExpertSequence() {
            return collegeExpertSequence++;
        }

        private int nextAcademyExpertSequence() {
            return academyExpertSequence++;
        }

        private int nextTutorSequence() {
            return tutorSequence++;
        }

        private int nextStudentSequence() {
            return studentSequence++;
        }

        private int nextItemId() {
            return itemId++;
        }

        private int nextUserItemId() {
            return userItemId++;
        }

        private int nextReviewId() {
            return reviewId++;
        }

        private int nextInformId() {
            return informId++;
        }

        private int nextAcademyReviewGroupIndex(String academyName) {
            List<ReviewGroup> groups = academyReviewGroups.get(academyName);
            int current = academyGroupCursor.getOrDefault(academyName, 0);
            academyGroupCursor.put(academyName, (current + 1) % groups.size());
            return current;
        }

        private int nextCollegeReviewGroupIndex() {
            int current = collegeGroupCursor;
            collegeGroupCursor = (collegeGroupCursor + 1) % collegeReviewGroups.size();
            return current;
        }
    }

    private static final class AcademyProfile {
        private final String name;
        private final String shortName;
        private final List<String> majors;

        private AcademyProfile(String name, String shortName, List<String> majors) {
            this.name = name;
            this.shortName = shortName;
            this.majors = majors;
        }
    }

    private static final class ReviewGroup {
        private final String academyName;
        private final int groupIndex;
        private final int roleId;
        private final List<GeneratedUser> members = new ArrayList<>();

        private ReviewGroup(String academyName, int groupIndex, int roleId) {
            this.academyName = academyName;
            this.groupIndex = groupIndex;
            this.roleId = roleId;
        }
    }

    private static final class GeneratedUser {
        private final String userNum;
        private final String userName;
        private final int roleId;
        private final String academy;
        private final String major;
        private final String grade;
        private final int title;
        private final int sex;
        private final int userStatus;
        private final LocalDate birthDate;
        private final String passwordHash;

        private GeneratedUser(String userNum, String userName, int roleId, String academy, String major,
                              String grade, int title, int sex, int userStatus, LocalDate birthDate,
                              String passwordHash) {
            this.userNum = userNum;
            this.userName = userName;
            this.roleId = roleId;
            this.academy = academy;
            this.major = major;
            this.grade = grade;
            this.title = title;
            this.sex = sex;
            this.userStatus = userStatus;
            this.birthDate = birthDate;
            this.passwordHash = passwordHash;
        }
    }

    private static final class GeneratedItem {
        private final int itemId;
        private final String itemNum;
        private final String itemName;
        private final String leaderNum;
        private final String leaderName;
        private final String tutorNum;
        private final String tutorName;
        private final int itemStatus;
        private final int itemLevel;
        private final int itemType;
        private final String summary;
        private final BigDecimal collegeFunds;
        private final BigDecimal governFunds;
        private final LocalDateTime itemDate;

        private GeneratedItem(int itemId, String itemNum, String itemName, String leaderNum, String leaderName,
                              String tutorNum, String tutorName, int itemStatus, int itemLevel, int itemType,
                              String summary, BigDecimal collegeFunds, BigDecimal governFunds, LocalDateTime itemDate) {
            this.itemId = itemId;
            this.itemNum = itemNum;
            this.itemName = itemName;
            this.leaderNum = leaderNum;
            this.leaderName = leaderName;
            this.tutorNum = tutorNum;
            this.tutorName = tutorName;
            this.itemStatus = itemStatus;
            this.itemLevel = itemLevel;
            this.itemType = itemType;
            this.summary = summary;
            this.collegeFunds = collegeFunds;
            this.governFunds = governFunds;
            this.itemDate = itemDate;
        }
    }

    private static final class RoleSeed {
        private final int roleId;
        private final String roleName;
        private final String roleDesc;

        private RoleSeed(int roleId, String roleName, String roleDesc) {
            this.roleId = roleId;
            this.roleName = roleName;
            this.roleDesc = roleDesc;
        }
    }

    private static final class PermSeed {
        private final int permId;
        private final String permName;
        private final String permType;
        private final String url;
        private final String icon;
        private final int parentId;
        private final String permCode;
        private final String permDesc;

        private PermSeed(int permId, String permName, String permType, String url,
                         String icon, int parentId, String permCode, String permDesc) {
            this.permId = permId;
            this.permName = permName;
            this.permType = permType;
            this.url = url;
            this.icon = icon;
            this.parentId = parentId;
            this.permCode = permCode;
            this.permDesc = permDesc;
        }
    }

    private static final class RolePermSeed {
        private final int id;
        private final int roleId;
        private final int permId;

        private RolePermSeed(int id, int roleId, int permId) {
            this.id = id;
            this.roleId = roleId;
            this.permId = permId;
        }
    }

    private static final class UserItemSeed {
        private final int id;
        private final String userNum;
        private final String itemNum;
        private final int identity;

        private UserItemSeed(int id, String userNum, String itemNum, int identity) {
            this.id = id;
            this.userNum = userNum;
            this.itemNum = itemNum;
            this.identity = identity;
        }
    }

    private static final class ReviewSeed {
        private final int id;
        private final String userNum;
        private final String itemNum;
        private final BigDecimal reviewScore;
        private final String reviewOption;
        private final int reviewType;
        private final int reviewLevel;
        private final LocalDateTime reviewTime;

        private ReviewSeed(int id, String userNum, String itemNum, BigDecimal reviewScore,
                           String reviewOption, int reviewType, int reviewLevel, LocalDateTime reviewTime) {
            this.id = id;
            this.userNum = userNum;
            this.itemNum = itemNum;
            this.reviewScore = reviewScore;
            this.reviewOption = reviewOption;
            this.reviewType = reviewType;
            this.reviewLevel = reviewLevel;
            this.reviewTime = reviewTime;
        }
    }

    private static final class InformSeed {
        private final int id;
        private final String head;
        private final String publisher;
        private final int roleId;
        private final String subject;
        private final String content;
        private final String files;
        private final LocalDateTime pubdate;

        private InformSeed(int id, String head, String publisher, int roleId, String subject,
                           String content, String files, LocalDateTime pubdate) {
            this.id = id;
            this.head = head;
            this.publisher = publisher;
            this.roleId = roleId;
            this.subject = subject;
            this.content = content;
            this.files = files;
            this.pubdate = pubdate;
        }
    }

    private static final class ScoreRange {
        private final int min;
        private final int max;

        private ScoreRange(int min, int max) {
            this.min = min;
            this.max = max;
        }
    }
}
