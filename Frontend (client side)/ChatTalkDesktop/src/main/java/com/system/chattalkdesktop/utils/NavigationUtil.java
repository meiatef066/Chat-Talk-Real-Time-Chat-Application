package com.system.chattalkdesktop.utils;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

public class NavigationUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(NavigationUtil.class);
    private static final double FADE_DURATION_MS = 300.0;
    private static final double SCALE_DURATION_MS = 300.0;
    private static final double SCALE_FROM = 0.95;
    private static final double SCALE_TO = 1.0;

    /**
     * Switches to a new scene with a modern fade + scale transition.
     *
     * @param event       The event triggering the navigation
     * @param fxmlPath    Path to the FXML file
     * @param windowTitle The title of the new window
     */
    public static void switchScene(Event event, String fxmlPath, String windowTitle) {
        switchScene(event, fxmlPath, windowTitle, null);
    }

    /**
     * Switches to a new scene with a modern fade + scale transition, allowing controller initialization.
     *
     * @param event               The event triggering the navigation
     * @param fxmlPath            Path to the FXML file
     * @param windowTitle         The title of the new window
     * @param controllerInitializer Optional consumer to initialize the controller
     * @param <T>                 The controller type
     */
    public static <T> void switchScene(Event event, String fxmlPath, String windowTitle, Consumer<T> controllerInitializer) {
        Platform.runLater(() -> {
            try {
                final Stage stage = getStageFromEvent(event);
                if (stage == null) {
                    LOGGER.error("Cannot switch scene: Stage is null for event {}", event);
                    return;
                }

                final FXMLLoader loader = new FXMLLoader(NavigationUtil.class.getResource(fxmlPath));
                final Parent newRoot = loader.load();
                if (controllerInitializer != null) {
                    T controller = loader.getController();
                    controllerInitializer.accept(controller);
                }

                final Scene currentScene = stage.getScene();
                if (currentScene == null) {
                    Scene newScene = new Scene(newRoot);
                    stage.setScene(newScene);
                    stage.setTitle(windowTitle);
                    stage.show();
                    applyFadeInTransition(newRoot);
                    return;
                }

                FadeTransition fadeOut = createFadeTransition(currentScene.getRoot(), 1.0, 0.0);
                ScaleTransition scaleOut = createScaleTransition(currentScene.getRoot(), 1.0, SCALE_FROM);
                ParallelTransition outTransition = new ParallelTransition(fadeOut, scaleOut);

                outTransition.setOnFinished(e -> {
                    Scene newScene = new Scene(newRoot);
                    stage.setScene(newScene);
                    stage.setTitle(windowTitle);
                    applyFadeInTransition(newRoot);
                });

                outTransition.play();
            } catch (IOException ex) {
                LOGGER.error("Failed to load FXML file {}: {}", fxmlPath, ex.getMessage(), ex);
            } catch (Exception ex) {
                LOGGER.error("Error during scene switch to {}: {}", fxmlPath, ex.getMessage(), ex);
            }
        });
    }

    /**
     * Creates a fade transition for a node.
     *
     * @param node      The node to animate
     * @param fromValue Starting opacity
     * @param toValue   Ending opacity
     * @return The configured FadeTransition
     */
    private static FadeTransition createFadeTransition(Node node, double fromValue, double toValue) {
        FadeTransition ft = new FadeTransition(Duration.millis(FADE_DURATION_MS), node);
        ft.setFromValue(fromValue);
        ft.setToValue(toValue);
        ft.setCycleCount(1);
        ft.setAutoReverse(false);
        return ft;
    }

    /**
     * Creates a scale transition for a node.
     *
     * @param node      The node to animate
     * @param fromScale Starting scale
     * @param toScale   Ending scale
     * @return The configured ScaleTransition
     */
    private static ScaleTransition createScaleTransition(Node node, double fromScale, double toScale) {
        ScaleTransition st = new ScaleTransition(Duration.millis(SCALE_DURATION_MS), node);
        st.setFromX(fromScale);
        st.setFromY(fromScale);
        st.setToX(toScale);
        st.setToY(toScale);
        st.setCycleCount(1);
        st.setAutoReverse(false);
        return st;
    }

    /**
     * Applies a fade-in + scale-in transition to a node.
     *
     * @param node The node to animate
     */
    private static void applyFadeInTransition(Node node) {
        node.setOpacity(0.0);
        node.setScaleX(SCALE_FROM);
        node.setScaleY(SCALE_FROM);
        FadeTransition fadeIn = createFadeTransition(node, 0.0, 1.0);
        ScaleTransition scaleIn = createScaleTransition(node, SCALE_FROM, SCALE_TO);
        ParallelTransition transition = new ParallelTransition(fadeIn, scaleIn);
        transition.play();
    }

    /**
     * Extracts the Stage from an event.
     *
     * @param event The JavaFX event
     * @return The Stage, or null if not found
     */
    private static Stage getStageFromEvent(Event event) {
        if (event.getSource() instanceof Node node) {
            return (Stage) node.getScene().getWindow();
        }
        LOGGER.warn("Event source is not a Node: {}", event.getSource());
        return null;
    }
}