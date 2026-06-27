package com.feilonglab.smtp.attachments;

import java.io.File;
import java.util.List;

import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.feilonglab.smtp.basic.SmtpClient;

/**
 * 演示如何使用 {@link AttachmentMailHelper} 构建带有附件的 HTML 邮件消息，并通过 {@link SmtpClient}
 * 发送。
 * 重点演示了：
 * 1. 异常分级隔离：细致区分连接错误、附件构建/读取错误以及邮件发送错误。
 * 2. 统一日志：统一采用 SLF4J/Log4j 记录日志和追踪异常。
 * 3. 附件处理：引用项目根路径下的真实文件作为附件。
 * </p>
 *
 * @author feilonglab
 * @version 0.1.0
 */
public class AttachmentSmtpDemo {

	private static final Logger logger = LoggerFactory.getLogger(AttachmentSmtpDemo.class);

	public static void main(String[] args) {
		logger.info("开始执行【附件邮件发送示例】...");

		try (SmtpClient client = new SmtpClient()) {

			// 步骤一：建立连接（捕获连接异常）
			try {
				client.open();
			} catch (Exception e) {
				logger.error("【连接错误】无法建立与 SMTP 服务器的连接！", e);
				return; // 连接失败，不再继续
			}

			// 步骤二：准备附件文件，使用项目根目录下的测试文件
			List<File> attachments = List.of(
					new File("src/main/resources/attachments/ShuXinJiaohuo.pdf"));

			String html = "<h1>带附件的邮件</h1><p>请查收附件。</p>";

			// 步骤三：构建带附件的 MimeMessage（捕获文件读取及构建异常）
			MimeMessage message;
			try {
				message = AttachmentMailHelper.buildMessageWithAttachments(
						client.getSession(),
						"阿观",
						"sender@example.com",
						"收件人",
						"recipient@example.com",
						"测试附件邮件",
						html,
						attachments);
			} catch (Exception e) {
				logger.error("【构建错误】构建带附件的邮件消息失败，请检查附件路径或文件访问权限！", e);
				return; // 构建失败，终止发送
			}

			// 步骤四：执行邮件发送（捕获发送异常）
			try {
				client.sendMimeMessage(message);
			} catch (Exception e) {
				logger.error("【发送错误】发送带附件的邮件失败！", e);
			}

		} catch (Exception e) {
			logger.error("【资源关闭错误】关闭 SmtpClient 资源时发生异常！", e);
		}
	}

}
