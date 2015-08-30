package models;

import play.api.http.MediaRange;
import play.i18n.Lang;
import play.mvc.Http;

import java.util.List;
import java.util.Map;

public class TestHttpRequest extends Http.Request {

    private Http.RequestBody body;
    private String uri;
    private String method = "GET";
    private String version = "HTTP/1.1";
    private String remoteAddress = "127.0.0.1";
    private boolean secure = false;
    private String host;
    private String path;
    private List<Lang> acceptLanguages;
    private List<String> accept;
    private List<MediaRange> acceptedTypes;
    private boolean accepts;
    private Map<String, String[]> queryString;
    private Http.Cookies cookies;
    private Map<String, String[]> headers;

    @Override
    public Http.RequestBody body() {
        return body;
    }

    public TestHttpRequest setBody(Http.RequestBody body) {
        this.body = body;
        return this;
    }

    @Override
    public String uri() {
        return uri;
    }

    public TestHttpRequest setUri(String uri) {
        this.uri = uri;
        return this;
    }

    @Override
    public String method() {
        return method;
    }

    public TestHttpRequest setMethod(String method) {
        this.method = method;
        return this;
    }

    @Override
    public String version() {
        return version;
    }

    public TestHttpRequest setVersion(String version) {
        this.version = version;
        return this;
    }

    @Override
    public String remoteAddress() {
        return remoteAddress;
    }

    public TestHttpRequest setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
        return this;
    }

    @Override
    public boolean secure() {
        return secure;
    }

    public TestHttpRequest setSecure(boolean secure) {
        this.secure = secure;
        return this;
    }

    @Override
    public String host() {
        return host;
    }

    public TestHttpRequest setHost(String host) {
        this.host = host;
        return this;
    }

    @Override
    public String path() {
        return path;
    }

    public TestHttpRequest setPath(String path) {
        this.path = path;
        return this;
    }

    @Override
    public List<Lang> acceptLanguages() {
        return acceptLanguages;
    }

    public TestHttpRequest setAcceptLanguages(List<Lang> acceptLanguages) {
        this.acceptLanguages = acceptLanguages;
        return this;
    }

    @Override
    public List<String> accept() {
        return accept;
    }

    public TestHttpRequest setAccept(List<String> accept) {
        this.accept = accept;
        return this;
    }

    @Override
    public List<MediaRange> acceptedTypes() {
        return acceptedTypes;
    }

    public TestHttpRequest setAcceptedTypes(List<MediaRange> acceptedTypes) {
        this.acceptedTypes = acceptedTypes;
        return this;
    }

    @Override
    public boolean accepts(String s) {
        return accepts;
    }

    public TestHttpRequest setAccepts(boolean accepts) {
        this.accepts = accepts;
        return this;
    }

    @Override
    public Map<String, String[]> queryString() {
        return queryString;
    }

    public TestHttpRequest setQueryString(Map<String, String[]> queryString) {
        this.queryString = queryString;
        return this;
    }

    @Override
    public Http.Cookies cookies() {
        return cookies;
    }

    public TestHttpRequest setCookies(Http.Cookies cookies) {
        this.cookies = cookies;
        return this;
    }

    @Override
    public Map<String, String[]> headers() {
        return headers;
    }

    public TestHttpRequest setHeaders(Map<String, String[]> headers) {
        this.headers = headers;
        return this;
    }

    @Override
    public String toString() {
        return toString(this);
    }
    
    public static String toString(Http.Request request) {
        return "Http.Request{" +
                "body=" + request.body() +
                ", uri='" + request.uri() + '\'' +
                ", method='" + request.method() + '\'' +
                ", version='" + request.version() + '\'' +
                ", remoteAddress='" + request.remoteAddress() + '\'' +
                ", secure=" + request.secure() +
                ", host='" + request.host() + '\'' +
                ", path='" + request.path() + '\'' +
                ", acceptLanguages=" + request.acceptLanguages() +
                ", accept=" + request.accept() +
                ", acceptedTypes=" + request.acceptedTypes() +
                ", queryString=" + request.queryString() +
                ", cookies=" + request.cookies() +
                ", headers=" + request.headers() +
                '}';
    }
}
