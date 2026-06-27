package com.feilonglab.smtp.unified;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.feilonglab.smtp.basic.SmtpClient;

/**
 * 演示使用统一的 {@link MailRequest} 请求体进行邮件发送（支持模板渲染、附件、多群发等组合功能）。
 * 重点演示了：
 * 1. 组合发送：一封邮件同时具备 HTML 模板、纯文本备用、附件、抄送及密送。
 * 2. 链式/对象式请求传参：简化了方法签名参数过多的问题。
 * 3. 统一日志：采用统一的 SLF4J/Log4j 输出。
 *
 * @author feilonglab
 * @version 0.1.0
 */
public class UnifiedSmtpDemo {

	private static final Logger logger = LoggerFactory.getLogger(UnifiedSmtpDemo.class);

	public static void main(String[] args) {
		logger.info("开始执行【统一 API 邮件发送示例】...");

		try (SmtpClient client = new SmtpClient()) {

			// 步骤一：建立连接
			try {
				client.open();
			} catch (Exception e) {
				logger.error("【连接错误】无法建立与 SMTP 服务器的连接！", e);
				return;
			}

			// 步骤二：准备模板参数（如果有）
			Map<String, String> params = new HashMap<>();
			params.put("username", "阿观");
			params.put("email", "unified@example.com");

			// 步骤三：构造 MailRequest 组合对象（使用链式调用 Builder 模式）
			MailRequest request = new MailRequest()
					.to("to-user1@example.com", "to-user2@example.com")
					.cc("cc-user@example.com")
					.bcc("bcc-user@example.com")
					.subject("【统一API测试】包含模板与附件的邮件")
					.template("/templates/welcome.template", params)
					.textContent("你好，阿观。你的账号已经成功创建。登录邮箱：unified@example.com")
					.addAttachment("src/main/resources/attachments/attachment_test.txt");

			// 步骤四：执行发送
			try {
				client.send(request);
			} catch (Exception e) {
				logger.error("【发送错误】统一 API 邮件发送失败！", e);
			}

		} catch (Exception e) {
			logger.error("【资源关闭错误】关闭 SmtpClient 资源时发生异常！", e);
		}
	}

}
