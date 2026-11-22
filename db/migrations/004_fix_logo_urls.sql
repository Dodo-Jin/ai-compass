-- AI Compass 마이그레이션: 깨지는 외부 logo_url을 Google Favicon API(128px)로 일괄 수정 (A 담당)
-- 원인: cdn.simpleicons.org/openai 삭제(404) 및 일부 사이트의 핫링크/Cloudflare 차단 해결
USE ai_compass;

SET SQL_SAFE_UPDATES = 0;

UPDATE `AI_Models` SET `logo_url` = 'https://www.google.com/s2/favicons?domain=chatgpt.com&sz=128'          WHERE `name` = 'ChatGPT';
UPDATE `AI_Models` SET `logo_url` = 'https://www.google.com/s2/favicons?domain=claude.ai&sz=128'            WHERE `name` = 'Claude';
UPDATE `AI_Models` SET `logo_url` = 'https://www.google.com/s2/favicons?domain=gemini.google.com&sz=128'    WHERE `name` = 'Gemini';
UPDATE `AI_Models` SET `logo_url` = 'https://www.google.com/s2/favicons?domain=perplexity.ai&sz=128'        WHERE `name` = 'Perplexity';
UPDATE `AI_Models` SET `logo_url` = 'https://www.google.com/s2/favicons?domain=midjourney.com&sz=128'       WHERE `name` = 'Midjourney';
UPDATE `AI_Models` SET `logo_url` = 'https://www.google.com/s2/favicons?domain=openai.com&sz=128'           WHERE `name` = 'DALL-E 3';
UPDATE `AI_Models` SET `logo_url` = 'https://www.google.com/s2/favicons?domain=stability.ai&sz=128'         WHERE `name` = 'Stable Diffusion';
UPDATE `AI_Models` SET `logo_url` = 'https://www.google.com/s2/favicons?domain=leonardo.ai&sz=128'          WHERE `name` = 'Leonardo AI';
UPDATE `AI_Models` SET `logo_url` = 'https://www.google.com/s2/favicons?domain=runwayml.com&sz=128'         WHERE `name` = 'Runway Gen-3';
UPDATE `AI_Models` SET `logo_url` = 'https://www.google.com/s2/favicons?domain=sora.com&sz=128'             WHERE `name` = 'Sora';
UPDATE `AI_Models` SET `logo_url` = 'https://www.google.com/s2/favicons?domain=suno.com&sz=128'             WHERE `name` = 'Suno';
UPDATE `AI_Models` SET `logo_url` = 'https://www.google.com/s2/favicons?domain=elevenlabs.io&sz=128'        WHERE `name` = 'ElevenLabs';
UPDATE `AI_Models` SET `logo_url` = 'https://www.google.com/s2/favicons?domain=github.com&sz=128'           WHERE `name` = 'GitHub Copilot';
UPDATE `AI_Models` SET `logo_url` = 'https://www.google.com/s2/favicons?domain=cursor.com&sz=128'           WHERE `name` = 'Cursor';
UPDATE `AI_Models` SET `logo_url` = 'https://www.google.com/s2/favicons?domain=v0.dev&sz=128'               WHERE `name` = 'v0 by Vercel';
UPDATE `AI_Models` SET `logo_url` = 'https://www.google.com/s2/favicons?domain=notion.so&sz=128'            WHERE `name` = 'Notion AI';
UPDATE `AI_Models` SET `logo_url` = 'https://www.google.com/s2/favicons?domain=gamma.app&sz=128'            WHERE `name` = 'Gamma';
UPDATE `AI_Models` SET `logo_url` = 'https://www.google.com/s2/favicons?domain=deepl.com&sz=128'            WHERE `name` = 'DeepL';
UPDATE `AI_Models` SET `logo_url` = 'https://www.google.com/s2/favicons?domain=notebooklm.google.com&sz=128' WHERE `name` = 'NotebookLM';
UPDATE `AI_Models` SET `logo_url` = 'https://www.google.com/s2/favicons?domain=wrtn.ai&sz=128'              WHERE `name` = 'Wrtn (뤼튼)';

SET SQL_SAFE_UPDATES = 1;