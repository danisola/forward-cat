package controllers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import org.junit.Test;
import play.mvc.Http;
import play.mvc.Result;
import play.test.FakeRequest;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static controllers.ContainsHtmlLang.containsHtmlLang;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static play.mvc.Http.Status.MOVED_PERMANENTLY;
import static play.test.Helpers.*;

public class RedirectActionTest extends PlayTest {

    @Test
    public void noLangIsDefined_contentShouldBeInEnglish() throws Exception {
        List<String> pages = Lists.newArrayList("/", "/report");

        for (String test : pages) {
            Result result = route(request("forward.cat", test + "?param1=a"));
            assertThat(status(result), is(OK));
            assertThat(contentAsString(result), containsHtmlLang("en"));
        }
    }

    @Test
    public void enInPath_contentShouldBeInEnglish() throws Exception {
        List<String> pages = Lists.newArrayList("/en/", "/en/report");

        for (String test : pages) {
            Result result = route(request("forward.cat", test + "?param1=a"));
            assertThat(status(result), is(NOT_FOUND));
        }
    }

    @Test
    public void langInPath_contentShouldBeInTheGivenLang() throws Exception {
        Map<String, String> tests = ImmutableMap.<String, String>builder().
                put("/es/", "es").
                put("/ca/", "ca").
                put("/es/report", "es").
                put("/ca/report", "ca").
                build();

        for (Entry<String, String> entry : tests.entrySet()) {
            String path = entry.getKey() + "?param1=a";
            Result result = route(request("forward.cat", path));

            assertThat(status(result), is(OK));
            assertThat(contentAsString(result), containsHtmlLang(entry.getValue()));
        }
    }

    @Test
    public void incorrectPathStartsWithLang_shouldNotMatchAnyController() throws Exception {
        Result result = route(request("forward.cat", "/calculator?param1=a"));
        assertThat(result, is(nullValue()));
    }

    @Test
    public void langOrWwwInSubdomain_requestShouldBeRedirected() throws Exception {
        Map<String, String> tests = ImmutableMap.<String, String>builder().
                put("www.forward.cat", "forward.cat").
                put("en.forward.cat", "forward.cat").
                put("es.forward.cat", "forward.cat/es").
                put("ca.forward.cat", "forward.cat/ca").
                build();

        for (Entry<String, String> entry : tests.entrySet()) {
            Result result = route(request(entry.getKey(), "/?param1=a&param2=b"));
            assertThat(status(result), is(MOVED_PERMANENTLY));

            String expectedRedirect = "http://" + entry.getValue() + "/?param1=a&param2=b";
            assertThat(location(result), is(expectedRedirect));
        }
    }

    private FakeRequest request(String host, String path) {
        return fakeRequest(GET, path)
                .withHeader(Http.HeaderNames.HOST, host);
    }

    private String location(Result result) {
        return result.toScala().header().headers().apply(LOCATION);
    }

    @Override
    public AbstractModule getModule() throws IOException {
        return new AbstractModule() {
            @Override
            protected void configure() {
                // Nothing to do
            }
        };
    }
}
