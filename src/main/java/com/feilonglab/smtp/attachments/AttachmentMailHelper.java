package com.feilonglab.smtp.attachments;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

/**
 * 带有附件的邮件消息构建辅助工具类。
 * <p>
 * 提供便捷的静态方法用于构建符合 MIME 规范、包含 HTML 正文和多个附件的 {@link MimeMessage} 实例。
 * 优化点包括：
 * 1. 严格的参数校验：校验 Session、收发件人邮箱等，避免在构建中途抛出隐式的 NullPointerException。
 * 2. 附件合法性校验：在添加附件前，校验文件是否存在且不是目录，否则抛出明确的异常（如 FileNotFoundException），防止附件发送出错。
 * 3. 解决中文附件名乱码问题：使用 {@link MimeUtility#encodeText(String)} 对附件名进行 UTF-8
 * 编码，防止在部分客户端（如 Outlook、Gmail）上显示乱码。
 * 4. 优化异常声明：将通用的 {@code throws Exception} 精确化为具体的 {@code MessagingException} 和
 * {@code IOException}。
 * </p>
 *
 * @author feilonglab
 * @version 0.1.0
 */
public class AttachmentMailHelper {

	/**
	 * 构建包含 HTML 内容和多个附件的 MimeMessage 邮件消息体。
	 *
	 * @param session        邮件服务器会话对象，不能为 null
	 * @param senderName     发件人姓名
	 * @param senderEmail    发件人邮箱地址，不能为 null 或空
	 * @param recipientName  收件人姓名
	 * @param recipientEmail 收件人邮箱地址，不能为 null 或空
	 * @param subject        邮件主题
	 * @param htmlContent    邮件正文 HTML 内容
	 * @param attachments    附件文件列表，可以为 null 或空
	 * @return 构建好的 MimeMessage 实例
	 * @throws MessagingException 在邮件参数设置或邮件结构构建出错时抛出
	 * @throws IOException        在读取附件文件出错时抛出
	 */
	public static MimeMessage buildMessageWithAttachments(
			Session session,
			String senderName,
			String senderEmail,
			String recipientName,
			String recipientEmail,
			String subject,
			String htmlContent,
			List<File> attachments) throws MessagingException, IOException {

		// 参数安全性校验
		if (session == null) {
			throw new IllegalArgumentException("Session 不能为空。");
		}
		if (senderEmail == null || senderEmail.isEmpty()) {
			throw new IllegalArgumentException("发件人邮箱地址 (senderEmail) 不能为空。");
		}
		if (recipientEmail == null || recipientEmail.isEmpty()) {
			throw new IllegalArgumentException("收件人邮箱地址 (recipientEmail) 不能为空。");
		}

		MimeMessage message = new MimeMessage(session);

		// 设置发件人，带姓名别名并进行编码
		message.setFrom(new InternetAddress(senderEmail, senderName, "UTF-8"));

		// 设置收件人，带姓名别名并进行编码
		message.setRecipient(Message.RecipientType.TO,
				new InternetAddress(recipientEmail, recipientName, "UTF-8"));

		// 设置邮件主题
		message.setSubject(subject, "UTF-8");

		// 创建 multipart 容器。默认 subtype 为 "mixed"，适合正文加附件的混合结构
		MimeMultipart multipart = new MimeMultipart();

		// 创建并填充 HTML 正文部分
		MimeBodyPart htmlPart = new MimeBodyPart();
		htmlPart.setContent(htmlContent != null ? htmlContent : "", "text/html;charset=UTF-8");
		multipart.addBodyPart(htmlPart);

		// 创建并填充附件部分
		if (attachments != null && !attachments.isEmpty()) {
			for (File file : attachments) {
				if (file == null) {
					continue;
				}
				// 校验附件文件合法性
				if (!file.exists()) {
					throw new FileNotFoundException("附件文件不存在：" + file.getAbsolutePath());
				}
				if (file.isDirectory()) {
					throw new IllegalArgumentException("附件路径不能为目录：" + file.getAbsolutePath());
				}

				MimeBodyPart attachmentPart = new MimeBodyPart();
				// 关联文件数据到 Part
				attachmentPart.attachFile(file);
				// 对附件文件名进行 UTF-8 RFC 2047 编码，防止在邮件客户端显示为乱码或未命名附件
				attachmentPart.setFileName(MimeUtility.encodeText(file.getName(), "UTF-8", null));

				multipart.addBodyPart(attachmentPart);
			}
		}

		// 将构建好的多段结构内容设置进 Message 实体中
		message.setContent(multipart);
		return message;
	}
}
