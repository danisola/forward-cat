import alerts.SendAlertJob;
import com.google.common.base.Throwables;
import com.google.inject.Guice;
import com.google.inject.Injector;
import models.Options;
import org.quartz.*;
import play.Application;
import play.GlobalSettings;

public class Global extends GlobalSettings {

    private static final String JOB_NAME = "sendAlertJob";
    private static final Injector INJECTOR = Guice.createInjector(new Module());
    private Scheduler alertScheduler;
    private Options options;

    @Override
    public void onStart(Application application) {
        alertScheduler = INJECTOR.getInstance(Scheduler.class);
        options = INJECTOR.getInstance(Options.class);

        scheduleAlerts();
    }

    @Override
    public void onStop(Application application) {
        try {
            alertScheduler.shutdown();
        } catch (SchedulerException e) {
            Throwables.propagate(e);
        }
    }

    @Override
    public <A> A getControllerInstance(Class<A> controllerClass) throws Exception {
        return INJECTOR.getInstance(controllerClass);
    }

    private void scheduleAlerts() {
        JobDetail jobDetail = JobBuilder.newJob(SendAlertJob.class)
                .withIdentity(new JobKey(JOB_NAME))
                .build();

        SimpleScheduleBuilder schedule = SimpleScheduleBuilder
                .repeatMinutelyForever(options.getMinutesBetweenAlerts());

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(JOB_NAME)
                .startNow()
                .withSchedule(schedule)
                .build();

        try {
            alertScheduler.scheduleJob(jobDetail, trigger);
        } catch (SchedulerException e) {
            throw Throwables.propagate(e);
        }
    }
}
