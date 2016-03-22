package models;

import com.google.common.collect.ImmutableMap;
import play.i18n.Lang;
import play.libs.F;
import play.mvc.Action;
import play.mvc.Http;
import play.mvc.Result;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static play.mvc.Results.movedPermanently;
import static play.mvc.Results.notFound;

public class DomainManager {

    private static final Lang DEFAULT_LANG = Lang.forCode("en");
    private static final Pattern PATH_LANG_PATTTERN = Pattern.compile("^/(?<lang>[a-z]{2})([/\\?].*)?");
    private static final Map<String, String> SUBDOMAIN_REDIRECTS = ImmutableMap.<String, String>builder()
            .put("www", "https://forward.cat")
            .build();

    public static F.Promise<Result> redirect(Http.Context ctx, Action<?> delegate) throws Throwable {
        Http.Request request = ctx.request();
        String host = request.host();

        // www subdomain: redirect
        if (host != null && host.startsWith("www.")) {
            String redirectUrl = buildUrl("www", request.path(), request.queryString());
            return promise(movedPermanently(redirectUrl));
        }

        // Lang in path: validate and set
        Matcher matcher = PATH_LANG_PATTTERN.matcher(request.path());
        if (matcher.matches()) {
            String lang = matcher.group("lang");
            if (!isValidLang(lang) || DEFAULT_LANG.code().equals(lang)) {
                return promise(notFound());
            } else {
                ctx.changeLang(lang);
                return delegate.call(ctx);
            }
        }

        // No lang: use default
        ctx.changeLang(DEFAULT_LANG);
        return delegate.call(ctx);
    }

    public static String buildUrl(play.api.i18n.Lang lang, String path) {
        checkArgument(path.startsWith("/"), "path must start with /");
        checkArgument(isValidLang(lang.language()), "%s is not a valid language", lang.language());

        String language = lang.language();
        if (language.equals(DEFAULT_LANG.language())) {
            return path;
        } else {
            return String.format("/%s%s", language, path);
        }
    }

    private static String buildUrl(String subdomain, String path, Map<String, String[]> query) {
        String hostAndPath = SUBDOMAIN_REDIRECTS.get(subdomain);
        return hostAndPath + path + queryString(query);
    }

    private static String queryString(Map<String, String[]> query) {
        Optional<String> params = query.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getValue() != null && e.getValue().length > 0)
                .map(e -> e.getKey() + "=" + e.getValue()[0])
                .reduce((p1, p2) -> p1 + "&" + p2);
        return params.map(p -> "?" + p).orElse("");
    }

    private static boolean isValidLang(String langCode) {
        Lang lang = Lang.forCode(langCode);
        return Lang.availables().contains(lang);
    }

    private static <A> F.Promise<A> promise(A result) {
        return F.Promise.pure(result);
    }

    private DomainManager() {
        // Non-instantiable
    }
}
