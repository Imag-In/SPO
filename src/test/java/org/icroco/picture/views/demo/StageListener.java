package org.icroco.picture.views.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;

//@Component
public class StageListener { //implements ApplicationListener<StageReadyEvent> {

    private final String applicationTitle;
    private final Resource fxml;
    private final ApplicationContext applicationContext;

    //    private final ViewLoader viewLoader;
    public StageListener(@Value("${spring.application.ui.title}") String applicationTitle,
                         @Value("classpath:/ui.fxml") Resource fxml, ApplicationContext applicationContext) {//, ViewLoader viewLoader) {
        this.applicationTitle = applicationTitle;
        this.fxml = fxml;
        this.applicationContext = applicationContext;
//        this.viewLoader = viewLoader;
//        this.viewLoader = viewLoader;
    }

//    @Override
//    public void onApplicationEvent(StageReadyEvent stageReadyEvent) {
//
//        try {
//            Stage stage = stageReadyEvent.getStage();
//            URL url = fxml.getURL();
//            FXMLLoader fxmlLoader = new FXMLLoader(url);
//            fxmlLoader.setControllerFactory(applicationContext::getBean);
//            Parent root = fxmlLoader.load();
//            Scene scene = new Scene(root, 600, 600);
//            stage.setScene(scene);
//            stage.setTitle(this.applicationTitle);
//            stage.show();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }
}