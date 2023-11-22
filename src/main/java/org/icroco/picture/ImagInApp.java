package org.icroco.picture;

import atlantafx.base.theme.PrimerLight;
import atlantafx.base.util.Animations;
import jakarta.persistence.EntityManagerFactory;
import javafx.application.*;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import lombok.extern.slf4j.Slf4j;
import net.codecrete.usb.USBDevice;
import org.controlsfx.dialog.ExceptionDialog;
import org.icroco.picture.splashscreen.LoaderProgressNotification;
import org.icroco.picture.splashscreen.SpoPreLoader;
import org.icroco.picture.util.Error;
import org.icroco.picture.util.Resources;
import org.icroco.picture.views.MainView;
import org.icroco.picture.views.pref.UserPreference;
import org.icroco.picture.views.pref.UserPreferenceService;
import org.icroco.picture.views.util.ImageUtils;
import org.icroco.picture.views.util.Nodes;
import org.scenicview.ScenicView;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.EnableAsync;

import java.awt.*;


@SpringBootApplication()
@EnableAsync(proxyTargetClass = true)
//@ImportAutoConfiguration(classes = ViewAutoConfiguration.class)
@Slf4j
public class ImagInApp extends Application {
    public static final String IMAGES_128_PX_GNOME_PHOTOS_LOGO_2019_SVG_PNG = "/images/128px-GNOME_Photos_logo_2019.svg.png";
    // Application startup analysis: https://www.amitph.com/spring-boot-startup-monitoring/#applicationstartup_metrics_with_java_flight_recorder
    // Icon IRes: https://dlsc.com/2017/08/29/javafx-tip-27-hires-retina-icons/

    @Autowired
    UserPreferenceService userPref;

    @Autowired
    MainView mainView;

    /**
     * The application context created by the JavaFX starter.
     * This context will only be available once the {@link #init()} has been invoked by JavaFX.
     */
    protected ConfigurableApplicationContext applicationContext;

    /**
     * Launch a JavaFX application with for the given class and program arguments.
     *
     * @param appClass The class to launch the JavaFX application for.
     * @param args     The program arguments.
     */
    @SuppressWarnings("unused")
    public static void launch(Class<? extends Application> appClass, String... args) {
        System.setProperty("javafx.preloader", SpoPreLoader.class.getName());
        Application.launch(appClass, args);
    }

    @SuppressWarnings("unused")
    public static void launch(Class<? extends Application> appClass, Class<? extends Preloader> preloaderClass, String... args) {
        System.setProperty("javafx.preloader", preloaderClass.getName());
        Application.launch(appClass, args);
    }

    @SuppressWarnings("unused")
    public static void launch(String... args) {
        System.setProperty("javafx.preloader", SpoPreLoader.class.getName());
        Application.launch(args);
    }

    @Override
    public final void init() {
        try {
            log.debug("Init: {}", getClass().getSimpleName());
            notifyPreloader(new LoaderProgressNotification(.1, "Starting ..."));
            ApplicationContextInitializer<GenericApplicationContext> initializer = genericApplicationContext -> {
                genericApplicationContext.registerBean(Application.class, () -> ImagInApp.this);
                genericApplicationContext.registerBean(Parameters.class, this::getParameters);
                genericApplicationContext.registerBean(HostServices.class, this::getHostServices);
                genericApplicationContext.registerBean(ProgressBeanPostProcessor.class, ProgressBeanPostProcessor::new);
            };
            applicationContext = new SpringApplicationBuilder().sources(getClass())
                                                               .bannerMode(Banner.Mode.OFF)
                                                               .headless(false)
                                                               .initializers(initializer)
                                                               .build()
                                                               .run(getParameters().getRaw().toArray(new String[0]));

        } catch (Exception ex) {
            log.error("Unexpected error while init", ex);
            throw ex;
        }
    }


    @Override
    public final void start(Stage primaryStage) {
        try {
            log.debug("Start: {}", getClass().getSimpleName());
            Thread.setDefaultUncaughtExceptionHandler(this::showError);
            preStart(primaryStage);
            primaryStage.setOnCloseRequest(this::closeRequest);
//            Platform.runLater(() -> applicationContext.publishEvent(new StageReadyEvent(primaryStage)));

            primaryStage.getScene().getRoot().setOpacity(0);
            primaryStage.show();
            Animations.fadeIn(primaryStage.getScene().getRoot(), Duration.millis(1000)).playFromStart();
//            Platform.runLater(primaryStage::show);
//            USB.setOnDeviceConnected((device) -> printDetails(device, "Connected"));
//            USB.setOnDeviceDisconnected((device) -> printDetails(device, "Disconnected"));
        } catch (Exception ex) {
            log.error("Unexpected error while starting", ex);
        }
    }

    private static void printDetails(USBDevice device, String event) {
        log.info("{}: {}", event, device.toString());
    }

    @Override
    public final void stop() throws Exception {
        applicationContext.close();
        Thread.sleep(100);
        System.exit(0);
    }

    private void showError(Thread thread, Throwable throwable) {
        log.error("An unexpected error occurred in thread: {}, ", thread, throwable);
        if (Platform.isFxApplicationThread()) {
            showErrorToUser(throwable);
        }
    }

    protected void preStart(final Stage primaryStage) {
//        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Image'In");
//        System.setProperty("apple.laf.useScreenMenuBar", "true");
//        OpenCV.loadShared();
        ImageUtils.readImageIoCodec();
        primaryStage.getIcons().addAll(SpoPreLoader.getIcons());
        if (Taskbar.isTaskbarSupported()) {
            var taskbar = Taskbar.getTaskbar();
            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                final Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
                var           dockIcon       = defaultToolkit.getImage(getClass().getResource("/images/spo-256x256.png"));
                taskbar.setIconImage(dockIcon);
            }
        }
        primaryStage.setTitle("Imag'In");
//        Application.setUserAgentStylesheet(new NordDark().getUserAgentStylesheet());
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        var antialiasing = Platform.isSupported(ConditionalFeature.SCENE3D)
                           ? SceneAntialiasing.BALANCED
                           : SceneAntialiasing.DISABLED;
        var scene = new Scene(mainView.getRootContent(), 1200, 800, false, antialiasing);
        Nodes.setStageSizeAndPos(primaryStage,
                                 userPref.getUserPreference().getMainWindow().getPosX(),
                                 userPref.getUserPreference().getMainWindow().getPosY(),
                                 userPref.getUserPreference().getMainWindow().getWidth(),
                                 userPref.getUserPreference().getMainWindow().getHeight());
        primaryStage.setScene(scene);

        scene.getStylesheets().addAll(Resources.resolve("/styles/index.css"));
        if (Boolean.getBoolean("SCENIC")) {
            ScenicView.show(scene);
        }
    }

    protected void showErrorToUser(final Throwable throwable) {
        Throwable       t   = Error.findOwnedException(throwable);
        ExceptionDialog dlg = new ExceptionDialog(t);
        Nodes.showDialog(dlg);
    }

    protected void closeRequest(final WindowEvent windowEvent) {
        if (windowEvent.getTarget() instanceof Stage stage) {
            userPref.getUserPreference().getMainWindow().setPosX(stage.getX());
            userPref.getUserPreference().getMainWindow().setPosY(stage.getY());
            userPref.getUserPreference().getMainWindow().setWidth(stage.getWidth());
            userPref.getUserPreference().getMainWindow().setHeight(stage.getHeight());
        }
    }

    public static void main(String[] args) {
        launch(ImagInApp.class, args);
    }

    class ProgressBeanPostProcessor implements BeanPostProcessor {
        @Override
        public Object postProcessAfterInitialization(@NonNull Object bean, String beanName) throws BeansException {
            if (bean instanceof UserPreference up) {
                notifyPreloader(new LoaderProgressNotification(0.2, "User preferences loaded"));
            } else if (bean instanceof EntityManagerFactory) {
                notifyPreloader(new LoaderProgressNotification(.2, "Database loaded"));
            } else if (beanName.equals("cacheAutoConfigurationValidator")) {
                notifyPreloader(new LoaderProgressNotification(.8, "Cache loaded"));
            } else if (beanName.equals("viewResolver")) {
                notifyPreloader(new LoaderProgressNotification(.4, "Views loaded"));
            } else {
//                log.info("Loaded: {}", beanName);
            }

            return bean;
        }
    }
}
