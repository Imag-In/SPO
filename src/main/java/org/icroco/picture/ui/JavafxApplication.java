package org.icroco.picture.ui;

import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.controlsfx.dialog.ExceptionDialog;
import org.icroco.javafx.StageReadyEvent;
import org.icroco.javafx.ViewManager;
import org.icroco.picture.ui.util.Error;
import org.icroco.picture.ui.util.Nodes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.GenericApplicationContext;

@Slf4j
public class JavafxApplication extends Application {

    private ConfigurableApplicationContext context;



    @Override
    public void init() throws Exception {
        log.info("Init ...");
        ApplicationContextInitializer<GenericApplicationContext> initializer = genericApplicationContext -> {
            genericApplicationContext.registerBean(Application.class, () -> JavafxApplication.this);
            genericApplicationContext.registerBean(Parameters.class, this::getParameters);
            genericApplicationContext.registerBean(HostServices.class, this::getHostServices);
//            genericApplicationContext.registerBean(ApplicationEventMulticaster.class, this::simpleApplicationEventMulticaster);
        };

        this.context = new SpringApplicationBuilder()
                .sources(PictureApplication.class)
                .bannerMode(Banner.Mode.OFF)
                .initializers(initializer)
                .build()
                .run(getParameters().getRaw().toArray(new String[0]));
//        this.context.getBeanFactory().addBeanPostProcessor(new BeanPostProcessor() {
//            @Override
//            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
//                if (bean.getClass().getName().startsWith("org.icroco")) {
//                    log.info("Post Process: {}", bean.getClass().getName());
//                }
//                return bean;
//            }
//        });
    }

    private ApplicationEventMulticaster simpleApplicationEventMulticaster() {
        var eventBus = new SimpleApplicationEventMulticaster();

        eventBus.setTaskExecutor(Platform::runLater);

        return eventBus;
    }

    @Override
    public void start(Stage primaryStage) {
        Thread.setDefaultUncaughtExceptionHandler(this::showError);
        this.context.publishEvent(new StageReadyEvent(primaryStage));
    }

    private void showError(Thread thread, Throwable throwable) {
        log.error("An unexpected error occurred: ", throwable);
        if (Platform.isFxApplicationThread()) {
            showErrorDialog(throwable);
        }
    }

    private void showErrorDialog(Throwable throwable) {
        Throwable       t   = Error.findOwnedException(throwable);
        ExceptionDialog dlg = new ExceptionDialog(t);
        Nodes.showDialog(dlg);
    }

    @Override
    public void stop() throws Exception {
        this.context.close();
        Platform.exit();
    }

}