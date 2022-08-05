package com.custom.postprocessing.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.mail.internet.MimeMultipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.custom.postprocessing.email.api.dto.MailRequest;
import com.custom.postprocessing.email.api.dto.MailResponse;
import com.custom.postprocessing.email.api.dto.MailResponseDTO;

/**
 * @author kumar.charanswain
 *
 */

@Component
public class EmailUtility {

	public static final Logger logger = LoggerFactory.getLogger(EmailUtility.class);

	@Value("${mail.from}")
	private String mailForm;

	@Value("${mail.to}")
	private String mailTo;

	@Value("${mail.pcl.subject}")
	private String postProcessingSubject;
	
	@Value("${mail.archive.subject}")
	private String archiveSubject;

	@Value("${mail.smtp.starttls.key}")
	private String starttlsKey;

	@Value("${mail.smtp.starttls.value}")
	private String starttlsValue;

	@Value("${mail.smtp.host.key}")
	private String hostKey;

	@Value("${mail.smtp.host.value}")
	private String hostValue;

	@Value("${mail.smtp.port.key}")
	private String portKey;

	@Value("${mail.smtp.port.value}")
	private String portValue;

	@Value("${mail.smtp.auth.key}")
	private String authKey;

	@Value("${mail.smtp.auth.value}")
	private String authValue;

	@Value("${status.message}")
	private String statusMessage;

	public MailResponse sendEmail(MailRequest request, Map<String, Object> model, String currentDate) {
		MailResponse response = new MailResponse();
		try {
			Properties props = new Properties();
			props.put(starttlsKey, starttlsValue);
			props.put(hostKey, hostValue);
			props.put(portKey, portValue);
			props.put(authKey, authValue);
			Session session = Session.getDefaultInstance(props);
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(mailForm));
			message.setRecipient(RecipientType.TO, new InternetAddress(mailTo));
			message.setSubject(postProcessingSubject);
			message.setText(statusMessage, "UTF-8");
			File documentTxtFile = addAttachment(currentDate, request.getFileNames());
			response.setFile(documentTxtFile);
			MimeBodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.attachFile(documentTxtFile.getName());
			File logFileName = new File("smartcompostprocessing" + "_" + currentDate + ".log");
			messageBodyPart.setFileName(logFileName.toString());
			messageBodyPart.setHeader("Content-Type",
					"text/plain; charset=\"us-ascii\"; name=" + documentTxtFile.toString());

			MimeMultipart multipart = new MimeMultipart();
			multipart.addBodyPart(messageBodyPart);
			message.setContent(multipart);
			message.setSentDate(new Date());
			response.setFile(documentTxtFile);
			Transport.send(message);

			response.setMessage("mail send successfully : " + request.getTo());
			response.setStatus(Boolean.TRUE);

		} catch (AddressException addressException) {
			logger.info("email address invalid sendEmail() " + addressException.getMessage());
			response.setMessage("mail sending failure");
			response.setStatus(Boolean.FALSE);
		} catch (MessagingException messagingException) {
			logger.info("message invalid sendEmail() :" + messagingException.getMessage());
			response.setMessage("mail sending failure");
			response.setStatus(Boolean.FALSE);
		} catch (Exception exception) {
			logger.info("exception sendEmail() :" + exception.getMessage());
			response.setMessage("mail sending failure");
			response.setStatus(Boolean.FALSE);
		}
		return response;
	}

	public MailResponse sendEmail(String messageStatus) {
		MailResponse response = new MailResponse();
		try {
			Properties props = new Properties();
			props.put(starttlsKey, starttlsValue);
			props.put(hostKey, hostValue);
			props.put(portKey, portValue);
			props.put(authKey, authValue);
			Session session = Session.getDefaultInstance(props);
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(mailForm));
			message.setRecipient(RecipientType.TO, new InternetAddress(mailTo));
			message.setSubject(archiveSubject);
			message.setText(messageStatus, "UTF-8");
			message.setSentDate(new Date());
			response.setMessage(messageStatus);
			response.setStatus(Boolean.TRUE);
			Transport.send(message);
		} catch (AddressException addressException) {
			logger.info("email address invalid sendEmail() :" + addressException.getMessage());
			response.setMessage("mail sending failure");
			response.setStatus(Boolean.FALSE);
		} catch (MessagingException messagingException) {
			logger.info("message invalid sendEmail() :" + messagingException.getMessage());
			response.setMessage("mail sending failure");
			response.setStatus(Boolean.FALSE);
		} catch (Exception exception) {
			logger.info("exception sendEmail() :" + exception.getMessage());
			response.setMessage("mail sending failure");
			response.setStatus(Boolean.FALSE);
		}
		return response;
	}

	public File addAttachment(String currentDate, List<String> fileNames) {
		File file = null;
		try {
			String documentFileName = "completed-" + currentDate + ".txt";
			file = new File(documentFileName);
			final FileOutputStream outputStream = new FileOutputStream(file);
			PrintWriter writer = new PrintWriter(outputStream);
			writer.println("process file type summary" + '\n');
			for (String fileName : fileNames) {
				writer.println(fileName);
			}
			outputStream.close();
			writer.close();
		} catch (Exception exception) {
			logger.info("Exception addAttachment() :" + exception.getMessage());
		}
		return file;
	}

	public void emailProcess(ConcurrentHashMap<String, List<String>> updatePostProcessMap, String currentDate, String statusMessage) {
		logger.info("current date:" + currentDate);
		try {
			List<String> updateFileNames = new LinkedList<String>();
			List<MailResponseDTO> mailResponseDTOList = new LinkedList<MailResponseDTO>();
			for (String fileType : updatePostProcessMap.keySet()) {
				List<String> fileNames = updatePostProcessMap.get(fileType);
				addFileNameList(fileNames, updateFileNames);

				MailResponseDTO mailResponseDTO = new MailResponseDTO();
				mailResponseDTO.setFileType(fileType);
				mailResponseDTO.setTotalSize(fileNames.size());
				mailResponseDTOList.add(mailResponseDTO);
			}

			MailRequest mailRequest = new MailRequest();
			mailRequest.setFrom(mailForm);
			mailRequest.setTo(mailTo);
			mailRequest.setSubject(postProcessingSubject);
			mailRequest.setFileNames(updateFileNames);

			Map<String, Object> model = new HashMap<>();
			model.put("mailResponseList", mailResponseDTOList);
			MailResponse mailResponse = sendEmail(mailRequest, model, currentDate);
			if (Objects.nonNull(mailResponse.getFile()))
				mailResponse.getFile().delete();
		} catch (Exception exception) {
			logger.info("exception emailProcess():" + exception.getMessage());
		}
	}

	public void addFileNameList(List<String> fileNames, List<String> updateFileNames) {
		for (String fileName : fileNames) {
			updateFileNames.add(fileName);
		}
	}

}
