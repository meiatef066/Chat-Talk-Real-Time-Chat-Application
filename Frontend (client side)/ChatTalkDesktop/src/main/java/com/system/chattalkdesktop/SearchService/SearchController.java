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

/**
 * Optimized search controller with proper thread management and debouncing
 */
@Slf4j
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

    // Thread-safe data management
    private final List<SearchUserResultDTO> lastLoadedUsers = new ArrayList<>();
    private final AtomicReference<Task<ApiSearchUsers.PagedSearchResults>> currentSearchTask = new AtomicReference<>();
    private final AtomicReference<ScheduledFuture<?>> debounceTask = new AtomicReference<>();
    
    // Performance optimization service
    private final PerformanceOptimizationService performanceService = PerformanceOptimizationService.getInstance();
    
    // Dedicated executor for debouncing
    private final ScheduledExecutorService debounceExecutor = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
    
    // Search state
    private int currentPage = 0;
    private int totalPages = 1;
    private final int pageSize = 20;
    private Button loadMoreButton;
    
    // Debouncing configuration
    private static final long SEARCH_DEBOUNCE_DELAY = 500; // 500ms delay

    @FXML
    public void initialize() {
        log.debug("SearchController initialized");
        setupSearchField();
        setupPerformanceOptimization();
    }

    /**
     * Setup search field with proper event handling
     */
    private void setupSearchField() {
        // Add listener for text changes with debouncing
        contactSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                debounceSearch(newValue.trim());
            }
        });
    }

    /**
     * Setup performance optimization integration
     */
    private void setupPerformanceOptimization() {
        // Register this controller with the performance service
        performanceService.startPolling();
        
        // Setup periodic cache cleanup
        setupCacheCleanup();
    }
    
    /**
     * Setup periodic cache cleanup
     */
    private void setupCacheCleanup() {
        // Clean up expired cache entries every 10 minutes
        debounceExecutor.scheduleAtFixedRate(() -> {
            try {
                ApiSearchUsers.clearExpiredCache();
            } catch (Exception e) {
                log.error("Error during cache cleanup: {}", e.getMessage());
            }
        }, 10, 10, TimeUnit.MINUTES);
    }

    /**
     * Debounced search to prevent excessive API calls
     */
    private void debounceSearch(String query) {
        // Cancel previous debounce task
        ScheduledFuture<?> previousTask = debounceTask.getAndSet(null);
        if (previousTask != null && !previousTask.isCancelled()) {
            previousTask.cancel(false);
        }

        if (query.isEmpty()) {
            clearSearchResults();
            return;
        }

        // Schedule new search task
        ScheduledFuture<?> newTask = performanceService.schedulePollingTask(
            "search-debounce",
            () -> Platform.runLater(() -> performSearch(query)),
            0,
            SEARCH_DEBOUNCE_DELAY,
            TimeUnit.MILLISECONDS
        );
        
        debounceTask.set(newTask);
    }

    /**
     * Perform the actual search operation
     */
    private void performSearch(String query) {
        if (query.isEmpty()) {
            clearSearchResults();
            return;
        }

        // Cancel any ongoing search
        cancelCurrentSearch();

        // Show loading state
        showLoadingState();

        Long currentUserId = SessionManager.getInstance().getCurrentUser() != null
                ? SessionManager.getInstance().getCurrentUser().getId()
                : null;

        if (currentUserId == null) {
            showErrorState("User not logged in.");
            return;
        }

        // Check for duplicate search
        if (ApiSearchUsers.isDuplicateSearch(query, currentUserId)) {
            log.debug("Skipping duplicate search for query: {}", query);
            return;
        }

        // Reset pagination
        currentPage = 0;
        totalPages = 1;

        // Create and execute search task
        Task<ApiSearchUsers.PagedSearchResults> searchTask = ApiSearchUsers.searchUsers(query, currentUserId, currentPage, pageSize);
        
        searchTask.setOnSucceeded(e -> {
            try {
                ApiSearchUsers.PagedSearchResults pageResult = searchTask.getValue();
                if (pageResult != null) {
                    handleSearchSuccess(pageResult, query, currentUserId);
                } else {
                    showErrorState("No results found.");
                }
            } catch (Exception ex) {
                log.error("Error handling search results: {}", ex.getMessage(), ex);
                showErrorState("Error processing search results.");
            }
        });

        searchTask.setOnFailed(e -> {
            Throwable exception = searchTask.getException();
            log.error("Search failed: {}", exception.getMessage(), exception);
            showErrorState("Search failed. Please try again.");
        });

        searchTask.setOnCancelled(e -> {
            log.debug("Search was cancelled");
            hideLoadingState();
        });

        // Store the current task
        currentSearchTask.set(searchTask);

        // Execute the task using performance optimization service
        performanceService.executeBackgroundTask(() -> {
            searchTask.run();
        });
    }

    /**
     * Handle successful search results
     */
    private void handleSearchSuccess(ApiSearchUsers.PagedSearchResults pageResult, String query, Long currentUserId) {
        List<SearchUserResultDTO> users = pageResult.getResults();
        
        // Update search cache
        ApiSearchUsers.updateLastSearch(query, currentUserId);
        
        // Update state
        lastLoadedUsers.clear();
        if (users != null) {
            lastLoadedUsers.addAll(users);
        }
        
        currentPage = pageResult.getPage();
        totalPages = pageResult.getTotalPages();

        // Update UI on JavaFX thread
        Platform.runLater(() -> {
            hideLoadingState();
            searchResultsVBox.getChildren().clear();

            if (users == null || users.isEmpty()) {
                showEmptyState();
            } else {
                hideEmptyState();
                displaySearchResults(users);
                ensureLoadMoreButton(currentUserId, query);
            }
        });
    }

    /**
     * Display search results in the UI
     */
    private void displaySearchResults(List<SearchUserResultDTO> users) {
        for (SearchUserResultDTO user : users) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/system/chatdesktopapp/SearchPage/AddUserItem.fxml"));
                Node node = loader.load();
                SearchItemController controller = loader.getController();
                controller.setUserData(user);
                searchResultsVBox.getChildren().add(node);
            } catch (Exception ex) {
                log.error("Error loading search result item: {}", ex.getMessage(), ex);
            }
        }
    }

    /**
     * Show loading state
     */
    private void showLoadingState() {
        Platform.runLater(() -> {
            loadingIndicator.setVisible(true);
            loadingIndicator.setManaged(true);
            searchResultsVBox.getChildren().setAll(loadingIndicator);
            emptyState.setVisible(false);
            emptyState.setManaged(false);
        });
    }

    /**
     * Hide loading state
     */
    private void hideLoadingState() {
        Platform.runLater(() -> {
            loadingIndicator.setVisible(false);
            loadingIndicator.setManaged(false);
        });
    }

    /**
     * Show empty state
     */
    private void showEmptyState() {
        Platform.runLater(() -> {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            searchResultsVBox.getChildren().clear();
        });
    }

    /**
     * Hide empty state
     */
    private void hideEmptyState() {
        Platform.runLater(() -> {
            emptyState.setVisible(false);
            emptyState.setManaged(false);
        });
    }

    /**
     * Show error state
     */
    private void showErrorState(String message) {
        Platform.runLater(() -> {
            hideLoadingState();
            searchResultsVBox.getChildren().clear();
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            noResultsLabel.setText(message);
        });
    }

    /**
     * Clear search results
     */
    private void clearSearchResults() {
        Platform.runLater(() -> {
            searchResultsVBox.getChildren().clear();
            emptyState.setVisible(false);
            emptyState.setManaged(false);
            loadingIndicator.setVisible(false);
            loadingIndicator.setManaged(false);
        });
    }

    /**
     * Cancel current search task
     */
    private void cancelCurrentSearch() {
        Task<ApiSearchUsers.PagedSearchResults> currentTask = currentSearchTask.getAndSet(null);
        if (currentTask != null && !currentTask.isCancelled()) {
            currentTask.cancel(true);
        }
    }

    /**
     * Legacy method for backward compatibility
     */
    @FXML
    public void searchUsers(KeyEvent keyEvent) {
        // This method is now handled by the text property listener with debouncing
        // Keeping for backward compatibility but not implementing logic here
    }

    @FXML
    public void navigateBack(ActionEvent actionEvent) {
        // Cancel any ongoing operations
        cancelCurrentSearch();
        
        // Clear search cache
        ApiSearchUsers.clearSearchCache();
        
        // Navigate back
        NavigationUtil.switchScene(actionEvent, "/com/system/chatdesktopapp/MainChat/ChatApp.fxml", "chat 💌🎶");
    }

    /**
     * Ensure load more button is properly managed
     */
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

    /**
     * Load next page of search results
     */
    private void loadNextPage(Long currentUserId, String query) {
        if (currentPage >= totalPages - 1) {
            return;
        }

        int nextPage = currentPage + 1;
        loadMoreButton.setDisable(true);

        Task<ApiSearchUsers.PagedSearchResults> searchTask = ApiSearchUsers.searchUsers(query, currentUserId, nextPage, pageSize);
        
        searchTask.setOnSucceeded(e -> {
            try {
                ApiSearchUsers.PagedSearchResults pageResult = searchTask.getValue();
                if (pageResult != null) {
                    List<SearchUserResultDTO> users = pageResult.getResults();
                    if (users != null && !users.isEmpty()) {
                        lastLoadedUsers.addAll(users);
                        currentPage = pageResult.getPage();
                        totalPages = pageResult.getTotalPages();
                        
                        Platform.runLater(() -> {
                            displaySearchResults(users);
                            loadMoreButton.setDisable(false);
                            ensureLoadMoreButton(currentUserId, query);
                        });
                    }
                }
            } catch (Exception ex) {
                log.error("Error loading next page: {}", ex.getMessage(), ex);
                Platform.runLater(() -> loadMoreButton.setDisable(false));
            }
        });

        searchTask.setOnFailed(e -> {
            log.error("Failed to load next page: {}", searchTask.getException().getMessage());
            Platform.runLater(() -> loadMoreButton.setDisable(false));
        });

        // Execute using performance optimization service
        performanceService.executeBackgroundTask(() -> searchTask.run());
    }

    /**
     * Cleanup method to be called when controller is destroyed
     */
    public void cleanup() {
        cancelCurrentSearch();
        
        // Cancel debounce task
        ScheduledFuture<?> debounceTask = this.debounceTask.getAndSet(null);
        if (debounceTask != null && !debounceTask.isCancelled()) {
            debounceTask.cancel(false);
        }
        
        // Shutdown debounce executor
        debounceExecutor.shutdown();
        try {
            if (!debounceExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                debounceExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            debounceExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Clear search cache
        ApiSearchUsers.clearSearchCache();
        
        log.debug("SearchController cleanup completed");
    }
}
