package models;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import play.GlobalSettings;
import play.i18n.Lang;
import play.test.FakeApplication;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static play.test.Helpers.*;

public class DomainManagerTest {

    Lang EN = new Lang(Lang.get("en").get());
    Lang IT = new Lang(Lang.get("it").get());
    Lang ES = new Lang(Lang.get("es").get());

    FakeApplication fakeApplication;

    @Before
    public void setup() {
        fakeApplication = fakeApplication(inMemoryDatabase(), new GlobalSettings());
        start(fakeApplication);
    }

    @After
    public void cleanup() {
        stop(fakeApplication);
    }

    @Test
    public void buildUrl_defaultLanguage_doesntSetLanguageInPath() throws Exception {
        String path = DomainManager.buildUrl(EN, "/my-path");
        assertThat(path, is("/my-path"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildUrl_unknownLanguage_throwsException() throws Exception {
        DomainManager.buildUrl(IT, "/my-path");
    }

    @Test
    public void buildUrl_notDefaultLanguage_languageIsSetInPath() throws Exception {
        String path = DomainManager.buildUrl(ES, "/my-path");
        assertThat(path, is("/es/my-path"));
    }
}
