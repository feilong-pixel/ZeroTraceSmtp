# 📬 Java SMTP 邮件发送集成与示例项目 -- Java SMTP Client Library & Examples

本仓库是一个开箱即用的 Java SMTP 邮件发送组件与示例项目。它基于底层 `javax.mail` 构建，提供了网络连接建立、群发列表解析校验、模板正则填装、嵌套 MIME 容器拼装、中文名防乱码以及一站式 API 封装等功能，适用于各种 Java 邮件发送场景。

---

## 🗺️ 项目模块与包结构设计

项目划分了清晰的功能子包，分别实现了不同的邮件发送逻辑与特性：

1. **基础客户端：`com.feilonglab.smtp.basic`**
   * **核心实现**：SMTP 服务端的基本参数配置，利用 `Session` 和 `Transport` 实现与邮件服务器的连接建立，以及基于 `try-with-resources` 的套接字连接资源自动释放。
2. **多收件人发送：`com.feilonglab.smtp.multiple`**
   * **核心实现**：支持将单封邮件同时投递至 TO (收件人) / CC (抄送) / BCC (密送) 列表，并实现单个邮箱 RFC 822 规范校验及异常隔离。
3. **模板引擎渲染：`com.feilonglab.smtp.Template`**
   * **核心实现**：采用轻量且高性能的单遍扫描正则表达式引擎，完成 `${key}` 占位符的渲染与异常处理。
4. **附件拼接与乱码处理：`com.feilonglab.smtp.attachments`**
   * **核心实现**：基于 RFC MIME 规范的附件加载，并使用 `MimeUtility.encodeText` 对中文附件名进行编码，解决客户端乱码问题。
5. **统一链式 API 封装：`com.feilonglab.smtp.unified`**
   * **核心实现**：采用组合式参数传输对象 `MailRequest` 进行链式配置 (Builder-like)；实现 **嵌套 MIME 复合模型** (`multipart/alternative` 内嵌在 `multipart/mixed` 中)；提供本地 Intercept 拦截模式。
6. **云服务（Amazon SES）适配：`com.feilonglab.smtp.amazon.ses`**
   * **核心实现**：支持通过 Amazon SES SMTP endpoints 进行云端邮件投递。

---

## 📘 SMTP 配置项含义说明

系统配置采用 `.properties` 文件载入，各参数详解如下：

| **属性名** | **含义** | **默认值** | **适用场景** | **工程实践与避坑指南** |
| :--- | :--- | :--- | :--- | :--- |
| **mail.smtp.host** | SMTP 服务器主机地址 | 无 | 必须设置 | 决定邮件中转站。例如 Gmail 的 `smtp.gmail.com` 或 AWS SES 的 `email-smtp.ap-northeast-1.amazonaws.com` |
| **mail.smtp.port** | SMTP 服务端口 | `587` | 必须设置 | 常用端口：`25` (明文), `465` (SSL), `587` (STARTTLS) |
| **mail.smtp.username** | 认证账号用户名 | 无 | 开启认证时必填 | **踩坑注意**：主流邮箱及 AWS SES 需要使用专门生成的 **SMTP 密码/授权码**，不能使用 IAM 密钥或主密码 |
| **mail.smtp.password** | 认证账号密码 | 无 | 配套用户名使用 | 如果报 `535 Authentication Credentials Invalid`，请优先检查此处配置的密码是否为 SMTP 专用的授权码 |
| **mail.smtp.starttls.enable** | 启用 STARTTLS 加密 | `true` | STARTTLS 端口 | 连接建立后再升级为 TLS 加密，若端口为 `465` 请将其设为 `false` |
| **mail.smtp.ssl.enable** | 直接启用 SSL 加密 | `false` | SSL 端口 (465) | 一上来直接建立加密通道。与 STARTTLS 互斥，两者不能同时为 `true` |
| **mail.smtp.connectiontimeout** | 连接超时时间（毫秒） | `5000` | 生产环境推荐配置 | 防止因网络不可达或防火墙拦截导致连接阶段线程无限假死挂起 |
| **mail.smtp.timeout** | 响应读取超时（毫秒） | `5000` | 生产环境推荐配置 | 防止成功建立连接后，读取邮件系统指令响应时卡死 |
| **mail.smtp.writetimeout** | 数据写入超时（毫秒） | `5000` | 生产环境推荐配置 | 防止向服务器 Socket 发送超大邮件附件包时网络阻塞导致无限挂起 |
| **mail.smtp.sender.name** | 默认发件人显示别名 | 无 | 选填 | 收件人收件箱中发件人处展示的名称，例如 `Fei Long Lab` |
| **mail.smtp.sender.email** | 默认发件人邮箱地址 | 无 | 必填 | 发送邮件的主体地址。**AWS SES 踩坑点**：该地址必须在 SES 控制台中完成 Verified 验证，否则会报拒绝错误 |
| **mail.debug** | javax.mail 协议日志调试 | `false` | 疑难杂症排查 | 设为 `true` 将打印底层 SMTP 原始套接字交互命令（生产环境不建议开启） |

---

## 🚀 邮件发送使用示例 (Code Examples)

#### 1. 基础单发邮件示例 (Basic Single Sending)
最简单的发送邮件场景：单发 HTML 邮件给单个收件人，捕获连接阶段和发送阶段各自的异常。

```java
import com.feilonglab.smtp.basic.SmtpClient;

public class BasicSendApp {
    public static void main(String[] args) {
        // 使用 try-with-resources 自动关闭连接和底层 Socket 资源
        try (SmtpClient client = new SmtpClient()) {
            
            // 步骤一：连接服务器（捕获连接错误）
            try {
                client.open();
            } catch (Exception e) {
                System.err.println("【连接错误】无法与邮件服务器建立连接：" + e.getMessage());
                return;
            }

            // 步骤二：准备收件人信息和正文并发送
            String recipientName = "测试收件人";
            String recipientEmail = "recipient@example.com";
            String subject = "测试单发邮件主题";
            String content = "<h1>你好</h1><p>这是一封测试单发 HTML 邮件。</p>";

            try {
                client.sendMail(recipientName, recipientEmail, subject, content);
            } catch (Exception e) {
                System.err.println("【发送错误】邮件发送失败：" + e.getMessage());
            }
            
        } catch (Exception e) {
            System.err.println("【资源关闭错误】关闭客户端资源时发生异常：" + e.getMessage());
        }
    }
}
```

#### 2. 多收件人群发示例 (TO/CC/BCC Group Sending)
通过传入 List 参数，单次调用即可将一封邮件同时发送给主收件人（TO）、抄送人（CC）和密送人（BCC）。

```java
import com.feilonglab.smtp.basic.SmtpClient;
import java.util.List;

public class MultipleRecipientsApp {
    public static void main(String[] args) {
        try (SmtpClient client = new SmtpClient()) {
            client.open();

            // 准备多个收件人、抄送人和密送人列表
            List<String> toList = List.of("to1@example.com", "to2@example.com");
            List<String> ccList = List.of("cc1@example.com");
            List<String> bccList = List.of("bcc1@example.com");

            String subject = "多收件人群发测试邮件（TO/CC/BCC）";
            String content = "<h1>群发测试</h1><p>这是一封同时投递给多个收件人、抄送和密送群组的邮件。</p>";

            client.sendMail(toList, ccList, bccList, subject, content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

#### 3. 动态 HTML 模板渲染发送示例 (Template Rendering)
使用内置模板渲染引擎 `TemplateEngine` 读取 `/templates/welcome.template`，并动态填入 Map 参数后发送。

```java
import com.feilonglab.smtp.basic.SmtpClient;
import com.feilonglab.smtp.Template.TemplateEngine;
import java.util.Map;

public class TemplateSendApp {
    public static void main(String[] args) {
        try (SmtpClient client = new SmtpClient()) {
            client.open();
            
            // 准备模板替换参数
            Map<String, String> params = Map.of(
                "username", "阿观",
                "email", "guan@example.com"
            );

            // 渲染类路径下的 HTML 模板文件
            String htmlBody = TemplateEngine.render("/templates/welcome.template", params);
            
            client.sendMail("阿观", "guan@example.com", "欢迎加入我们的平台！", htmlBody);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

#### 4. 多附件中文防乱码发送示例 (Attachments Sending)
使用 `AttachmentMailHelper` 组装带附件的符合 MIME 规范的 `MimeMessage` 对象并发送，它能够自动转义中文附件文件名，防止客户端接收时出现乱码问题。

```java
import com.feilonglab.smtp.basic.SmtpClient;
import com.feilonglab.smtp.attachments.AttachmentMailHelper;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.List;

public class AttachmentSendApp {
    public static void main(String[] args) {
        try (SmtpClient client = new SmtpClient()) {
            client.open();

            // 准备附件文件
            List<File> files = List.of(new File("src/main/resources/attachments/ShuXinJiaohuo.pdf"));

            // 组装包含 HTML 正文和附件的 MimeMessage
            MimeMessage message = AttachmentMailHelper.buildMessageWithAttachments(
                    client.getSession(),
                    "发件人别名", "sender@example.com",
                    "收件人", "recipient@example.com",
                    "测试附件邮件",
                    "<h1>正文</h1><p>请查收下方的 PDF 附件文件。</p>",
                    files
            );

            // 统一投递 MimeMessage 对象
            client.sendMimeMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

#### 5. 一站式统一链式 API 发送示例 (Unified MailRequest)
通过统一 API 对象 `MailRequest` 一站式以链式调用（Builder-like Pattern）组装上述所有的收发件人、HTML正文、备用纯文本、动态模板以及附件。

```java
import com.feilonglab.smtp.basic.SmtpClient;
import com.feilonglab.smtp.unified.MailRequest;
import java.util.Map;

public class UnifiedSendApp {
    public static void main(String[] args) {
        try (SmtpClient client = new SmtpClient()) {
            client.open();

            // 一键链式构建组合请求对象
            MailRequest request = new MailRequest()
                    .to("to1@example.com", "to2@example.com")
                    .cc("cc1@example.com")
                    .subject("【统一API】月度账单与交付通知")
                    .template("/templates/welcome.template", Map.of("username", "客户A", "email", "client@example.com"))
                    .textContent("备用降级纯文本：月度交付件请查收。")
                    .addAttachment("src/main/resources/attachments/ShuXinJiaohuo.pdf");

            // 一键发送，底层采用嵌套结构渲染并投递
            client.send(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```
