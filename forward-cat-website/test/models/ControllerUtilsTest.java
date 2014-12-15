package models;

import org.apache.mailet.MailAddress;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import play.i18n.Lang;
import play.mvc.Http;
import play.test.FakeApplication;

import java.util.Optional;

import static models.ControllerUtils.getBestLanguage;
import static models.ControllerUtils.toMailAddress;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static play.test.Helpers.*;

@RunWith(MockitoJUnitRunner.class)
public class ControllerUtilsTest  {

    Lang EN = new Lang(Lang.get("en").get());
    FakeApplication fakeApplication;
    @Mock Http.Request request;

    @Before
    public void setup() {
        fakeApplication = fakeApplication();
        start(fakeApplication);
    }

    @After
    public void cleanup() {
        stop(fakeApplication);
    }

    @Test
    public void getParamValueAsMailAddress_invalidMail_returnsNull() throws Exception {
        Optional<MailAddress> email = toMailAddress("not a mail");
        assertThat(email, is(Optional.empty()));
    }

    @Test
    public void getParamValueAsMailAddress_correctParam_returnsMailAddress() throws Exception {
        Optional<MailAddress> email = toMailAddress("my@mail.com");
        assertThat(email, is(not(Optional.empty())));
    }

    @Test
    public void unknownHost_shouldReturnDefaultLang() {
        Http.Request request = setRequest("zz.forward.cat");
        Lang language = getBestLanguage(request, EN);
        assertThat(language.code(), is("en"));
    }

    @Test
    public void knownHost_shouldReturnItsLang() {
        Http.Request request = setRequest("es.forward.cat");
        Lang language = getBestLanguage(request, EN);
        assertThat(language.code(), is("es"));
    }

    private Http.Request setRequest(String host) {
        when(request.host()).thenReturn(host);
        return request;
    }
}
