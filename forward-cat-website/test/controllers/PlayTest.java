package controllers;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import org.junit.After;
import org.junit.Before;
import play.GlobalSettings;
import play.test.FakeApplication;
import play.test.Helpers;

import static play.test.Helpers.*;

public abstract class PlayTest {

    private FakeApplication fakeApplication;

    @Before
    public void setUp() throws Exception {
        final AbstractModule module = getModule();
        GlobalSettings testGlobal = new GlobalSettings() {
            @Override
            public <A> A getControllerInstance(Class<A> controllerClass) throws Exception {
                return Guice.createInjector(module).getInstance(controllerClass);
            }
        };

        fakeApplication = fakeApplication(Helpers.inMemoryDatabase(), testGlobal);
        start(fakeApplication);
    }

    public abstract AbstractModule getModule() throws Exception;

    @After
    public void cleanup() {
        stop(fakeApplication);
    }
}
