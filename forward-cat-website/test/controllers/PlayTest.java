package controllers;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import org.junit.After;
import org.junit.Before;
import play.GlobalSettings;
import play.test.FakeApplication;

import java.io.IOException;

import static play.test.Helpers.*;

public abstract class PlayTest {

    private FakeApplication fakeApplication;

    @Before
    public void setUp() throws IOException, InterruptedException {
        final AbstractModule module = getModule();
        GlobalSettings testGlobal = new GlobalSettings() {
            @Override
            public <A> A getControllerInstance(Class<A> controllerClass) throws Exception {
                return Guice.createInjector(module).getInstance(controllerClass);
            }
        };

        fakeApplication = fakeApplication(testGlobal);
        start(fakeApplication);
    }

    public abstract AbstractModule getModule() throws IOException;

    @After
    public void cleanup() {
        stop(fakeApplication);
    }
}
