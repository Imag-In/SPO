package org.icroco.picture;

import jakarta.persistence.EntityManagerFactory;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.application.Preloader;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import net.codecrete.usb.UsbDevice;
import org.icroco.picture.infra.github.GitHubProperties;
import org.icroco.picture.persistence.MediaFileRepository;
import org.icroco.picture.persistence.model.MediaFileEntity;
import org.icroco.picture.splashscreen.LoaderProgressNotification;
import org.icroco.picture.splashscreen.SpoPreLoader;
import org.icroco.picture.util.Env;
import org.icroco.picture.util.StageReadyEvent;
import org.icroco.picture.views.MainView;
import org.icroco.picture.views.pref.UserPreference;
import org.icroco.picture.views.pref.UserPreferenceService;
import org.icroco.picture.views.theme.ThemeRepository;
import org.icroco.picture.views.util.FxPlatformExecutor;
import org.icroco.picture.views.util.ImageUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.awt.*;
import java.io.File;
import java.util.Optional;
import java.util.Properties;

@SpringBootApplication
//@ComponentScan(excludeFilters = { @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
//                                  @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class),
//                                  @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*ParentConfig")})
//@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*ParentConfig"))
//@Configuration
//@ComponentScan()
//@Import({ PropertyPlaceholderAutoConfiguration.class,
//          TaskExecutionAutoConfiguration.class,
//          TransactionAutoConfiguration.class,
//          DataSourceAutoConfiguration.class,
//          DataSourceTransactionManagerAutoConfiguration.class,
//          JdbcClientAutoConfiguration.class,
//          JdbcTemplateAutoConfiguration.class,
//          JpaBaseConfiguration.class,
//          JacksonAutoConfiguration.class,
//          CacheAutoConfiguration.class,
//          HibernateJpaAutoConfiguration.class,
//          JpaRepositoriesAutoConfiguration.class,
//          ProjectInfoAutoConfiguration.class,
//          RestClientAutoConfiguration.class})
@EnableJpaRepositories(basePackageClasses = MediaFileRepository.class)
@EntityScan(basePackageClasses = MediaFileEntity.class)
@EnableAsync(proxyTargetClass = true)
@EnableScheduling
@EnableConfigurationProperties(value = { GitHubProperties.class })
//@ImportAutoConfiguration(classes = ViewAutoConfiguration.class)
@Slf4j
public class ImagInApp extends Application {

    public static final String CONF_HOME = System.getProperty("imagin.spo.home", STR.".imagin\{File.separatorChar}spo");

    public static final String IMAGES_128_PX_GNOME_PHOTOS_LOGO_2019_SVG_PNG = "/images/128px-GNOME_Photos_logo_2019.svg.png";
    // Application startup analysis: https://www.amitph.com/spring-boot-startup-monitoring/#applicationstartup_metrics_with_java_flight_recorder
    // Icon IRes: https://dlsc.com/2017/08/29/javafx-tip-27-hires-retina-icons/


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
            var profiles = Optional.ofNullable(System.getenv("SPO_PROFILE"))
                                   .map(p -> p.split(","))
                                   .orElseGet(() -> new String[] { "default" });
            notifyPreloader(new LoaderProgressNotification(.1, "Starting ..."));
            ApplicationContextInitializer<GenericApplicationContext> initializer = genericApplicationContext -> {
                var env             = new Env(genericApplicationContext.getEnvironment());
                var themeRepository = new ThemeRepository(env);
                var pref            = new UserPreferenceService(themeRepository);
                genericApplicationContext.registerBean(Application.class, () -> ImagInApp.this);
                genericApplicationContext.registerBean(Parameters.class, this::getParameters);
                genericApplicationContext.registerBean(HostServices.class, this::getHostServices);
                genericApplicationContext.registerBean(ProgressBeanPostProcessor.class, ProgressBeanPostProcessor::new);
                genericApplicationContext.registerBean(Env.class, () -> env);
                genericApplicationContext.registerBean(ThemeRepository.class, () -> themeRepository);
                genericApplicationContext.registerBean(UserPreferenceService.class, () -> pref);
                Properties props = new Properties();
                props.put("SPO_DB_PATH", CONF_HOME);
                props.put("SPO_DB_NAME", pref.getUserPreference().getCatalogName());
                genericApplicationContext.getEnvironment()
                                         .getPropertySources()
                                         .addFirst(new PropertiesPropertySource("DB", props));
            };
            applicationContext = new SpringApplicationBuilder().sources(ImagInApp.class)
                                                               .bannerMode(Banner.Mode.OFF)
                                                               .headless(false)
                                                               .initializers(initializer)
                                                               .profiles(profiles)
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
            applicationContext.publishEvent(new StageReadyEvent(primaryStage));

//            Platform.runLater(primaryStage::show);
//            Usb.setOnDeviceConnected((device) -> printDetails(device, "Connected"));
//            Usb.setOnDeviceDisconnected((device) -> printDetails(device, "Disconnected"));
        } catch (Exception ex) {
            log.error("Unexpected error while starting", ex);
        }
    }

    private static void printDetails(UsbDevice device, String event) {
        log.info("USB device, {}: {}", event, device.toString());
    }

    @Override
    public final void stop() throws Exception {
        applicationContext.close();
        Thread.sleep(100);
        Platform.exit();
        System.exit(0);
    }

    private void showError(Thread thread, Throwable throwable) {
        FxPlatformExecutor.fxRun(() -> applicationContext.getBean(MainView.class).showErrorToUser(thread, throwable));
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
    }

    public static void main(String[] args) {
        launch(ImagInApp.class, args);
    }

    class ProgressBeanPostProcessor implements BeanPostProcessor {
        @Override
        public Object postProcessAfterInitialization(@NonNull Object bean, String beanName) throws BeansException {
            if (bean instanceof UserPreference up) {
                notifyPreloader(new LoaderProgressNotification(.2, "User preferences loaded"));
            } else if (bean instanceof EntityManagerFactory) {
                notifyPreloader(new LoaderProgressNotification(.3, "Database loaded"));
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
