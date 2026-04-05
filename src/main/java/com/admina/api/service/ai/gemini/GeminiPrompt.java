package com.admina.api.service.ai.gemini;

import java.time.LocalDate;

public class GeminiPrompt {

  public String buildTranslatePrompt(String targetLanguage) {
    return """
        You are a translation assistant. ALL OUTPUT MUST BE IN %s.
        If you ever include any text that is not in %s, the response will be considered INVALID.

        Task:
        Analyze the document provided. Ignore all images, photos, logos, diagrams, and visual artifacts.
        Extract only the legible text, translate it into %s, and produce a JSON object with the following properties only:

        - "translatedText": a single string containing the FULL translated document text in %s.
        - "structuredTranslatedText": an object mapping page keys to HTML strings for each page, e.g. { "page1": "...", "page2": "..." }.
          Each page string must be valid HTML (semantic tags) and include only the translated text (no original-language text), with inline Tailwind CSS classes allowed.

        REQUIRED FORMAT (must be raw JSON only):
        - Respond with raw JSON only. Do NOT add any surrounding text, Markdown, or code fences.
        - The JSON must parse without error.
        - Property names must be exactly: translatedText, structuredTranslatedText.

        IMPORTANT BEHAVIORAL RULES:
        1. ALL textual content (including inside structuredTranslatedText HTML) MUST BE IN %s. Do NOT include any original-language fragments or English.
        2. The value of translatedText MUST EXACTLY MATCH the textual content inside the structuredTranslatedText HTML (ignoring HTML tags).
        3. Use semantic HTML elements (p, h1-h6, ul/li, etc.). You may include inline Tailwind classes on those tags.
        4. Do NOT include any additional properties, metadata, comments, or explanatory text.
        5. If you cannot translate or detect the language, return this JSON with an "error" property: { "error": "explanation in %s" }.
        6. If the translation would be identical to the input (e.g., input already in %s), still return the JSON object with translatedText in %s.

        7. STRICTLY IGNORE IMAGES AND GRAPHICS:
           - Do NOT attempt to translate text inside complex logos, stamps, or low-quality images.
           - Do NOT write descriptions of images (e.g., do NOT write "[Logo]", "[Signature]", or "[Image of house]").
           - Do NOT use HTML <img> tags.
           - Focus ONLY on the main body text, headers, and tables.

        Raw JSON only. No markdown, no code fences, no explanation. All content must be in %s. IGNORE IMAGES.
        """
        .formatted(
            targetLanguage, targetLanguage, targetLanguage, targetLanguage,
            targetLanguage, targetLanguage, targetLanguage, targetLanguage,
            targetLanguage);
  }

  public String buildSummarizePrompt(String translatedText, String targetLanguage) {
    String today = LocalDate.now().toString();
    return """
        You are a multilingual document analysis assistant.

        Your task:
        1. Carefully read the following document written in %s.
        2. Extract structured information and produce a concise, factual JSON summary.
        3. All textual fields (title, sender, summary, action plan, action plans, etc.) must be written fully in %s.

        ---

        DOCUMENT CONTENT:
        %s

        ---

        OUTPUT FORMAT:
        Return only raw JSON (no markdown, no code fences, no explanations).
        The JSON must include these exact keys:

        {
          "title": "string — short descriptive title of the document (in %s)",
          "receivedDate": "string — date when the document was received (yyyy-MM-dd, e.g. %s)",
          "sender": "string — name of the person, organization, or institution that sent the document (in %s)",
          "summary": "string — comprehensive and factual summary of the document's full content (in %s)",

          "actionPlan": [
            {
              "title": "string — key point or recommendation derived from the document (in %s)",
              "reason": "string — explanation for why this key point is important (in %s)"
            }
          ],

          "actionPlans": [
            {
              "title": "string — specific actionable task the user should perform (in %s)",
              "due_date": "string — date by which this task should be completed (yyyy-MM-dd). If no due date is mentioned in the document, default to %s.",
              "completed": false,
              "location": "string — where this action should take place (in %s)"
            }
          ]
        }

        ---

        DIFFERENCE BETWEEN actionPlan AND actionPlans:
        - actionPlan = Key insights or recommendations summarized from the document.
        - actionPlans = Specific actionable tasks the user needs to do as a result of the document.

        ---

        CRITICAL INSTRUCTIONS:
        - All text must be written fully in %s.
        - Ensure the JSON is valid and can be parsed directly.
        - Do NOT include markdown, explanations, or text outside the JSON.
        - If any information is missing, infer it if possible, or leave the field as an empty string.
        - due_date must always be a valid date string in yyyy-MM-dd format. Never leave it empty or null. If unknown, use %s.
        """
        .formatted(
            targetLanguage, targetLanguage, translatedText,
            targetLanguage, today, targetLanguage, targetLanguage,
            targetLanguage, targetLanguage, targetLanguage,
            today, targetLanguage, targetLanguage, today);
  }
}