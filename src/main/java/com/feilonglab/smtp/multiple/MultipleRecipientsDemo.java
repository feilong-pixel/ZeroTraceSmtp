package com.feilonglab.smtp.multiple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.feilonglab.smtp.basic.SmtpClient;

import java.util.List;

/**
 * 演示单封邮件同时发送给多收件人（TO/CC/BCC）。
 * 重点演示了：
 * 1. 单次方法调用发送给多个收件人
 * 2. 正常发送与配置文件的动态加载
 * 3. 统一日志管理（SLF4J/Log4j）
 * 4. 嵌套异常隔离机制
 *
 * @author feilonglab
 * @version 0.1.0
 */
public class MultipleRecipientsDemo {

	private static final Logger logger = LoggerFactory.getLogger(MultipleRecipientsDemo.class);

	public static void main(String[] args) {
		// 示例 1：使用默认配置文件发送多收件人邮件
		runDefaultConfigDemo();

		logger.info("========================================");

		// 示例 2：使用指定的 AWS SES 配置文件发送多收件人邮件
		runCustomConfigDemo();
	}

	/**
	 * 演示使用默认配置 (/mail.properties) 发送多收件人邮件。
	 */
	private static void runDefaultConfigDemo() {
		logger.info("开始执行【示例 1：使用默认配置文件发送】...");

		try (SmtpClient client = new SmtpClient()) {
			// 步骤一：建立连接（捕获连接异常）
			try {
				client.open();
			} catch (Exception e) {
				logger.error("【连接错误】无法建立与 SMTP 服务器的连接！", e);
				return; // 连接失败，不继续执行发送
			}

			// 步骤二：准备收件人、抄送及密送列表
			List<String> toList = List.of("to1@example.com", "to2@example.com");
			List<String> ccList = List.of("cc1@example.com", "cc2@example.com");
			List<String> bccList = List.of("bcc1@example.com");

			String subject = "【群发测试】默认配置多收件人邮件（TO/CC/BCC）";
			String content = "<h1>多收件人测试（默认配置）</h1><p>这是一封通过 SmtpClient 默认配置发送的群发测试邮件。</p>";

			// 步骤三：执行邮件发送（捕获发送异常）
			try {
				client.sendMail(toList, ccList, bccList, subject, content);
			} catch (Exception e) {
				logger.error("【发送错误】默认配置邮件发送失败！", e);
			}

		} catch (Exception e) {
			logger.error("【资源关闭错误】关闭 SmtpClient 资源时发生异常！", e);
		}
	}

	/**
	 * 演示使用自定义 AWS SES 配置文件 (/mail-ses.properties) 发送多收件人邮件。
	 */
	private static void runCustomConfigDemo() {
		logger.info("开始执行【示例 2：使用自定义 AWS SES 配置文件发送】...");

		try (SmtpClient client = new SmtpClient("/mail-ses.properties")) {
			// 步骤一：建立连接（捕获连接异常）
			try {
				client.open();
			} catch (Exception e) {
				logger.error("【连接错误】无法建立与 Amazon SES SMTP 服务器的连接！", e);
				return; // 连接失败，不继续执行发送
			}

			// 步骤二：准备收件人、抄送及密送列表
			List<String> toList = List.of("ses-to1@example.com");
			List<String> ccList = List.of("ses-cc1@example.com");
			List<String> bccList = null; // 密送为空

			String subject = "【Amazon SES 群发测试】多收件人邮件";
			String content = "<h1>Hello from AWS SES</h1><p>这是一封通过 Amazon SES 发送的多收件人邮件。</p>";

			// 步骤三：执行邮件发送（捕获发送异常）
			try {
				client.sendMail(toList, ccList, bccList, subject, content);
			} catch (Exception e) {
				logger.error("【发送错误】Amazon SES 邮件发送失败！", e);
			}

		} catch (Exception e) {
			logger.error("【资源关闭错误】关闭 SmtpClient 资源时发生异常！", e);
		}
	}

}