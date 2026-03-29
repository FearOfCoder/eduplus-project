package com.example.doan10diem.service;

import com.example.doan10diem.model.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Gửi email xác thực tài khoản cho user.
     *
     * @param user    User cần xác thực
     * @param siteUrl URL gốc của site (vd: http://localhost:8080)
     */
    public void sendVerificationEmail(User user, String siteUrl) throws MessagingException {
        String verificationLink = siteUrl + "/verify?code=" + user.getVerificationCode();
        String subject = "Chào mừng bạn đến với EduPlus! 🎓";

        String htmlContent = """
            <html>
            <body style="margin: 0; padding: 0;">
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
                    <div style="margin-bottom: 20px;">
                        <strong style="color: #0d6efd; font-size: 20px;">EduPlus</strong>
                    </div>
                    <p>Xin chào <strong>%s</strong>,</p>
                    <p>Cảm ơn bạn đã đăng ký tài khoản. Vui lòng nhấn vào nút dưới đây để kích hoạt:</p>
                    <p>
                        <a href="%s" style="display: inline-block; background-color: #0d6efd; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold; margin-top: 10px; margin-bottom: 10px;">Kích hoạt tài khoản ngay</a>
                    </p>
                    <p style="color: #666; font-size: 14px;">Nếu bạn không yêu cầu, vui lòng bỏ qua email này. Trân trọng, Đội ngũ EduPlus.</p>
                </div>
            </body>
            </html>
            """.formatted(user.getFullName(), verificationLink);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(user.getEmail());
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    /**
     * Gửi email chứa mã OTP xác thực thanh toán.
     *
     * @param user    User cần nhận OTP
     * @param otp     Mã OTP (5 chữ số)
     * @param courseTitle Tên khóa học đang thanh toán
     */
    public void sendOtpEmail(User user, String otp, String courseTitle) throws MessagingException {
        String subject = "Mã xác thực thanh toán EduPlus";

        String htmlContent = """
            <html>
            <body style="margin: 0; padding: 0;">
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
                    <div style="margin-bottom: 20px;">
                        <strong style="color: #0d6efd; font-size: 20px;">EduPlus</strong>
                    </div>
                    <p>Xin chào <strong>%s</strong>,</p>
                    <p>Bạn đang thực hiện thanh toán cho khóa học: <strong>%s</strong></p>
                    <p>Mã xác thực OTP của bạn là:</p>
                    <div style="background-color: #f8f9fa; border: 2px dashed #0d6efd; padding: 20px; text-align: center; margin: 20px 0; border-radius: 5px;">
                        <h1 style="color: #0d6efd; font-size: 36px; letter-spacing: 8px; margin: 0;">%s</h1>
                    </div>
                    <p style="color: #666; font-size: 14px;">Mã này có hiệu lực trong 10 phút. Vui lòng không chia sẻ mã này với bất kỳ ai.</p>
                    <p style="color: #666; font-size: 14px;">Nếu bạn không yêu cầu thanh toán này, vui lòng bỏ qua email này. Trân trọng, Đội ngũ EduPlus.</p>
                </div>
            </body>
            </html>
            """.formatted(user.getFullName(), courseTitle, otp);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(user.getEmail());
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    /**
     * Gửi email chứa mã OTP xác thực rút tiền cho giảng viên.
     */
    public void sendWithdrawalOtpEmail(User user, String otp) throws MessagingException {
        String subject = "Mã OTP xác thực rút tiền - EduPlus";

        String htmlContent = """
            <html>
            <body style="margin: 0; padding: 0;">
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
                    <div style="margin-bottom: 20px;">
                        <strong style="color: #0d6efd; font-size: 20px;">EduPlus</strong>
                    </div>
                    <p>Xin chào <strong>%s</strong>,</p>
                    <p>Bạn đang thực hiện yêu cầu rút tiền. Mã xác thực OTP của bạn là:</p>
                    <div style="background-color: #f8f9fa; border: 2px dashed #0d6efd; padding: 20px; text-align: center; margin: 20px 0; border-radius: 5px;">
                        <h1 style="color: #0d6efd; font-size: 36px; letter-spacing: 8px; margin: 0;">%s</h1>
                    </div>
                    <p style="color: #666; font-size: 14px;">Mã này có hiệu lực trong 10 phút. Vui lòng không chia sẻ mã này với bất kỳ ai.</p>
                    <p style="color: #666; font-size: 14px;">Nếu bạn không yêu cầu rút tiền, vui lòng bỏ qua email này. Trân trọng, Đội ngũ EduPlus.</p>
                </div>
            </body>
            </html>
            """.formatted(user.getFullName(), otp);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(user.getEmail());
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
}
