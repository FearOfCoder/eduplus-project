package com.example.doan10diem.service;

import com.example.doan10diem.dto.GradingResultDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.MapperFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service chấm điểm bài tập bằng OpenAI (ChatGPT) API.
 * Giữ logic sanitize + Regex Fallback để xử lý mọi định dạng trả về.
 */
@Service
@Slf4j
public class OpenAIGradingService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAIGradingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Chấm điểm bài làm của sinh viên bằng OpenAI.
     */
    public GradingResultDTO gradeSubmission(String question, String studentAnswer, String correctAnswer) {
        System.out.println("DEBUG: OpenAIGradingService chấm điểm đã được gọi!");
        System.out.println("DEBUG: Key đang dùng: " + (apiKey != null && apiKey.length() >= 5 ? apiKey.substring(0, 5) + "..." : (apiKey != null ? "***" : "NULL")));
        try {
            String prompt = buildGradingPrompt(question, studentAnswer, correctAnswer);

            // Request body theo format OpenAI Chat Completions
            JsonNode requestBody = buildOpenAIRequest(prompt);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey != null ? apiKey : "");

            HttpEntity<String> requestEntity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            System.out.println("DEBUG: Đang gọi URL: " + apiUrl);
            log.info("Đang gọi OpenAI API (model={})...", model);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    apiUrl,
                    requestEntity,
                    String.class
            );

            String responseBody = response.getBody();
            System.out.println("DEBUG RAW AI: " + responseBody);
            log.info("OpenAI API raw response: {}", responseBody);

            if (responseBody == null || responseBody.trim().isEmpty()) {
                log.warn("OpenAI trả về response rỗng");
                return null;
            }

            // Parse response: choices[0].message.content
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode choices = rootNode.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                log.warn("OpenAI response không có choices");
                return null;
            }
            JsonNode firstChoice = choices.get(0);
            JsonNode message = firstChoice.path("message");
            JsonNode contentNode = message.path("content");
            if (contentNode.isMissingNode() || contentNode.isNull()) {
                log.warn("OpenAI message.content rỗng");
                return null;
            }
            String rawText = contentNode.asText();
            if (rawText == null || rawText.trim().isEmpty()) {
                log.warn("OpenAI message.content rỗng");
                return null;
            }

            System.out.println("DEBUG EXTRACTED TEXT: " + rawText);

            // Làm sạch (Regex) + Parse JSON + Fallback (giữ nguyên logic thần thánh)
            String cleanedJson = sanitizeAiResponse(rawText);
            System.out.println("DEBUG CLEANED JSON: " + cleanedJson);

            GradingResultDTO result = parseGradingResult(cleanedJson);
            if (result != null) {
                return result;
            }
            result = extractScoreByRegexFallback(rawText);
            if (result != null) {
                System.out.println("DEBUG: JSON lỗi, đã dùng Regex Fallback để lấy điểm!");
                return result;
            }
            return null;

        } catch (RestClientException e) {
            System.out.println("OPENAI ERROR (RestClientException): " + e.getMessage());
            e.printStackTrace();
            log.error("Lỗi khi gọi OpenAI API: {}", e.getMessage(), e);
            return null;
        } catch (Exception e) {
            System.out.println("OPENAI ERROR (Exception): " + e.getMessage());
            e.printStackTrace();
            log.error("Lỗi không mong đợi khi chấm điểm: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Hỏi đáp với AI dựa trên nội dung bài học.
     * System: trợ lý học tập + context bài học. User: câu hỏi của học viên.
     * @return Nội dung phản hồi từ AI, hoặc null nếu lỗi.
     */
    public String chatWithLessonContext(String lessonTitle, String lessonContent, String userQuestion) {
        if (userQuestion == null || userQuestion.trim().isEmpty()) {
            return "Bạn chưa nhập câu hỏi.";
        }
        try {
            String systemContent = "Bạn là trợ lý học tập vui tính. Nhiệm vụ của bạn là giải đáp thắc mắc của học viên DỰA TRÊN nội dung bài học được cung cấp dưới đây. Nếu câu hỏi không liên quan đến bài học, hãy khéo léo từ chối và gợi ý học viên đặt câu hỏi liên quan đến bài.\n\n--- NỘI DUNG BÀI HỌC ---\nTiêu đề: "
                    + (lessonTitle != null ? lessonTitle : "(Không có tiêu đề)")
                    + "\n\n"
                    + (lessonContent != null && !lessonContent.trim().isEmpty() ? lessonContent : "(Bài học chưa có nội dung text.)");

            com.fasterxml.jackson.databind.node.ArrayNode messages = objectMapper.createArrayNode()
                    .add(objectMapper.createObjectNode().put("role", "system").put("content", systemContent))
                    .add(objectMapper.createObjectNode().put("role", "user").put("content", userQuestion.trim()));

            JsonNode requestBody = objectMapper.createObjectNode()
                    .put("model", model)
                    .set("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey != null ? apiKey : "");

            HttpEntity<String> requestEntity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, requestEntity, String.class);
            String responseBody = response.getBody();

            if (responseBody == null || responseBody.trim().isEmpty()) {
                log.warn("OpenAI chat trả về response rỗng");
                return "Xin lỗi, tôi không nhận được phản hồi từ AI. Vui lòng thử lại.";
            }

            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode choices = rootNode.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                log.warn("OpenAI chat response không có choices");
                return "Xin lỗi, không thể xử lý phản hồi từ AI.";
            }
            JsonNode firstChoice = choices.get(0);
            JsonNode message = firstChoice.path("message");
            JsonNode contentNode = message.path("content");
            if (contentNode.isMissingNode() || contentNode.isNull()) {
                return "Xin lỗi, AI chưa trả lời được.";
            }
            String reply = contentNode.asText();
            return reply != null ? reply.trim() : "";
        } catch (RestClientException e) {
            log.error("Lỗi khi gọi OpenAI chat: {}", e.getMessage(), e);
            return "Lỗi kết nối AI: " + e.getMessage();
        } catch (Exception e) {
            log.error("Lỗi không mong đợi khi chat: {}", e.getMessage(), e);
            return "Đã xảy ra lỗi: " + e.getMessage();
        }
    }

    /** Tạo request body theo format OpenAI Chat Completions. */
    private JsonNode buildOpenAIRequest(String userPrompt) throws JsonProcessingException {
        String systemPrompt = "Bạn là trợ lý chấm bài. Trả về đúng format JSON được yêu cầu, không thêm markdown hay giải thích.";
        return objectMapper.createObjectNode()
                .put("model", model)
                .set("messages", objectMapper.createArrayNode()
                        .add(objectMapper.createObjectNode().put("role", "system").put("content", systemPrompt))
                        .add(objectMapper.createObjectNode().put("role", "user").put("content", userPrompt)));
    }

    private String buildGradingPrompt(String question, String studentAnswer, String correctAnswer) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Bạn là một giảng viên khó tính. Hãy chấm điểm bài làm sau theo thang điểm 10.\n\n");
        prompt.append("Câu hỏi: ").append(question != null ? question : "Không có").append("\n\n");

        if (correctAnswer != null && !correctAnswer.trim().isEmpty()) {
            prompt.append("Đáp án chuẩn / Tiêu chí chấm điểm:\n").append(correctAnswer).append("\n\n");
            prompt.append("Hãy so sánh bài làm của sinh viên với đáp án chuẩn và tiêu chí trên để chấm điểm.\n\n");
        } else {
            prompt.append("Không có đáp án chuẩn được cung cấp. Hãy tự đánh giá bài làm dựa trên kiến thức lập trình chuẩn của bạn.\n\n");
        }

        prompt.append("Bài làm sinh viên:\n").append(studentAnswer).append("\n\n");
        prompt.append("Yêu cầu output: Chỉ trả về JSON format gồm 2 trường:\n");
        prompt.append("- score: Double (điểm từ 0.0 đến 10.0)\n");
        prompt.append("- feedback: String (nhận xét ngắn gọn, súc tích dưới 100 từ)\n\n");
        prompt.append("Ví dụ: {\"score\": 8.5, \"feedback\": \"Bài làm tốt, logic đúng. Thiếu xử lý exception.\"}\n\n");
        prompt.append("IMPORTANT: Return ONLY raw JSON. Do not use Markdown backticks. Do not add explanations.");

        return prompt.toString();
    }

    private String sanitizeAiResponse(String text) {
        if (text == null) return "";
        String s = text.trim();
        if (s.isEmpty()) return "";
        Pattern pattern = Pattern.compile("\\{[\\s\\S]*\\}");
        Matcher matcher = pattern.matcher(s);
        if (matcher.find()) return matcher.group();
        return s;
    }

    private GradingResultDTO parseGradingResult(String jsonText) {
        try {
            GradingResultDTO dto = objectMapper.readValue(jsonText, GradingResultDTO.class);
            Double score = dto != null ? dto.getScore() : null;
            String feedback = dto != null ? dto.getFeedback() : null;
            if (score == null && dto != null) {
                JsonNode node = objectMapper.readTree(jsonText);
                score = findScoreFromNode(node);
            }
            if (score == null || feedback == null) return null;
            if (score <= 10.0) score = score * 10.0;
            return GradingResultDTO.builder().score(score).feedback(feedback != null ? feedback : "").build();
        } catch (Exception e) {
            log.debug("JSON parse failed: {}", e.getMessage());
            return null;
        }
    }

    private Double findScoreFromNode(JsonNode node) {
        for (String key : new String[]{"score", "Score", "SCORE", "grade", "Grade"}) {
            if (node.has(key) && !node.get(key).isNull()) {
                try { return node.get(key).asDouble(); } catch (Exception ignored) { }
            }
        }
        return null;
    }

    private GradingResultDTO extractScoreByRegexFallback(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) return null;
        Pattern pattern = Pattern.compile("(?i)(score|điểm|grade)\\s*[\":]+\\s*([\\d.]+)");
        Matcher matcher = pattern.matcher(rawText);
        if (matcher.find()) {
            try {
                double parsed = Double.parseDouble(matcher.group(2));
                if (parsed < 0) parsed = 0;
                if (parsed <= 10.0) parsed = parsed * 10.0;
                String feedback = rawText.length() > 500 ? rawText.substring(0, 500) + "..." : rawText;
                return GradingResultDTO.builder().score(parsed).feedback(feedback).build();
            } catch (NumberFormatException e) { return null; }
        }
        return null;
    }
}
