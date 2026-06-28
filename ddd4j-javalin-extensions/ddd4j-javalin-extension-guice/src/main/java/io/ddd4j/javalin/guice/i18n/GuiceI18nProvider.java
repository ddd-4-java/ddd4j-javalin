package io.ddd4j.javalin.guice.i18n;

import io.ddd4j.core.context.I18nProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guice 实现的国际化提供者
 * <p>
 * 基于 ResourceBundle 实现，替代 Spring 的 MessageSource。
 *
 * @author Loong Wan
 */
public class GuiceI18nProvider implements I18nProvider {

    private static final Logger logger = LoggerFactory.getLogger(GuiceI18nProvider.class);
    private static final String BUNDLE_PREFIX = "i18n/message";
    private static final Map<String, ResourceBundle> CACHE = new ConcurrentHashMap<>();

    private final String defaultLang;

    public GuiceI18nProvider() {
        this("zh");
    }

    public GuiceI18nProvider(String defaultLang) {
        this.defaultLang = defaultLang;
    }

    @Override
    public String getMessage(String key, Object... args) {
        if (key == null || key.isEmpty()) return "";

        String result = resolve(defaultLang, key);
        if (args != null && args.length > 0) {
            try {
                result = String.format(result, args);
            } catch (Exception e) {
                logger.warn("Could not format i18n msg, lang={}, key={}", defaultLang, key);
            }
        }
        return result;
    }

    private String resolve(String lang, String key) {
        ResourceBundle bundle = CACHE.computeIfAbsent(lang, this::loadBundle);
        if (bundle != null && bundle.containsKey(key)) {
            return bundle.getString(key);
        }
        // 回退：zh-TW → zh
        if (lang.contains("-")) {
            String fallback = lang.substring(0, lang.indexOf("-"));
            ResourceBundle fb = CACHE.computeIfAbsent(fallback, this::loadBundle);
            if (fb != null && fb.containsKey(key)) {
                return fb.getString(key);
            }
        }
        return key;
    }

    private ResourceBundle loadBundle(String lang) {
        try {
            Locale locale = Locale.forLanguageTag(lang);
            return ResourceBundle.getBundle(BUNDLE_PREFIX, locale);
        } catch (MissingResourceException e) {
            logger.debug("i18n bundle not found for lang={}", lang);
            return null;
        }
    }
}
