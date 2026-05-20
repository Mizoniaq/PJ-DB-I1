package com.project.artconnect.ui;

import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.application.Platform;

public class MainController {
    @FXML private TabPane mainTabPane;
    @FXML private ArtworkController artworksTabController;

    // Artworks tab is at index 2 (Discover=0, Artists=1, Artworks=2)
    private static final int ARTWORKS_TAB_INDEX = 2;

    @FXML
    public void initialize() {
        mainTabPane.getSelectionModel().selectedIndexProperty().addListener((obs, oldIdx, newIdx) -> {
            if (newIdx.intValue() == ARTWORKS_TAB_INDEX && artworksTabController != null) {
                artworksTabController.refreshTable();
            }
        });
    }

    @FXML
    private void handleExit() {
        Platform.exit();
    }
}
