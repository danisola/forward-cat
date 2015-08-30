package controllers;

import org.hamcrest.Matcher;
import org.hamcrest.core.StringContains;

import static java.lang.String.format;

public class ContainsHtmlLang extends StringContains {

    private ContainsHtmlLang(String lang) {
        super(format("<html lang=\"%s\">", lang));
    }

    public static Matcher<String> containsHtmlLang(String lang) {
        return new ContainsHtmlLang(lang);
    }
}
