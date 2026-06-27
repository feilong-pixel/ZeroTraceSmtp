package com.feilonglab.smtp.Template;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.feilonglab.smtp.basic.SmtpClient;

/**
 * 演示如何结合模板引擎 {@link TemplateEngine} 渲染 HTML 邮件正文，并通过 {@link SmtpClient} 发送。
 * 重点演示了：
 * 1. 异常分级管理：清晰隔离连接错误、模板渲染错误以及邮件发送错误。
 * 2. 统一日志：统一采用 SLF4J/Log4j 记录流程及异常。
 * 3. 资源自动释放：使用 try-with-resources 安全关闭 SmtpClient。
 * </p>
 *
 * @author feilonglab
 * @version 0.1.0
 */
public class TemplateSmtpDemo {

	private static final Logger logger = LoggerFactory.getLogger(TemplateSmtpDemo.class);

	public static void main(String[] args) {
		logger.info("开始执行【模板邮件发送示例】...");

		try (SmtpClient client = new SmtpClient()) {

			// 步骤一：建立连接（捕获连接异常）
			try {
				client.open();
			} catch (Exception e) {
				logger.error("【连接错误】无法建立与 SMTP 服务器的连接！", e);
				return; // 连接失败直接返回，不再继续发送
			}

			// 步骤二：准备模板渲染所需的数据参数
			Map<String, String> params = new HashMap<>();
			params.put("username", "阿观");
			params.put("email", "user@example.com");

			// 步骤三：读取并解析模板（捕获模板渲染异常）
			String content;
			try {
				content = TemplateEngine.render("/templates/welcome.template", params);
			} catch (Exception e) {
				logger.error("【渲染错误】邮件模板渲染失败，请检查模板路径及语法！", e);
				return; // 模板解析失败导致内容为空，中止发送流程
			}

			// 步骤四：执行邮件发送（捕获发送异常）
			try {
				client.sendMail(
						"阿观",
						"recipient@example.com",
						"欢迎加入！",
						content);
			} catch (Exception e) {
				logger.error("【发送错误】模板邮件发送失败！", e);
			}

		} catch (Exception e) {
			logger.error("【资源关闭错误】关闭 SmtpClient 资源时发生异常！", e);
		}
	}

}
