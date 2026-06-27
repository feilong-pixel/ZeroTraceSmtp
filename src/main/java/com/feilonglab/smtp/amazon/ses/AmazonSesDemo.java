package com.feilonglab.smtp.amazon.ses;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.feilonglab.smtp.basic.SmtpClient;

/**
 * 演示如何使用 {@link SmtpClient} 结合专门的 AWS SES 配置文件来发送邮件。
 * 重点演示了：
 * 1. 构造函数直接指定配置文件：使用 {@code new SmtpClient("/mail-ses.properties")} 直接载入 SES
 * 属性。
 * 2. 异常分层隔离：清晰区分连接错误和邮件发送错误。
 * 3. 统一日志管理：使用 SLF4J/Log4j 记录日志和追踪异常，不直接使用 stderr。
 * </p>
 *
 * @author feilonglab
 * @version 0.1.0
 */
public class AmazonSesDemo {

	private static final Logger logger = LoggerFactory.getLogger(AmazonSesDemo.class);

	public static void main(String[] args) {
		logger.info("开始执行【Amazon SES 邮件发送示例】...");

		// 使用指定的 SES 配置文件构造客户端
		try (SmtpClient client = new SmtpClient("/mail-ses.properties")) {

			// 步骤一：建立连接（捕获连接异常）
			try {
				client.open();
			} catch (Exception e) {
				logger.error("【连接错误】建立与 Amazon SES SMTP 服务器的连接失败！", e);
				return; // 连接失败，不再继续
			}

			// 步骤二：准备测试邮件数据
			String recipientName = "收件人姓名";
			String recipientEmail = "recipient@example.com";
			String subject = "Amazon SES 测试邮件";
			String content = "<h1>Hello SES</h1><p>这是一封通过 Amazon SES 发送的测试邮件。</p>";

			// 步骤三：发送邮件（捕获发送异常）
			try {
				client.sendMail(recipientName, recipientEmail, subject, content);
			} catch (Exception e) {
				logger.error("【发送错误】向 Amazon SES 发送邮件失败！", e);
			}

		} catch (Exception e) {
			logger.error("【资源关闭错误】关闭 SmtpClient 资源时发生异常！", e);
		}
	}

}
