package controllers;

import com.google.inject.AbstractModule;
import org.junit.Test;
import play.mvc.Result;
import play.test.FakeRequest;

import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static play.test.Helpers.*;

public class LandingTest extends PlayTest {
    @Override
    public AbstractModule getModule() throws IOException {
        return new AbstractModule() {
            @Override
            protected void configure() {
                // Nothing to do
            }
        };
    }

    @Test
    public void everythingFine_sendLandingPage() throws Exception {
        Result route = route(request("/"));
        assertThat(status(route), is(OK));
    }

    private FakeRequest request(String path) {
        return fakeRequest(GET, path);
    }
}
