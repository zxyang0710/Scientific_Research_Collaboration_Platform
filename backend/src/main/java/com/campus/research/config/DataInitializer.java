package com.campus.research.config;

import com.campus.research.entity.Notification;
import com.campus.research.entity.ProjectDiscussion;
import com.campus.research.entity.ProjectFile;
import com.campus.research.entity.ProjectMember;
import com.campus.research.entity.ResearchPaper;
import com.campus.research.entity.ResearchProject;
import com.campus.research.entity.ResearchTask;
import com.campus.research.entity.User;
import com.campus.research.mapper.NotificationMapper;
import com.campus.research.mapper.ProjectDiscussionMapper;
import com.campus.research.mapper.ProjectFileMapper;
import com.campus.research.mapper.ProjectMemberMapper;
import com.campus.research.mapper.ResearchPaperMapper;
import com.campus.research.mapper.ResearchProjectMapper;
import com.campus.research.mapper.ResearchTaskMapper;
import com.campus.research.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final ResearchProjectMapper projectMapper;
    private final ProjectMemberMapper memberMapper;
    private final ResearchTaskMapper taskMapper;
    private final ProjectFileMapper fileMapper;
    private final ResearchPaperMapper paperMapper;
    private final ProjectDiscussionMapper discussionMapper;
    private final NotificationMapper notificationMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userMapper.selectCount(null) > 0) {
            return;
        }

        User admin = user("admin", "系统管理员", "admin@campus.edu.cn", "信息中心", "平台管理员", "ADMIN");
        User zhang = user("zhangwei", "张伟", "zhangwei@campus.edu.cn", "计算机科学与技术学院", "教授", "RESEARCHER");
        User li = user("liming", "李明", "liming@campus.edu.cn", "计算机科学与技术学院", "副教授", "RESEARCHER");
        User wang = user("wangfang", "王芳", "wangfang@campus.edu.cn", "人工智能研究院", "讲师", "RESEARCHER");
        User chen = user("chenyu", "陈宇", "chenyu@campus.edu.cn", "计算机科学与技术学院", "研究生", "RESEARCHER");
        userMapper.insert(admin);
        userMapper.insert(zhang);
        userMapper.insert(li);
        userMapper.insert(wang);
        userMapper.insert(chen);

        ResearchProject project1 = project("面向复杂场景的多模态智能感知研究", "KY-2026-001",
                "融合视觉、文本与传感数据的复杂场景感知方法研究，建立可复用的多模态实验平台。",
                "人工智能", "国家自然科学基金", zhang.getId(), "IN_PROGRESS", 62,
                LocalDate.of(2025, 9, 1), LocalDate.of(2027, 8, 31), new BigDecimal("860000"));
        ResearchProject project2 = project("高校科研数据安全共享关键技术", "KY-2026-002",
                "面向跨机构科研数据协作的隐私计算与细粒度访问控制技术研究。",
                "网络空间安全", "省重点研发计划", zhang.getId(), "IN_PROGRESS", 38,
                LocalDate.of(2026, 1, 1), LocalDate.of(2028, 12, 31), new BigDecimal("520000"));
        ResearchProject project3 = project("生成式人工智能辅助教学创新", "JX-2026-008",
                "研究生成式人工智能在高校课程中的可信应用与学习效果评价。",
                "教育技术", "校级教改项目", li.getId(), "PLANNING", 15,
                LocalDate.of(2026, 9, 1), LocalDate.of(2027, 12, 31), new BigDecimal("120000"));
        projectMapper.insert(project1);
        projectMapper.insert(project2);
        projectMapper.insert(project3);

        member(project1.getId(), zhang.getId(), "LEADER");
        member(project1.getId(), li.getId(), "MANAGER");
        member(project1.getId(), wang.getId(), "MEMBER");
        member(project1.getId(), chen.getId(), "MEMBER");
        member(project2.getId(), zhang.getId(), "LEADER");
        member(project2.getId(), wang.getId(), "MANAGER");
        member(project2.getId(), chen.getId(), "MEMBER");
        member(project3.getId(), li.getId(), "LEADER");
        member(project3.getId(), chen.getId(), "MEMBER");

        task(project1.getId(), "完成多模态数据采集规范 V2", "统一图像、文本和传感器数据标注字段。",
                wang.getId(), zhang.getId(), "HIGH", "IN_PROGRESS", LocalDate.now().plusDays(8));
        task(project1.getId(), "视觉编码模块消融实验", "完成三个公开数据集上的基线对比与消融实验。",
                chen.getId(), zhang.getId(), "HIGH", "IN_PROGRESS", LocalDate.now().plusDays(14));
        task(project1.getId(), "搭建跨模态检索演示系统", "实现可交互的跨模态检索原型。",
                li.getId(), zhang.getId(), "MEDIUM", "TODO", LocalDate.now().plusDays(35));
        task(project1.getId(), "阶段成果技术报告", "整理第一阶段研究进展与实验结果。",
                zhang.getId(), zhang.getId(), "MEDIUM", "DONE", LocalDate.now().minusDays(3));
        task(project2.getId(), "调研联邦学习隐私攻击方法", "形成隐私威胁分类和防御策略综述。",
                chen.getId(), zhang.getId(), "HIGH", "IN_PROGRESS", LocalDate.now().plusDays(12));
        task(project2.getId(), "设计细粒度访问控制模型", "根据科研数据分级分类设计访问控制策略。",
                wang.getId(), zhang.getId(), "HIGH", "TODO", LocalDate.now().plusDays(30));
        task(project3.getId(), "课程试点方案设计", "确定试点班级、教学流程与数据采集边界。",
                chen.getId(), li.getId(), "MEDIUM", "TODO", LocalDate.now().plusDays(20));

        file(project1.getId(), zhang.getId(), "多模态研究方案.pdf", "application/pdf", 2480000, "DOCUMENT", "项目总体研究方案");
        file(project1.getId(), wang.getId(), "数据采集规范V1.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", 680000, "DOCUMENT", "第一阶段数据规范");
        file(project1.getId(), chen.getId(), "实验记录-0718.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", 320000, "DATA", "视觉编码器对比实验原始记录");
        file(project2.getId(), zhang.getId(), "隐私计算技术调研.pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation", 5100000, "DOCUMENT", "组会汇报材料");

        paper(project1.getId(), "Cross-Modal Representation Learning for Complex Campus Scenes",
                "张伟, 李明, 王芳", "IEEE Transactions on Multimedia", LocalDate.of(2026, 5, 18),
                "10.1109/TMM.2026.100001", "PUBLISHED", "提出一种面向复杂校园场景的跨模态表征学习方法。", zhang.getId());
        paper(project1.getId(), "面向多源传感数据的鲁棒目标识别方法",
                "王芳, 陈宇", "计算机学报", null, null, "UNDER_REVIEW",
                "研究多源传感信息缺失条件下的鲁棒目标识别。", wang.getId());
        paper(project2.getId(), "科研数据跨域共享的隐私风险与治理框架",
                "张伟, 陈宇", "信息安全研究", null, null, "DRAFT",
                "梳理跨域科研数据共享中的隐私风险，提出分级治理框架。", zhang.getId());

        discussion(project1.getId(), zhang.getId(), null,
                "本周五下午两点进行阶段进展会，请大家提前更新任务进度和实验记录。", LocalDateTime.now().minusDays(2));
        discussion(project1.getId(), wang.getId(), null,
                "数据规范 V2 已补充时序传感器的字段说明，今天会提交到项目文件。", LocalDateTime.now().minusHours(20));
        discussion(project1.getId(), chen.getId(), null,
                "视觉分支基线实验已完成，F1 指标比上周提升 3.2%，结果表稍后上传。", LocalDateTime.now().minusHours(5));
        discussion(project2.getId(), zhang.getId(), null,
                "请重点补充跨机构场景下的数据最小化原则，下次组会讨论。", LocalDateTime.now().minusDays(1));

        notification(zhang.getId(), wang.getId(), "任务进度已更新",
                "王芳将“完成多模态数据采集规范 V2”的进度更新为 70%。", "TASK", 1L, "/projects/" + project1.getId(), 0,
                LocalDateTime.now().minusHours(3));
        notification(zhang.getId(), chen.getId(), "提交了实验进展",
                "陈宇在项目讨论中发布了新的实验进展。", "DISCUSSION", project1.getId(), "/projects/" + project1.getId(), 0,
                LocalDateTime.now().minusHours(5));
        notification(chen.getId(), zhang.getId(), "新任务已分配",
                "您被分配负责“视觉编码模块消融实验”。", "TASK", 2L, "/tasks", 1,
                LocalDateTime.now().minusDays(1));
    }

    private User user(String username, String realName, String email, String department,
                      String title, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode("123456"));
        user.setRealName(realName);
        user.setEmail(email);
        user.setDepartment(department);
        user.setTitle(title);
        user.setRole(role);
        user.setStatus(1);
        return user;
    }

    private ResearchProject project(String name, String code, String description, String category,
                                    String source, Long leaderId, String status, int progress,
                                    LocalDate startDate, LocalDate endDate, BigDecimal budget) {
        ResearchProject project = new ResearchProject();
        project.setName(name);
        project.setCode(code);
        project.setDescription(description);
        project.setCategory(category);
        project.setSource(source);
        project.setLeaderId(leaderId);
        project.setStatus(status);
        project.setProgress(progress);
        project.setStartDate(startDate);
        project.setEndDate(endDate);
        project.setBudget(budget);
        return project;
    }

    private void member(Long projectId, Long userId, String role) {
        ProjectMember member = new ProjectMember();
        member.setProjectId(projectId);
        member.setUserId(userId);
        member.setProjectRole(role);
        member.setJoinedAt(LocalDateTime.now());
        memberMapper.insert(member);
    }

    private void task(Long projectId, String title, String description, Long assigneeId, Long creatorId,
                      String priority, String status, LocalDate deadline) {
        ResearchTask task = new ResearchTask();
        task.setProjectId(projectId);
        task.setTitle(title);
        task.setDescription(description);
        task.setAssigneeId(assigneeId);
        task.setCreatorId(creatorId);
        task.setPriority(priority);
        task.setStatus(status);
        task.setDeadline(deadline);
        taskMapper.insert(task);
    }

    private void file(Long projectId, Long uploaderId, String name, String contentType,
                      long size, String category, String description) {
        ProjectFile file = new ProjectFile();
        file.setProjectId(projectId);
        file.setUploaderId(uploaderId);
        file.setOriginalName(name);
        file.setStoredName("demo-" + System.nanoTime() + "-" + name);
        file.setFilePath("demo/" + file.getStoredName());
        file.setFileSize(size);
        file.setContentType(contentType);
        file.setCategory(category);
        file.setDescription(description);
        file.setCreatedAt(LocalDateTime.now());
        file.setDeleted(0);
        fileMapper.insert(file);
    }

    private void paper(Long projectId, String title, String authors, String journal,
                       LocalDate publicationDate, String doi, String status, String abstractText, Long createdBy) {
        ResearchPaper paper = new ResearchPaper();
        paper.setProjectId(projectId);
        paper.setTitle(title);
        paper.setAuthors(authors);
        paper.setJournal(journal);
        paper.setPublicationDate(publicationDate);
        paper.setDoi(doi);
        paper.setStatus(status);
        paper.setAbstractText(abstractText);
        paper.setCreatedBy(createdBy);
        paperMapper.insert(paper);
    }

    private void discussion(Long projectId, Long authorId, Long parentId, String content,
                            LocalDateTime createdAt) {
        ProjectDiscussion discussion = new ProjectDiscussion();
        discussion.setProjectId(projectId);
        discussion.setAuthorId(authorId);
        discussion.setParentId(parentId);
        discussion.setContent(content);
        discussion.setCreatedAt(createdAt);
        discussion.setUpdatedAt(createdAt);
        discussion.setDeleted(0);
        discussionMapper.insert(discussion);
    }

    private void notification(Long receiverId, Long senderId, String title, String content,
                              String type, Long businessId, String link, int isRead,
                              LocalDateTime createdAt) {
        Notification notification = new Notification();
        notification.setReceiverId(receiverId);
        notification.setSenderId(senderId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setBusinessId(businessId);
        notification.setLink(link);
        notification.setIsRead(isRead);
        notification.setCreatedAt(createdAt);
        notificationMapper.insert(notification);
    }
}
