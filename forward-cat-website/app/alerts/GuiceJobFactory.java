package alerts;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

/**
 * {@link JobFactory} that uses Guice, based on
 * <a href="http://www.codesmell.org/blog/2009/01/quartz-fits/">http://www.codesmell.org/blog/2009/01/quartz-fits/</a>
 */
@Singleton
public class GuiceJobFactory implements JobFactory {

    private final Injector injector;

    @Inject
    public GuiceJobFactory(Injector injector) {
        this.injector = injector;
    }

    @Override
    public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
        Class<? extends Job> jobClass = bundle.getJobDetail().getJobClass();
        return injector.getInstance(jobClass);
    }
}