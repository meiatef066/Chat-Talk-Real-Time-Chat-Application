package com.system.chattalkdesktop.SearchService;

import com.system.chattalkdesktop.SearchService.ApiSearchUsers;
import com.system.chattalkdesktop.SearchService.SearchUserResultDTO;
import com.system.chattalkdesktop.service.PerformanceOptimizationService;
import com.system.chattalkdesktop.utils.NavigationUtil;
import com.system.chattalkdesktop.utils.SessionManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class SearchController {
    @FXML
    public Label headerLabel;
    @FXML
    public TextField contactSearchField;
    @FXML
    public ScrollPane scrollPane;
    @FXML
    public VBox searchResultsVBox;
    @FXML
    public VBox emptyState;
    @FXML
    public Label noResultsLabel;
    @FXML
    public ProgressIndicator loadingIndicator;
    @FXML
    public Button backButton;
    private List<SearchUserResultDTO> lastLoadedUsers = new ArrayList<>();

    private Thread currentSearchThread;
    private int currentPage = 0;
    private int totalPages = 1;
    private final int pageSize = 20;
    private Button loadMoreButton;

    @FXML
    public void searchUsers(KeyEvent keyEvent) {
        String query = contactSearchField.getText().trim();
        if (query.isEmpty()) {
            searchResultsVBox.getChildren().clear();
            emptyState.setVisible(false);
            emptyState.setManaged(false);
            loadingIndicator.setVisible(false);
            loadingIndicator.setManaged(false);
            return;
        }
        // Show loading indicator prominently
        loadingIndicator.setVisible(true);
        loadingIndicator.setManaged(true);
        searchResultsVBox.getChildren().setAll(loadingIndicator);
        emptyState.setVisible(false);
        emptyState.setManaged(false);

        Long currentUserId = SessionManager.getInstance().getCurrentUser() != null
                ? SessionManager.getInstance().getCurrentUser().getId()
                : null;
        if (currentUserId == null) {
            loadingIndicator.setVisible(false);
            loadingIndicator.setManaged(false);
            noResultsLabel.setText("User not logged in.");
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            return;
        }

        currentPage = 0;
        totalPages = 1;

        Task<ApiSearchUsers.PagedSearchResults> searchTask = ApiSearchUsers.searchUsers(query, currentUserId, currentPage, pageSize);
        searchTask.setOnSucceeded(e -> {
            ApiSearchUsers.PagedSearchResults pageResult = searchTask.getValue();
            System.out.println("Search succeeded. PageResult: " + pageResult);
            List<SearchUserResultDTO> users = pageResult != null ? pageResult.getResults() : null;
            System.out.println("Users found: " + (users != null ? users.size() : "null"));
            lastLoadedUsers = users != null ? users : new ArrayList<>();
            currentPage = pageResult != null ? pageResult.getPage() : 0;
            totalPages = pageResult != null ? pageResult.getTotalPages() : 1;

            loadingIndicator.setVisible(false);
            loadingIndicator.setManaged(false);
            searchResultsVBox.getChildren().clear();

            if (users == null || users.isEmpty()) {
                emptyState.setVisible(true);
                emptyState.setManaged(true);
            } else {
                emptyState.setVisible(false);
                emptyState.setManaged(false);
                for (SearchUserResultDTO user : users) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/system/chattalkdesktop/SearchPage/AddUserItem.fxml"));
                        Node node = loader.load();
                        SearchItemController controller = loader.getController();
                        controller.setUserData(user);
                        searchResultsVBox.getChildren().add(node);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                ensureLoadMoreButton(currentUserId, query);
            }
        });
        searchTask.setOnFailed(e -> {
            System.out.println("Search failed with exception: " + searchTask.getException());
            loadingIndicator.setVisible(false);
            loadingIndicator.setManaged(false);
            searchResultsVBox.getChildren().clear();
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            noResultsLabel.setText("Error loading users.");
        });

        // Cancel previous prev search by interrupting its thread ane create new one
        if (currentSearchThread != null && currentSearchThread.isAlive()) {
            currentSearchThread.interrupt();
        }
        currentSearchThread = new Thread(searchTask, "search-users-thread");
        currentSearchThread.setDaemon(true);
        currentSearchThread.start();
    }

    @FXML
    public void navigateBack(ActionEvent actionEvent) {
        // Implement navigation logic as needed
        NavigationUtil.switchScene(actionEvent,"/com/system/chattalkdesktop/MainChat/ChatApp.fxml","chat 💌🎶");
    }

    private void ensureLoadMoreButton(Long currentUserId, String query) {
        if (loadMoreButton == null) {
            loadMoreButton = new Button("Load more");
            loadMoreButton.getStyleClass().add("load-more-button");
            loadMoreButton.setOnAction(evt -> loadNextPage(currentUserId, query));
        }
        if (currentPage < totalPages - 1) {
            if (!searchResultsVBox.getChildren().contains(loadMoreButton)) {
                searchResultsVBox.getChildren().add(loadMoreButton);
            }
        } else {
            searchResultsVBox.getChildren().remove(loadMoreButton);
        }
    }

    private void loadNextPage(Long currentUserId, String query) {
        if (currentPage >= totalPages - 1) {
            return;
        }
        int nextPage = currentPage + 1;
        loadMoreButton.setDisable(true);

        Task<ApiSearchUsers.PagedSearchResults> searchTask = ApiSearchUsers.searchUsers(query, currentUserId, nextPage, pageSize);
        searchTask.setOnSucceeded(e -> {
            ApiSearchUsers.PagedSearchResults pageResult = searchTask.getValue();
            List<SearchUserResultDTO> users = pageResult != null ? pageResult.getResults() : null;
            if (users != null && !users.isEmpty()) {
                lastLoadedUsers.addAll(users);
                for (SearchUserResultDTO user : users) {


                    try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/system/chattalkdesktop/SearchPage/AddUserItem.fxml"));
                    Node node = loader.load();
                    SearchItemController controller = loader.getController();
                    controller.setUserData(user);
                    searchResultsVBox.getChildren().add(searchResultsVBox.getChildren().size() - 0, node);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                }
            }
            currentPage = pageResult != null ? pageResult.getPage() : currentPage;
            totalPages = pageResult != null ? pageResult.getTotalPages() : totalPages;
            loadMoreButton.setDisable(false);
            ensureLoadMoreButton(currentUserId, query);
        });
        searchTask.setOnFailed(e -> {
            loadMoreButton.setDisable(false);
        });

        new Thread(searchTask, "search-users-next-page").start();
    }
}
