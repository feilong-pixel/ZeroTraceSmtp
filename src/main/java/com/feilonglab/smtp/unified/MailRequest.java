package com.feilonglab.smtp.unified;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一邮件发送请求数据传输对象 (DTO)。
 * <p>
 * 该类采用链式调用设计 (Builder-like Pattern)，以便于灵活、优雅地构建复杂的邮件发送请求。
 * 支持以下组合配置：
 * 1. 多个 TO、CC、BCC 收件人。
 * 2. 文本正文和 HTML 正文。
 * 3. 动态 HTML 模板文件路径及参数映射。
 * 4. 多附件关联。
 * </p>
 *
 * @author feilonglab
 * @version 0.1.0
 */
public class MailRequest {

    /** 主要收件人列表 (TO) */
    public List<String> to = new ArrayList<>();

    /** 抄送收件人列表 (CC) */
    public List<String> cc = new ArrayList<>();

    /** 密送收件人列表 (BCC) */
    public List<String> bcc = new ArrayList<>();

    /** 邮件主题 */
    public String subject;

    /** 邮件 HTML 正文内容 */
    public String htmlContent;

    /** 邮件纯文本备用正文内容 */
    public String textContent;

    /** 邮件附件文件列表 */
    public List<File> attachments = new ArrayList<>();

    /** 模板文件在类路径下的路径 (例如 /templates/welcome.template) */
    public String templatePath;

    /** 替换模板占位符的参数 Map */
    public Map<String, String> templateParams = new HashMap<>();

    /** 是否使用模板渲染功能，默认不开启 */
    public boolean useTemplate = false;

    // ==================== 链式调用 Setter 方法 ====================

    /**
     * 设置主要收件人列表 (TO，覆盖原有收件人)。
     *
     * @param emails 邮箱地址列表
     * @return 当前请求对象，支持链式调用
     */
    public MailRequest to(String... emails) {
        this.to = new ArrayList<>(List.of(emails));
        return this;
    }

    /**
     * 设置主要收件人列表 (TO，覆盖原有收件人)。
     *
     * @param toList 邮箱地址列表
     * @return 当前请求对象，支持链式调用
     */
    public MailRequest to(List<String> toList) {
        this.to = toList != null ? new ArrayList<>(toList) : new ArrayList<>();
        return this;
    }

    /**
     * 添加单个主要收件人 (TO)。
     *
     * @param email 邮箱地址
     * @return 当前请求对象，支持链式调用
     */
    public MailRequest addTo(String email) {
        if (email != null && !email.trim().isEmpty()) {
            if (this.to == null) {
                this.to = new ArrayList<>();
            }
            this.to.add(email.trim());
        }
        return this;
    }

    /**
     * 设置抄送收件人列表 (CC，覆盖原有抄送人)。
     *
     * @param emails 邮箱地址列表
     * @return 当前请求对象，支持链式调用
     */
    public MailRequest cc(String... emails) {
        this.cc = new ArrayList<>(List.of(emails));
        return this;
    }

    /**
     * 设置抄送收件人列表 (CC，覆盖原有抄送人)。
     *
     * @param ccList 邮箱地址列表
     * @return 当前请求对象，支持链式调用
     */
    public MailRequest cc(List<String> ccList) {
        this.cc = ccList != null ? new ArrayList<>(ccList) : new ArrayList<>();
        return this;
    }

    /**
     * 添加单个抄送收件人 (CC)。
     *
     * @param email 抄送邮箱地址
     * @return 当前请求对象，支持链式调用
     */
    public MailRequest addCc(String email) {
        if (email != null && !email.trim().isEmpty()) {
            if (this.cc == null) {
                this.cc = new ArrayList<>();
            }
            this.cc.add(email.trim());
        }
        return this;
    }

    /**
     * 设置密送收件人列表 (BCC，覆盖原有密送人)。
     *
     * @param emails 邮箱地址列表
     * @return 当前请求对象，支持链式调用
     */
    public MailRequest bcc(String... emails) {
        this.bcc = new ArrayList<>(List.of(emails));
        return this;
    }

    /**
     * 设置密送收件人列表 (BCC，覆盖原有密送人)。
     *
     * @param bccList 邮箱地址列表
     * @return 当前请求对象，支持链式调用
     */
    public MailRequest bcc(List<String> bccList) {
        this.bcc = bccList != null ? new ArrayList<>(bccList) : new ArrayList<>();
        return this;
    }

    /**
     * 添加单个密送收件人 (BCC)。
     *
     * @param email 密送邮箱地址
     * @return 当前请求对象，支持链式调用
     */
    public MailRequest addBcc(String email) {
        if (email != null && !email.trim().isEmpty()) {
            if (this.bcc == null) {
                this.bcc = new ArrayList<>();
            }
            this.bcc.add(email.trim());
        }
        return this;
    }

    /**
     * 设置邮件主题。
     *
     * @param subject 邮件主题
     * @return 当前请求对象，支持链式调用
     */
    public MailRequest subject(String subject) {
        this.subject = subject;
        return this;
    }

    /**
     * 设置 HTML 格式的正文内容。
     *
     * @param htmlContent HTML 格式字符串内容
     * @return 当前请求对象，支持链式调用
     */
    public MailRequest htmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
        return this;
    }

    /**
     * 设置纯文本格式的正文内容（可作为备用内容）。
     *
     * @param textContent 纯文本正文内容
     * @return 当前请求对象，支持链式调用
     */
    public MailRequest textContent(String textContent) {
        this.textContent = textContent;
        return this;
    }

    /**
     * 设置附件文件列表 (覆盖原有附件)。
     *
     * @param files 附件文件列表
     * @return 当前请求对象，支持链式调用
     */
    public MailRequest attachments(File... files) {
        this.attachments = new ArrayList<>(List.of(files));
        return this;
    }

    /**
     * 设置附件文件列表 (覆盖原有附件)。
     *
     * @param attachmentList 附件文件列表
     * @return 当前请求对象，支持链式调用
     */
    public MailRequest attachments(List<File> attachmentList) {
        this.attachments = attachmentList != null ? new ArrayList<>(attachmentList) : new ArrayList<>();
        return this;
    }

    /**
     * 添加单个附件文件。
     *
     * @param file 附件文件对象
     * @return 当前请求对象，支持链式调用
     */
    public MailRequest addAttachment(File file) {
        if (file != null) {
            if (this.attachments == null) {
                this.attachments = new ArrayList<>();
            }
            this.attachments.add(file);
        }
        return this;
    }

    /**
     * 添加单个附件文件（基于文件路径）。
     *
     * @param filePath 附件文件在本地系统上的绝对/相对路径
     * @return 当前请求对象，支持链式调用
     */
    public MailRequest addAttachment(String filePath) {
        if (filePath != null && !filePath.trim().isEmpty()) {
            addAttachment(new File(filePath.trim()));
        }
        return this;
    }

    /**
     * 配置模板渲染参数。
     *
     * @param templatePath   模板文件在类路径下的相对路径
     * @param templateParams 替换模板变量的参数 Map
     * @return 当前请求对象，支持链式调用
     */
    public MailRequest template(String templatePath, Map<String, String> templateParams) {
        this.useTemplate = true;
        this.templatePath = templatePath;
        this.templateParams = templateParams != null ? new HashMap<>(templateParams) : new HashMap<>();
        return this;
    }

    /**
     * 向现有模板参数列表中添加/更新单个模板替换键值对。
     *
     * @param key   模板参数变量名
     * @param value 变量对应的值
     * @return 当前请求对象，支持链式调用
     */
    public MailRequest addTemplateParam(String key, String value) {
        if (key != null) {
            if (this.templateParams == null) {
                this.templateParams = new HashMap<>();
            }
            this.templateParams.put(key, value);
        }
        return this;
    }

    /**
     * 显式设定是否启用模板解析。
     *
     * @param useTemplate true 表示启用模板；false 表示不启用
     * @return 当前请求对象，支持链式调用
     */
    public MailRequest useTemplate(boolean useTemplate) {
        this.useTemplate = useTemplate;
        return this;
    }

}
